package com.reley.messenger.user.controller;

import com.reley.messenger.configuration.AppVersion;
import com.reley.messenger.dialog.service.DialogService;
import com.reley.messenger.tg.service.TgAuthService;
import com.reley.messenger.user.service.HydrateUserInfoApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final HydrateUserInfoApi userServiceInfo;
    private final DialogService dialogService;
    private final AppVersion appVersion;
    private final Optional<TgAuthService> tgAuthService;


    @GetMapping("/")
    public Mono<String> home(Model model) {

        return userServiceInfo.getCurrentUser()
                .flatMap(user -> {

                    model.addAttribute("user", user);
                    model.addAttribute("appVersion", appVersion.getVersion());

                    Mono<Boolean> telegramConnected =
                            tgAuthService
                                    .map(TgAuthService::isAuthCurrentUser)
                                    .orElseGet(() -> Mono.just(false));

                    return telegramConnected
                            .doOnNext(connected ->
                                    model.addAttribute("telegramConnected", connected)
                            )
                            .then(
                                    dialogService.getMyDialogs()
                                            .collectList()
                                            .doOnNext(dialogs ->
                                                    model.addAttribute("dialogs", dialogs))
                            )
                            .thenReturn("index");
                });
    }

}
