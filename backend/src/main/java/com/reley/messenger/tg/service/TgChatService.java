package com.reley.messenger.tg.service;

import com.reley.messenger.tg.dto.TgChatInfo;
import com.reley.messenger.tg.dto.TgDialogResponse;
import com.reley.messenger.tg.dto.TgMessageResponse;
import reactor.core.publisher.Mono;

import java.util.List;

public interface TgChatService {

    Mono<List<TgDialogResponse>> getAvailableDialogs(Long userId);

    Mono<List<TgDialogResponse>> getSelectedDialogs(Long userId);

    Mono<Void> saveSelectedDialogs(
            Long userId,
            List<Long> chatIds
    );

    Mono<List<Long>> getSelectedDialogIds(Long userId);

    Mono<List<TgMessageResponse>> getMessages(
            Long userId,
            Long chatId,
            int limit,
            Long fromMessageId
    );

    Mono<TgMessageResponse> sendMessage(Long userId, Long chatId, String text);

    Mono<TgChatInfo> getChatInfo(Long userId, Long chatId);

}
