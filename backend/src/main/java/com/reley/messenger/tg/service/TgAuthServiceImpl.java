package com.reley.messenger.tg.service;

import com.reley.messenger.tg.configuration.TelegramProperties;
import com.reley.messenger.tg.dto.TgAuthQrResponse;
import com.reley.messenger.tg.dto.TgAuthStatusResponse;
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
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

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


    // =========================================================
    // AUTHORIZATION
    // =========================================================

    @Override
    public Mono<TgAuthQrResponse> start(Long userId) {

        return Mono.fromCallable(() -> {

            log.debug(
                    "[TG AUTH] Starting Telegram authorization. userId={}",
                    userId
            );

            /*
             * Если клиент уже существует в памяти —
             * повторно его не создаём.
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
             * Создаём новый Telegram client.
             *
             * Для первичной авторизации используем QR.
             */
            createClient(
                    userId,
                    true
            );

            log.debug(
                    "[TG AUTH] Telegram authorization started successfully. userId={}",
                    userId
            );

            return new TgAuthQrResponse(null);
        });
    }


    // =========================================================
    // CREATE TELEGRAM CLIENT
    // =========================================================

    /**
     * Создаёт TDLight client.
     *
     * @param userId пользователь Reley
     * @param qrAuthentication true — первичная авторизация через QR
     */
    private SimpleTelegramClient createClient(
            Long userId,
            boolean qrAuthentication
    ) {

        log.debug(
                "[TG AUTH] Creating Telegram client. userId={}, qrAuthentication={}",
                userId,
                qrAuthentication
        );

        /*
         * Путь к существующей TDLib session.
         *
         * ВАЖНО:
         * после рестарта приложения используется тот же путь,
         * поэтому TDLib сможет восстановить существующую авторизацию.
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

        try {

            Files.createDirectories(sessionPath);

            Path databasePath =
                    sessionPath.resolve("data");

            Path downloadsPath =
                    sessionPath.resolve("downloads");

            Files.createDirectories(databasePath);
            Files.createDirectories(downloadsPath);

            log.debug(
                    "[TG AUTH] TDLib database path: {}",
                    databasePath.toAbsolutePath()
            );

            log.debug(
                    "[TG AUTH] TDLib downloads path: {}",
                    downloadsPath.toAbsolutePath()
            );

            /*
             * Telegram API credentials.
             */
            APIToken apiToken =
                    new APIToken(
                            properties.getApiId(),
                            properties.getApiHash()
                    );

            /*
             * TDLib settings.
             */
            TDLibSettings settings =
                    TDLibSettings.create(apiToken);

            settings.setDatabaseDirectoryPath(
                    databasePath
            );

            settings.setDownloadedFilesDirectoryPath(
                    downloadsPath
            );

            /*
             * Builder.
             */
            SimpleTelegramClientBuilder builder =
                    clientFactory.builder(settings);

            /*
             * QR callback.
             *
             * При восстановлении уже существующей сессии
             * этот callback обычно не вызывается.
             *
             * Но если TDLib всё-таки потребует повторную авторизацию,
             * мы сможем получить новый QR.
             */
            builder.setClientInteraction(
                    new TgClientInteraction(
                            qr -> {

                                if (qr != null && !qr.isBlank()) {

                                    qrCodeManager.put(
                                            userId,
                                            qr
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

            SimpleAuthenticationSupplier<?> auth =
                    AuthenticationSupplier.qrCode();

            CompletableFuture<SimpleTelegramClient> readyFuture =
                    new CompletableFuture<>();

            AtomicReference<SimpleTelegramClient> clientReference =
                    new AtomicReference<>();

            builder.addUpdateHandler(
                    TdApi.UpdateAuthorizationState.class,
                    update -> {

                        try {

                            TdApi.AuthorizationState state =
                                    update.authorizationState;

                            /*
                             * Авторизация полностью завершена.
                             */
                            if (state instanceof TdApi.AuthorizationStateReady) {

                                qrCodeManager.remove(userId);

                                SimpleTelegramClient client =
                                        clientReference.get();

                                if (client == null) {

                                    client =
                                            clientManager.get(userId);
                                }

                                if (client == null) {

                                    log.error(
                                            "[TG AUTH] Telegram client not found after AuthorizationStateReady. userId={}",
                                            userId
                                    );

                                    readyFuture.completeExceptionally(
                                            new IllegalStateException(
                                                    "Telegram client not found after AuthorizationStateReady"
                                            )
                                    );

                                    return;
                                }

                                clientManager.put(
                                        userId,
                                        client
                                );

                                readyFuture.complete(client);

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
                                                                "[TG AUTH] Telegram user received. " +
                                                                        "userId={}, telegramUserId={}, firstName={}, lastName={}",
                                                                userId,
                                                                telegramUser.id,
                                                                telegramUser.firstName,
                                                                telegramUser.lastName
                                                        );

                                                        /*
                                                         * Username.
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

                                                        saveTelegramAccount(
                                                                userId,
                                                                telegramUser,
                                                                username
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

                            readyFuture.completeExceptionally(e);
                        }
                    }
            );

            SimpleTelegramClient client =
                    builder.build(auth);

            clientReference.set(client);

            clientManager.put(
                    userId,
                    client
            );

            return client;

        } catch (Exception e) {

            log.error(
                    "[TG AUTH] Failed to create Telegram client. userId={}",
                    userId,
                    e
            );

            throw new IllegalStateException(
                    "Failed to create Telegram client",
                    e
            );
        }
    }


    // =========================================================
    // RESTORE CLIENT
    // =========================================================

    /**
     * Возвращает Telegram client из памяти.
     *
     * Если приложение было перезапущено,
     * clientManager будет пустым.
     *
     * В этом случае проверяем tg_accounts и
     * восстанавливаем TDLib client из существующей session.
     */
    @Override
    public Mono<SimpleTelegramClient> getOrRestoreClient(
            Long userId
    ) {

        /*
         * 1. Сначала проверяем память.
         */
        SimpleTelegramClient existing =
                clientManager.get(userId);

        if (existing != null) {

            log.debug(
                    "[TG AUTH] Telegram client already exists in manager. userId={}",
                    userId
            );

            return Mono.just(existing);
        }

        log.info(
                "[TG AUTH] Telegram client not found in manager. " +
                        "Checking tg_accounts for restore. userId={}",
                userId
        );

        return tgAccountRepository
                .findByUserId(userId)

                .switchIfEmpty(
                        Mono.error(
                                new IllegalStateException(
                                        "Telegram account is not connected"
                                )
                        )
                )

                .flatMap(account -> {

                    log.info(
                            "[TG AUTH] Telegram account found in DB. " +
                                    "Restoring client. userId={}, telegramUserId={}",
                            userId,
                            account.getTelegramUserId()
                    );

                    /*
                     * 3. Создаём client с той же TDLib session.
                     */
                    SimpleTelegramClient client =
                            createClient(
                                    userId,
                                    false
                            );

                    /*
                     * 4. Ждём AuthorizationStateReady.
                     *
                     * Здесь TDLib должна восстановить
                     * существующую авторизацию из data/telegram/user-{id}.
                     */
                    return waitForClientReady(
                            userId,
                            client
                    );
                });
    }


    /**
     * Ждём, пока TDLib переведёт клиент
     * в AuthorizationStateReady.
     *
     * Обычно при восстановлении существующей
     * session это происходит практически сразу.
     */
    private Mono<SimpleTelegramClient> waitForClientReady(
            Long userId,
            SimpleTelegramClient client
    ) {

        /*
         * В данном случае createClient уже зарегистрировал
         * AuthorizationStateReady handler.
         *
         * Поэтому проверяем состояние через getMe().
         *
         * Если getMe() успешно выполняется — client уже готов.
         */
        return Mono.fromFuture(
                        client.getMeAsync()
                )
                .timeout(
                        Duration.ofSeconds(30)
                )
                .doOnNext(user ->
                        log.info(
                                "[TG AUTH] Telegram client restored successfully. " +
                                        "userId={}, telegramUserId={}",
                                userId,
                                user.id
                        )
                )
                .thenReturn(client)
                .doOnError(error ->
                        log.error(
                                "[TG AUTH] Failed to restore Telegram client. " +
                                        "userId={}",
                                userId,
                                error
                        )
                );
    }


    // =========================================================
    // SAVE TG ACCOUNT
    // =========================================================

    private void saveTelegramAccount(
            Long userId,
            TdApi.User telegramUser,
            String username
    ) {

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
                .flatMap(existingAccount -> {

                    log.debug(
                            "[TG AUTH] Existing tg_account found. " +
                                    "userId={}, accountId={}",
                            userId,
                            existingAccount.getId()
                    );

                    existingAccount.setTelegramUserId(
                            telegramUser.id
                    );

                    existingAccount.setUsername(
                            username
                    );

                    existingAccount.setFirstName(
                            telegramUser.firstName
                    );

                    existingAccount.setLastName(
                            telegramUser.lastName
                    );

                    existingAccount.setUpdatedAt(
                            OffsetDateTime.now()
                    );

                    log.debug(
                            "[TG AUTH] Updating tg_account. " +
                                    "userId={}, accountId={}",
                            userId,
                            existingAccount.getId()
                    );

                    return tgAccountRepository.save(
                            existingAccount
                    );
                })

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

                .subscribe(
                        saved -> {

                            log.debug(
                                    "[TG AUTH] Telegram account saved. " +
                                            "userId={}, accountId={}, " +
                                            "telegramUserId={}, username={}",
                                    saved.getUserId(),
                                    saved.getId(),
                                    saved.getTelegramUserId(),
                                    saved.getUsername()
                            );

                        },

                        saveError -> {

                            log.error(
                                    "[TG AUTH] Telegram account save failed. " +
                                            "userId={}",
                                    userId,
                                    saveError
                            );
                        }
                );
    }


    // =========================================================
    // DISCONNECT
    // =========================================================

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
                                .flatMap(
                                        tgAccountRepository::delete
                                )
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


    // =========================================================
    // QR
    // =========================================================

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


    // =========================================================
    // STATUS
    // =========================================================

    @Override
    public Mono<TgAuthStatusResponse> getStatus(
            Long userId
    ) {

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


    // =========================================================
    // CANCEL PENDING AUTH
    // =========================================================

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

    // =========================================================
    // CURRENT USER AUTH
    // =========================================================

    @Override
    public Mono<Boolean> isAuthCurrentUser() {

        return userInfoApi
                .getCurrentUserId()
                .flatMap(
                        tgAccountRepository::existsByUserId
                );
    }
}