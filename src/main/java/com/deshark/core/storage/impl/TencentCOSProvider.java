package com.deshark.core.storage.impl;

import com.deshark.core.storage.CloudStorageProvider;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.zip.InflaterInputStream;

public class TencentCOSProvider implements CloudStorageProvider {
    private static final String DEFLATE_ENCODING = "deflate";
    private final String bucketName;
    private final COSClient cosClient;

    public TencentCOSProvider(String secretId, String secretKey, String region,
                              String bucketName) {
        Objects.requireNonNull(secretId, "Secret ID cannot be null");
        Objects.requireNonNull(secretKey, "Secret Key cannot be null");
        Objects.requireNonNull(region, "Region cannot be null");
        this.bucketName = Objects.requireNonNull(bucketName, "Bucket name cannot be null");

        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        clientConfig.setMaxConnectionsCount(50);
        clientConfig.setConnectionTimeout(5000);
        clientConfig.setSocketTimeout(10000);
        cosClient = new COSClient(cred, clientConfig);
    }

    @Override
    public void upload(Path file, String key, boolean compressed) throws IOException {
        Objects.requireNonNull(file, "File cannot be null");
        Objects.requireNonNull(key, "Key cannot be null");
        PutObjectRequest putRequest = new PutObjectRequest(bucketName, key, file.toFile());
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(Files.size(file));
        if (compressed) {
            metadata.setContentEncoding(DEFLATE_ENCODING);
        }
        putRequest.setMetadata(metadata);
        upload(putRequest);
    }

    @Override
    public void upload(InputStream is, String key, boolean compressed) throws IOException {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(is.available());
        if (compressed) {
            metadata.setContentEncoding(DEFLATE_ENCODING);
        }
        PutObjectRequest putRequest = new PutObjectRequest(bucketName, key, is, metadata);
        upload(putRequest);
    }

    @Override
    public boolean fileExists(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        try {
            cosClient.getObjectMetadata(bucketName, key);
            return true;
        } catch (CosServiceException e) {
            if (e.getStatusCode() == 404) {
                return false;
            } else {
                throw e;
            }
        }
    }

    @Override
    public InputStream getObjectStream(String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        return getObjectStream(new GetObjectRequest(bucketName, key));
    }

    @Override
    public void shutdown() {
        cosClient.shutdown();
    }

    public void upload(PutObjectRequest putRequest) {
        try {
            cosClient.putObject(putRequest);
        } catch (CosServiceException e) {
            throw new RuntimeException("Failed to upload file: " + putRequest.getKey(), e);
        }
    }

    public InputStream getObjectStream(GetObjectRequest getRequest) {
        try {
            COSObject object = cosClient.getObject(getRequest);
            InputStream is = object.getObjectContent();
            boolean isDeflated = DEFLATE_ENCODING.equals(object.getObjectMetadata().getContentEncoding());

            return new FilterInputStream(isDeflated ? new InflaterInputStream(is) : is) {
                @Override
                public void close() throws IOException {
                    try {
                        super.close();
                    } finally {
                        object.close();
                    }
                }
            };
        } catch (CosServiceException e) {
            if (e.getStatusCode() == 404) {
                return null;
            }
            throw new RuntimeException("Failed to get object: " + getRequest.getKey(), e);
        }
    }
}
