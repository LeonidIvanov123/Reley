package com.reley.messenger.dialog.service;

import com.reley.messenger.dialog.repository.DialogParticipantRepository;
import com.reley.messenger.dialog.repository.DialogRepository;
import com.reley.messenger.exception.ChatHaveNoAccess;
import com.reley.messenger.message.reaction.repository.MessageReactionRepository;
import com.reley.messenger.message.repository.MessageAttachmentRepository;
import com.reley.messenger.message.repository.MessageRepository;
import com.reley.messenger.user.service.HydrateUserInfoApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DialogDeletionService {

    private final HydrateUserInfoApi userServiceInfo;

    private final DialogRepository dialogRepository;
    private final DialogParticipantRepository participantRepository;

    private final MessageRepository messageRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final MessageReactionRepository messageReactionRepository;

    private final TransactionalOperator transactionalOperator;

    public Mono<Void> deleteGroup(Long dialogId) {

        return userServiceInfo.getCurrentUserId()
                .flatMap(userId ->
                        participantRepository
                                .findByDialogIdAndUserId(dialogId, userId)
                                .switchIfEmpty(
                                        Mono.error(
                                                new ChatHaveNoAccess(
                                                        "Нет доступа к групповому чату"
                                                )
                                        )
                                )
                                .flatMap(participant -> {

                                    if (!participant.isAdmin()) {
                                        return Mono.error(
                                                new ChatHaveNoAccess(
                                                        "Удалить группу может только администратор"
                                                )
                                        );
                                    }

                                    return deleteGroupTransactional(dialogId);
                                })
                );
    }

    private Mono<Void> deleteGroupTransactional(Long dialogId) {

        return dialogRepository
                .findGroupById(dialogId)
                .switchIfEmpty(
                        Mono.error(
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Группа не найдена"
                                )
                        )
                )

                // REACTIONS
                .then(
                        messageReactionRepository
                                .deleteByDialogId(dialogId)
                )

                // MESSAGE ATTACHMENTS
                .then(
                        messageAttachmentRepository
                                .deleteByDialogId(dialogId)
                )

                // CLEAR LAST READ MESSAGE
                .then(
                        participantRepository
                                .clearLastReadMessageId(dialogId)
                )

                // MESSAGES
                .then(
                        messageRepository
                                .deleteByDialogId(dialogId)
                )

                // PARTICIPANTS
                .then(
                        participantRepository
                                .deleteByDialogId(dialogId)
                )

                // DIALOG
                .then(
                        dialogRepository
                                .deleteGroup(dialogId)
                )

                .then()

                .as(transactionalOperator::transactional);
    }
}