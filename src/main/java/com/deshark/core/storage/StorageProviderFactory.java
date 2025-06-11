package com.deshark.core.storage;

import com.deshark.core.storage.impl.TencentCOSProvider;

public class StorageProviderFactory {

    public enum StorageType {
        TENCENT_COS,
        ALIYUN_OSS
    }

    public static CloudStorageProvider createProvider(StorageType type, String... params) {
        switch (type) {
            case TENCENT_COS:
                return new TencentCOSProvider(params[0], params[1], params[2], params[3]);
            default:
                throw new IllegalArgumentException("不支持的存储类型: " + type);
        }
    }
}
