package com.parazit.panel.application.telegram;

import com.parazit.panel.config.properties.TelegramBotProperties;
import com.parazit.panel.config.properties.TelegramUpdateMode;
import java.time.Duration;
import java.util.Set;

public final class TelegramTestProperties {

    private TelegramTestProperties() {
    }

    public static TelegramBotProperties properties() {
        return new TelegramBotProperties(
                true,
                "123456:test-token",
                "PanelBot",
                TelegramUpdateMode.LONG_POLLING,
                Duration.ofSeconds(30),
                Duration.ofSeconds(2),
                50,
                Set.of("message", "callback_query"),
                4096,
                64,
                Duration.ofSeconds(5),
                Duration.ofSeconds(35),
                5,
                Duration.ofSeconds(1),
                Duration.ofSeconds(30),
                2.0,
                true,
                true,
                "",
                "/telegram/webhook",
                "01234567890123456789012345678901",
                Duration.ofMinutes(15)
        );
    }
}
