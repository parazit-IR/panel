package com.parazit.panel.application.plan.selection.command;

import java.util.UUID;

public record SelectPlanCommand(
        Long telegramUserId,
        UUID planId
) {
}
