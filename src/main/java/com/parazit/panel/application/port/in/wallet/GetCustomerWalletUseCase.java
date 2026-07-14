package com.parazit.panel.application.port.in.wallet;

import com.parazit.panel.application.wallet.command.GetCustomerWalletCommand;
import com.parazit.panel.application.wallet.result.CustomerWalletResult;

public interface GetCustomerWalletUseCase {

    CustomerWalletResult get(GetCustomerWalletCommand command);
}
