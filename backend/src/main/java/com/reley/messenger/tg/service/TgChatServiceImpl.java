package com.reley.messenger.tg.service;

import com.reley.messenger.tg.dto.TgChatInfo;
import com.reley.messenger.tg.dto.TgChatSendStatus;
import com.reley.messenger.tg.dto.TgDialogResponse;
import com.reley.messenger.tg.dto.TgMessageResponse;
import com.reley.messenger.tg.entity.TgDialog;
import com.reley.messenger.tg.repository.TgDialogRepository;
import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.jni.TdApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
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
    private final TgClientManager clientManager;

    @Override
    public Mono<List<TgDialogResponse>> getAvailableDialogs(Long userId) {

        return tgAuthService.getOrRestoreClient(userId)
                .flatMapMany(client ->

                        Mono.fromFuture(
                                        client.send(new TdApi.GetMe())
                                )
                                .flatMapMany(me ->

                                        Mono.fromFuture(
                                                        client.send(
                                                                new TdApi.GetChats(
                                                                        new TdApi.ChatListMain(),
                                                                        100
                                                                )
                                                        )
                                                )
                                                .flatMapMany(chats -> {

                                                    Flux<Long> chatIds =
                                                            Flux.fromStream(
                                                                            Arrays.stream(chats.chatIds)
                                                                                    .boxed()
                                                                    )
                                                                    .concatWith(
                                                                            Mono.just((long) me.id)
                                                                    )
                                                                    .distinct();

                                                    return chatIds.concatMap(chatId ->
                                                            Mono.fromFuture(
                                                                    client.send(
                                                                            new TdApi.GetChat(chatId)
                                                                    )
                                                            )
                                                    );
                                                })
                                )
                )
                .filter(chat -> {

                    boolean archived =
                            chat.positions != null
                                    && Arrays.stream(chat.positions)
                                    .anyMatch(position ->
                                            position.list instanceof TdApi.ChatListArchive
                                    );

                    if (archived) {
                        log.debug(
                                "[TG DATA] Skipping archived chat. userId={}, chatId={}, title={}",
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

    @Override
    public Mono<List<TgMessageResponse>> getMessages(
            Long userId,
            Long chatId,
            int limit,
            Long fromMessageId
    ) {
        SimpleTelegramClient client =
                clientManager.get(userId);

        if (client == null) {
            return Mono.error(
                    new IllegalStateException(
                            "Telegram client is not connected"
                    )
            );
        }

        int actualLimit = Math.min(Math.max(limit, 1), 100);

        long fromId =
                fromMessageId == null
                        ? 0
                        : fromMessageId;

        TdApi.GetChatHistory request =
                new TdApi.GetChatHistory(
                        chatId,
                        fromId,
                        0,
                        actualLimit,
                        false
                );

        return Mono.fromFuture(client.send(request))
                .flatMap(messages -> {

                    int count = messages.messages.length;

                    if (fromId != 0 || count != 1) {
                        return Mono.just(messages);
                    }

                    return Mono.delay(Duration.ofMillis(500))
                            .then(Mono.fromFuture(
                                    client.send(request)
                            ));
                })
                .map(messages ->
                        Arrays.stream(messages.messages)
                                .map(TgCodeUtil::toMessageResponse)
                                .toList()
                );
    }

    @Override
    public Mono<TgMessageResponse> sendMessage(
            Long userId,
            Long chatId,
            String text
    ) {
        SimpleTelegramClient client = clientManager.get(userId);

        if (client == null) {
            return Mono.error(
                    new IllegalStateException(
                            "Telegram client is not connected"
                    )
            );
        }

        if (text == null || text.isBlank()) {
            return Mono.error(
                    new IllegalArgumentException(
                            "Message text must not be empty"
                    )
            );
        }

        TdApi.InputMessageText inputMessageText =
                new TdApi.InputMessageText(
                        new TdApi.FormattedText(text, null),
                        null,
                        false
                );

        TdApi.SendMessage request =
                new TdApi.SendMessage(
                        chatId,
                        null,
                        null,
                        new TdApi.MessageSendOptions(),
                        null,
                        inputMessageText
                );

        return Mono.fromFuture(client.send(request))
                .map(TgCodeUtil::toMessageResponse);
    }

    @Override
    public Mono<TgChatInfo> getChatInfo(Long userId, Long chatId) {

        SimpleTelegramClient client = clientManager.get(userId);

        if (client == null) {
            return Mono.error(
                    new IllegalStateException("Telegram client is not connected")
            );
        }

        return Mono.fromFuture(
                client.send(new TdApi.GetChat(chatId))
        ).map(chat -> {

            TdApi.ChatType type = chat.type;

            if (type instanceof TdApi.ChatTypeSupergroup supergroup
                    && supergroup.isChannel) {

                return new TgChatInfo(
                        chat.id,
                        chat.title,
                        TgChatSendStatus.CHANNEL,
                        false
                );
            }

            boolean canSend =
                    chat.permissions != null
                            && chat.permissions.canSendBasicMessages;

            return new TgChatInfo(
                    chat.id,
                    chat.title,
                    canSend
                            ? TgChatSendStatus.ALLOWED
                            : TgChatSendStatus.READ_ONLY,
                    canSend
            );
        });
    }

}
