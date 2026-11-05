package com.reley.messenger.exception;

import java.util.Map;

public class PasswordAlreadyExistsException extends GlobalException {

    public PasswordAlreadyExistsException(String email) {
        super(
                "Пароль для этого email уже установлен",
                "register",
                Map.of("email", email)
        );
    }
}