package com.parazit.panel.domain.wallet;

public class WalletCurrencyMismatchException extends WalletException {

    public WalletCurrencyMismatchException() {
        super("wallet currency mismatch");
    }
}
