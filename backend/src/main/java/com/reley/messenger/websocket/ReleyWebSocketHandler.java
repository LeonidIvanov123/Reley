package com.reley.messenger.websocket;

import com.reley.messenger.dialog.entity.DialogParticipant;
import com.reley.messenger.dialog.repository.DialogParticipantRepository;
import com.reley.messenger.user.service.UserService;
import com.reley.messenger.websocket.event.UserPresenceEvent;
import com.reley.messenger.websocket.event.WebSocketEvent;
import com.reley.messenger.websocket.event.WebSocketEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReleyWebSocketHandler implements WebSocketHandler {

    private final ReleyWebSocketService webSocketService;
    private final UserService userService;
    private final DialogParticipantRepository dialogParticipantRepository;

    @Override
    public Mono<Void> handle(WebSocketSession session) {

        return session.getHandshakeInfo()
                .getPrincipal()
                .flatMap(principal -> {

                    String name = principal.getName();

                    if (name == null || name.isBlank()) {

                        log.warn(
                                "WebSocket connection without user: session={}",
                                session.getId()
                        );

                        return session.close();
                    }

                    Long userId = Long.valueOf(name);

                    boolean wasOnline =
                            webSocketService.isOnline(userId);

                    webSocketService.addSession(
                            userId,
                            session
                    );

                    Mono<Void> onlineNotification =
                            wasOnline
                                    ? Mono.empty()
                                    : sendPresence(
                                    userId,
                                    WebSocketEventType.USER_ONLINE,
                                    null
                            );

                    return onlineNotification
                            .then(
                                    session.receive()
                                            .then()
                            )
                            .then(
                                    Mono.defer(() ->
                                            handleDisconnect(
                                                    userId,
                                                    session
                                            )
                                    )
                            );
                });
    }

    private Mono<Void> handleDisconnect(Long userId, WebSocketSession session) {

        webSocketService.removeSession(userId, session);

        /*
         * У пользователя могла закрыться одна из нескольких
         * WebSocket-сессий.
         *
         * Если осталась хотя бы одна — пользователь всё ещё online.
         */
        if (webSocketService.isOnline(userId)) {
            return Mono.empty();
        }

        OffsetDateTime lastSeenAt = OffsetDateTime.now();

        return userService
                .updateLastSeenAt(userId)
                .then(
                        sendPresence(
                                userId,
                                WebSocketEventType.USER_OFFLINE,
                                lastSeenAt
                        )
                )
                .doOnSuccess(ignored ->
                        log.info(
                                "User is now offline: user={}, lastSeenAt={}",
                                userId,
                                lastSeenAt
                        )
                )
                .doOnError(error ->
                        log.error(
                                "Failed to process user disconnect: user={}",
                                userId,
                                error
                        )
                );
    }

    private Mono<Void> sendPresence(
            Long userId,
            WebSocketEventType eventType,
            OffsetDateTime lastSeenAt
    ) {

        WebSocketEvent event =
                new WebSocketEvent(
                        eventType,
                        null,
                        new UserPresenceEvent(
                                userId,
                                lastSeenAt
                        )
                );

        return dialogParticipantRepository
                .findByUserId(userId)
                .map(DialogParticipant::getUserId)
                .filter(participantUserId ->
                        !Objects.equals(
                                participantUserId,
                                userId
                        )
                )
                .distinct()
                .flatMap(participantUserId ->
                        webSocketService.sendToUser(
                                participantUserId,
                                event
                        )
                )
                .then();
    }
}