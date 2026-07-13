package com.parazit.panel.application.telegram;

import java.time.Duration;

public class TelegramClientException extends RuntimeException {

    private final int statusCode;
    private final String failureCode;
    private final Duration retryAfter;
    private final boolean uncertain;

    public TelegramClientException(String message, int statusCode, String failureCode, Duration retryAfter, boolean uncertain) {
        super(message);
        this.statusCode = statusCode;
        this.failureCode = failureCode;
        this.retryAfter = retryAfter;
        this.uncertain = uncertain;
    }

    public int statusCode() {
        return statusCode;
    }

    public String failureCode() {
        return failureCode;
    }

    public Duration retryAfter() {
        return retryAfter;
    }

    public boolean uncertain() {
        return uncertain;
    }
}
