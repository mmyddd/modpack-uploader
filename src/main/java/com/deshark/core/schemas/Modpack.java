package com.deshark.core.schemas;

import java.util.List;
import java.util.Map;

public class Modpack {
    private List<ModpackFile> files;
    private String version;
    private Map<String, String> libraries;

    public Modpack() {
    }

    public Modpack(List<ModpackFile> files, String version, Map<String, String> libraries) {
        this.files = files;
        this.version = version;
        this.libraries = libraries;
    }

    public List<ModpackFile> getFiles() {
        return files;
    }

    public void setFiles(List<ModpackFile> files) {
        this.files = files;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, String> getLibraries() {
        return libraries;
    }

    public void setLibraries(Map<String, String> libraries) {
        this.libraries = libraries;
    }
}