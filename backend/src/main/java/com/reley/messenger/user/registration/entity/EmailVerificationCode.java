package com.reley.messenger.user.registration.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("email_verification_codes")
public class EmailVerificationCode {

    @Id
    private Long id;

    private String email;

    @Column("code_hash")
    private String codeHash;

    @Column("expires_at")
    private OffsetDateTime expiresAt;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("used_at")
    private OffsetDateTime usedAt;

    private Integer attempts;
}
