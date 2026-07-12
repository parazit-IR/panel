package com.parazit.panel.application.provisioning.outbox;

import java.util.UUID;

public record CreateVpnProvisioningPayloadV1(
        UUID orderId,
        UUID paymentId,
        UUID userId,
        Long telegramUserId,
        UUID planId,
        UUID planSelectionId,
        Long preferredInboundId
) {
}
