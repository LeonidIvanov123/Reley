package com.reley.messenger.exception;

public class UserNotFoundException extends GlobalException {

    public UserNotFoundException(String email) {
        super(
                "User not found: " + email,
                "index"
        );
    }
}