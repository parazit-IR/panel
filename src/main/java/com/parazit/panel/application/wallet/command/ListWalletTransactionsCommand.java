package com.parazit.panel.application.wallet.command;

public record ListWalletTransactionsCommand(long telegramUserId, int page, int size) {

    public ListWalletTransactionsCommand {
        if (telegramUserId <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
        page = Math.max(0, page);
        size = Math.max(1, size);
    }
}
