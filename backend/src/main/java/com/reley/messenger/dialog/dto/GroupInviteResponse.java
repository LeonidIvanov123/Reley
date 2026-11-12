package com.reley.messenger.dialog.dto;

import java.util.List;

public record GroupInviteResponse(
        Long id,
        String link,
        String name,
        boolean isPublic,
        List<GroupParticipantResponse> participants
) {
}