package com.reley.messenger.tg.service;

import com.reley.messenger.tg.dto.TgDialogResponse;
import com.reley.messenger.tg.entity.TgDialog;
import com.reley.messenger.tg.repository.TgDialogRepository;
import it.tdlight.jni.TdApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "telegram",
        name = "api-enabled",
        havingValue = "true"
)
public class TgChatServiceImpl implements TgChatService {

    private final TgDialogRepository tgDialogRepository;
    private final TgAuthService tgAuthService;

    @Override
    public Mono<List<TgDialogResponse>> getAvailableDialogs(Long userId) {

        return tgAuthService.getOrRestoreClient(userId)
                .flatMapMany(client ->
                        Mono.fromFuture(
                                        client.send(
                                                new TdApi.GetChats(
                                                        new TdApi.ChatListMain(),
                                                        100
                                                )
                                        )
                                )
                                .flatMapMany(chats ->
                                        Flux.fromStream(
                                                        Arrays.stream(chats.chatIds)
                                                                .boxed()
                                                )
                                                .concatMap(chatId ->
                                                        Mono.fromFuture(
                                                                client.send(
                                                                        new TdApi.GetChat(chatId)
                                                                )
                                                        )
                                                )
                                )
                )
                .filter(chat -> {

                    boolean archived =
                            chat.positions != null
                                    && Arrays.stream(chat.positions)
                                    .anyMatch(
                                            position ->
                                                    position.list
                                                            instanceof TdApi.ChatListArchive
                                    );

                    if (archived) {
                        log.debug(
                                "[TG DATA] Skipping archived chat. " +
                                        "userId={}, chatId={}, title={}",
                                userId,
                                chat.id,
                                chat.title
                        );
                    }

                    return !archived;
                })
                .map(TgCodeUtil::toDialogResponse)
                .collectList();
    }

    @Override
    public Mono<List<TgDialogResponse>> getSelectedDialogs(Long userId) {

        return tgAuthService.getOrRestoreClient(userId)
                .flatMapMany(client ->
                        tgDialogRepository
                                .findAllByUserId(userId)
                                .map(TgDialog::getTelegramChatId)
                                .concatMap(chatId ->
                                        Mono.fromFuture(
                                                client.send(
                                                        new TdApi.GetChat(chatId)
                                                )
                                        )
                                )
                )
                .map(TgCodeUtil::toDialogResponse)
                .collectList();
    }

    @Override
    public Mono<List<Long>> getSelectedDialogIds(Long userId) {

        return tgDialogRepository
                .findAllByUserId(userId)
                .map(TgDialog::getTelegramChatId)
                .collectList();
    }

    @Override
    public Mono<Void> saveSelectedDialogs(
            Long userId,
            List<Long> chatIds
    ) {

        List<Long> selectedChatIds =
                chatIds == null
                        ? List.of()
                        : chatIds.stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .limit(50)
                        .toList();

        return tgDialogRepository
                .deleteAllByUserId(userId)
                .thenMany(
                        Flux.fromIterable(selectedChatIds)
                                .map(chatId ->
                                        TgDialog.builder()
                                                .userId(userId)
                                                .telegramChatId(chatId)
                                                .createdAt(OffsetDateTime.now())
                                                .updatedAt(OffsetDateTime.now())
                                                .build()
                                )
                                .as(tgDialogRepository::saveAll)
                )
                .then();
    }

    // =========================================================
    // TELEGRAM DIALOGS
    // =========================================================

    @Override
    public Mono<List<TgDialogResponse>> getDialogs(Long userId) {

        return tgAuthService.getOrRestoreClient(userId)

                .flatMapMany(client -> {

                    return Mono.fromFuture(
                                    client.send(
                                            new TdApi.GetChats(
                                                    new TdApi.ChatListMain(),
                                                    100
                                            )
                                    )
                            )
                            .flatMapMany(chats -> {

                                if (chats.chatIds == null
                                        || chats.chatIds.length == 0) {
                                    log.warn(
                                            "[TG DATA] GetChats returned no chatIds. userId={}",
                                            userId
                                    );

                                    return Flux.empty();
                                }

                                return Flux.fromStream(
                                                Arrays.stream(
                                                        chats.chatIds
                                                ).boxed()
                                        )

                                        .doOnNext(chatId ->
                                                log.debug(
                                                        "[TG DATA] Loading chat. " +
                                                                "userId={}, chatId={}",
                                                        userId,
                                                        chatId
                                                )
                                        )

                                        .concatMap(chatId ->
                                                Mono.fromFuture(
                                                                client.send(
                                                                        new TdApi.GetChat(
                                                                                chatId
                                                                        )
                                                                )
                                                        )

                                                        .doOnNext(chat ->
                                                                log.debug(
                                                                        "[TG DATA] GetChat completed. " +
                                                                                "userId={}, chatId={}, title={}",
                                                                        userId,
                                                                        chatId,
                                                                        chat.title
                                                                )
                                                        )

                                                        .doOnError(error ->
                                                                log.error(
                                                                        "[TG DATA] GetChat failed. " +
                                                                                "userId={}, chatId={}",
                                                                        userId,
                                                                        chatId,
                                                                        error
                                                                )
                                                        )
                                        );
                            });
                })

                .map(TgCodeUtil::toDialogResponse)
                .collectList()
                .doOnSuccess(dialogs ->
                        log.info(
                                "[TG DATA] getDialogs() completed successfully. " +
                                        "userId={}",
                                userId
                        )
                )
                .doOnError(error ->
                        log.error(
                                "[TG DATA] getDialogs() failed. " +
                                        "userId={}",
                                userId,
                                error
                        )
                );
    }

}
