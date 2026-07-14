package com.parazit.panel.application.port.in.wallet;

import com.parazit.panel.application.wallet.command.DebitWalletCommand;
import com.parazit.panel.application.wallet.result.WalletOperationResult;

public interface DebitWalletUseCase {

    WalletOperationResult debit(DebitWalletCommand command);
}
