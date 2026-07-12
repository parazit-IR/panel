package com.parazit.panel.application.payment.zarinpal.command;

public record HandleZarinpalCallbackCommand(
        String authority,
        String callbackStatus
) {
}
