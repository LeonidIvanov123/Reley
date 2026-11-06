package com.reley.messenger.websocket.event;

public enum WebSocketEventType {

    NEW_MESSAGE,
    MESSAGE_READ,
    MESSAGE_DELETED,
    MESSAGE_EDITED,
    DIALOG_UPDATED;

    @Override
    public String toString() {
        return this.name();
    }
}
