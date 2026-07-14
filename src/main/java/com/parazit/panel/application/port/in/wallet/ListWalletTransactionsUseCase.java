package com.parazit.panel.application.port.in.wallet;

import com.parazit.panel.application.wallet.command.ListWalletTransactionsCommand;
import com.parazit.panel.application.wallet.result.WalletTransactionPageResult;

public interface ListWalletTransactionsUseCase {

    WalletTransactionPageResult list(ListWalletTransactionsCommand command);
}
