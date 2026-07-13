package com.parazit.panel.application.telegram.handler;

import com.parazit.panel.application.telegram.TelegramKeyboardFactory;
import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.application.telegram.command.SendTelegramMessageCommand;
import com.parazit.panel.application.telegram.model.TelegramCommand;
import com.parazit.panel.application.telegram.model.TelegramInteractionContext;
import com.parazit.panel.application.telegram.model.TelegramParseMode;
import com.parazit.panel.application.telegram.model.TelegramResponseAction;
import com.parazit.panel.application.telegram.model.TelegramResponsePlan;
import com.parazit.panel.config.properties.TelegramBotProperties;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class StartTelegramCommandHandler implements TelegramCommandHandler {

    private final TelegramMessageCatalog catalog;
    private final TelegramKeyboardFactory keyboardFactory;
    private final TelegramBotProperties properties;

    public StartTelegramCommandHandler(
            TelegramMessageCatalog catalog,
            TelegramKeyboardFactory keyboardFactory,
            TelegramBotProperties properties
    ) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.keyboardFactory = Objects.requireNonNull(keyboardFactory, "keyboardFactory must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public TelegramCommand command() {
        return TelegramCommand.START;
    }

    @Override
    public TelegramResponsePlan handle(TelegramInteractionContext context) {
        String text = catalog.text(context.language(), "welcome") + "\n\n" + catalog.text(context.language(), "main_menu_title");
        return new TelegramResponsePlan(List.of(TelegramResponseAction.sendMessage(
                new SendTelegramMessageCommand(
                        context.chatId(),
                        text,
                        TelegramParseMode.NONE,
                        keyboardFactory.mainMenu(context.telegramUserId(), context.receivedAt()),
                        properties.disableLinkPreview(),
                        null
                ),
                false
        )), "command:start");
    }
}
