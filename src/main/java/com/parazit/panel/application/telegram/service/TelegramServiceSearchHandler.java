package com.parazit.panel.application.telegram.service;

import com.parazit.panel.application.customer.result.CustomerServiceSummaryResult;
import com.parazit.panel.application.port.in.customer.SearchCustomerServicesUseCase;
import com.parazit.panel.application.telegram.TelegramKeyboardFactory;
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
import com.parazit.panel.config.properties.CustomerServicesTelegramProperties;
import com.parazit.panel.config.properties.TelegramBotProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TelegramServiceSearchHandler {

    private final SearchCustomerServicesUseCase searchUseCase;
    private final TelegramServiceSearchSessionStore sessionStore;
    private final TelegramMyServicesMessageFormatter formatter;
    private final TelegramKeyboardFactory keyboardFactory;
    private final TelegramMessageCatalog catalog;
    private final TelegramMenuLabelProvider labelProvider;
    private final CustomerServicesTelegramProperties serviceProperties;
    private final TelegramBotProperties telegramProperties;
    private final TelegramCustomerServiceMetrics metrics;

    public TelegramServiceSearchHandler(
            SearchCustomerServicesUseCase searchUseCase,
            TelegramServiceSearchSessionStore sessionStore,
            TelegramMyServicesMessageFormatter formatter,
            TelegramKeyboardFactory keyboardFactory,
            TelegramMessageCatalog catalog,
            TelegramMenuLabelProvider labelProvider,
            CustomerServicesTelegramProperties serviceProperties,
            TelegramBotProperties telegramProperties,
            TelegramCustomerServiceMetrics metrics
    ) {
        this.searchUseCase = Objects.requireNonNull(searchUseCase, "searchUseCase must not be null");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore must not be null");
        this.formatter = Objects.requireNonNull(formatter, "formatter must not be null");
        this.keyboardFactory = Objects.requireNonNull(keyboardFactory, "keyboardFactory must not be null");
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.labelProvider = Objects.requireNonNull(labelProvider, "labelProvider must not be null");
        this.serviceProperties = Objects.requireNonNull(serviceProperties, "serviceProperties must not be null");
        this.telegramProperties = Objects.requireNonNull(telegramProperties, "telegramProperties must not be null");
        this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
    }

    public TelegramResponsePlan begin(TelegramInteractionContext context, int returnPage) {
        sessionStore.start(context.telegramUserId(), Math.max(1, returnPage), context.receivedAt().plus(serviceProperties.searchConversationTtl()));
        metrics.serviceSearch("started");
        return new TelegramResponsePlan(List.of(TelegramResponseAction.sendMessage(new SendTelegramMessageCommand(
                context.chatId(),
                catalog.text(context.language(), "telegram.services.search_prompt"),
                TelegramParseMode.NONE,
                backHomeKeyboard(context, returnPage),
                telegramProperties.disableLinkPreview(),
                null
        ), false)), "telegram:service-search-begin");
    }

    public java.util.Optional<TelegramResponsePlan> handleIfAwaiting(TelegramInteractionContext context, String text) {
        if (sessionStore.expired(context.telegramUserId(), context.receivedAt())) {
            metrics.serviceSearch("expired");
            return java.util.Optional.of(message(context, catalog.text(context.language(), "telegram.services.search_expired"), backHomeKeyboard(context, 1), "telegram:service-search-expired"));
        }
        return sessionStore.active(context.telegramUserId(), context.receivedAt())
                .map(session -> {
                    sessionStore.clear(context.telegramUserId());
                    if (text == null || text.isBlank()) {
                        return message(context, catalog.text(context.language(), "telegram.services.search_prompt"), backHomeKeyboard(context, session.returnPage()), "telegram:service-search-empty");
                    }
                    List<CustomerServiceSummaryResult> results;
                    try {
                        results = searchUseCase.search(context.telegramUserId(), text, serviceProperties.searchResultLimit());
                    } catch (IllegalArgumentException exception) {
                        metrics.serviceSearch("invalid");
                        return message(context, catalog.text(context.language(), "telegram.services.search_too_short"), backHomeKeyboard(context, session.returnPage()), "telegram:service-search-invalid");
                    }
                    if (results.isEmpty()) {
                        metrics.serviceSearch("empty");
                        return message(context, catalog.text(context.language(), "telegram.services.search_no_result"), backHomeKeyboard(context, session.returnPage()), "telegram:service-search-empty-result");
                    }
                    metrics.serviceSearch("success");
                    return message(context, catalog.text(context.language(), "telegram.services.search_results"), searchKeyboard(context, results, session.returnPage()), "telegram:service-search-results");
                });
    }

    public void clear(long telegramUserId) {
        sessionStore.clear(telegramUserId);
    }

    private TelegramResponsePlan message(TelegramInteractionContext context, String text, TelegramInlineKeyboard keyboard, String key) {
        return new TelegramResponsePlan(List.of(TelegramResponseAction.sendMessage(new SendTelegramMessageCommand(
                context.chatId(),
                text,
                TelegramParseMode.HTML,
                keyboard,
                telegramProperties.disableLinkPreview(),
                null
        ), false)), key);
    }

    private TelegramInlineKeyboard searchKeyboard(TelegramInteractionContext context, List<CustomerServiceSummaryResult> results, int returnPage) {
        List<TelegramInlineKeyboardRow> rows = new ArrayList<>();
        for (CustomerServiceSummaryResult service : results) {
            rows.add(keyboardFactory.row(keyboardFactory.button(
                    formatter.buttonLabel(service, context.language()),
                    TelegramCallbackAction.SHOW_SERVICE_DETAILS,
                    context.telegramUserId(),
                    service.subscriptionId(),
                    returnPage,
                    null,
                    context.receivedAt()
            )));
        }
        rows.add(navRow(context, returnPage));
        return keyboardFactory.rows(rows);
    }

    private TelegramInlineKeyboard backHomeKeyboard(TelegramInteractionContext context, int returnPage) {
        return keyboardFactory.rows(List.of(navRow(context, returnPage)));
    }

    private TelegramInlineKeyboardRow navRow(TelegramInteractionContext context, int returnPage) {
        return keyboardFactory.row(
                keyboardFactory.button(labelProvider.label(context.language(), com.parazit.panel.application.telegram.navigation.TelegramNavigationAction.BACK), TelegramCallbackAction.BACK_TO_MY_SERVICES, context.telegramUserId(), Math.max(1, returnPage), "", context.receivedAt()),
                keyboardFactory.button(labelProvider.label(context.language(), com.parazit.panel.application.telegram.navigation.TelegramNavigationAction.HOME), TelegramCallbackAction.BACK_TO_MAIN, context.telegramUserId(), null, null, null, context.receivedAt())
        );
    }
}
