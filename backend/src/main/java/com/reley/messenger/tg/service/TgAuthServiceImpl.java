package com.reley.messenger.tg.service;

import com.reley.messenger.tg.configuration.TelegramProperties;
import com.reley.messenger.tg.dto.TgAuthQrResponse;
import com.reley.messenger.tg.dto.TgAuthStatusResponse;
import com.reley.messenger.tg.dto.TgDialogResponse;
import com.reley.messenger.tg.entity.TgAccount;
import com.reley.messenger.tg.repository.TgAccountRepository;
import com.reley.messenger.user.service.HydrateUserInfoApi;
import it.tdlight.client.APIToken;
import it.tdlight.client.AuthenticationSupplier;
import it.tdlight.client.SimpleAuthenticationSupplier;
import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.client.SimpleTelegramClientBuilder;
import it.tdlight.client.SimpleTelegramClientFactory;
import it.tdlight.client.TDLibSettings;
import it.tdlight.jni.TdApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "telegram",
        name = "api-enabled",
        havingValue = "true"
)
public class TgAuthServiceImpl implements TgAuthService {

    private final TelegramProperties properties;
    private final SimpleTelegramClientFactory clientFactory;
    private final TgClientManager clientManager;
    private final TgQrCodeManager qrCodeManager;
    private final TgAccountRepository tgAccountRepository;
    private final HydrateUserInfoApi userInfoApi;

    @Override
    public Mono<TgAuthQrResponse> start(Long userId) {

        return Mono.fromCallable(() -> {

            log.debug(
                    "[TG AUTH] Starting Telegram authorization. userId={}",
                    userId
            );

            /*
             * Проверяем, нет ли уже запущенного клиента.
             */
            SimpleTelegramClient existing =
                    clientManager.get(userId);

            if (existing != null) {

                log.debug(
                        "[TG AUTH] Client already exists. userId={}",
                        userId
                );

                return new TgAuthQrResponse(null);
            }

            /*
             * Путь к TDLib session.
             */
            Path sessionPath =
                    Path.of(
                            properties.getDataDirectory(),
                            "user-" + userId
                    );

            log.debug(
                    "[TG AUTH] Session path: {}",
                    sessionPath.toAbsolutePath()
            );

            Files.createDirectories(sessionPath);

            /*
             * Telegram API credentials.
             */
            APIToken apiToken =
                    new APIToken(
                            properties.getApiId(),
                            properties.getApiHash()
                    );

            log.debug(
                    "[TG AUTH] Telegram API token created. userId={}",
                    userId
            );

            /*
             * TDLib settings.
             */
            TDLibSettings settings =
                    TDLibSettings.create(apiToken);

            Path databasePath =
                    sessionPath.resolve("data");

            Path downloadsPath =
                    sessionPath.resolve("downloads");

            settings.setDatabaseDirectoryPath(
                    databasePath
            );

            settings.setDownloadedFilesDirectoryPath(
                    downloadsPath
            );

            log.debug(
                    "[TG AUTH] TDLib database path: {}",
                    databasePath.toAbsolutePath()
            );

            log.debug(
                    "[TG AUTH] TDLib downloads path: {}",
                    downloadsPath.toAbsolutePath()
            );

            /*
             * Создаём builder.
             */
            SimpleTelegramClientBuilder builder =
                    clientFactory.builder(settings);

            log.debug(
                    "[TG AUTH] SimpleTelegramClientBuilder created. userId={}",
                    userId
            );

            /*
             * QR callback.
             *
             * TDLight передаст сюда tg://login?token=...
             */
            builder.setClientInteraction(
                    new TgClientInteraction(
                            qr -> {

                                log.debug(
                                        "[TG AUTH] QR code received. userId={}, length={}",
                                        userId,
                                        qr == null ? null : qr.length()
                                );

                                if (qr != null && !qr.isBlank()) {

                                    qrCodeManager.put(
                                            userId,
                                            qr
                                    );

                                    log.debug(
                                            "[TG AUTH] QR saved to TgQrCodeManager. userId={}",
                                            userId
                                    );

                                } else {

                                    log.warn(
                                            "[TG AUTH] Received empty QR. userId={}",
                                            userId
                                    );
                                }
                            }
                    )
            );

            /*
             * Authentication supplier.
             */
            SimpleAuthenticationSupplier<?> auth =
                    AuthenticationSupplier.qrCode();

            log.debug(
                    "[TG AUTH] QR authentication supplier created. userId={}",
                    userId
            );

            /*
             * ВАЖНО:
             *
             * Handler регистрируем ДО builder.build(auth).
             */
            builder.addUpdateHandler(
                    TdApi.UpdateAuthorizationState.class,
                    update -> {

                        try {

                            TdApi.AuthorizationState state =
                                    update.authorizationState;

                            log.debug(
                                    "[TG AUTH STATE] userId={}, state={}",
                                    userId,
                                    state
                            );

                            /*
                             * Авторизация полностью завершена.
                             */
                            if (state
                                    instanceof TdApi.AuthorizationStateReady) {

                                log.debug(
                                        "[TG AUTH] Telegram authorization ready. userId={}",
                                        userId
                                );

                                /*
                                 * QR больше не нужен.
                                 */
                                qrCodeManager.remove(userId);

                                log.debug(
                                        "[TG AUTH] QR removed from TgQrCodeManager. userId={}",
                                        userId
                                );

                                /*
                                 * Получаем уже созданный Telegram client
                                 * из manager.
                                 */
                                SimpleTelegramClient client =
                                        clientManager.get(userId);

                                if (client == null) {

                                    log.error(
                                            "[TG AUTH] Telegram client not found in TgClientManager. userId={}",
                                            userId
                                    );

                                    return;
                                }

                                log.debug(
                                        "[TG AUTH] Telegram client found. userId={}",
                                        userId
                                );

                                /*
                                 * Получаем текущего Telegram пользователя.
                                 */
                                log.debug(
                                        "[TG AUTH] Calling Telegram getMe(). userId={}",
                                        userId
                                );

                                client.getMeAsync()
                                        .whenComplete(
                                                (telegramUser, error) -> {

                                                    try {

                                                        if (error != null) {

                                                            log.error(
                                                                    "[TG AUTH] getMe() failed. userId={}",
                                                                    userId,
                                                                    error
                                                            );

                                                            return;
                                                        }

                                                        if (telegramUser == null) {

                                                            log.error(
                                                                    "[TG AUTH] getMe() returned null. userId={}",
                                                                    userId
                                                            );

                                                            return;
                                                        }

                                                        log.debug(
                                                                "[TG AUTH] Telegram user received. userId={}, telegramUserId={}, firstName={}, lastName={}",
                                                                userId,
                                                                telegramUser.id,
                                                                telegramUser.firstName,
                                                                telegramUser.lastName
                                                        );

                                                        /*
                                                         * Получаем username.
                                                         */
                                                        String username;

                                                        if (telegramUser.usernames != null
                                                                && telegramUser.usernames.activeUsernames != null
                                                                && telegramUser.usernames.activeUsernames.length > 0) {

                                                            username =
                                                                    telegramUser.usernames
                                                                            .activeUsernames[0];

                                                        } else {

                                                            username = null;
                                                        }

                                                        log.debug(
                                                                "[TG AUTH] Telegram username. userId={}, username={}",
                                                                userId,
                                                                username
                                                        );

                                                        /*
                                                         * Ищем существующую связь.
                                                         */
                                                        log.debug(
                                                                "[TG AUTH] Searching tg_accounts. userId={}",
                                                                userId
                                                        );

                                                        tgAccountRepository
                                                                .findByUserId(userId)

                                                                /*
                                                                 * Если аккаунт уже есть —
                                                                 * обновляем его.
                                                                 */
                                                                .flatMap(
                                                                        existingAccount -> {

                                                                            log.debug(
                                                                                    "[TG AUTH] Existing tg_account found. userId={}, accountId={}",
                                                                                    userId,
                                                                                    existingAccount.getId()
                                                                            );

                                                                            existingAccount
                                                                                    .setTelegramUserId(
                                                                                            telegramUser.id
                                                                                    );

                                                                            existingAccount
                                                                                    .setUsername(
                                                                                            username
                                                                                    );

                                                                            existingAccount
                                                                                    .setFirstName(
                                                                                            telegramUser.firstName
                                                                                    );

                                                                            existingAccount
                                                                                    .setLastName(
                                                                                            telegramUser.lastName
                                                                                    );

                                                                            existingAccount
                                                                                    .setUpdatedAt(
                                                                                            OffsetDateTime.now()
                                                                                    );

                                                                            log.debug(
                                                                                    "[TG AUTH] Updating tg_account. userId={}, accountId={}",
                                                                                    userId,
                                                                                    existingAccount.getId()
                                                                            );

                                                                            return tgAccountRepository.save(
                                                                                    existingAccount
                                                                            );
                                                                        }
                                                                )

                                                                /*
                                                                 * Если аккаунта ещё нет —
                                                                 * создаём.
                                                                 */
                                                                .switchIfEmpty(
                                                                        Mono.defer(() -> {

                                                                            log.debug(
                                                                                    "[TG AUTH] No existing tg_account found. userId={}",
                                                                                    userId
                                                                            );

                                                                            TgAccount account =
                                                                                    TgAccount.builder()
                                                                                            .userId(userId)
                                                                                            .telegramUserId(
                                                                                                    telegramUser.id
                                                                                            )
                                                                                            .username(
                                                                                                    username
                                                                                            )
                                                                                            .firstName(
                                                                                                    telegramUser.firstName
                                                                                            )
                                                                                            .lastName(
                                                                                                    telegramUser.lastName
                                                                                            )
                                                                                            .createdAt(
                                                                                                    OffsetDateTime.now()
                                                                                            )
                                                                                            .updatedAt(
                                                                                                    OffsetDateTime.now()
                                                                                            )
                                                                                            .build();

                                                                            log.debug(
                                                                                    "[TG AUTH] Creating new tg_account. userId={}",
                                                                                    userId
                                                                            );

                                                                            return tgAccountRepository.save(
                                                                                    account
                                                                            );
                                                                        })
                                                                )

                                                                /*
                                                                 * ВАЖНО: подписываемся
                                                                 * и логируем результат.
                                                                 */
                                                                .subscribe(
                                                                        saved -> {

                                                                            log.debug(
                                                                                    "[TG AUTH] Telegram account saved. userId={}, accountId={}, telegramUserId={}, username={}",
                                                                                    saved.getUserId(),
                                                                                    saved.getId(),
                                                                                    saved.getTelegramUserId(),
                                                                                    saved.getUsername()
                                                                            );

                                                                        },

                                                                        saveError -> {

                                                                            log.error(
                                                                                    "[TG AUTH] Telegram account save failed. userId={}",
                                                                                    userId,
                                                                                    saveError
                                                                            );
                                                                        }
                                                                );

                                                    } catch (Throwable e) {

                                                        log.error(
                                                                "[TG AUTH] Error inside getMe callback. userId={}",
                                                                userId,
                                                                e
                                                        );
                                                    }
                                                }
                                        );
                            }

                        } catch (Throwable e) {

                            log.error(
                                    "[TG AUTH] Authorization state handler error. userId={}",
                                    userId,
                                    e
                            );
                        }
                    }
            );

            log.debug(
                    "[TG AUTH] Authorization state handler registered. userId={}",
                    userId
            );

            /*
             * Теперь создаём Telegram client.
             *
             * Handler уже зарегистрирован выше.
             */
            log.debug(
                    "[TG AUTH] Building Telegram client. userId={}",
                    userId
            );

            SimpleTelegramClient client =
                    builder.build(auth);

            log.debug(
                    "[TG AUTH] Telegram client created. userId={}",
                    userId
            );

            /*
             * Сохраняем client в manager.
             */
            clientManager.put(
                    userId,
                    client
            );

            log.debug(
                    "[TG AUTH] Telegram client stored in TgClientManager. userId={}",
                    userId
            );

            log.debug(
                    "[TG AUTH] Telegram authorization started successfully. userId={}",
                    userId
            );

            return new TgAuthQrResponse(null);
        });
    }

    @Override
    public Mono<Void> disconnect(Long userId) {

        log.debug(
                "[TG AUTH] Disconnecting Telegram account. userId={}",
                userId
        );

        SimpleTelegramClient client =
                clientManager.get(userId);

        qrCodeManager.remove(userId);

        Mono<Void> logout;

        if (client != null) {

            log.debug(
                    "[TG AUTH] Sending Telegram LogOut. userId={}",
                    userId
            );

            logout = Mono.fromFuture(
                            client.send(
                                    new TdApi.LogOut()
                            )
                    )
                    .then();

        } else {

            log.debug(
                    "[TG AUTH] Telegram client not found. userId={}",
                    userId
            );

            logout = Mono.empty();
        }

        return logout
                .doFinally(signal -> {

                    clientManager.remove(userId);

                    log.debug(
                            "[TG AUTH] Telegram client removed from manager. userId={}",
                            userId
                    );
                })
                .then(
                        tgAccountRepository
                                .findByUserId(userId)
                                .flatMap(tgAccountRepository::delete)
                                .then()
                )
                .doOnSuccess(unused ->
                        log.debug(
                                "[TG AUTH] Telegram account disconnected successfully. userId={}",
                                userId
                        )
                )
                .doOnError(error ->
                        log.error(
                                "[TG AUTH] Telegram account disconnect failed. userId={}",
                                userId,
                                error
                        )
                );
    }

    @Override
    public Mono<byte[]> getQr(Long userId) {

        return Mono.fromCallable(
                        () -> qrCodeManager.get(userId)
                )
                .flatMap(qr -> {

                    if (qr == null) {
                        return Mono.empty();
                    }

                    return Mono.fromCallable(
                            () -> TgCodeUtil.generate(qr)
                    );
                });
    }

    @Override
    public Mono<TgAuthStatusResponse> getStatus(Long userId) {

        return tgAccountRepository
                .findByUserId(userId)
                .map(account ->
                        new TgAuthStatusResponse(
                                true,
                                account.getUsername(),
                                account.getFirstName(),
                                account.getLastName()
                        )
                )
                .defaultIfEmpty(
                        new TgAuthStatusResponse(
                                false,
                                null,
                                null,
                                null
                        )
                );
    }

    @Override
    public Mono<Void> cancelPendingAuth(Long userId) {

        log.debug(
                "[TG AUTH] Cancelling pending Telegram authorization. userId={}",
                userId
        );

        return tgAccountRepository
                .findByUserId(userId)
                .flatMap(account -> {

                    log.debug(
                            "[TG AUTH] Telegram account is already connected. " +
                                    "Pending authorization will not be cancelled. userId={}",
                            userId
                    );

                    return Mono.empty();
                })
                .switchIfEmpty(
                        Mono.defer(() -> {

                            SimpleTelegramClient client =
                                    clientManager.get(userId);

                            qrCodeManager.remove(userId);

                            if (client == null) {

                                log.debug(
                                        "[TG AUTH] No pending Telegram client found. userId={}",
                                        userId
                                );

                                return Mono.empty();
                            }

                            log.debug(
                                    "[TG AUTH] Closing pending Telegram client. userId={}",
                                    userId
                            );

                            clientManager.remove(userId);

                            return Mono.fromRunnable(() -> {
                                try {
                                    client.close();
                                } catch (Exception e) {
                                    log.warn(
                                            "[TG AUTH] Failed to close pending Telegram client. userId={}",
                                            userId,
                                            e
                                    );
                                }
                            });
                        })
                )
                .then();
    }

    @Override
    public Mono<List<TgDialogResponse>> getDialogs(Long userId) {

        SimpleTelegramClient client =
                clientManager.get(userId);

        if (client == null) {
            return Mono.error(new IllegalStateException("Telegram client is not connected"));
        }

        return Mono.fromFuture(
                        client.send(
                                new TdApi.GetChats(new TdApi.ChatListMain(), 100)
                        )
                )
                .flatMapMany(chats ->
                        Flux.fromStream(Arrays.stream(chats.chatIds).boxed())
                                .concatMap(chatId ->
                                        Mono.fromFuture(
                                                client.send(
                                                        new TdApi.GetChat(chatId)
                                                )
                                        )
                                )
                )
                .map(TgCodeUtil::toDialogResponse)
                .collectList();
    }

    @Override
    public Mono<Boolean> isAuthCurrentUser() {
        return userInfoApi.getCurrentUserId()
                .flatMap(tgAccountRepository::existsByUserId);
    }

}