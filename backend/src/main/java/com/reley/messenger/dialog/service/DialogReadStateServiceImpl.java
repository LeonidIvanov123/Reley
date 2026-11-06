package com.reley.messenger.dialog.service;

import com.reley.messenger.dialog.entity.DialogReadState;
import com.reley.messenger.dialog.repository.DialogReadStateRepository;
import com.reley.messenger.message.dto.MessageReadDomainEvent;
import com.reley.messenger.message.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class DialogReadStateServiceImpl implements DialogReadStateService {

    private final DialogReadStateRepository readStateRepository;
    private final MessageRepository messageRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Mono<Void> markAsRead(Long dialogId, Long userId) {

        return messageRepository
                .findLastIncomingMessage(dialogId, userId)
                .flatMap(lastIncomingMessage -> {

                    Long lastReadMessageId =
                            lastIncomingMessage.getId();

                    return readStateRepository
                            .findByDialogIdAndUserId(dialogId, userId)
                            .flatMap(state -> {

                                Long currentLastReadId =
                                        state.getLastReadMessageId();

                                if (currentLastReadId != null
                                        && currentLastReadId >= lastReadMessageId) {

                                    return Mono.empty();
                                }

                                return readStateRepository
                                        .upsertReadState(
                                                dialogId,
                                                userId,
                                                lastReadMessageId,
                                                OffsetDateTime.now()
                                        )
                                        .doOnSuccess(result ->
                                                eventPublisher.publishEvent(
                                                        new MessageReadDomainEvent(
                                                                dialogId,
                                                                userId,
                                                                lastReadMessageId
                                                        )
                                                )
                                        )
                                        .then();
                            })
                            .switchIfEmpty(
                                    readStateRepository
                                            .upsertReadState(
                                                    dialogId,
                                                    userId,
                                                    lastReadMessageId,
                                                    OffsetDateTime.now()
                                            )
                                            .doOnSuccess(result ->
                                                    eventPublisher.publishEvent(
                                                            new MessageReadDomainEvent(
                                                                    dialogId,
                                                                    userId,
                                                                    lastReadMessageId
                                                            )
                                                    )
                                            )
                                            .then()
                            );
                })
                .then();
    }

    @Override
    public Mono<Long> getUnreadCount(Long dialogId, Long userId) {
        return readStateRepository
                .countUnreadMessages(dialogId, userId);
    }

    @Override
    public Mono<Long> getLastReadMessageId(Long dialogId, Long userId) {
        return readStateRepository
                .findByDialogIdAndUserId(dialogId, userId)
                .map(DialogReadState::getLastReadMessageId)
                .defaultIfEmpty(0L);
    }
}