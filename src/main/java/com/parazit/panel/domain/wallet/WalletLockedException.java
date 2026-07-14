package com.parazit.panel.domain.wallet;

public class WalletLockedException extends WalletException {

    public WalletLockedException() {
        super("wallet is locked");
    }
}
