package com.parazit.panel.domain.wallet;

public class InvalidWalletAmountException extends WalletException {

    public InvalidWalletAmountException() {
        super("wallet amount must be positive");
    }
}
