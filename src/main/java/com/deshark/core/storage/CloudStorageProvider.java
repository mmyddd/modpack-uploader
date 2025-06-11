package com.deshark.core.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public interface CloudStorageProvider {

    void upload(File file, String key, boolean compressed) throws IOException;

    void upload(InputStream is, String key, boolean compressed) throws IOException;

    boolean fileExists(String path);

    InputStream getObjectStream(String key);

    void shutdown();
}
