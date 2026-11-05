package com.reley.messenger.exception;

import java.util.Map;

public class PasswordNotMatchException extends GlobalException {

    public PasswordNotMatchException(String email) {
        super(
                "Пароли не совпадают",
                "register",
                Map.of(
                        "email", email,
                        "verified", true
                ));
    }
}
