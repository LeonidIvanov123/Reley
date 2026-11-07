package com.reley.messenger.configuration;

import lombok.Getter;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

@Getter
@Component
public class AppVersion {

    private final String version;

    public AppVersion(BuildProperties buildProperties) {
        this.version = buildProperties.getVersion();
    }

}
