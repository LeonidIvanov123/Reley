package com.reley.messenger.dialog.service;

import com.reley.messenger.dialog.dto.DialogAvatar;
import com.reley.messenger.dialog.repository.DialogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DialogAvatarServiceImpl implements DialogAvatarService {

    private static final long MAX_AVATAR_SIZE =
            5 * 1024 * 1024;

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of(
                    MediaType.IMAGE_JPEG_VALUE,
                    MediaType.IMAGE_PNG_VALUE,
                    "image/webp"
            );

    private final DialogRepository dialogRepository;

    @Override
    public Mono<DialogAvatar> getAvatar(Long dialogId) {

        return dialogRepository
                .findAvatar(dialogId)
                .switchIfEmpty(
                        Mono.error(
                                new IllegalArgumentException("Диалог не найден")
                        )
                )
                .flatMap(avatar -> {

                    if (avatar.getAvatar() == null
                            || avatar.getAvatarContentType() == null) {

                        return Mono.empty();
                    }

                    return Mono.just(
                            new DialogAvatar(avatar.getAvatar(), avatar.getAvatarContentType())
                    );
                });
    }

    @Override
    public Mono<Void> updateAvatar(
            Long dialogId,
            FilePart file
    ) {

        if (file == null) {
            return Mono.error(
                    new IllegalArgumentException(
                            "Файл не передан"
                    )
            );
        }

        String contentType =
                file.headers().getContentType() != null
                        ? file.headers()
                        .getContentType()
                        .toString()
                        : null;

        if (contentType == null
                || !ALLOWED_CONTENT_TYPES.contains(contentType)) {

            return Mono.error(
                    new IllegalArgumentException(
                            "Поддерживаются только JPEG, PNG и WebP"
                    )
            );
        }

        return readFile(file)
                .flatMap(bytes -> {

                    if (bytes.length == 0) {
                        return Mono.error(
                                new IllegalArgumentException(
                                        "Файл пустой"
                                )
                        );
                    }

                    if (bytes.length > MAX_AVATAR_SIZE) {
                        return Mono.error(
                                new IllegalArgumentException(
                                        "Размер аватара не должен превышать 5 МБ"
                                )
                        );
                    }

                    return dialogRepository
                            .updateAvatar(
                                    dialogId,
                                    bytes,
                                    contentType,
                                    OffsetDateTime.now()
                            )
                            .flatMap(updated -> {

                                if (updated == 0) {
                                    return Mono.error(
                                            new IllegalArgumentException(
                                                    "Диалог не найден"
                                            )
                                    );
                                }

                                return Mono.empty();
                            });
                });
    }

    @Override
    public Mono<Void> deleteAvatar(Long dialogId) {

        return dialogRepository
                .deleteAvatar(
                        dialogId,
                        OffsetDateTime.now()
                )
                .flatMap(updated -> {

                    if (updated == 0) {
                        return Mono.error(
                                new IllegalArgumentException(
                                        "Диалог не найден"
                                )
                        );
                    }

                    return Mono.empty();
                });
    }

    private Mono<byte[]> readFile(FilePart file) {

        return DataBufferUtils.join(file.content())
                .map(dataBuffer -> {

                    try {

                        int readableByteCount =
                                dataBuffer.readableByteCount();

                        if (readableByteCount
                                > MAX_AVATAR_SIZE) {

                            throw new IllegalArgumentException(
                                    "Размер аватара не должен превышать 5 МБ"
                            );
                        }

                        byte[] bytes =
                                new byte[readableByteCount];

                        dataBuffer.read(bytes);

                        return bytes;

                    } finally {
                        DataBufferUtils.release(dataBuffer);
                    }
                });
    }
}
