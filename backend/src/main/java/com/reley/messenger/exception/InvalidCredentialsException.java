package com.reley.messenger.exception;

public class InvalidCredentialsException extends GlobalException {

    public InvalidCredentialsException() {
        super("Invalid password.", "login");
    }
}
