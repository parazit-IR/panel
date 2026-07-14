package com.parazit.panel.domain.wallet;

public class WalletIdempotencyConflictException extends WalletException {

    public WalletIdempotencyConflictException() {
        super("wallet idempotency key conflicts with a different request");
    }
}
