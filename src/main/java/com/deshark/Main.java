package com.deshark;

import com.deshark.core.ConfigManager;
import com.deshark.core.schemas.*;
import com.deshark.core.storage.CloudStorageProvider;
import com.deshark.core.storage.StorageProviderFactory;
import com.deshark.core.task.ModpackFileUploadTask;
import com.deshark.core.utils.FileUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static CloudStorageProvider storageProvider;

    public static void main(String[] args) {

        ConfigManager configManager = new ConfigManager();

        // fetch config
        String secretId = configManager.getSecretId();
        String secretKey = configManager.getSecretKey();
        String region = configManager.getRegion();
        String bucketName = configManager.getBucketName();
        String downloadUrl = configManager.getDownloadUrl();
        String sourceDirStr = configManager.getSourceDir();
        String projectId = configManager.getProjectId();
        String versionName = configManager.getVersionName();

        Path sourceDir = Paths.get(sourceDirStr);

        // set libraries(whats the use)
        Map<String, String> libraries = new HashMap<>();
        libraries.put("net.minecraft", "1.7.10");
        libraries.put("net.minecraftforge", "10.13.4.1614");

        storageProvider = StorageProviderFactory.createProvider(
                StorageProviderFactory.StorageType.TENCENT_COS,
                secretId, secretKey, region, bucketName
        );

        String modpackKey = "stable/" + projectId + "/versions/" + versionName + "/modpack.json";
        String versionsKey = "stable/" + projectId + "/versions.json";
        String metaKey = "stable/" + projectId + "/meta.json";

        // check version
        try {
            checkExistingVersions(metaKey, versionsKey, versionName);
        } catch (IOException e) {
            logger.error("Version check failed", e);
            return;
        }

        List<Path> fileList;
        try {
            fileList = FileUtil.collectFiles(sourceDir);
        } catch (IOException e) {
            logger.error("Failed to collect files", e);
            return;
        }
        logger.info("Found {} files", fileList.size());

        long startTime = System.currentTimeMillis();
        List<ModpackFile> results;
        try (ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2)) {
            List<CompletableFuture<ModpackFile>> futures = fileList.stream()
                    .map(file -> new ModpackFileUploadTask(storageProvider, file, getRelativePath(file, sourceDir), downloadUrl, 3)
                            .executeAsync(executor))
                    .toList();
            results = new ArrayList<>();
            for (CompletableFuture<ModpackFile> future : futures) {
                try {
                    results.add(future.get());
                } catch (ExecutionException | InterruptedException e) {
                    logger.error("File upload failed", e);
                    return;
                }
            }
        }

        long endTime = System.currentTimeMillis();
        logger.info("Files upload completed in {} ms", endTime - startTime);

        // meta files
        Modpack modpack = new Modpack(results, versionName, libraries);

        uploadConfigFile(modpack, modpackKey);

        String currentDate = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String modpackUrl = downloadUrl + "/" + modpackKey;
        String changelogUrl = downloadUrl + "/" + "stable/" + projectId + "/versions/" + versionName + "/changelog.json";
        VersionInfo lastestVersion = new VersionInfo(
                versionName, currentDate, modpackUrl, changelogUrl
        );

        List<VersionInfo> newVersions = getExistingVersions(versionsKey);
        newVersions.add(lastestVersion);
        Versions versions = new Versions(newVersions);

        uploadConfigFile(versions, versionsKey);

        String VersionsUrl = downloadUrl + "/" + versionsKey;

        MetaFile meta = new MetaFile(VersionsUrl, lastestVersion);

        uploadConfigFile(meta, metaKey);

        String metaUrl = downloadUrl + "/" + metaKey;
        logger.info("================");
        logger.info("发布完成! 耗时: {}s", (System.currentTimeMillis() - startTime) / 1000.0);
        logger.info("版本: {}", versionName);
        logger.info("meta.json: {}", metaUrl);

        storageProvider.shutdown();
    }

    private static void checkExistingVersions(String metaKey, String versionsKey, String versionName) throws IOException {
        try (InputStream is = storageProvider.getObjectStream(metaKey)) {
            if (is != null) {
                MetaFile meta = mapper.readValue(is, MetaFile.class);
                String latestVersionName = meta.latestVersion().versionName();
                logger.info("Latest Version Name: {}", latestVersionName);
            } else {
                logger.info("No meta data found");
            }
        }
        try (InputStream is = storageProvider.getObjectStream(versionsKey)) {
            if (is != null) {
                Versions versions = mapper.readValue(is, Versions.class);
                boolean duplicate = versions.versions().stream()
                        .anyMatch(v -> v.versionName().equals(versionName));
                if (duplicate) {
                    logger.error("Duplicate version name: {}", versionName);
                    throw new IOException("Duplicate version name");
                }
            }
        }
    }

    private static List<VersionInfo> getExistingVersions(String versionsKey) {
        if (storageProvider.fileExists(versionsKey)) {
            try (InputStream versionsStream = storageProvider.getObjectStream(versionsKey)) {
                return mapper.readValue(versionsStream, Versions.class).versions();
            } catch (IOException e) {
                throw new RuntimeException("Failed to read existing versions", e);
            }
        }
        return new ArrayList<>();
    }

    private static <T> void uploadConfigFile(T config, String key) {
        try {
            String configJson = mapper.writeValueAsString(config);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DeflaterOutputStream dos = new DeflaterOutputStream(baos, new Deflater(Deflater.BEST_COMPRESSION));
            dos.write(configJson.getBytes(StandardCharsets.UTF_8));
            dos.finish();
            dos.close();

            byte[] configBytes = baos.toByteArray();

            InputStream is = new ByteArrayInputStream(configBytes);
            storageProvider.upload(is, key, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getRelativePath(Path file, Path baseDir) {
        return baseDir.relativize(file).toString().replace("\\", "/");
    }
}