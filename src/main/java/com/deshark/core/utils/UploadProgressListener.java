package com.deshark.core.utils;

public interface UploadProgressListener {
    void onProgress(int current, int total, String currentFileName);
    void onComplete();
}
