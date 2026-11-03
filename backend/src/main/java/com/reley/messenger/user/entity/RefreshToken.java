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
@Table("refresh_tokens")
public class RefreshToken {

    @Id
    private Long id;

    @Column("session_id")
    private UUID sessionId;

    @Column("token_hash")
    private String tokenHash;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("expires_at")
    private OffsetDateTime expiresAt;

    @Column("used_at")
    private OffsetDateTime usedAt;

    @Column("revoked_at")
    private OffsetDateTime revokedAt;
}