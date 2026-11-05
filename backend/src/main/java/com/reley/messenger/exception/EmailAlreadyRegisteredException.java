package com.reley.messenger.exception;

import java.util.Map;

public class EmailAlreadyRegisteredException extends GlobalException {

    public EmailAlreadyRegisteredException(String email) {
        super(
                "Пользователь с таким email уже зарегистрирован",
                "register",
                Map.of("email", email)
        );
    }
}