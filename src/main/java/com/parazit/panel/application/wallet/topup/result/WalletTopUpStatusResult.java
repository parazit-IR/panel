package com.parazit.panel.application.wallet.topup.result;

import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.wallet.topup.WalletTopUpStatus;
import java.time.Instant;
import java.util.UUID;

public record WalletTopUpStatusResult(
        UUID topUpRequestId,
        Money amount,
        WalletTopUpStatus topUpStatus,
        PaymentStatus paymentStatus,
        PaymentMethod paymentMethod,
        Instant expiresAt,
        Instant creditedAt,
        Money balanceAfter
) {
}
