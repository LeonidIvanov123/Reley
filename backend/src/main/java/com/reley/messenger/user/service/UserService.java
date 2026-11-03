package com.reley.messenger.user.service;

import com.reley.messenger.user.dto.CreateUserRequest;
import com.reley.messenger.user.dto.UpdateProfileRequest;
import com.reley.messenger.user.dto.UserDirectoryResponse;
import com.reley.messenger.user.dto.UserResponse;
import com.reley.messenger.user.entity.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserService {

    Mono<UserResponse> createUser(CreateUserRequest request);

    Mono<UserResponse> updateProfile(UpdateProfileRequest request);

    Mono<String> getEmailByUserId(Long userId);

    Mono<User> findById(Long id);

    Flux<UserDirectoryResponse> getDirectoryUsers();

    Mono<User> findByEmail(String email);

}
