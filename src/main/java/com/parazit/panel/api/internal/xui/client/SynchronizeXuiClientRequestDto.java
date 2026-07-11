package com.parazit.panel.api.internal.xui.client;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record SynchronizeXuiClientRequestDto(
        @NotNull UUID operationId,
        @NotNull @Positive Long telegramUserId
) {
}
