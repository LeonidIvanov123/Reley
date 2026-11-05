package com.reley.messenger.user.dto;

public record UserResponse(
        Long id,
        String email,
        String name,
        String phone,
        boolean hiddenFromDirectory,
        String avatar,
        String avatarContentType) {
}
