package com.deshark.core.cos;

import com.deshark.core.schemas.*;
import com.deshark.core.utils.FileCompressor;
import com.deshark.core.utils.HashCalculator;
import com.deshark.core.utils.ProgressTracker;
import com.deshark.core.utils.UploadProgressListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class COSFileManager {

    private static final Logger logger = LoggerFactory.getLogger(COSFileManager.class);

    private final String secretId;
    private final String secretKey;
    private final String bucketName;
    private final String region;
    private final String baseUrl;
    private COSClient cosClient;

    private final int THREAD_POOL_SIZE = 16;
    private ExecutorService uploadExecutor;

    public COSFileManager(String secretId, String secretKey, String region, String bucketName) {
        this.secretId = secretId;
        this.secretKey = secretKey;
        this.region = region;
        this.bucketName = bucketName;
        baseUrl = "https://" + bucketName + ".cos." + region + ".myqcloud.com";
        initCOSClient();
        initThreadPool();
    }

    private void initCOSClient() {
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        clientConfig.setMaxConnectionsCount(50);
        clientConfig.setConnectionTimeout(5000);
        clientConfig.setSocketTimeout(10000);
        this.cosClient = new COSClient(cred, clientConfig);
    }

    private void initThreadPool() {
        this.uploadExecutor = new ThreadPoolExecutor(
                THREAD_POOL_SIZE,
                THREAD_POOL_SIZE,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadFactory() {
                    private final AtomicInteger counter = new AtomicInteger(1);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "COSUpload-" + counter.getAndIncrement());
                        t.setDaemon(true);
                        return t;
                    }
                }
        );
    }

    public String generateCOSPath(String hash) {
        // 格式: x/x/xxxxxxxxxx/其余部分
        return hash.charAt(0) + "/" +
                hash.charAt(1) + "/" +
                hash.substring(2, 12) + "/" +
                hash.substring(12);
    }

    private void collectAllFiles(File directory, List<File> fileList) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    collectAllFiles(file, fileList);
                } else {
                    fileList.add(file);
                }
            }
        }
    }

    private boolean fileExists(String cosPath) {
        try {
            cosClient.getObjectMetadata(bucketName, cosPath);
            return true;
        } catch (CosServiceException e) {
            if (e.getStatusCode() == 404) {
                return false;
            } else {
                throw e;
            }
        }
    }

    private void uploadFiles(File baseDir, List<File> filesToUpload, List<ModpackFile> resultFiles,
                             ProgressTracker tracker)
            throws InterruptedException, ExecutionException {

        if (tracker != null) {
            tracker.startReporting();
        }
        List<Future<ModpackFile>> futures = new ArrayList<>();
        for (File file : filesToUpload) {
            String relativePath = getRelativePath(baseDir, file);

            Future<ModpackFile> future = uploadExecutor.submit(() -> {
                if (tracker != null) {
                    tracker.startUpload(relativePath);
                }
                try {
                    return uploadFile(file, relativePath);
                } finally {
                    if (tracker != null) {
                        tracker.completeUpload(relativePath);
                    }
                }
            });
            futures.add(future);
        }
        for (Future<ModpackFile> future : futures) {
            resultFiles.add(future.get());
        }

        if (tracker != null) {
            tracker.stopReporting();
        }
    }

    public ModpackFile uploadFile(File file, String relativePath)
            throws IOException, NoSuchAlgorithmException {

        String originalHash = HashCalculator.calculateFileHash(file);
        String cosPath = generateCOSPath(originalHash);

        // should file be compressed?
        boolean shouldCompress = FileCompressor.shouldCompressFile(file);

        // is file always exits?
        if (fileExists(cosPath)) {
            return createModpackFile(file, relativePath, originalHash, shouldCompress);
        }

        File fileToUpload = file;

        if (shouldCompress) {
            fileToUpload = FileCompressor.compressFile(file);
        }

        // upload
        try {
            PutObjectRequest putRequest = new PutObjectRequest(bucketName, cosPath, fileToUpload);

            // 设置元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(fileToUpload.length());
            metadata.setContentEncoding("application/json");
            putRequest.setMetadata(metadata);

            cosClient.putObject(putRequest);

            return createModpackFile(file, relativePath, originalHash, shouldCompress);
        } finally {
            // 清理临时压缩文件
            if (shouldCompress && !fileToUpload.equals(file)) {
                fileToUpload.delete();
            }
        }
    }

    private ModpackFile createModpackFile(File originalFile, String relativePath, String originalHash, boolean compressed) {
        ModpackFile entry = new ModpackFile();
        entry.setFile(relativePath);
        entry.setHash(originalHash);
        entry.setLink(baseUrl + "/" + generateCOSPath(originalHash));
        entry.setSize(originalFile.length());
        entry.setCompressed(compressed);
        return entry;
    }

    private String getRelativePath(File baseDir, File file) {
        String basePath = baseDir.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        String relativePath = filePath.substring(basePath.length() + 1);
        return relativePath.replace("\\", "/");
    }

    public String uploadJsonConfig(Object config, String cosPath) throws IOException {
        // 创建临时文件
        File tempFile = File.createTempFile("config", ".json");
        try {
            // 写入JSON数据
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(tempFile, config);

            tempFile = FileCompressor.compressFile(tempFile);

            // 上传到COS
            PutObjectRequest putRequest = new PutObjectRequest(bucketName, cosPath, tempFile);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("application/json");
            metadata.setContentLength(tempFile.length());
            putRequest.setMetadata(metadata);

            cosClient.putObject(putRequest);

            return baseUrl + "/" + cosPath;

        } finally {
            // 清理临时文件
            tempFile.delete();
        }
    }

    public VersionList createVersionsJson(String versionsPath, String versionName)
            throws IOException {

        VersionList versions;

        // 尝试获取现有的versions.json
        try {
            GetObjectRequest getRequest = new GetObjectRequest(bucketName, versionsPath);
            COSObject cosObject = cosClient.getObject(getRequest);

            // 创建临时文件保存压缩内容
            File tempFile = File.createTempFile("versions", ".json.gz");
            try (InputStream in = cosObject.getObjectContent();
                 FileOutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            // 解压缩文件
            File uncompressedFile = FileCompressor.decompressFile(tempFile);

            ObjectMapper mapper = new ObjectMapper();
            versions = mapper.readValue(uncompressedFile, VersionList.class);
            cosObject.close();

            // 删除临时文件
            tempFile.delete();
            uncompressedFile.delete();

        } catch (CosServiceException e) {
            if (e.getStatusCode() == 404) {
                // 文件不存在，创建新的
                versions = new VersionList();
                versions.setVersions(new ArrayList<>());
            } else {
                throw e;
            }
        }

        // 检查是否已存在相同版本
        boolean versionExists = versions.getVersions().stream()
                .anyMatch(v -> v.getVersionName().equals(versionName));

        if (versionExists) {
            return null;
        }

        return versions;
    }

    public String updateVersionsJson(VersionList versions, String versionsPath, String versionName,
                                     String modpackUrl, String changelogUrl, String versionDate)
            throws IOException {
        // 添加新版本
        VersionInfo newVersion = new VersionInfo();
        newVersion.setVersionName(versionName);
        newVersion.setVersionDate(versionDate);
        newVersion.setPackFilePath(modpackUrl);
        newVersion.setChangelogPath(changelogUrl);

        versions.getVersions().add(newVersion);

        // 上传更新后的versions.json
        return uploadJsonConfig(versions, versionsPath);
    }

    public String updateMetaJson(String projectId, String versionsUrl,
                                 VersionInfo latestVersion) throws IOException {

        String metaPath = "stable/" + projectId + "/meta.json";

        ModpackMeta meta = new ModpackMeta();
        meta.setVersionsPath(versionsUrl);
        meta.setLatestVersion(latestVersion);

        return uploadJsonConfig(meta, metaPath);
    }

    public void publishModpackVersion(File sourceDir, String projectId, String versionName,
                                      Map<String, String> libraries, UploadProgressListener listener)
            throws IOException, ExecutionException, InterruptedException {

        // check version
        String versionsPath = "stable/" + projectId + "/versions.json";
        VersionList versions = createVersionsJson(versionsPath, versionName);
        if (versions == null) {
            logger.warn("版本名“{}”重复，已停止上传", versionName);
            return;
        }

        String currentDate = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        List<File> allFiles = new ArrayList<>();
        collectAllFiles(sourceDir, allFiles);
        int totalFiles = allFiles.size();

        if (listener != null) {
            listener.onProgress(0, totalFiles, "准备上传...");
        }

        // init tracker
        ProgressTracker tracker = listener != null ? new ProgressTracker(totalFiles, listener) : null;

        // upload modpack files
        List<ModpackFile> files = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        uploadFiles(sourceDir, allFiles, files, tracker);

        // update metadata
        String modpackPath = "stable/" + projectId + "/versions/" + versionName + "/modpack.json";

        Modpack modpack = new Modpack();
        modpack.setVersion(versionName);
        modpack.setLibraries(libraries);
        modpack.setFiles(files);
        String modpackUrl = uploadJsonConfig(modpack, modpackPath);

        String changelogUrl = "stable/" + projectId + "/versions/" + versionName +"/changelog.json";

        String versionsUrl = updateVersionsJson(versions, versionsPath, versionName, modpackUrl, changelogUrl, currentDate);

        VersionInfo latestVersion = new VersionInfo();
        latestVersion.setVersionName(versionName);
        latestVersion.setVersionDate(currentDate);
        latestVersion.setPackFilePath(modpackUrl);
        latestVersion.setChangelogPath(changelogUrl);
        String metaUrl = updateMetaJson(projectId, versionsUrl, latestVersion);

        logger.info("发布完成! 文件数量: {} 个，耗时: {}ms", totalFiles, System.currentTimeMillis() - startTime);
        logger.info("meta.json url: {}", metaUrl);
    }

    public void shutdown() {
        if (uploadExecutor != null) {
            uploadExecutor.shutdown();
            try {
                if (!uploadExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    uploadExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                uploadExecutor.shutdownNow();
            }
        }

        if (cosClient != null) {
            cosClient.shutdown();
        }
    }
}
