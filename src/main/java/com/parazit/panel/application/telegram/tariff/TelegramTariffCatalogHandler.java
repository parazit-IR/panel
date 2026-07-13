package com.parazit.panel.application.telegram.tariff;

import com.parazit.panel.application.plan.catalog.query.ListAvailablePlansQuery;
import com.parazit.panel.application.plan.catalog.result.AvailablePlanResult;
import com.parazit.panel.application.port.in.plan.catalog.ListAvailablePlansUseCase;
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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TelegramTariffCatalogHandler {

    private static final Logger log = LoggerFactory.getLogger(TelegramTariffCatalogHandler.class);
    private static final int PAGE_SIZE = 6;

    private final ListAvailablePlansUseCase listAvailablePlansUseCase;
    private final TelegramTariffMessageFormatter formatter;
    private final TelegramKeyboardFactory keyboardFactory;
    private final TelegramMenuLabelProvider labelProvider;
    private final TelegramMessageCatalog catalog;
    private final TelegramBotProperties properties;

    public TelegramTariffCatalogHandler(
            ListAvailablePlansUseCase listAvailablePlansUseCase,
            TelegramTariffMessageFormatter formatter,
            TelegramKeyboardFactory keyboardFactory,
            TelegramMenuLabelProvider labelProvider,
            TelegramMessageCatalog catalog,
            TelegramBotProperties properties
    ) {
        this.listAvailablePlansUseCase = Objects.requireNonNull(listAvailablePlansUseCase, "listAvailablePlansUseCase must not be null");
        this.formatter = Objects.requireNonNull(formatter, "formatter must not be null");
        this.keyboardFactory = Objects.requireNonNull(keyboardFactory, "keyboardFactory must not be null");
        this.labelProvider = Objects.requireNonNull(labelProvider, "labelProvider must not be null");
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public TelegramResponsePlan handle(TelegramInteractionContext context, int requestedPage) {
        TelegramTariffPage page = page(requestedPage);
        log.atInfo()
                .addKeyValue("updateId", context.updateId())
                .addKeyValue("telegramUserId", context.telegramUserId())
                .addKeyValue("page", page.page())
                .addKeyValue("locale", context.language())
                .log("Telegram tariffs viewed");
        return new TelegramResponsePlan(List.of(TelegramResponseAction.sendMessage(
                new SendTelegramMessageCommand(
                        context.chatId(),
                        formatter.format(context.language(), page),
                        TelegramParseMode.HTML,
                        keyboard(context, page),
                        properties.disableLinkPreview(),
                        null
                ),
                false
        )), "tariffs");
    }

    private TelegramTariffPage page(int requestedPage) {
        List<AvailablePlanResult> plans = listAvailablePlansUseCase.list(new ListAvailablePlansQuery(null))
                .stream()
                .sorted(Comparator.comparingInt(AvailablePlanResult::displayOrder).thenComparing(AvailablePlanResult::code))
                .toList();
        int totalPages = Math.max(1, (int) Math.ceil(plans.size() / (double) PAGE_SIZE));
        int safePage = Math.max(1, Math.min(requestedPage, totalPages));
        int from = Math.min((safePage - 1) * PAGE_SIZE, plans.size());
        int to = Math.min(from + PAGE_SIZE, plans.size());
        return new TelegramTariffPage(plans.subList(from, to), safePage, totalPages, safePage > 1, safePage < totalPages);
    }

    private TelegramInlineKeyboard keyboard(TelegramInteractionContext context, TelegramTariffPage page) {
        List<TelegramInlineKeyboardRow> rows = new ArrayList<>();
        List<TelegramInlineButton> paging = new ArrayList<>();
        if (page.hasPrevious()) {
            paging.add(keyboardFactory.button(catalog.text(context.language(), "telegram.pagination.previous"), TelegramCallbackAction.SHOW_TARIFF_PAGE, context.telegramUserId(), page.page() - 1, null, context.receivedAt()));
        }
        if (page.hasNext()) {
            paging.add(keyboardFactory.button(catalog.text(context.language(), "telegram.pagination.next"), TelegramCallbackAction.SHOW_TARIFF_PAGE, context.telegramUserId(), page.page() + 1, null, context.receivedAt()));
        }
        if (!paging.isEmpty()) {
            rows.add(new TelegramInlineKeyboardRow(paging));
        }
        rows.add(keyboardFactory.row(keyboardFactory.button(catalog.text(context.language(), "telegram.tariffs.buy"), TelegramCallbackAction.BUY_SUBSCRIPTION, context.telegramUserId(), null, null, context.receivedAt())));
        rows.add(keyboardFactory.row(
                keyboardFactory.button(labelProvider.label(context.language(), TelegramNavigationAction.BACK), TelegramCallbackAction.BACK_TO_MAIN, context.telegramUserId(), null, null, context.receivedAt()),
                keyboardFactory.button(labelProvider.label(context.language(), TelegramNavigationAction.HOME), TelegramCallbackAction.BACK_TO_MAIN, context.telegramUserId(), null, null, context.receivedAt())
        ));
        return keyboardFactory.rows(rows);
    }
}
