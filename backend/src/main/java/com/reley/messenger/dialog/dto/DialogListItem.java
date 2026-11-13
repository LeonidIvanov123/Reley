package com.reley.messenger.dialog.dto;

import com.reley.messenger.user.entity.User;

import java.time.OffsetDateTime;

public record DialogListItem(
        Long id,
        String dialogName,
        boolean groupChat,
        User otherParty,
        OffsetDateTime lastMessageAt,
        long unreadCount
) {
}