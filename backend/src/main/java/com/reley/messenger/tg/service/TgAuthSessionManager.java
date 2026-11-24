package com.reley.messenger.tg.service;

import com.reley.messenger.tg.dto.TgAuthSession;
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
public class TgAuthSessionManager {

    private final Map<Long, TgAuthSession> sessions = new ConcurrentHashMap<>();

    public TgAuthSession get(Long userId) {
        return sessions.get(userId);
    }

    public void put(
            Long userId,
            TgAuthSession session
    ) {
        sessions.put(userId, session);
    }

    public TgAuthSession remove(Long userId) {
        return sessions.remove(userId);
    }
}