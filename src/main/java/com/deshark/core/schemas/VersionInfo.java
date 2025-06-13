package com.deshark.core.schemas;

public record VersionInfo(
        String versionName,
        String versionDate,
        String packFilePath,
        String changelogPath
) {}
