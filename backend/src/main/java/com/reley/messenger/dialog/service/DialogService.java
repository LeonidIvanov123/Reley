package com.reley.messenger.dialog.service;

import com.reley.messenger.dialog.dto.DialogListItem;
import com.reley.messenger.dialog.dto.DialogResponse;
import com.reley.messenger.dialog.entity.Dialog;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DialogService {

    Mono<DialogResponse> createDialog(Long otherUserId);

    Flux<DialogListItem> getMyDialogs();

    Mono<DialogResponse> createFavoritesDialog(Long userId);

    Mono<Dialog> findById(Long id);

}
