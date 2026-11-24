package com.reley.messenger.search.service;

import com.reley.messenger.dialog.entity.Dialog;
import com.reley.messenger.dialog.repository.DialogRepository;
import com.reley.messenger.message.entity.Message;
import com.reley.messenger.message.repository.MessageRepository;
import com.reley.messenger.search.dto.SearchDialogResponse;
import com.reley.messenger.search.dto.SearchMessageResponse;
import com.reley.messenger.search.dto.SearchResponse;
import com.reley.messenger.search.dto.SearchUserResponse;
import com.reley.messenger.user.entity.User;
import com.reley.messenger.user.repository.UserRepository;
import com.reley.messenger.user.service.HydrateUserInfoApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private static final int USERS_LIMIT = 5;
    private static final int DIALOGS_LIMIT = 5;
    private static final int MESSAGES_LIMIT = 10;

    private final HydrateUserInfoApi userServiceInfo;

    private final UserRepository userRepository;
    private final DialogRepository dialogRepository;
    private final MessageRepository messageRepository;

    @Override
    public Mono<SearchResponse> search(String query) {

        String normalizedQuery =
                query == null
                        ? ""
                        : query.trim();

        if (normalizedQuery.isBlank()) {
            return Mono.just(
                    new SearchResponse(
                            List.of(),
                            List.of(),
                            List.of()
                    )
            );
        }

        return userServiceInfo
                .getCurrentUser()
                .flatMap(user -> {

                    Long userId = user.userId();

                    Mono<List<SearchUserResponse>> users =
                            userRepository
                                    .searchUsers(userId, normalizedQuery, USERS_LIMIT)
                                    .map(this::toUserResponse)
                                    .collectList();

                    Mono<List<SearchDialogResponse>> dialogs =
                            dialogRepository
                                    .searchDialogs(userId, normalizedQuery, DIALOGS_LIMIT)
                                    .map(this::toDialogResponse)
                                    .collectList();

                    Mono<List<SearchMessageResponse>> messages =
                            searchMessages(userId, normalizedQuery);

                    return Mono.zip(
                            users,
                            dialogs,
                            messages
                    ).map(tuple ->
                            new SearchResponse(
                                    tuple.getT1(),
                                    tuple.getT2(),
                                    tuple.getT3()
                            )
                    );
                });
    }

    private Mono<List<SearchMessageResponse>> searchMessages(
            Long userId,
            String query
    ) {
        return messageRepository
                .searchForUser(
                        userId,
                        query,
                        MESSAGES_LIMIT
                )
                .collectList()
                .flatMap(messages -> {

                    if (messages.isEmpty()) {
                        return Mono.just(List.of());
                    }

                    List<Long> dialogIds =
                            messages.stream()
                                    .map(Message::getDialogId)
                                    .distinct()
                                    .toList();

                    List<Long> senderIds =
                            messages.stream()
                                    .map(Message::getSenderId)
                                    .distinct()
                                    .toList();

                    return Mono.zip(
                            dialogRepository
                                    .findAllById(dialogIds)
                                    .collectMap(
                                            Dialog::getId,
                                            Function.identity()
                                    ),
                            userRepository
                                    .findAllById(senderIds)
                                    .collectMap(
                                            User::getId,
                                            Function.identity()
                                    )
                    ).map(tuple -> {

                        Map<Long, Dialog> dialogs =
                                tuple.getT1();

                        Map<Long, User> users =
                                tuple.getT2();

                        return messages.stream()
                                .map(message -> {

                                    Dialog dialog =
                                            dialogs.get(message.getDialogId());

                                    User sender =
                                            users.get(message.getSenderId());

                                    return new SearchMessageResponse(
                                            message.getId(),
                                            message.getDialogId(),
                                            dialog != null
                                                    ? dialog.getDialogName()
                                                    : null,
                                            message.getSenderId(),
                                            sender != null
                                                    ? sender.getName()
                                                    : null,
                                            sender != null
                                                    ? sender.getEmail()
                                                    : null,
                                            message.getContent(),
                                            message.getCreatedAt()
                                    );
                                })
                                .toList();
                    });
                });
    }

    private SearchUserResponse toUserResponse(User user) {
        return new SearchUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }

    private SearchDialogResponse toDialogResponse(Dialog dialog) {
        return new SearchDialogResponse(
                dialog.getId(),
                dialog.getDialogName(),
                dialog.isGroupChat()
        );
    }
}