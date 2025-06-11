package com.deshark.core.task;

import com.deshark.core.schemas.ModpackFile;
import com.deshark.core.storage.CloudStorageProvider;
import com.deshark.core.utils.FileCompressor;
import com.deshark.core.utils.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class FileUploadTask extends AbstractTask {

    private final Logger logger = LoggerFactory.getLogger(FileUploadTask.class);

    private final CloudStorageProvider storageProvider;
    private final File file;
    private final String relativePath;
    private final String downloadUrl;
    private ModpackFile result;

    public FileUploadTask(CloudStorageProvider storageProvider, File file, String relativePath, String downloadUrl) {
        this.storageProvider = storageProvider;
        this.file = file;
        this.relativePath = relativePath;
        this.downloadUrl = downloadUrl;
    }

    @Override
    public void execute() throws IOException {
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
        result = new ModpackFile(relativePath, hash, link, file.length(), null, shouldCompress);
        logger.debug("Task completed, upload result: {}", result);
    }

    public ModpackFile getResult() {
        return result;
    }

    private static String generateStorageKey(String fileHash) {
        return fileHash.charAt(0) + "/" +
                fileHash.substring(1, 3) + "/" +
                fileHash.substring(3, 7) + "/" +
                fileHash.substring(8);
    }


}
