package com.parazit.panel.application.port.in.wallet.topup;

import com.parazit.panel.application.wallet.topup.command.HandleApprovedWalletTopUpCommand;
import com.parazit.panel.application.wallet.topup.result.WalletTopUpApprovalResult;

public interface HandleApprovedWalletTopUpUseCase {

    WalletTopUpApprovalResult handle(HandleApprovedWalletTopUpCommand command);
}
