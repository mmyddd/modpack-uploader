package com.deshark.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigManager {
    private Map<String, String> config;
    private final File configFile = new File("mpu-config.json");
    private final ObjectMapper objectMapper;

    public ConfigManager() {
        this.objectMapper = new ObjectMapper();
        this.config = new LinkedHashMap<>();
        loadConfig();
    }

    private void loadConfig() {
        try {

            if (configFile.exists()) {
                config = objectMapper.readValue(configFile, new TypeReference<>() {});
            } else {
                config.put("secretId", "");
                config.put("secretKey", "");
                config.put("region", "");
                config.put("bucketName", "");
                config.put("sourceDir", "");
                config.put("baseUrl", "");
                config.put("projectId", "");
                config.put("versionName", "");
                saveConfig();
            }
        } catch (IOException e) {
            System.out.println("Failed to load config: " + e.getMessage());
        }
    }

    public void saveConfig() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(configFile, config);
        } catch (IOException e) {
            System.err.println("保存配置文件失败: " + e.getMessage());
        }
    }

    public String get(String key) {
        return config.getOrDefault(key, "");
    }

    public void set(String key, String value) {
        config.put(key, value);
    }
}