package com.parazit.panel.application.telegram.handler;

import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.application.telegram.command.SendTelegramMessageCommand;
import com.parazit.panel.application.telegram.menu.TelegramMainReplyKeyboardFactory;
import com.parazit.panel.application.telegram.model.TelegramInlineKeyboard;
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
public class HelpTelegramCommandHandler implements TelegramCommandHandler {

    private final TelegramMessageCatalog catalog;
    private final TelegramMainReplyKeyboardFactory replyKeyboardFactory;
    private final TelegramBotProperties properties;

    public HelpTelegramCommandHandler(
            TelegramMessageCatalog catalog,
            TelegramMainReplyKeyboardFactory replyKeyboardFactory,
            TelegramBotProperties properties
    ) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.replyKeyboardFactory = Objects.requireNonNull(replyKeyboardFactory, "replyKeyboardFactory must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public TelegramCommand command() {
        return TelegramCommand.HELP;
    }

    @Override
    public TelegramResponsePlan handle(TelegramInteractionContext context) {
        return new TelegramResponsePlan(List.of(TelegramResponseAction.sendMessage(
                new SendTelegramMessageCommand(
                        context.chatId(),
                        catalog.text(context.language(), "help"),
                        TelegramParseMode.NONE,
                        TelegramInlineKeyboard.empty(),
                        replyKeyboardFactory.mainKeyboard(context.language()),
                        properties.disableLinkPreview(),
                        null
                ),
                false
        )), "command:help");
    }
}
