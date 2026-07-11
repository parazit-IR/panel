package com.parazit.panel.api.plan.selection;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SelectPlanRequest(
        @NotNull
        UUID planId
) {
}
