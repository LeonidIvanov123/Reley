package com.reley.messenger.search.dto;

public record SearchUserResponse(
        Long userId,
        String name,
        String email
) {
}
