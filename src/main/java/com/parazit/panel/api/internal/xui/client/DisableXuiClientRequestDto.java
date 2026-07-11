package com.parazit.panel.api.internal.xui.client;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DisableXuiClientRequestDto(
        @NotNull
        @Positive
        Long telegramUserId
) {
}
