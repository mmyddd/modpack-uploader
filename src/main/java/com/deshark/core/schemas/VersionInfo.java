package com.deshark.core.schemas;

public class VersionInfo {
    private String versionName;
    private String versionDate;
    private String packFilePath;
    private String changelogPath;

    public VersionInfo() {
    }

    public VersionInfo(String versionName, String versionDate, String packFilePath, String changelogPath) {
        this.versionName = versionName;
        this.versionDate = versionDate;
        this.packFilePath = packFilePath;
        this.changelogPath = changelogPath;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getVersionDate() {
        return versionDate;
    }

    public void setVersionDate(String versionDate) {
        this.versionDate = versionDate;
    }

    public String getPackFilePath() {
        return packFilePath;
    }

    public void setPackFilePath(String packFilePath) {
        this.packFilePath = packFilePath;
    }

    public String getChangelogPath() {
        return changelogPath;
    }

    public void setChangelogPath(String changelogPath) {
        this.changelogPath = changelogPath;
    }
}
