package com.deshark.core.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class FileUtil {
    private static final int BUFFER_SIZE = 8192;
    private static final int INITIAL_BUFFER_SIZE = 16384;
    private static final Set<String> COMPRESSIBLE_EXTENSIONS = Set.of(
            ".json", ".txt", ".xml", ".toml", ".js", ".cfg", ".properties");
    private static final Set<String> NON_COMPRESSIBLE_EXTENSIONS = Set.of(
            ".png", ".zip", ".jar", ".gz", ".7z", ".rar");

    private FileUtil() {}

    public static byte[] readIgnoreSpace(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            return readIgnoreSpace(is);
        }
    }

    public static byte[] readIgnoreSpace(InputStream i) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(INITIAL_BUFFER_SIZE);
        int byteRead;
        byte[] data = new byte[BUFFER_SIZE];
        while ((byteRead = i.read(data, 0, data.length)) != -1) {
            for (int j = 0; j < byteRead; j++) {
                byte b = data[j];
                if (!(b == 9 || b == 10 || b == 13 || b == 32)) {
                    bos.write(b);
                }
            }
        }
        return bos.toByteArray();
    }

    public static List<Path> collectFiles(Path directory) throws IOException {
        List<Path> fileList = new ArrayList<>();
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                fileList.add(file);
                return FileVisitResult.CONTINUE;
            }
        });
        return fileList;
    }

    public static Path compressFile(Path source) throws IOException {
        Path tempFile = Files.createTempFile("compressed_", ".tmp");
        try (InputStream is = Files.newInputStream(source);
             OutputStream os = Files.newOutputStream(tempFile);
             DeflaterOutputStream dos = new DeflaterOutputStream(os,
                     new Deflater(Deflater.BEST_COMPRESSION))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
        }
        return tempFile;
    }

    public static boolean shouldCompressFile(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        String ext = fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
        return !NON_COMPRESSIBLE_EXTENSIONS.contains(ext) &&
                COMPRESSIBLE_EXTENSIONS.contains(ext);
    }
}
