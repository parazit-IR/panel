package com.parazit.panel.application.port.in.wallet;

import com.parazit.panel.application.wallet.command.CreditWalletCommand;
import com.parazit.panel.application.wallet.result.WalletOperationResult;

public interface CreditWalletUseCase {

    WalletOperationResult credit(CreditWalletCommand command);
}
