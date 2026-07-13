package com.parazit.panel.application.telegram;

import com.parazit.panel.application.telegram.model.TelegramCommand;
import com.parazit.panel.config.properties.TelegramBotProperties;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TelegramCommandParser {

    private final TelegramBotProperties properties;

    public TelegramCommandParser(TelegramBotProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public TelegramCommand parse(String text) {
        if (text == null || text.isBlank()) {
            return TelegramCommand.UNKNOWN;
        }
        String normalized = text.trim();
        if (normalized.length() > properties.maxMessageLength()) {
            return TelegramCommand.UNKNOWN;
        }
        String firstToken = normalized.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        if (!firstToken.startsWith("/")) {
            return TelegramCommand.UNKNOWN;
        }
        String command = firstToken.substring(1);
        int mention = command.indexOf('@');
        if (mention >= 0) {
            String target = command.substring(mention + 1);
            if (properties.username() != null && !properties.username().equalsIgnoreCase(target)) {
                return TelegramCommand.UNKNOWN;
            }
            command = command.substring(0, mention);
        }
        return switch (command) {
            case "start" -> TelegramCommand.START;
            case "menu" -> TelegramCommand.MENU;
            case "subscriptions" -> TelegramCommand.MY_SUBSCRIPTIONS;
            case "help" -> TelegramCommand.HELP;
            default -> TelegramCommand.UNKNOWN;
        };
    }
}
