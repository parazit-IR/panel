package com.parazit.panel.application.port.in.wallet.topup;

import com.parazit.panel.application.wallet.topup.command.CreateWalletTopUpPaymentCommand;
import com.parazit.panel.application.wallet.topup.result.WalletTopUpPaymentResult;

public interface CreateWalletTopUpPaymentUseCase {

    WalletTopUpPaymentResult create(CreateWalletTopUpPaymentCommand command);
}
