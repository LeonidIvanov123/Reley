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

    @Modifying
    @Query("""
        UPDATE users SET email = :email, name = :name, phone = :phone, hidden_from_directory = :hiddenFromDirectory, updated_at = :updatedAt
        WHERE id = :userId
        """)
    Mono<Integer> updateProfile(
            Long userId,
            String email,
            String name,
            String phone,
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

}
