package com.parazit.panel.application.telegram.support;

import com.parazit.panel.application.telegram.TelegramKeyboardFactory;
import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.application.telegram.command.SendTelegramMessageCommand;
import com.parazit.panel.application.telegram.menu.TelegramMenuLabelProvider;
import com.parazit.panel.application.telegram.model.TelegramCallbackAction;
import com.parazit.panel.application.telegram.model.TelegramInlineButton;
import com.parazit.panel.application.telegram.model.TelegramInlineKeyboard;
import com.parazit.panel.application.telegram.model.TelegramInlineKeyboardRow;
import com.parazit.panel.application.telegram.model.TelegramInteractionContext;
import com.parazit.panel.application.telegram.model.TelegramParseMode;
import com.parazit.panel.application.telegram.model.TelegramResponseAction;
import com.parazit.panel.application.telegram.model.TelegramResponsePlan;
import com.parazit.panel.application.telegram.navigation.TelegramNavigationAction;
import com.parazit.panel.config.properties.SupportProperties;
import com.parazit.panel.config.properties.TelegramBotProperties;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TelegramSupportMenuHandler {

    private static final Logger log = LoggerFactory.getLogger(TelegramSupportMenuHandler.class);

    private final SupportProperties supportProperties;
    private final TelegramSupportMessageFormatter formatter;
    private final TelegramSupportUrlFactory urlFactory;
    private final TelegramKeyboardFactory keyboardFactory;
    private final TelegramMenuLabelProvider labelProvider;
    private final TelegramMessageCatalog catalog;
    private final TelegramBotProperties botProperties;

    public TelegramSupportMenuHandler(
            SupportProperties supportProperties,
            TelegramSupportMessageFormatter formatter,
            TelegramSupportUrlFactory urlFactory,
            TelegramKeyboardFactory keyboardFactory,
            TelegramMenuLabelProvider labelProvider,
            TelegramMessageCatalog catalog,
            TelegramBotProperties botProperties
    ) {
        this.supportProperties = Objects.requireNonNull(supportProperties, "supportProperties must not be null");
        this.formatter = Objects.requireNonNull(formatter, "formatter must not be null");
        this.urlFactory = Objects.requireNonNull(urlFactory, "urlFactory must not be null");
        this.keyboardFactory = Objects.requireNonNull(keyboardFactory, "keyboardFactory must not be null");
        this.labelProvider = Objects.requireNonNull(labelProvider, "labelProvider must not be null");
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.botProperties = Objects.requireNonNull(botProperties, "botProperties must not be null");
    }

    public TelegramResponsePlan handle(TelegramInteractionContext context) {
        log.atInfo()
                .addKeyValue("updateId", context.updateId())
                .addKeyValue("telegramUserId", context.telegramUserId())
                .addKeyValue("locale", context.language())
                .log("Telegram support page viewed");
        return new TelegramResponsePlan(List.of(TelegramResponseAction.sendMessage(new SendTelegramMessageCommand(
                context.chatId(),
                formatter.format(context.language(), supportProperties),
                TelegramParseMode.HTML,
                keyboard(context),
                botProperties.disableLinkPreview(),
                null
        ), false)), "support");
    }

    private TelegramInlineKeyboard keyboard(TelegramInteractionContext context) {
        List<TelegramInlineKeyboardRow> rows = new ArrayList<>();
        if (supportProperties.enabled() && supportProperties.showFaq()) {
            rows.add(keyboardFactory.row(keyboardFactory.button(catalog.text(context.language(), "telegram.support.faq"), TelegramCallbackAction.SHOW_FAQ, context.telegramUserId(), 1, null, context.receivedAt())));
        }
        if (supportProperties.enabled() && supportProperties.showDirectMessage() && !supportProperties.telegramUsername().isBlank()) {
            URI url = urlFactory.create(supportProperties.telegramUsername());
            rows.add(keyboardFactory.row(TelegramInlineButton.url(catalog.text(context.language(), "telegram.support.direct_message"), url.toString())));
        }
        rows.add(keyboardFactory.row(
                keyboardFactory.button(labelProvider.label(context.language(), TelegramNavigationAction.BACK), TelegramCallbackAction.BACK_TO_MAIN, context.telegramUserId(), null, null, context.receivedAt()),
                keyboardFactory.button(labelProvider.label(context.language(), TelegramNavigationAction.HOME), TelegramCallbackAction.BACK_TO_MAIN, context.telegramUserId(), null, null, context.receivedAt())
        ));
        return keyboardFactory.rows(rows);
    }
}
