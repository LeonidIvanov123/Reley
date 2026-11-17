package com.reley.messenger.user.controller;

import com.reley.messenger.user.dto.UserPresenceResponse;
import com.reley.messenger.user.repository.UserRepository;
import com.reley.messenger.websocket.ReleyWebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserPresenceController {

    private final UserRepository userRepository;
    private final ReleyWebSocketService webSocketService;

    @GetMapping("/{userId}/presence")
    public Mono<UserPresenceResponse> getPresence(@PathVariable Long userId) {

        return userRepository.findById(userId)
                .switchIfEmpty(
                        Mono.error(
                                new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден")
                        )
                )
                .map(user ->
                        new UserPresenceResponse(
                                user.getId(),
                                webSocketService.isOnline(user.getId()),
                                user.getLastSeenAt()
                        )
                );
    }
}