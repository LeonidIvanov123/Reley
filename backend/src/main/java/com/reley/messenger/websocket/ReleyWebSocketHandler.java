package com.reley.messenger.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReleyWebSocketHandler implements WebSocketHandler {

    private final ReleyWebSocketService webSocketService;

    @Override
    public Mono<Void> handle(WebSocketSession session) {

        return session.getHandshakeInfo()
                .getPrincipal()
                .flatMap(principal -> {

                    String name = principal.getName();

                    if (name == null || name.isBlank()) {
                        log.warn("WebSocket connection without user: session={}", session.getId());

                        return session.close();
                    }

                    Long userId = Long.valueOf(principal.getName());

                    webSocketService.addSession(userId, session);

                    return session.receive()
                            .then()
                            .doFinally(signal ->
                                    webSocketService.removeSession(userId, session)
                            );
                });
    }

}