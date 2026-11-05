package com.reley.messenger.exception;

import java.util.Map;

public class InvalidVerificationCodeException extends GlobalException {

    public InvalidVerificationCodeException(String email) {
        super(
                "Неверный код подтверждения",
                "register",
                Map.of("email", email, "codeSent", true)
        );
    }
}