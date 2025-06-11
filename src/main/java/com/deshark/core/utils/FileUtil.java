package com.deshark.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;

public class FileUtil {

    private final static Logger logger = LoggerFactory.getLogger(FileUtil.class);

    public static byte[] readIgnoreSpace(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return readIgnoreSpace(fis);
        } catch (IOException e) {
            throw e;
        }
    }

    public static byte[] readIgnoreSpace(InputStream i) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(16384);
        int byteRead;
        byte[] data = new byte[8192];

        try {
            while ((byteRead = i.read(data, 0, data.length)) != -1) {
                for (int j = 0; j < byteRead; j++) {
                    byte b = data[j];
                    if (!(b == 9 || b == 10 || b == 13 || b == 32)) {
                        bos.write(b);
                    }
                }
            }
        } catch (IOException e) {
            throw e;
        }

        return bos.toByteArray();
    }

    public static void collectFiles(File directory, List<File> fileList) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    collectFiles(file, fileList);
                } else {
                    fileList.add(file);
                }
            }
        }
    }
}
