package com.parazit.panel.application.wallet.result;

import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.wallet.WalletStatus;
import java.util.UUID;

public record WalletCreationResult(
        UUID walletId,
        Money balance,
        WalletStatus status
) {
}
