package com.reley.messenger.websocket.event;

public enum WebSocketEventType {

    NEW_MESSAGE,
    MESSAGE_READ,
    MESSAGE_DELETED,
    MESSAGE_EDITED,
    DIALOG_UPDATED,
    MESSAGE_REACTION_CHANGED;

    @Override
    public String toString() {
        return this.name();
    }
}
