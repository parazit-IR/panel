package com.parazit.panel.domain.wallet;

public enum WalletOperationOutcome {
    APPLIED,
    REPLAYED,
    REJECTED_INSUFFICIENT_BALANCE,
    REJECTED_WALLET_LOCKED,
    REJECTED_WALLET_CLOSED,
    REJECTED_IDEMPOTENCY_CONFLICT
}
