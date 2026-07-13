package com.parazit.panel.application.renewal;

public class RenewalFlowException extends RuntimeException {

    private final String messageKey;

    public RenewalFlowException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }
}
