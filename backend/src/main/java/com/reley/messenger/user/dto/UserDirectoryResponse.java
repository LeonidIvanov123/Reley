package com.reley.messenger.user.dto;

import com.reley.messenger.user.entity.User;

public record UserDirectoryResponse(
        Long id,
        String name,
        String email
) {

    public static UserDirectoryResponse fromEntity(User user) {
        return new UserDirectoryResponse(user.getId(), user.getName(), user.getEmail());
    }
}
