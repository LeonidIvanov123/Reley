package com.reley.messenger.user.service;

import com.reley.messenger.exception.InvalidCredentialsException;
import com.reley.messenger.exception.InvalidRefreshTokenException;
import com.reley.messenger.user.dto.AuthenticationResponse;
import com.reley.messenger.user.repository.CredentialRepository;
import com.reley.messenger.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RefreshSessionService refreshSessionService;

    @Override
    public Mono<AuthenticationResponse> authenticate(String email, String password) {

        return userRepository.findByEmail(email)
                .flatMap(user ->
                        credentialRepository.findById(user.getId())
                                .flatMap(credentials -> {

                                    if (!passwordEncoder.matches(
                                            password,
                                            credentials.getPasswordHash()
                                    )) {
                                        return Mono.error(
                                                new InvalidCredentialsException()
                                        );
                                    }

                                    return refreshSessionService.createSession(user.getId())
                                            .flatMap(session -> {
                                                String token = tokenService.generateAccessToken(user);

                                                return Mono.just(
                                                        new AuthenticationResponse(
                                                                user.getId(),
                                                                user.getEmail(),
                                                                token,
                                                                session.refreshToken())
                                                );
                                            });
                                })
                )
                .switchIfEmpty(
                        Mono.error(new InvalidCredentialsException())
                );
    }

    public Mono<AuthenticationResponse> refresh(String refreshToken) {
        return refreshSessionService.rotate(refreshToken)
                .flatMap(rotatedToken ->
                        userRepository.findById(rotatedToken.userId())
                                .switchIfEmpty(Mono.error(
                                        new InvalidRefreshTokenException("Can't refresh token")
                                ))
                                .map(user -> new AuthenticationResponse(
                                        user.getId(),
                                        user.getEmail(),
                                        tokenService.generateAccessToken(user),
                                        rotatedToken.refreshToken()
                                ))
                );
    }

}
