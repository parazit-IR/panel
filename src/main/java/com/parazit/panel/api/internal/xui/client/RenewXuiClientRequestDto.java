package com.parazit.panel.api.internal.xui.client;

import com.parazit.panel.application.xui.client.model.RenewalMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record RenewXuiClientRequestDto(
        @NotNull UUID operationId,
        @NotNull @Positive Long telegramUserId,
        @NotNull @Positive Integer durationDays,
        @NotNull RenewalMode renewalMode
) {
}
