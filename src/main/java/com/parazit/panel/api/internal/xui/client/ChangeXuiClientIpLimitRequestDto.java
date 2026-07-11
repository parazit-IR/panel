package com.parazit.panel.api.internal.xui.client;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

public record ChangeXuiClientIpLimitRequestDto(
        @NotNull UUID operationId,
        @NotNull @Positive Long telegramUserId,
        @NotNull @PositiveOrZero Integer ipLimit
) {
}
