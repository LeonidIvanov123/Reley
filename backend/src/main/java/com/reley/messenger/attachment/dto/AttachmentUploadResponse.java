package com.reley.messenger.attachment.dto;

import java.util.UUID;

public record AttachmentUploadResponse(
        UUID id,
        String fileName,
        String extension,
        String contentType,
        long size
) {
}
