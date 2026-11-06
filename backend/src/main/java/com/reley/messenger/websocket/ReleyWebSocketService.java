package com.reley.messenger.websocket;

import com.reley.messenger.websocket.event.WebSocketEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ReleyWebSocketService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ConcurrentHashMap<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public void addSession(Long userId, WebSocketSession session) {
        sessions.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet())
                .add(session);

        log.info("WebSocket connected: user={}, session={}", userId, session.getId());
    }

    public void removeSession(Long userId, WebSocketSession session) {
        Set<WebSocketSession> userSessions = sessions.get(userId);

        if (userSessions == null) {
            return;
        }

        userSessions.remove(session);

        if (userSessions.isEmpty()) {
            sessions.remove(userId);
        }

        log.info("WebSocket disconnected: user={}, session={}", userId, session.getId());
    }

    public Mono<Void> sendToUser(Long userId, String message) {

        Set<WebSocketSession> userSessions = sessions.get(userId);

        if (userSessions == null || userSessions.isEmpty()) {
            return Mono.empty();
        }

        return Flux.fromIterable(userSessions)
                .filter(WebSocketSession::isOpen)
                .flatMap(session ->
                        session.send(
                                Mono.just(session.textMessage(message))
                        )
                )
                .then();
    }

    public Mono<Void> sendToUser(Long userId, WebSocketEvent event) {

        final String json = objectMapper.writeValueAsString(event);

        Set<WebSocketSession> userSessions = sessions.get(userId);

        if (userSessions == null || userSessions.isEmpty()) {
            return Mono.empty();
        }

        return Flux.fromIterable(userSessions)
                .filter(WebSocketSession::isOpen)
                .flatMap(session ->
                        session.send(
                                Mono.just(session.textMessage(json))
                        )
                )
                .then();
    }

}