package com.reley.messenger.dialog.dto;

public record GroupParticipantResponse(
        Long userId,
        String name,
        String email,
        boolean admin
) {
}