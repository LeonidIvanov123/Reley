package com.reley.messenger.tg.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TgAuthSession {

    private String qrCode;

    private boolean authorized;
}