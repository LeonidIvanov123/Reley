package com.reley.messenger.exception;

import java.util.Map;

public class EmailNotVerifiedException extends GlobalException {

    public EmailNotVerifiedException(String email) {
        super(
                "Email не подтверждён или срок подтверждения истёк",
                "register",
                Map.of("email", email)
        );
    }
}