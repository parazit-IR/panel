package com.parazit.panel.domain.payment.zarinpal;

public enum ZarinpalAttemptStatus {
    CREATED,
    REQUESTING,
    REDIRECT_READY,
    CALLBACK_RECEIVED,
    VERIFYING,
    VERIFIED,
    CANCELLED,
    FAILED,
    UNKNOWN
}
