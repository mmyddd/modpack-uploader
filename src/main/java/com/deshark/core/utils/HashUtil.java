package com.deshark.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class HashUtil {

    private static final Logger logger = LoggerFactory.getLogger(HashUtil.class);

    public static String SHA256(File file) {

        byte[] hashBytes = new byte[0];
        try {
            MessageDigest digest = getDigest();
            try {
                hashBytes = digest.digest(FileUtil.readIgnoreSpace(file));
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        } catch (NoSuchAlgorithmException e) {
            logger.error(e.getMessage());
        }

        return HexFormat.of().formatHex(hashBytes);
    }

    private static MessageDigest getDigest() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256");
    }
}
