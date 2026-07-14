package com.parazit.panel.domain.promotion;

public enum GiftCodeRejectionReason {
    NONE,
    FEATURE_DISABLED,
    INVALID_CODE,
    NOT_ACTIVE,
    NOT_STARTED,
    EXPIRED,
    EXHAUSTED,
    USER_LIMIT_REACHED,
    WALLET_UNAVAILABLE,
    CURRENCY_MISMATCH
}
