package com.deshark.core.schemas;

public class ModpackFile {
    private String file;
    private String hash;
    private String link;
    private long size;
    private String dist;
    private boolean compressed;

    public ModpackFile() {
    }

    public ModpackFile(String file, String hash, String link, long size, String dist, boolean compressed) {
        this.file = file;
        this.hash = hash;
        this.link = link;
        this.size = size;
        this.dist = dist;
        this.compressed = compressed;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getDist() {
        return dist;
    }

    public void setDist(String dist) {
        this.dist = dist;
    }

    public boolean isCompressed() {
        return compressed;
    }

    public void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }
}