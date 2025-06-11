package com.deshark.core.schemas;

import java.util.List;
import java.util.Map;

public record Modpack(
        List<ModpackFile> files,
        String version,
        Map<String, String> libraries
) {
}