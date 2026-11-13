package com.reley.messenger.attachment.dto;

import com.reley.messenger.attachment.entity.Attachment;

import java.util.UUID;

public record AttachmentResponse(
        UUID id,
        String fileName,
        String extension,
        String contentType,
        long size,
        String downloadUrl,
        Long createdBy
) {

    public static AttachmentResponse fromEntity(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getOriginalFileName(),
                attachment.getExtension(),
                attachment.getContentType(),
                attachment.getSize(),
                "/api/attachments/%s/download".formatted(attachment.getId()),
                attachment.getCreatedBy()
        );
    }
}