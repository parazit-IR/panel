package com.parazit.panel.application.wallet.payment;

public enum WalletOrderPaymentEligibilityReason {
    ELIGIBLE,
    FEATURE_DISABLED,
    ORDER_OWNER_MISMATCH,
    ORDER_STATUS_NOT_PAYABLE,
    ORDER_TYPE_NOT_ALLOWED,
    INVALID_AMOUNT,
    CURRENCY_MISMATCH,
    APPROVED_PAYMENT_EXISTS,
    CONFLICTING_PAYMENT_EXISTS
}
