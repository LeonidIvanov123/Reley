package com.reley.messenger.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public abstract class GlobalException extends RuntimeException {

    private final String description;
    private final String view;
    private final Map<String, Object> attributes;

    protected GlobalException(
            String description,
            String view
    ) {
        super(description);
        this.description = description;
        this.view = view;
        this.attributes = Map.of();
    }

    public GlobalException(
            String description,
            String view,
            Map<String, Object> attributes
    ) {
        super(description);
        this.description = description;
        this.view = view;
        this.attributes = attributes;
    }
}