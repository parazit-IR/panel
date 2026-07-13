package com.parazit.panel.api.internal.subscription;

import jakarta.validation.constraints.Size;

public record RevokeSubscriptionRequest(
        @Size(max = 500) String reason
) {
}
