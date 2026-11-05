package com.reley.messenger.user.dto;

public record CurrentUserInfo(
        Long userId,
        String email) {
}