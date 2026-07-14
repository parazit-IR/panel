package com.parazit.panel.application.port.in.wallet.payment;

import com.parazit.panel.application.wallet.payment.command.PayOrderWithWalletCommand;
import com.parazit.panel.application.wallet.payment.result.WalletOrderPaymentResult;

public interface PayOrderWithWalletUseCase {

    WalletOrderPaymentResult pay(PayOrderWithWalletCommand command);
}
