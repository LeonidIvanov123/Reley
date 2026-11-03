package com.reley.messenger.user.dto;

public record AuthenticationResponse(
        Long userId,
        String email,
        String accessToken,
        String refreshToken
) {
}
