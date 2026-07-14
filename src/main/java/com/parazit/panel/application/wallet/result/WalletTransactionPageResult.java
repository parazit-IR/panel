package com.parazit.panel.application.wallet.result;

import java.util.List;

public record WalletTransactionPageResult(
        List<WalletTransactionSummaryResult> items,
        int page,
        int size,
        boolean hasPrevious,
        boolean hasNext,
        long totalItems
) {
}
