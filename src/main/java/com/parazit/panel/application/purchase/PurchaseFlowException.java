package com.parazit.panel.application.purchase;

public class PurchaseFlowException extends RuntimeException {

    private final String messageKey;

    public PurchaseFlowException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }
}
