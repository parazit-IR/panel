package com.parazit.panel.application.wallet.topup.command;

import java.util.Objects;
import java.util.UUID;

public record HandleApprovedWalletTopUpCommand(
        UUID paymentId,
        UUID topUpRequestId,
        UUID approvalRequestId
) {

    public HandleApprovedWalletTopUpCommand {
        paymentId = Objects.requireNonNull(paymentId, "paymentId must not be null");
        topUpRequestId = Objects.requireNonNull(topUpRequestId, "topUpRequestId must not be null");
        approvalRequestId = Objects.requireNonNull(approvalRequestId, "approvalRequestId must not be null");
    }
}
