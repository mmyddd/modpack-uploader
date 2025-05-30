package com.deshark.core.schemas;

public class ModpackMeta {
    private String versionsPath;
    private VersionInfo latestVersion;

    public ModpackMeta() {
    }

    public ModpackMeta(String versionsPath, VersionInfo latestVersion) {
        this.versionsPath = versionsPath;
        this.latestVersion = latestVersion;
    }

    public String getVersionsPath() {
        return versionsPath;
    }

    public void setVersionsPath(String versionsPath) {
        this.versionsPath = versionsPath;
    }

    public VersionInfo getLatestVersion() {
        return latestVersion;
    }

    public void setLatestVersion(VersionInfo latestVersionInfo) {
        this.latestVersion = latestVersionInfo;
    }
}
