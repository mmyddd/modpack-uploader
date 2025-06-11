package com.deshark.core.task;

import com.deshark.core.schemas.ModpackFile;
import com.deshark.core.storage.CloudStorageProvider;
import com.deshark.core.utils.FileCompressor;
import com.deshark.core.utils.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class FileUploadTask extends AbstractTask {

    private final Logger logger = LoggerFactory.getLogger(FileUploadTask.class);
    private final CloudStorageProvider storageProvider;

    private final File file;
    private final String relativePath;
    private final String downloadUrl;

    private AtomicInteger attemptCount;
    private static final int MAX_RETRY = 3;

    public FileUploadTask(CloudStorageProvider storageProvider, File file, String relativePath, String downloadUrl) {
        this.storageProvider = storageProvider;
        this.file = file;
        this.relativePath = relativePath;
        this.downloadUrl = downloadUrl;
        this.attemptCount = new AtomicInteger(0);
    }

    @Override
    public ModpackFile call() throws IOException {
        int attempt = attemptCount.incrementAndGet();

        try {
            String hash = HashUtil.SHA256(file);
            String key = generateStorageKey(hash);
            boolean shouldCompress = FileCompressor.shouldCompressFile(file);
            if (storageProvider.fileExists(key)) {
                logger.info("File {} already exists, skip upload.", file.getPath());
            } else {
                File fileToUpload = file;
                if (shouldCompress) {
                    fileToUpload = FileCompressor.compressFile(file);
                }

                storageProvider.upload(fileToUpload, key, shouldCompress);

                logger.info("File {} successfully uploaded.", file.getPath());
            }
            String link = downloadUrl + "/" + key;
            return new ModpackFile(relativePath, hash, link, file.length(), null, shouldCompress);
        } catch (IOException e) {
            if (attempt < MAX_RETRY) {
                logger.warn("Failed to upload file {}, retry...", file.getPath(), e);
                return call();
            } else {
                logger.error("Failed tp upload file {}, no more retry...", file.getPath(), e);
                throw e;
            }
        }
    }

    private static String generateStorageKey(String fileHash) {
        return fileHash.charAt(0) + "/" +
                fileHash.substring(1, 3) + "/" +
                fileHash.substring(3, 7) + "/" +
                fileHash.substring(8);
    }


}
