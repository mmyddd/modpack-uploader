package com.deshark.core.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface CloudStorageProvider {

    void upload(Path file, String key, boolean compressed) throws IOException;

    void upload(InputStream is, String key, boolean compressed) throws IOException;

    boolean fileExists(String key);

    InputStream getObjectStream(String key);

    void shutdown();
}
