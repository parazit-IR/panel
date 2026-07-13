package com.parazit.panel.application.telegram.faq;

import com.parazit.panel.application.content.faq.FaqCatalog;
import com.parazit.panel.application.content.faq.FaqItem;
import com.parazit.panel.application.telegram.TelegramKeyboardFactory;
import com.parazit.panel.application.telegram.TelegramMessageChunker;
import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.application.telegram.command.SendTelegramMessageCommand;
import com.parazit.panel.application.telegram.menu.TelegramMenuLabelProvider;
import com.parazit.panel.application.telegram.model.TelegramCallbackAction;
import com.parazit.panel.application.telegram.model.TelegramInlineKeyboard;
import com.parazit.panel.application.telegram.model.TelegramInlineKeyboardRow;
import com.parazit.panel.application.telegram.model.TelegramInteractionContext;
import com.parazit.panel.application.telegram.model.TelegramParseMode;
import com.parazit.panel.application.telegram.model.TelegramResponseAction;
import com.parazit.panel.application.telegram.model.TelegramResponsePlan;
import com.parazit.panel.application.telegram.navigation.TelegramNavigationAction;
import com.parazit.panel.config.properties.TelegramBotProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TelegramFaqDetailHandler {

    private static final Logger log = LoggerFactory.getLogger(TelegramFaqDetailHandler.class);

    private final FaqCatalog faqCatalog;
    private final TelegramFaqMessageFormatter formatter;
    private final TelegramKeyboardFactory keyboardFactory;
    private final TelegramMenuLabelProvider labelProvider;
    private final TelegramMessageCatalog catalog;
    private final TelegramMessageChunker chunker;
    private final TelegramBotProperties properties;

    public TelegramFaqDetailHandler(
            FaqCatalog faqCatalog,
            TelegramFaqMessageFormatter formatter,
            TelegramKeyboardFactory keyboardFactory,
            TelegramMenuLabelProvider labelProvider,
            TelegramMessageCatalog catalog,
            TelegramMessageChunker chunker,
            TelegramBotProperties properties
    ) {
        this.faqCatalog = Objects.requireNonNull(faqCatalog, "faqCatalog must not be null");
        this.formatter = Objects.requireNonNull(formatter, "formatter must not be null");
        this.keyboardFactory = Objects.requireNonNull(keyboardFactory, "keyboardFactory must not be null");
        this.labelProvider = Objects.requireNonNull(labelProvider, "labelProvider must not be null");
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.chunker = Objects.requireNonNull(chunker, "chunker must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public TelegramResponsePlan handle(TelegramInteractionContext context, String faqId, int page) {
        return faqCatalog.findEnabled(faqId)
                .map(item -> detail(context, item, page))
                .orElseGet(() -> unavailable(context, page));
    }

    private TelegramResponsePlan detail(TelegramInteractionContext context, FaqItem item, int page) {
        log.atInfo()
                .addKeyValue("updateId", context.updateId())
                .addKeyValue("telegramUserId", context.telegramUserId())
                .addKeyValue("faqId", item.id())
                .log("Telegram FAQ item viewed");
        List<String> chunks = chunker.split(formatter.detail(context.language(), item));
        List<TelegramResponseAction> actions = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            TelegramInlineKeyboard keyboard = i == chunks.size() - 1 ? keyboard(context, page) : TelegramInlineKeyboard.empty();
            actions.add(TelegramResponseAction.sendMessage(new SendTelegramMessageCommand(
                    context.chatId(),
                    chunks.get(i),
                    TelegramParseMode.HTML,
                    keyboard,
                    properties.disableLinkPreview(),
                    null
            ), false));
        }
        return new TelegramResponsePlan(actions, "faq:detail");
    }

    private TelegramResponsePlan unavailable(TelegramInteractionContext context, int page) {
        return new TelegramResponsePlan(List.of(TelegramResponseAction.sendMessage(new SendTelegramMessageCommand(
                context.chatId(),
                catalog.text(context.language(), "telegram.faq.empty"),
                TelegramParseMode.NONE,
                keyboard(context, page),
                properties.disableLinkPreview(),
                null
        ), false)), "faq:missing");
    }

    private TelegramInlineKeyboard keyboard(TelegramInteractionContext context, int page) {
        List<TelegramInlineKeyboardRow> rows = List.of(keyboardFactory.row(
                keyboardFactory.button(catalog.text(context.language(), "telegram.faq.back"), TelegramCallbackAction.BACK_TO_FAQ, context.telegramUserId(), page, null, context.receivedAt()),
                keyboardFactory.button(catalog.text(context.language(), "telegram.support.title"), TelegramCallbackAction.BACK_TO_SUPPORT, context.telegramUserId(), null, null, context.receivedAt()),
                keyboardFactory.button(labelProvider.label(context.language(), TelegramNavigationAction.HOME), TelegramCallbackAction.BACK_TO_MAIN, context.telegramUserId(), null, null, context.receivedAt())
        ));
        return keyboardFactory.rows(rows);
    }
}
