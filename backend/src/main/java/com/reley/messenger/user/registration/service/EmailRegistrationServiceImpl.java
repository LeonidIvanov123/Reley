package com.reley.messenger.user.registration.service;

import com.reley.messenger.exception.EmailAlreadyRegisteredException;
import com.reley.messenger.exception.InvalidVerificationCodeException;
import com.reley.messenger.exception.PasswordAlreadyExistsException;
import com.reley.messenger.exception.VerificationCodeExpiredException;
import com.reley.messenger.notification.EmailService;
import com.reley.messenger.user.dto.CreateUserRequest;
import com.reley.messenger.user.entity.Credential;
import com.reley.messenger.user.registration.entity.EmailVerificationCode;
import com.reley.messenger.user.registration.repository.EmailVerificationCodeRepository;
import com.reley.messenger.user.repository.CredentialRepository;
import com.reley.messenger.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EmailRegistrationServiceImpl implements EmailRegistrationService {

    private static final Duration CODE_TTL = Duration.ofMinutes(120);

    private final EmailVerificationCodeRepository verificationCodeRepository;
    private final UserService userService;
    private final EmailService emailService;
    private final CredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public Mono<Void> sendVerificationCode(String email) {

        String normalizedEmail = email.trim().toLowerCase();

        return userService
                .findByEmail(normalizedEmail)
                .flatMap(user ->
                        Mono.<Void>error(new EmailAlreadyRegisteredException(normalizedEmail)
                        )
                )
                .switchIfEmpty(
                        Mono.defer(() -> {

                            String code = generateCode();

                            OffsetDateTime now =
                                    OffsetDateTime.now();

                            EmailVerificationCode newCode =
                                    EmailVerificationCode.builder()
                                            .email(normalizedEmail)
                                            .codeHash(hashCode(code))
                                            .expiresAt(now.plus(CODE_TTL))
                                            .createdAt(now)
                                            .attempts(0)
                                            .build();

                            return verificationCodeRepository
                                    .findByEmail(normalizedEmail)
                                    .flatMap(existingCode -> {

                                        existingCode.setCodeHash(newCode.getCodeHash());
                                        existingCode.setExpiresAt(newCode.getExpiresAt());
                                        existingCode.setCreatedAt(newCode.getCreatedAt());
                                        existingCode.setUsedAt(null);
                                        existingCode.setAttempts(0);

                                        return verificationCodeRepository
                                                .save(existingCode);
                                    })
                                    .switchIfEmpty(
                                            verificationCodeRepository
                                                    .save(newCode)
                                    )
                                    .then(
                                            emailService.sendVerificationCode(
                                                    normalizedEmail, code,
                                                    "%s минут".formatted(CODE_TTL.toMinutes()))
                                    );
                        })
                );
    }

    @Override
    public Mono<Void> verifyCode(String email, String code) {
        String normalizedEmail =
                email.trim().toLowerCase();

        return verificationCodeRepository
                .findLatestValidCode(normalizedEmail)
                .switchIfEmpty(
                        Mono.error(new VerificationCodeExpiredException(normalizedEmail))
                )
                .flatMap(verificationCode -> {

                    if (verificationCode.getAttempts() >= 5) {
                        return Mono.error(new VerificationCodeExpiredException(normalizedEmail));
                    }

                    verificationCode.setAttempts(
                            verificationCode.getAttempts() + 1
                    );

                    if (!Objects.equals(
                            verificationCode.getCodeHash(),
                            hashCode(code)
                    )) {
                        return verificationCodeRepository
                                .save(verificationCode)
                                .then(Mono.error(new InvalidVerificationCodeException(normalizedEmail)));
                    }

                    verificationCode.setUsedAt(
                            OffsetDateTime.now()
                    );

                    return verificationCodeRepository
                            .save(verificationCode)
                            .then();
                });
    }

    @Override
    public Mono<Void> setPassword(String email, String password) {
        String normalizedEmail =
                email.trim().toLowerCase();

        return verificationCodeRepository
                .findLatestVerifiedCode(normalizedEmail)
                .switchIfEmpty(
                        Mono.error(
                                new VerificationCodeExpiredException(normalizedEmail)
                        )
                )
                .then(userService.findByEmail(normalizedEmail))
                .flatMap(user ->
                        credentialRepository
                                .findById(user.getId())
                                .flatMap(credential ->
                                        Mono.<Void>error(
                                                new PasswordAlreadyExistsException(
                                                        normalizedEmail
                                                )
                                        )
                                )
                                .switchIfEmpty(
                                        Mono.defer(() ->
                                                saveCredential(
                                                        user.getId(),
                                                        password
                                                )
                                        )
                                )
                )
                .switchIfEmpty(
                        Mono.defer(() ->
                                createUserAndCredential(
                                        normalizedEmail,
                                        password
                                )
                        )
                );
    }

    private Mono<Void> saveCredential(Long userId, String password) {
        OffsetDateTime now = OffsetDateTime.now();

        Credential credential = Credential.builder()
                .userId(userId)
                .passwordHash(
                        passwordEncoder.encode(password)
                )
                .createdAt(now)
                .updatedAt(now)
                .build();

        return credentialRepository
                .save(credential)
                .then();
    }

    private Mono<Void> createUserAndCredential(String email, String password) {
        return userService
                .createUser(new CreateUserRequest(email, password))
                .then();
    }

    private String generateCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private String hashCode(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(code.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}