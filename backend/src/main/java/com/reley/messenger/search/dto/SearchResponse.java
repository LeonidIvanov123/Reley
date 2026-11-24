package com.reley.messenger.search.dto;

import java.util.List;

public record SearchResponse(
        List<SearchUserResponse> users,
        List<SearchDialogResponse> dialogs,
        List<SearchMessageResponse> messages
) {
}