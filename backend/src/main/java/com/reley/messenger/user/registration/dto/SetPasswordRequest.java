package com.reley.messenger.user.registration.dto;

public record SetPasswordRequest(String email, String password, String passwordConfirmation) {
}
