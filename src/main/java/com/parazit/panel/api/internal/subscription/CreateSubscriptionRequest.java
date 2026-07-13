package com.parazit.panel.api.internal.subscription;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateSubscriptionRequest(
        @NotNull UUID xuiClientProvisionId
) {
}
