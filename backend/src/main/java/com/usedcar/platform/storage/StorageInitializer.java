package com.usedcar.platform.storage;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * 应用启动后确保存储桶存在，避免首次上传才暴露桶不存在的问题。
 */
@Component
public class StorageInitializer {

    private final ObjectStorageService objectStorageService;

    public StorageInitializer(ObjectStorageService objectStorageService) {
        this.objectStorageService = objectStorageService;
    }

    @PostConstruct
    public void init() {
        objectStorageService.ensureBucket();
    }
}
