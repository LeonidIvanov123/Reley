package com.reley.messenger.user.dto;

public record UpdateProfileRequest(
        String email, String name, String phone, Boolean hiddenFromDirectory) {
}
