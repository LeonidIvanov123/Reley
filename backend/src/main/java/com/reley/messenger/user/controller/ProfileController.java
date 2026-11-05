package com.reley.messenger.user.controller;

import com.reley.messenger.user.dto.UpdateProfileRequest;
import com.reley.messenger.user.repository.UserRepository;
import com.reley.messenger.user.service.HydrateUserInfo;
import com.reley.messenger.user.service.HydrateUserInfoApi;
import com.reley.messenger.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final HydrateUserInfoApi userServiceInfo;

    @GetMapping("/profile/edit")
    public Mono<String> editProfile(Model model) {

        return userServiceInfo.getCurrentUserId()
                .flatMap(userRepository::findById)
                .doOnNext(user ->
                        model.addAttribute("user", user)
                )
                .thenReturn("profile-edit");
    }

    @PostMapping("/profile")
    public Mono<String> updateProfile(
            @ModelAttribute UpdateProfileRequest request
    ) {
        return userService.updateProfile(request)
                .thenReturn("redirect:/profile");
    }

    @GetMapping("/profile")
    public Mono<String> profile(Model model) {

        return userServiceInfo.getCurrentUserId()
                .flatMap(userRepository::findById)
                .doOnNext(user ->
                        model.addAttribute("user", user)
                )
                .thenReturn("profile");
    }

    @PostMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<String> updateAvatar(@RequestPart("avatar") FilePart avatar) {
        return userService
                .updateAvatar(avatar)
                .thenReturn("redirect:/profile/edit");
    }

}
