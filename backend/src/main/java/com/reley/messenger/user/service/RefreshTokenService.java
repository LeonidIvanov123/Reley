package com.reley.messenger.user.service;

import com.reley.messenger.user.dto.GeneratedRefreshToken;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

    private final SecureRandom secureRandom = new SecureRandom();

    public GeneratedRefreshToken generate() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        return new GeneratedRefreshToken(
                token,
                hash(token)
        );
    }

    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "RefreshTokenService.hash : SHA-256 algorithm is unavailable", e
            );
        }
    }
}