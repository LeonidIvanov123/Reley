package com.reley.messenger.user.service;


import com.reley.messenger.exception.InvalidRefreshTokenException;
import com.reley.messenger.user.configuration.RefreshTokenProperties;
import com.reley.messenger.user.dto.CreatedSession;
import com.reley.messenger.user.dto.GeneratedRefreshToken;
import com.reley.messenger.user.dto.RotatedRefreshToken;
import com.reley.messenger.user.entity.RefreshToken;
import com.reley.messenger.user.entity.UserSession;
import com.reley.messenger.user.repository.RefreshTokenRepository;
import com.reley.messenger.user.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshSessionServiceImpl implements RefreshSessionService {

    private final R2dbcEntityTemplate entityTemplate;
    private final UserSessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenProperties properties;
    private final TransactionalOperator transactionalOperator;

    @Override
    public Mono<CreatedSession> createSession(Long userId) {
        OffsetDateTime now = OffsetDateTime.now();

        UserSession session = UserSession.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .createdAt(now)
                .absoluteExpiresAt(now.plus(properties.getAbsoluteSessionTtl()))
                .build();

        GeneratedRefreshToken generatedToken = refreshTokenService.generate();

        RefreshToken refreshToken = RefreshToken.builder()
                .sessionId(session.getId())
                .tokenHash(generatedToken.hash())
                .createdAt(now)
                .expiresAt(now.plus(properties.getTtl()))
                .build();

        return entityTemplate.insert(UserSession.class)
                .using(session)
                .then(refreshTokenRepository.save(refreshToken))
                .thenReturn(new CreatedSession(
                        session.getId(),
                        generatedToken.value()
                ))
                .as(transactionalOperator::transactional);
    }

    private record RotationOutcome(
            RotatedRefreshToken result,
            boolean tokenReuseDetected
    ) {}

    @Override
    public Mono<RotatedRefreshToken> rotate(String rawToken) {
        String tokenHash = refreshTokenService.hash(rawToken);

        return rotateInTransaction(tokenHash)
                .as(transactionalOperator::transactional)
                .flatMap(outcome -> {
                    if (outcome.tokenReuseDetected()) {
                        return Mono.error(new InvalidRefreshTokenException("rotate refresh token exception"));
                    }

                    return Mono.just(outcome.result());
                });
    }

    private Mono<RotationOutcome> rotateInTransaction(String tokenHash) {
        OffsetDateTime now = OffsetDateTime.now();

        return refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
                .switchIfEmpty(Mono.error(new InvalidRefreshTokenException("rotateInTransaction refresh token exception")))
                .flatMap(oldToken -> {
                    if (oldToken.getUsedAt() != null) {
                        return revokeSessionInternal(oldToken.getSessionId(), now)
                                .thenReturn(new RotationOutcome(null, true));
                    }

                    if (oldToken.getRevokedAt() != null
                            || !oldToken.getExpiresAt().isAfter(now)) {
                        return Mono.error(new InvalidRefreshTokenException("rotateInTransaction refresh token exception"));
                    }

                    return sessionRepository.findById(oldToken.getSessionId())
                            .switchIfEmpty(Mono.error(new InvalidRefreshTokenException("rotateInTransaction refresh token exception")))
                            .flatMap(session -> {
                                if (session.getRevokedAt() != null
                                        || !session.getAbsoluteExpiresAt().isAfter(now)) {
                                    return Mono.error(new InvalidRefreshTokenException("rotateInTransaction refresh token exception"));
                                }

                                return markOldTokenAsUsed(oldToken, now)
                                        .flatMap(ignored ->
                                                createRotatedToken(session, now)
                                        );
                            });
                });
    }

    private Mono<Integer> markOldTokenAsUsed(
            RefreshToken oldToken,
            OffsetDateTime now
    ) {
        return refreshTokenRepository.markAsUsed(oldToken.getId(), now)
                .flatMap(updated -> updated == 1
                        ? Mono.just(updated)
                        : Mono.error(new InvalidRefreshTokenException("markOldTokenAsUsed refresh token exception")));
    }

    private Mono<RotationOutcome> createRotatedToken(
            UserSession session,
            OffsetDateTime now
    ) {
        GeneratedRefreshToken generated = refreshTokenService.generate();

        OffsetDateTime expiresAt = now.plus(properties.getTtl());

        if (expiresAt.isAfter(session.getAbsoluteExpiresAt())) {
            expiresAt = session.getAbsoluteExpiresAt();
        }

        RefreshToken newToken = RefreshToken.builder()
                .sessionId(session.getId())
                .tokenHash(generated.hash())
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        return refreshTokenRepository.save(newToken)
                .then(sessionRepository.updateLastUsedAt(session.getId(), now))
                .thenReturn(new RotationOutcome(
                        new RotatedRefreshToken(
                                session.getUserId(),
                                session.getId(),
                                generated.value()
                        ),
                        false
                ));
    }

    @Override
    public Mono<Void> revokeSession(UUID sessionId) {
        return revokeSessionInternal(sessionId, OffsetDateTime.now())
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<Void> revokeByRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Mono.empty();
        }

        String tokenHash = refreshTokenService.hash(rawToken);

        return refreshTokenRepository.findByTokenHash(tokenHash)
                .flatMap(token -> revokeSession(token.getSessionId()));
    }

    private Mono<Void> revokeSessionInternal(UUID sessionId, OffsetDateTime now) {
        return sessionRepository.revokeSession(sessionId, now)
                .then(refreshTokenRepository.revokeAllBySessionId(sessionId, now))
                .then();
    }
}
