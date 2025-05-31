
package com.deshark;

import com.deshark.core.ConfigManager;
import com.deshark.core.cos.COSFileManager;
import com.deshark.core.utils.UploadProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        ConfigManager configManager = new ConfigManager();

        // 获取配置信息
        String secretId = configManager.get("secretId");
        String secretKey = configManager.get("secretKey");
        String region = configManager.get("region");
        String bucketName = configManager.get("bucketName");
        String sourceDirStr = configManager.get("sourceDir");

        String projectId = configManager.get("projectId");
        String versionName = configManager.get("versionName");

        File sourceDir = new File(sourceDirStr);

        // 设置依赖库
        Map<String, String> libraries = new HashMap<>();
        libraries.put("net.minecraft", "1.20.1");
        libraries.put("net.minecraftforge", "47.4.0");

        try {
            COSFileManager manager = new COSFileManager(
                    secretId,
                    secretKey,
                    region,
                    bucketName
            );

            manager.publishModpackVersion(
                    sourceDir,
                    projectId,
                    versionName,
                    libraries,
                    new UploadProgressListener() {
                        @Override
                        public void onProgress(int current, int total, String activeFiles) {
                            double percent = total > 0 ? (current * 100.0) / total : 0;
                            logger.info("进度: {}/{} ({}%) | 正在上传: {}",
                                    current, total, percent,
                                    activeFiles.isEmpty() ? "无" : activeFiles);
                        }

                        @Override
                        public void onComplete() {
                            logger.info("所有文件上传完成！");
                        }
                    }
            );

            manager.shutdown();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}