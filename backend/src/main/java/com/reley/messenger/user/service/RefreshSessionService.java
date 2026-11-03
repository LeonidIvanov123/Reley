package com.reley.messenger.user.service;

import com.reley.messenger.user.dto.CreatedSession;
import com.reley.messenger.user.dto.RotatedRefreshToken;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RefreshSessionService {

    Mono<CreatedSession> createSession(Long userId);

    Mono<RotatedRefreshToken> rotate(String refreshToken);

    Mono<Void> revokeSession(UUID sessionId);

    Mono<Void> revokeByRefreshToken(String refreshToken);

}