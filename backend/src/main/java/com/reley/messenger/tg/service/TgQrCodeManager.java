package com.reley.messenger.tg.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(
        prefix = "telegram",
        name = "api-enabled",
        havingValue = "true"
)
public class TgQrCodeManager {

    private final Map<Long, String> qrCodes =
            new ConcurrentHashMap<>();

    public String get(Long userId) {
        return qrCodes.get(userId);
    }

    public void put(
            Long userId,
            String qrCode
    ) {
        qrCodes.put(userId, qrCode);
    }

    public void remove(Long userId) {
        qrCodes.remove(userId);
    }
}