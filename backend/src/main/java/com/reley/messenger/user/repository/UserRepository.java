package com.reley.messenger.user.repository;

import com.reley.messenger.user.dto.UserSenderProjection;
import com.reley.messenger.user.entity.User;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Repository
public interface UserRepository extends ReactiveCrudRepository<User, Long> {

    Mono<User> findByEmail(String email);

    Mono<Boolean> existsByEmail(String email);

    Mono<User> findByUsername(String username);

    Mono<Boolean> existsByUsername(String username);

    @Modifying
    @Query("""
        UPDATE users SET email = :email, name = :name, phone = :phone, username = :username, 
                hidden_from_directory = :hiddenFromDirectory, updated_at = :updatedAt
        WHERE id = :userId
        """)
    Mono<Integer> updateProfile(
            Long userId,
            String email,
            String name,
            String phone,
            String username,
            boolean hiddenFromDirectory,
            OffsetDateTime updatedAt
    );

    @Query("""
    SELECT *
    FROM users
    WHERE hidden_from_directory = false
      AND id <> :currentUserId
    ORDER BY id, email
    """)
    Flux<User> findAllVisibleUsers(Long currentUserId);

    @Query("""
        SELECT u.id,
               u.name,
               u.email
        FROM users u
        JOIN dialog_participants dp
          ON dp.user_id = u.id
        WHERE dp.dialog_id = :dialogId
        """)
    Flux<UserSenderProjection> findSendersByDialogId(Long dialogId);

    @Query("""
        UPDATE users
        SET last_seen_at = :lastSeenAt
        WHERE id = :userId
        """)
    Mono<Integer> updateLastSeenAt(Long userId, OffsetDateTime lastSeenAt);

    @Query("""
    SELECT *
    FROM users
    WHERE hidden_from_directory = false
      AND id <> :currentUserId
      AND (
          name ILIKE '%' || :query || '%'
          OR email ILIKE '%' || :query || '%'
          OR username ILIKE '%' || :query || '%'
      )
    ORDER BY name, email
    LIMIT :limit
    """)
    Flux<User> searchUsers(
            Long currentUserId,
            String query,
            int limit
    );

}
