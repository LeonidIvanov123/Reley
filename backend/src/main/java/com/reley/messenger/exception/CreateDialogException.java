package com.reley.messenger.exception;

public class CreateDialogException extends GlobalException {

    public CreateDialogException(String cause) {
        super("Не удалось создать диалог (Cause: %s).".formatted(cause), "dialog-new");
    }
}