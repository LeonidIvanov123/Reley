package com.reley.messenger.search.dto;

public record SearchDialogResponse(
        Long dialogId,
        String dialogName,
        boolean groupChat
) {
}