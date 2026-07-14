package com.parazit.panel.application.wallet.payment.result;

import java.util.UUID;

public record WalletOrderPaymentReconciliationResult(
        UUID orderId,
        boolean approvedWalletPaymentExists,
        boolean walletTransactionExists,
        boolean orderPaid,
        boolean amountMatches,
        boolean userMatches,
        boolean consistent
) {
}
