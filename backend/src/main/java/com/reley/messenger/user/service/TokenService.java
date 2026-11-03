package com.reley.messenger.user.service;

import com.reley.messenger.user.entity.User;

public interface TokenService {

    String generateAccessToken(User user);

}
