package com.parazit.panel.application.wallet.topup.result;

import com.parazit.panel.application.payment.manual.result.InitializeManualCardPaymentResult;
import com.parazit.panel.application.payment.zarinpal.result.InitializeZarinpalPaymentResult;
import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.wallet.topup.WalletTopUpStatus;
import java.time.Instant;
import java.util.UUID;

public record WalletTopUpPaymentResult(
        UUID topUpRequestId,
        UUID paymentId,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        WalletTopUpStatus topUpStatus,
        Money amount,
        Instant expiresAt,
        InitializeManualCardPaymentResult manualPayment,
        InitializeZarinpalPaymentResult onlinePayment
) {
}
