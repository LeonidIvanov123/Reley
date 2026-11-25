package com.reley.messenger.tg.dto;

import java.util.List;

public record TgSelectedDialogsRequest(
        List<Long> chatIds
) {
}