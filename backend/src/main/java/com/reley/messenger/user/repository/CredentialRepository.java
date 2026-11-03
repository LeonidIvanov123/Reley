package com.reley.messenger.user.repository;

import com.reley.messenger.user.entity.Credential;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Repository
public interface CredentialRepository extends ReactiveCrudRepository<Credential, Long> {

    @Query("""
            INSERT INTO credentials (user_id, password_hash, created_at, updated_at)
            VALUES (:userId, :passwordHash, :createdAt, :updatedAt)
            """)
    Mono<Void> insert(
            Long userId, String passwordHash, OffsetDateTime createdAt, OffsetDateTime updatedAt
    );
}
