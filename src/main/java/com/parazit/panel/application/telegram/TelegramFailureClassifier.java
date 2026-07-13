package com.parazit.panel.application.telegram;

import org.springframework.stereotype.Component;

@Component
public class TelegramFailureClassifier {

    public TelegramFailureClassification classify(RuntimeException exception) {
        if (exception instanceof TelegramClientException telegram) {
            if (telegram.uncertain()) {
                return TelegramFailureClassification.UNKNOWN;
            }
            int status = telegram.statusCode();
            if (status == 429 || status == 500 || status == 502 || status == 503 || status == 504) {
                return TelegramFailureClassification.RETRYABLE;
            }
            if (status == 401 || status == 400 || status == 403 || status == 404) {
                return TelegramFailureClassification.NON_RETRYABLE;
            }
        }
        return TelegramFailureClassification.RETRYABLE;
    }
}
