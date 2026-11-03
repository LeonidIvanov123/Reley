package com.reley.messenger.exception;

public class InvalidRefreshTokenException extends GlobalException {
    public InvalidRefreshTokenException(String cause) {
        super(cause, "login");
    }
}
