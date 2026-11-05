package com.reley.messenger.user.registration.service;

import reactor.core.publisher.Mono;

public interface EmailRegistrationService {

    Mono<Void> sendVerificationCode(String email);

    Mono<Void> verifyCode(String email, String code);

    Mono<Void> setPassword(String email, String password);

}
