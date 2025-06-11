
package com.deshark;

import com.deshark.core.ConfigManager;
import com.deshark.core.schemas.*;
import com.deshark.core.storage.CloudStorageProvider;
import com.deshark.core.storage.StorageProviderFactory;
import com.deshark.core.task.FileUploadTask;
import com.deshark.core.utils.FileUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static CloudStorageProvider storageProvider;

    private static final int maxConcurrency = 12;
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(maxConcurrency,
            new ThreadFactory() {
                private AtomicInteger counter = new AtomicInteger(0);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "FileUpload-" + counter.incrementAndGet());
                    t.setDaemon(false);
                    return t;
                }
            });
    private static final CopyOnWriteArrayList<ModpackFile> successFiles = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<ModpackFile> failedFiles = new CopyOnWriteArrayList<>();
    private static AtomicInteger completedTasks = new AtomicInteger(0);
    private static AtomicInteger successTasks = new AtomicInteger(0);

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

        File sourceDir = new File(sourceDirStr);

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
        MetaFile meta;
        try (InputStream is = storageProvider.getObjectStream(metaKey)) {
            if (is != null) {
                meta = mapper.readValue(is, MetaFile.class);
                String lastestVersionName = meta.latestVersion().versionName();
                logger.info("Lastest Version Name: {}", lastestVersionName);
            } else {
                logger.info("No meta data found");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Versions versions;
        try (InputStream is = storageProvider.getObjectStream(versionsKey)) {
            if (is != null) {
                versions = mapper.readValue(is, Versions.class);
                boolean duplicate = versions.versions().stream()
                        .anyMatch(v -> v.versionName().equals(versionName));
                if (duplicate) {
                    logger.info("Duplicate version name: {}", versionName);
                    return;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<File> fileList = new ArrayList<>();
        FileUtil.collectFiles(sourceDir, fileList);
        logger.info("Found {} files", fileList.size());

        long startTime = System.currentTimeMillis();

        List<Future<ModpackFile>> futures = new ArrayList<>();

        for (File file : fileList) {
            FileUploadTask task = new FileUploadTask(storageProvider, file, getRelativePath(file, sourceDir), downloadUrl);
            Future<ModpackFile> future = threadPool.submit(task);
            futures.add(future);
        }

        for (Future<ModpackFile> future : futures) {
            try {
                ModpackFile file = future.get();
                successFiles.add(file);
                successTasks.incrementAndGet();
            } catch (ExecutionException | InterruptedException e) {
                logger.error(e.getMessage());
            }
            completedTasks.incrementAndGet();
        }

        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }

        long endTime = System.currentTimeMillis();
        logger.info("Files upload completed in {} ms", endTime - startTime);

        // meta files
        Modpack modpack = new Modpack(successFiles, versionName, libraries);

        uploadConfigFile(modpack, modpackKey);

        String currentDate = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String modpackUrl = downloadUrl + "/" + modpackKey;
        String changelogUrl = downloadUrl + "/" + "stable/" + projectId + "/versions/" + versionName + "/changelog.json";
        VersionInfo lastestVersion = new VersionInfo(
                versionName, currentDate, modpackUrl, changelogUrl
        );

        List<VersionInfo> newVersions;

        if (storageProvider.fileExists(versionsKey)) {
            InputStream versionsStream = storageProvider.getObjectStream(versionsKey);
            try {
                newVersions = mapper.readValue(versionsStream, Versions.class).versions();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            newVersions = new ArrayList<>();
        }

        newVersions.add(lastestVersion);
        versions = new Versions(newVersions);

        uploadConfigFile(versions, versionsKey);

        String VersionsUrl = downloadUrl + "/" + versionsKey;

        meta = new MetaFile(VersionsUrl, lastestVersion);

        uploadConfigFile(meta, metaKey);

        String metaUrl = downloadUrl + "/" + metaKey;
        logger.info("================");
        logger.info("发布完成! 耗时: {}s", (System.currentTimeMillis() - startTime) / 1000.0);
        logger.info("版本: {}", versionName);
        logger.info("meta.json url: {}", metaUrl);

        storageProvider.shutdown();
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

    private static String getRelativePath(File file, File baseDir) {
        String basePath = baseDir.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        String relativePath = filePath.substring(basePath.length() + 1);
        return relativePath.replace("\\", "/");
    }
}