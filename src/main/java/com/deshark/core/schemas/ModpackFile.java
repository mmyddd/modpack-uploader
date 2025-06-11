package com.deshark.core.schemas;

public record ModpackFile(
        String file,
        String hash,
        String link,
        long size,
        String dist,
        boolean compressed
) {}