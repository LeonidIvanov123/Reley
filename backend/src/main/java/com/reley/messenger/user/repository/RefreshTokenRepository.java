package com.reley.messenger.user.repository;

import com.reley.messenger.user.entity.RefreshToken;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends ReactiveCrudRepository<RefreshToken, Long> {

    Mono<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
        UPDATE refresh_tokens SET used_at = :usedAt
        WHERE id = :id AND used_at IS NULL AND revoked_at IS NULL
        """)
    Mono<Integer> markAsUsed(Long id, OffsetDateTime usedAt);

    @Modifying
    @Query("""
        UPDATE refresh_tokens SET revoked_at = :revokedAt
        WHERE session_id = :sessionId AND revoked_at IS NULL
        """)
    Mono<Integer> revokeAllBySessionId(UUID sessionId, OffsetDateTime revokedAt);

    @Query("""
    SELECT * FROM refresh_tokens
    WHERE token_hash = :tokenHash FOR UPDATE
    """)
    Mono<RefreshToken> findByTokenHashForUpdate(String tokenHash);

}
