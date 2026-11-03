package com.reley.messenger.user.repository;

import com.reley.messenger.user.entity.UserSession;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends ReactiveCrudRepository<UserSession, UUID> {

    @Modifying
    @Query("""
        UPDATE user_sessions SET revoked_at = :revokedAt
        WHERE id = :sessionId AND revoked_at IS NULL
        """)
    Mono<Integer> revokeSession(UUID sessionId, OffsetDateTime revokedAt);

    @Query("SELECT * FROM user_sessions WHERE id = :sessionId")
    Mono<UserSession> findSession(UUID sessionId);

    @Modifying
    @Query("""
        UPDATE user_sessions SET last_used_at = :lastUsedAt
        WHERE id = :sessionId AND revoked_at IS NULL
        """)
    Mono<Integer> updateLastUsedAt(UUID sessionId, OffsetDateTime lastUsedAt);

}
