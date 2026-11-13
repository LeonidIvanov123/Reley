package com.reley.messenger.message.service;

import com.reley.messenger.message.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class MessageReaderService {

    private final MessageRepository messageRepository;

    public Mono<OffsetDateTime> findLastMessageDateByDialogId(Long dialogId) {
        return messageRepository.findLastMessageDate(dialogId)
                .defaultIfEmpty(OffsetDateTime.parse("1970-01-01T00:00:00Z"));
    }

    public Mono<Long> countMessagesAfterId(
            Long dialogId,
            Long messageId
    ) {
        return messageRepository.countMessagesAfterId(
                dialogId,
                messageId
        );
    }

    public Mono<Long> findLastMessageId(Long dialogId) {
        return messageRepository.findLastMessageId(dialogId);
    }

}
