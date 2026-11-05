package com.reley.messenger.user.registration.repository;

import com.reley.messenger.user.registration.entity.EmailVerificationCode;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface EmailVerificationCodeRepository extends ReactiveCrudRepository<EmailVerificationCode, Long> {

    @Query("""
        SELECT * FROM email_verification_codes
        WHERE email = :email AND used_at IS NULL AND expires_at > CURRENT_TIMESTAMP
        ORDER BY created_at DESC LIMIT 1
        """)
    Mono<EmailVerificationCode> findLatestValidCode(String email);

    @Query("""
        SELECT * FROM email_verification_codes
        WHERE email = :email AND used_at IS NOT NULL AND expires_at > CURRENT_TIMESTAMP
        ORDER BY used_at DESC LIMIT 1
        """)
    Mono<EmailVerificationCode> findLatestVerifiedCode(String email);

    Mono<EmailVerificationCode> findByEmail(String email);

}
