package com.reley.messenger.dialog.service;

import com.reley.messenger.dialog.entity.DialogReadState;
import com.reley.messenger.dialog.repository.DialogReadStateRepository;
import com.reley.messenger.message.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class DialogReadStateServiceImpl implements DialogReadStateService {

    private final DialogReadStateRepository readStateRepository;
    private final MessageRepository messageRepository;

    @Override
    public Mono<Void> markAsRead(Long dialogId, Long userId) {

        return messageRepository
                .findLastIncomingMessage(dialogId, userId)
                .flatMap(lastIncomingMessage ->

                        readStateRepository
                                .findByDialogIdAndUserId(dialogId, userId)
                                .flatMap(state -> {

                                    Long currentLastReadId =
                                            state.getLastReadMessageId();

                                    if (currentLastReadId != null
                                            && currentLastReadId >= lastIncomingMessage.getId()) {

                                        return Mono.just(state);
                                    }

                                    state.setLastReadMessageId(
                                            lastIncomingMessage.getId()
                                    );

                                    state.setUpdatedAt(
                                            OffsetDateTime.now()
                                    );

                                    return readStateRepository.save(state);
                                })
                                .switchIfEmpty(Mono.defer(() ->
                                        readStateRepository.save(
                                                DialogReadState.builder()
                                                        .dialogId(dialogId)
                                                        .userId(userId)
                                                        .lastReadMessageId(
                                                                lastIncomingMessage.getId()
                                                        )
                                                        .updatedAt(
                                                                OffsetDateTime.now()
                                                        )
                                                        .build()
                                        )
                                ))
                )
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