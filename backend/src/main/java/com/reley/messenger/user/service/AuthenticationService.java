package com.reley.messenger.user.service;

import com.reley.messenger.user.dto.AuthenticationResponse;
import com.reley.messenger.user.dto.UserResponse;
import reactor.core.publisher.Mono;

public interface AuthenticationService {

    Mono<AuthenticationResponse> authenticate(String email, String password);

    Mono<AuthenticationResponse> refresh(String refreshToken);
}
