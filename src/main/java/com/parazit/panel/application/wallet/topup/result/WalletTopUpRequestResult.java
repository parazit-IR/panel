package com.parazit.panel.application.wallet.topup.result;

import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.wallet.topup.WalletTopUpStatus;
import java.time.Instant;
import java.util.UUID;

public record WalletTopUpRequestResult(
        UUID topUpRequestId,
        Money amount,
        WalletTopUpStatus status,
        Instant expiresAt,
        boolean manualPaymentAvailable,
        boolean onlinePaymentAvailable
) {
}
