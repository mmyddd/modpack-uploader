package com.deshark.core.utils;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class HashCalculator {

    private static final int BUFFER_SIZE = 8192;

    public static String calculateFileHash(File file) throws IOException, NoSuchAlgorithmException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }

        if (!file.exists()) {
            throw new FileNotFoundException("File does not exist: " + file.getAbsolutePath());
        }

        if (!file.isFile()) {
            throw new IllegalArgumentException("Path is not a file: " + file.getAbsolutePath());
        }

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[BUFFER_SIZE];

        try (InputStream inputStream = new FileInputStream(file);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)) {

            int bytesRead;
            while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        byte[] hashBytes = digest.digest();
        return HexFormat.of().formatHex(hashBytes);
    }
}
