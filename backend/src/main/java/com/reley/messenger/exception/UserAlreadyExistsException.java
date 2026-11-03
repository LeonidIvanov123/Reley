package com.reley.messenger.exception;

public class UserAlreadyExistsException extends GlobalException {

    public UserAlreadyExistsException(String email) {
        super("User with email already exists: " + email, "register");
    }
}
