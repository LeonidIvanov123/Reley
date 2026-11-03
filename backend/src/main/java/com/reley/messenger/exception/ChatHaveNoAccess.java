package com.reley.messenger.exception;

public class ChatHaveNoAccess extends GlobalException {
    public ChatHaveNoAccess(String cause) {
        super(
                "Проблема с диалогом (Cause: %s).".formatted(cause),
                "index"
        );
    }
}
