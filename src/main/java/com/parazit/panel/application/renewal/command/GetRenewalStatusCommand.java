package com.parazit.panel.application.renewal.command;

import java.util.Objects;
import java.util.UUID;

public record GetRenewalStatusCommand(long telegramUserId, UUID renewalOrderId) {

    public GetRenewalStatusCommand {
        Objects.requireNonNull(renewalOrderId, "renewalOrderId must not be null");
    }
}
