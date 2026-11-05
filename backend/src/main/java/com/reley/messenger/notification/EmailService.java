package com.reley.messenger.notification;

import reactor.core.publisher.Mono;

public interface EmailService {

    Mono<Void> sendVerificationCode(String email, String code, String codeTtlText);

}
