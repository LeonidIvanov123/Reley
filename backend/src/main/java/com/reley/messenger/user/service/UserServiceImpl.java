package com.reley.messenger.user.service;

import com.reley.messenger.exception.GlobalException;
import com.reley.messenger.exception.UserAlreadyExistsException;
import com.reley.messenger.user.dto.CreateUserRequest;
import com.reley.messenger.user.dto.UpdateProfileRequest;
import com.reley.messenger.user.dto.UserDirectoryResponse;
import com.reley.messenger.user.dto.UserResponse;
import com.reley.messenger.user.entity.Credential;
import com.reley.messenger.user.entity.User;
import com.reley.messenger.user.repository.CredentialRepository;
import com.reley.messenger.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionalOperator transactionalOperator;
    private final HydrateUserInfoApi userServiceInfo;

    @Override
    public Mono<UserResponse> createUser(CreateUserRequest request) {

        return userRepository.existsByEmail(request.email())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.<User>error(
                                new UserAlreadyExistsException(request.email())
                        );
                    }

                    OffsetDateTime now = OffsetDateTime.now();

                    User user = User.builder()
                            .email(request.email())
                            .createdAt(now)
                            .updatedAt(now)
                            .build();

                    return userRepository.save(user)
                            .flatMap(savedUser -> {

                                return credentialRepository.insert(
                                        savedUser.getId(), passwordEncoder.encode(request.password()), now, now)
                                        .thenReturn(savedUser);
                            });
                })
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getName(),
                        user.getPhone(),
                        user.isHiddenFromDirectory())
                ).as(transactionalOperator::transactional);
    }

    @Override
    public Mono<UserResponse> updateProfile(UpdateProfileRequest request) {

        return userServiceInfo.getCurrentUserId()
                .flatMap(userId ->
                        userRepository.updateProfile(
                                        userId,
                                        request.email(),
                                        request.name(),
                                        request.phone(),
                                        Optional.ofNullable(request.hiddenFromDirectory()).orElse(false),
                                        OffsetDateTime.now()
                                )
                                .flatMap(updated -> {
                                    if (updated == 0) {
                                        return Mono.error(
                                                new GlobalException(
                                                        "Пользователь не найден",
                                                        "profile-edit"
                                                ) {}
                                        );
                                    }

                                    return userRepository.findById(userId);
                                })
                )
                .map(user ->
                        new UserResponse(
                                user.getId(),
                                user.getEmail(),
                                user.getName(),
                                user.getPhone(),
                                user.isHiddenFromDirectory())
                );
    }

    @Override
    public Mono<String> getEmailByUserId(Long userId) {
        return userRepository.findById(userId)
                .map(User::getEmail);
    }

    @Override
    public Mono<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Flux<UserDirectoryResponse> getDirectoryUsers() {
        return userServiceInfo.getCurrentUser()
                .flatMapMany(user -> userRepository.findAllVisibleUsers(user.userId()))
                .map(UserDirectoryResponse::fromEntity);
    }

    @Override
    public Mono<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
