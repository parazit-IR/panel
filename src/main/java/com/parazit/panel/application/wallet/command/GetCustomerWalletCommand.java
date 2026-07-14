package com.parazit.panel.application.wallet.command;

public record GetCustomerWalletCommand(long telegramUserId) {

    public GetCustomerWalletCommand {
        if (telegramUserId <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
    }
}
