package com.parazit.panel.application.port.in.wallet.payment;

import com.parazit.panel.application.wallet.payment.command.GetWalletOrderPaymentPreviewCommand;
import com.parazit.panel.application.wallet.payment.result.WalletOrderPaymentPreviewResult;

public interface GetWalletOrderPaymentPreviewUseCase {

    WalletOrderPaymentPreviewResult preview(GetWalletOrderPaymentPreviewCommand command);
}
