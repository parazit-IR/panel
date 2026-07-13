package com.parazit.panel.application.telegram;

import com.parazit.panel.application.telegram.handler.TelegramCommandHandler;
import com.parazit.panel.application.telegram.model.TelegramCommand;
import com.parazit.panel.application.telegram.model.TelegramInteractionContext;
import com.parazit.panel.application.telegram.model.TelegramResponsePlan;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TelegramCommandRouter {

    private final Map<TelegramCommand, TelegramCommandHandler> handlers;

    public TelegramCommandRouter(List<TelegramCommandHandler> handlers) {
        Objects.requireNonNull(handlers, "handlers must not be null");
        Map<TelegramCommand, TelegramCommandHandler> mapped = new EnumMap<>(TelegramCommand.class);
        for (TelegramCommandHandler handler : handlers) {
            TelegramCommandHandler previous = mapped.put(handler.command(), handler);
            if (previous != null) {
                throw new IllegalStateException("Duplicate Telegram command handler for " + handler.command());
            }
        }
        this.handlers = Map.copyOf(mapped);
    }

    public TelegramResponsePlan route(TelegramCommand command, TelegramInteractionContext context) {
        TelegramCommand effective = command == null ? TelegramCommand.UNKNOWN : command;
        TelegramCommandHandler handler = handlers.getOrDefault(effective, handlers.get(TelegramCommand.UNKNOWN));
        if (handler == null) {
            return TelegramResponsePlan.empty("command:missing");
        }
        return handler.handle(context);
    }
}
