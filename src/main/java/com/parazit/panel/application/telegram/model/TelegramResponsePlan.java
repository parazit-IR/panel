package com.parazit.panel.application.telegram.model;

import java.util.List;

public record TelegramResponsePlan(List<TelegramResponseAction> actions, String handlerKey) {

    public TelegramResponsePlan {
        actions = actions == null ? List.of() : List.copyOf(actions);
        handlerKey = handlerKey == null || handlerKey.isBlank() ? "unknown" : handlerKey.trim();
        if (actions.size() > 8) {
            throw new IllegalArgumentException("Telegram response plan has too many actions");
        }
    }

    public static TelegramResponsePlan empty(String handlerKey) {
        return new TelegramResponsePlan(List.of(), handlerKey);
    }
}
