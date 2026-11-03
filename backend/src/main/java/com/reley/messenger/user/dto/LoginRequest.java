package com.reley.messenger.user.dto;

public record LoginRequest(
        String email,
        String password
) {}