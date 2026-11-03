package com.reley.messenger.message.service;

import com.reley.messenger.message.dto.MessageResponse;
import com.reley.messenger.message.dto.SendMessageRequest;
import com.reley.messenger.message.entity.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

public interface MessageService {

    Flux<MessageResponse> getMessages(Long dialogId, Long beforeId);

    Mono<MessageResponse> sendMessage(Long dialogId, SendMessageRequest request);

    Mono<MessageResponse> deleteMessage(Long dialogId, Long messageId);

    Mono<OffsetDateTime> findLastMessageDateByDialogId(Long dialogId);
}