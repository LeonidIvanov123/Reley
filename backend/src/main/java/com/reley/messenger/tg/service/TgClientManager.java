package com.reley.messenger.tg.service;

import it.tdlight.client.SimpleTelegramClient;
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
public class TgClientManager {

    private final Map<Long, SimpleTelegramClient> clients =
            new ConcurrentHashMap<>();

    public SimpleTelegramClient get(Long userId) {
        return clients.get(userId);
    }

    public void put(
            Long userId,
            SimpleTelegramClient client
    ) {
        clients.put(userId, client);
    }

    public SimpleTelegramClient remove(Long userId) {
        return clients.remove(userId);
    }
}