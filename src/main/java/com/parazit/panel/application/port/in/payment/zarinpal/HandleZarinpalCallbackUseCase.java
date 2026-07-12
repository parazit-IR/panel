package com.parazit.panel.application.port.in.payment.zarinpal;

import com.parazit.panel.application.payment.zarinpal.command.HandleZarinpalCallbackCommand;
import com.parazit.panel.application.payment.zarinpal.result.HandleZarinpalCallbackResult;

public interface HandleZarinpalCallbackUseCase {

    HandleZarinpalCallbackResult handle(HandleZarinpalCallbackCommand command);
}
