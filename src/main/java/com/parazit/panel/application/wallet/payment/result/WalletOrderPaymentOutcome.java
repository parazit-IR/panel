package com.parazit.panel.application.wallet.payment.result;

public enum WalletOrderPaymentOutcome {
    PAID,
    ALREADY_PAID,
    INSUFFICIENT_BALANCE,
    CONFLICTING_PAYMENT_EXISTS,
    ORDER_NOT_ELIGIBLE,
    WALLET_UNAVAILABLE
}
