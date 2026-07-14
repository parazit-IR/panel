package com.parazit.panel.domain.wallet;

public class InsufficientWalletBalanceException extends WalletException {

    public InsufficientWalletBalanceException() {
        super("wallet balance is insufficient");
    }
}
