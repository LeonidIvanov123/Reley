package com.reley.messenger.tg.configuration;

import it.tdlight.Init;
import it.tdlight.Log;
import it.tdlight.Slf4JLogMessageHandler;
import it.tdlight.client.SimpleTelegramClientFactory;
import it.tdlight.util.UnsupportedNativeLibraryException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty(
        prefix = "telegram",
        name = "api-enabled",
        havingValue = "true"
)
public class TgClientConfig {

    @PostConstruct
    public void initTdLight() {
        try {
            Init.init();
        } catch (UnsupportedNativeLibraryException e) {
            log.error("TgClientConfig initTdLight exception", e);
            throw new RuntimeException(e);
        }

        Log.setLogMessageHandler(
                1,
                new Slf4JLogMessageHandler()
        );
    }

    @Bean(destroyMethod = "close")
    public SimpleTelegramClientFactory telegramClientFactory() {
        return new SimpleTelegramClientFactory();
    }
}