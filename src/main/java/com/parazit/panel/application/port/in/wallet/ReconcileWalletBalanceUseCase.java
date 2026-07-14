package com.parazit.panel.application.port.in.wallet;

import com.parazit.panel.application.wallet.command.ReconcileWalletBalanceCommand;
import com.parazit.panel.application.wallet.result.WalletReconciliationResult;

public interface ReconcileWalletBalanceUseCase {

    WalletReconciliationResult reconcile(ReconcileWalletBalanceCommand command);
}
