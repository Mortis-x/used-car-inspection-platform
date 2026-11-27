package com.usedcar.platform.attachment.dto;

import java.time.LocalDateTime;

public record AttachmentView(
        Long id,
        Long itemId,
        String fileName,
        String contentType,
        Long fileSize,
        String status,
        String originHash,
        String originUrl,
        String thumbUrl,
        String errorMsg,
        LocalDateTime createdAt
) {
}
