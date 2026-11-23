package com.reley.messenger.dialog.service;

import com.reley.messenger.dialog.dto.DialogAvatar;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

public interface DialogAvatarService {

    Mono<DialogAvatar> getAvatar(Long dialogId);

    Mono<Void> updateAvatar(Long dialogId, FilePart file);

    Mono<Void> deleteAvatar(Long dialogId);
}
