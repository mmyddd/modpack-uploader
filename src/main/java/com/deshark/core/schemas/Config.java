package com.deshark.core.schemas;

public record Config(
        StorageConfig storage,
        String sourceDir,
        String sourceServerDir,
        String sourceClientDir,
        String versionName
) {
    public static Config empty() {
        return new Config(StorageConfig.empty(), "source", "source-server", "source-client", "1.0.0");
    }
}
