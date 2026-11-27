package com.reley.messenger.tg.dto;

public record TgAuthStatusResponse(
        boolean connected,
        String username,
        String firstName,
        String lastName
) {
}