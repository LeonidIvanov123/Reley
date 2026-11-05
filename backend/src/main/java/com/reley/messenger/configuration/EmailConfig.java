package com.reley.messenger.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Data
@Configuration
@ConfigurationProperties(prefix = "reley.mail")
public class EmailConfig {

    private String host;
    private int port = 587;
    private String username;
    private String password;
    private Smtp smtp = new Smtp();

    @Data
    public static class Smtp {

        private boolean auth;

        private Starttls starttls = new Starttls();

        @Data
        public static class Starttls {
            private boolean enable;
        }
    }

    @Bean
    public JavaMailSender mailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();

        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);

        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.smtp.auth", smtp.auth);
        properties.put(
                "mail.smtp.starttls.enable",
                smtp.starttls.enable
        );

        return sender;
    }

}