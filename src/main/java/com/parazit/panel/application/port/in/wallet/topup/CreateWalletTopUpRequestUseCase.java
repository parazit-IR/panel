package com.parazit.panel.application.port.in.wallet.topup;

import com.parazit.panel.application.wallet.topup.command.CreateWalletTopUpRequestCommand;
import com.parazit.panel.application.wallet.topup.result.WalletTopUpRequestResult;

public interface CreateWalletTopUpRequestUseCase {

    WalletTopUpRequestResult create(CreateWalletTopUpRequestCommand command);
}
