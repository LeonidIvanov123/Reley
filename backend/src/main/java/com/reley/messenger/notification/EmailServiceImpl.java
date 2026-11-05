package com.reley.messenger.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;


    @Value("${reley.registration.base-url}")
    private String baseUrl;

    @Override
    public Mono<Void> sendVerificationCode(String email, String code, String codeTtlText) {

        return Mono.fromRunnable(() -> {

            String verificationUrl = UriComponentsBuilder
                    .fromUriString(baseUrl)
                    .path("/register/verify")
                    .queryParam("email", email)
                    .queryParam("code", code)
                    .build()
                    .encode()
                    .toUriString();

            Context context = new Context();
            context.setVariable("code", code);
            context.setVariable("verificationUrl", verificationUrl);
            context.setVariable("codeTtl", codeTtlText);

            String html = templateEngine.process("email/verification-code", context);

            MimeMessage message = mailSender.createMimeMessage();

            try {
                MimeMessageHelper helper =
                        new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

                helper.setTo(email);
                helper.setSubject("Reley — подтверждение email");
                helper.setText(html, true);

                mailSender.send(message);

            } catch (MessagingException e) {
                throw new IllegalStateException("Не удалось отправить email", e);
            }
        });
    }
}
