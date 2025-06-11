package com.deshark.core.schemas;


public record StorageConfig(
        String secretId,
        String secretKey,
        String region,
        String bucketName,
        String downloadUrl,
        String projectId
) {
    public static StorageConfig empty() {
        return new StorageConfig("", "", "", "", "", "");
    }
}