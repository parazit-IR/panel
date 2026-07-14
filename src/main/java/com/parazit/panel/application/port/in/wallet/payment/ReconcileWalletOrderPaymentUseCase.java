package com.parazit.panel.application.port.in.wallet.payment;

import com.parazit.panel.application.wallet.payment.command.ReconcileWalletOrderPaymentCommand;
import com.parazit.panel.application.wallet.payment.result.WalletOrderPaymentReconciliationResult;

public interface ReconcileWalletOrderPaymentUseCase {

    WalletOrderPaymentReconciliationResult reconcile(ReconcileWalletOrderPaymentCommand command);
}
