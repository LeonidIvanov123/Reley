package com.reley.messenger.exception;

import lombok.Getter;

@Getter
public abstract class GlobalException extends RuntimeException {

    private final String description;
    private final String view;

    protected GlobalException(
            String description,
            String view
    ) {
        super(description);
        this.description = description;
        this.view = view;
    }
}