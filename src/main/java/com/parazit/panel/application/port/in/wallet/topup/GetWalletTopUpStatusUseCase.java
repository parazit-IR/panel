package com.parazit.panel.application.port.in.wallet.topup;

import com.parazit.panel.application.wallet.topup.command.GetWalletTopUpStatusCommand;
import com.parazit.panel.application.wallet.topup.result.WalletTopUpStatusResult;

public interface GetWalletTopUpStatusUseCase {

    WalletTopUpStatusResult get(GetWalletTopUpStatusCommand command);
}
