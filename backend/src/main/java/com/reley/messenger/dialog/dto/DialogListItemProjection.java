package com.reley.messenger.dialog.dto;

import java.time.OffsetDateTime;

public interface DialogListItemProjection {

    Long getDialogId();

    String getDialogName();

    Long getOtherUserId();

    String getOtherUserEmail();

    OffsetDateTime getCreatedAt();

    OffsetDateTime getUpdatedAt();
}