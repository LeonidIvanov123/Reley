package com.reley.messenger.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user_sessions")
public class UserSession {

    @Id
    private UUID id;

    @Column("user_id")
    private Long userId;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("last_used_at")
    private OffsetDateTime lastUsedAt;

    @Column("absolute_expires_at")
    private OffsetDateTime absoluteExpiresAt;

    @Column("revoked_at")
    private OffsetDateTime revokedAt;
}