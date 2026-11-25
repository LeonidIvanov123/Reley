package com.reley.messenger.tg.dto;

import java.util.List;

public record TgSelectedDialogsResponse(
        List<Long> chatIds
) {
}