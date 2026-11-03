package com.reley.messenger.user.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "reley.security.refresh-token")
public class RefreshTokenProperties {

    private Duration ttl = Duration.ofDays(5);

    private Duration absoluteSessionTtl = Duration.ofDays(30);
}
