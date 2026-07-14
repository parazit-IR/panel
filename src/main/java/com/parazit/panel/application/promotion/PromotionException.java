package com.parazit.panel.application.promotion;

public class PromotionException extends RuntimeException {

    private final String messageKey;

    public PromotionException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }
}
