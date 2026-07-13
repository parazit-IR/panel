package com.parazit.panel.application.telegram.menu;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TelegramMenuMetrics {

    private final MeterRegistry meterRegistry;

    public TelegramMenuMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
    }

    public void recordMenuAction(TelegramMainMenuAction action, String result) {
        meterRegistry.counter(
                "telegram_menu_actions_total",
                "action", action == null ? "unknown" : action.name().toLowerCase(),
                "result", result == null ? "unknown" : result
        ).increment();
    }

    public void recordUnknownMessage(String chatType) {
        meterRegistry.counter(
                "telegram_unknown_messages_total",
                "chat_type", chatType == null ? "unknown" : chatType.toLowerCase()
        ).increment();
    }

    public void recordNavigation(String from, String to) {
        meterRegistry.counter(
                "telegram_navigation_total",
                "from", from == null ? "unknown" : from.toLowerCase(),
                "to", to == null ? "unknown" : to.toLowerCase()
        ).increment();
    }
}
