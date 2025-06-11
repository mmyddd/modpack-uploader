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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.zip.InflaterInputStream;

public class TencentCOSProvider implements CloudStorageProvider {

    private final String bucketName;
    private final COSClient cosClient;

    public TencentCOSProvider(String secretId, String secretKey, String region,
                              String bucketName) {
        this.bucketName = bucketName;

        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        clientConfig.setMaxConnectionsCount(50);
        clientConfig.setConnectionTimeout(5000);
        clientConfig.setSocketTimeout(10000);
        cosClient = new COSClient(cred, clientConfig);
    }

    @Override
    public void upload(File file, String path, boolean compressed) throws IOException {
        PutObjectRequest putRequest = new PutObjectRequest(bucketName, path, file);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.length());
        if (compressed) {
            metadata.setContentEncoding("deflate");
        }
        putRequest.setMetadata(metadata);
        upload(putRequest);
    }

    @Override
    public void upload(InputStream is, String key, boolean compressed) throws IOException {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(is.available());
        if (compressed) {
            metadata.setContentEncoding("deflate");
        }
        PutObjectRequest putRequest = new PutObjectRequest(bucketName, key, is, metadata);
        upload(putRequest);
    }

    @Override
    public boolean fileExists(String path) {
        try {
            cosClient.getObjectMetadata(bucketName, path);
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
        return getObjectStream(new GetObjectRequest(bucketName, key));
    }

    @Override
    public void shutdown() {
        cosClient.shutdown();
    }

    public void upload(PutObjectRequest putRequest) {
        cosClient.putObject(putRequest);
    }

    public InputStream getObjectStream(GetObjectRequest getRequest) {
        try {
            COSObject object = cosClient.getObject(getRequest);
            InputStream is = object.getObjectContent();

            if (Objects.equals(object.getObjectMetadata().getContentEncoding(), "deflate")) {
                return new InflaterInputStream(is);
            }
            return is;
        } catch (CosServiceException e) {
            if (e.getStatusCode() == 404) {
                return null;
            }
            throw e;
        }
    }
}
