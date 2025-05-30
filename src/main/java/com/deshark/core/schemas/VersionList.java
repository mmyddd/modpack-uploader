package com.deshark.core.schemas;

import java.util.List;

public class VersionList {
    private List<VersionInfo> versions;

    public VersionList() {
    }

    public VersionList(List<VersionInfo> versions) {
        this.versions = versions;
    }

    public List<VersionInfo> getVersions() {
        return versions;
    }

    public void setVersions(List<VersionInfo> versions) {
        this.versions = versions;
    }
}
