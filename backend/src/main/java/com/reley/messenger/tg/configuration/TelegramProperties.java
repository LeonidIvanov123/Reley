package com.reley.messenger.tg.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {

    private boolean apiEnabled;

    private Integer apiId;

    private String apiHash;

    private String dataDirectory;

}