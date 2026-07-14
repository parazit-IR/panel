package com.parazit.panel.domain.wallet;

public class WalletClosedException extends WalletException {

    public WalletClosedException() {
        super("wallet is closed");
    }
}
