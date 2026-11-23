package com.reley.messenger.dialog.controller;

import com.reley.messenger.dialog.service.DialogAvatarService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class DialogAvatarController {

    private final DialogAvatarService dialogAvatarService;

    @GetMapping("/api/dialogs/{dialogId}/avatar")
    @PreAuthorize("@dialogAccess.canAccess(#dialogId, authentication)")
    public Mono<ResponseEntity<DataBuffer>> getAvatar(
            @PathVariable Long dialogId
    ) {
        return dialogAvatarService
                .getAvatar(dialogId)
                .map(avatar -> {

                    DataBuffer buffer =
                            DefaultDataBufferFactory
                                    .sharedInstance
                                    .wrap(avatar.content());

                    return ResponseEntity
                            .ok()
                            .contentType(
                                    MediaType.parseMediaType(
                                            avatar.contentType()
                                    )
                            )
                            .body(buffer);
                })
                .defaultIfEmpty(
                        ResponseEntity
                                .noContent()
                                .build()
                );
    }

    @PutMapping(
            value = "/api/dialogs/{dialogId}/avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Mono<Void> setAvatar(
            @PathVariable Long dialogId,
            @RequestPart("file") FilePart file
    ) {

        return dialogAvatarService.updateAvatar(
                dialogId,
                file
        );
    }

    @DeleteMapping("/api/dialogs/{dialogId}/avatar")
    public Mono<Void> deleteAvatar(@PathVariable Long dialogId) {
        return dialogAvatarService.deleteAvatar(dialogId);
    }
}