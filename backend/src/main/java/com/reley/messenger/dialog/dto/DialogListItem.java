package com.reley.messenger.dialog.dto;

import java.time.OffsetDateTime;

public record DialogListItem(
        Long id,
        String dialogName,
        Long otherUserId,
        String otherUserName,
        String otherUserEmail,
        OffsetDateTime lastMessageAt
) {
}