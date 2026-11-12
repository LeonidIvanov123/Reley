package com.reley.messenger.dialog.service;

import com.reley.messenger.dialog.dto.DialogListItem;
import com.reley.messenger.dialog.dto.DialogResponse;
import com.reley.messenger.dialog.dto.GroupInviteResponse;
import com.reley.messenger.dialog.dto.GroupSettingsResponse;
import com.reley.messenger.dialog.entity.Dialog;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DialogService {

    Mono<DialogResponse> createDialog(Long otherUserId);

    Mono<DialogResponse> createGroup(String name);

    Flux<DialogListItem> getMyDialogs();

    Mono<DialogResponse> createFavoritesDialog(Long userId);

    Mono<Dialog> findById(Long id);

    Mono<Void> addParticipant(Long dialogId, Long userId);

    Mono<Void> markAsRead(Long dialogId, Long userId);

    Mono<Long> getLastReadMessageId(Long dialogId, Long userId);

    Mono<Void> checkAccess(Long dialogId, Long userId);

    Mono<Void> updateGroup(Long dialogId, String name, boolean isPublic, String dialogLink);

    Mono<Void> removeParticipant(Long dialogId, Long userId);

    Mono<Void> makeAdmin(Long dialogId, Long userId);

    Mono<Void> removeAdmin(Long dialogId, Long userId);

    Mono<Void> leaveGroup(Long dialogId);

    Mono<GroupSettingsResponse> getGroupSettings(Long dialogId);

    Mono<GroupInviteResponse> getGroupByLink(String dialogLink);

    Mono<Long> joinGroup(String dialogLink);

}
