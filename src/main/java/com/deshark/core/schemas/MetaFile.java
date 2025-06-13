package com.deshark.core.schemas;

public record MetaFile(
        String versionsPath,
        VersionInfo latestVersion
) {}
