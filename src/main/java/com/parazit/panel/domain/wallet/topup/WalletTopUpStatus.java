package com.parazit.panel.domain.wallet.topup;

public enum WalletTopUpStatus {
    CREATED,
    AWAITING_PAYMENT_METHOD,
    PENDING_PAYMENT,
    PAYMENT_APPROVED,
    CREDITED,
    CANCELLED,
    EXPIRED,
    FAILED
}
