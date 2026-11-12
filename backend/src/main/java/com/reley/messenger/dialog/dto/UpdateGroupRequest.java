package com.reley.messenger.dialog.dto;

public record UpdateGroupRequest(
        String name,
        boolean isPublic,
        String dialogLink
) {
}