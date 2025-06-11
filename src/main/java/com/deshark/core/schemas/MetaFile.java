package com.deshark.core.schemas;

public record MetaFile(
        String versionsUrl,
        VersionInfo latestVersion
) {}
