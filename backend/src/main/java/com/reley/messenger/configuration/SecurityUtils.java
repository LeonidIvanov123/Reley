package com.reley.messenger.configuration;

import org.springframework.http.ResponseCookie;

import java.time.Duration;

public class SecurityUtils {

    public static final String ACCESS_TOKEN_COOKIE = "RELEY_ACCESS_TOKEN";
    public static final String REFRESH_TOKEN_COOKIE = "RELEY_REFRESH_TOKEN";

    public static ResponseCookie deleteCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(false) // true в production
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    public static ResponseCookie createAccessTokenCookie(String token) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(false) // true в production
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMinutes(5))
                .build();
    }

    public static ResponseCookie createRefreshTokenCookie(String token) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(false) // true в production
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(5))
                .build();
    }
}
