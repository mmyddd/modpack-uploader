package com.deshark.core;

import com.deshark.core.schemas.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final static Logger logger = LoggerFactory.getLogger(ConfigManager.class);

    private Config config;
    private final File configFile = new File("mup-config.json");
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public ConfigManager() {
        loadConfig();
    }

    private void loadConfig() {
        try {
            if (configFile.exists()) {
                config = objectMapper.readValue(configFile, Config.class);
            } else {
                config = Config.empty();
                saveConfig();
                logger.info("mpu-config.json created, Please fill in the configuration");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveConfig() {
        try {
            objectMapper.writeValue(configFile, config);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getSecretId() {
        return config.storage().secretId();
    }
    public String getSecretKey() {
        return config.storage().secretKey();
    }
    public String getRegion() {
        return config.storage().region();
    }
    public String getBucketName() {
        return config.storage().bucketName();
    }
    public String getDownloadUrl() {
        return config.storage().downloadUrl();
    }
    public String getProjectId() {
        return config.storage().projectId();
    }
    public String getSourceDir() {
        return config.sourceDir();
    }
    public String getVersionName() {
        return config.versionName();
    }
}