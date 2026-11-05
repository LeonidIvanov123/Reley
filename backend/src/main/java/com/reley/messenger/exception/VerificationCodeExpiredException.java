package com.reley.messenger.exception;

import java.util.Map;

public class VerificationCodeExpiredException extends GlobalException {

    public VerificationCodeExpiredException(String email) {
        super(
                "Код подтверждения недействителен или истек",
                "register",
                Map.of("email", email, "codeSent", true)
        );
    }
}