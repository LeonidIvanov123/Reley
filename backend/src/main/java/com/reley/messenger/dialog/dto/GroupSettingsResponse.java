package com.reley.messenger.dialog.dto;

import java.util.List;

public record GroupSettingsResponse(
        Long id,
        String name,
        boolean isPublic,
        boolean currentUserAdmin,
        List<GroupParticipantResponse> participants
) {
}
