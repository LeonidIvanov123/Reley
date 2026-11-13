package com.reley.messenger.message.dto;

import java.util.List;
import java.util.UUID;

public record SendMessageRequest(
        String content,
        List<UUID> attachmentIds
) {

    public List<UUID> attachmentIds() {
        return attachmentIds == null
                ? List.of()
                : attachmentIds;
    }

}