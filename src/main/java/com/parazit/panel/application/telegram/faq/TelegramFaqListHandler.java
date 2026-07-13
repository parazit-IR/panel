package com.parazit.panel.application.telegram.faq;

import com.parazit.panel.application.content.faq.FaqCatalog;
import com.parazit.panel.application.content.faq.FaqItem;
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
import com.parazit.panel.config.properties.TelegramBotProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TelegramFaqListHandler {

    private static final Logger log = LoggerFactory.getLogger(TelegramFaqListHandler.class);

    private final FaqCatalog faqCatalog;
    private final TelegramFaqMessageFormatter formatter;
    private final TelegramKeyboardFactory keyboardFactory;
    private final TelegramMenuLabelProvider labelProvider;
    private final TelegramMessageCatalog catalog;
    private final TelegramBotProperties properties;

    public TelegramFaqListHandler(
            FaqCatalog faqCatalog,
            TelegramFaqMessageFormatter formatter,
            TelegramKeyboardFactory keyboardFactory,
            TelegramMenuLabelProvider labelProvider,
            TelegramMessageCatalog catalog,
            TelegramBotProperties properties
    ) {
        this.faqCatalog = Objects.requireNonNull(faqCatalog, "faqCatalog must not be null");
        this.formatter = Objects.requireNonNull(formatter, "formatter must not be null");
        this.keyboardFactory = Objects.requireNonNull(keyboardFactory, "keyboardFactory must not be null");
        this.labelProvider = Objects.requireNonNull(labelProvider, "labelProvider must not be null");
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public TelegramResponsePlan handle(TelegramInteractionContext context, int requestedPage) {
        int totalPages = faqCatalog.totalPages();
        int page = Math.max(1, Math.min(requestedPage, totalPages));
        List<FaqItem> items = faqCatalog.page(page);
        log.atInfo()
                .addKeyValue("updateId", context.updateId())
                .addKeyValue("telegramUserId", context.telegramUserId())
                .addKeyValue("page", page)
                .addKeyValue("faqCount", items.size())
                .log("Telegram FAQ list viewed");
        String text = items.isEmpty() ? formatter.empty(context.language()) : formatter.list(context.language());
        return new TelegramResponsePlan(List.of(TelegramResponseAction.sendMessage(new SendTelegramMessageCommand(
                context.chatId(),
                text,
                TelegramParseMode.HTML,
                keyboard(context, items, page, totalPages),
                properties.disableLinkPreview(),
                null
        ), false)), "faq:list");
    }

    private TelegramInlineKeyboard keyboard(TelegramInteractionContext context, List<FaqItem> items, int page, int totalPages) {
        List<TelegramInlineKeyboardRow> rows = new ArrayList<>();
        for (FaqItem item : items) {
            rows.add(keyboardFactory.row(keyboardFactory.button(
                    item.question(),
                    TelegramCallbackAction.SHOW_FAQ_ITEM,
                    context.telegramUserId(),
                    page,
                    item.id(),
                    context.receivedAt()
            )));
        }
        List<TelegramInlineButton> paging = new ArrayList<>();
        if (page > 1) {
            paging.add(keyboardFactory.button(catalog.text(context.language(), "telegram.pagination.previous"), TelegramCallbackAction.SHOW_FAQ_PAGE, context.telegramUserId(), page - 1, null, context.receivedAt()));
        }
        if (page < totalPages) {
            paging.add(keyboardFactory.button(catalog.text(context.language(), "telegram.pagination.next"), TelegramCallbackAction.SHOW_FAQ_PAGE, context.telegramUserId(), page + 1, null, context.receivedAt()));
        }
        if (!paging.isEmpty()) {
            rows.add(new TelegramInlineKeyboardRow(paging));
        }
        rows.add(keyboardFactory.row(
                keyboardFactory.button(labelProvider.label(context.language(), TelegramNavigationAction.BACK), TelegramCallbackAction.BACK_TO_SUPPORT, context.telegramUserId(), null, null, context.receivedAt()),
                keyboardFactory.button(labelProvider.label(context.language(), TelegramNavigationAction.HOME), TelegramCallbackAction.BACK_TO_MAIN, context.telegramUserId(), null, null, context.receivedAt())
        ));
        return keyboardFactory.rows(rows);
    }
}
