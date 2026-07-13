package com.parazit.panel.application.telegram.service;

import com.parazit.panel.application.customer.result.CustomerServicePageResult;
import com.parazit.panel.application.customer.result.CustomerServiceSort;
import com.parazit.panel.application.customer.result.CustomerServiceStatusFilter;
import com.parazit.panel.application.port.in.customer.ListCustomerServicesUseCase;
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
public class TelegramMyServicesHandler {

    private final ListCustomerServicesUseCase useCase;
    private final TelegramMyServicesMessageFormatter formatter;
    private final TelegramKeyboardFactory keyboardFactory;
    private final TelegramMessageCatalog catalog;
    private final TelegramMenuLabelProvider labelProvider;
    private final CustomerServicesTelegramProperties serviceProperties;
    private final TelegramBotProperties telegramProperties;
    private final TelegramCustomerServiceMetrics metrics;

    public TelegramMyServicesHandler(
            ListCustomerServicesUseCase useCase,
            TelegramMyServicesMessageFormatter formatter,
            TelegramKeyboardFactory keyboardFactory,
            TelegramMessageCatalog catalog,
            TelegramMenuLabelProvider labelProvider,
            CustomerServicesTelegramProperties serviceProperties,
            TelegramBotProperties telegramProperties,
            TelegramCustomerServiceMetrics metrics
    ) {
        this.useCase = Objects.requireNonNull(useCase, "useCase must not be null");
        this.formatter = Objects.requireNonNull(formatter, "formatter must not be null");
        this.keyboardFactory = Objects.requireNonNull(keyboardFactory, "keyboardFactory must not be null");
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.labelProvider = Objects.requireNonNull(labelProvider, "labelProvider must not be null");
        this.serviceProperties = Objects.requireNonNull(serviceProperties, "serviceProperties must not be null");
        this.telegramProperties = Objects.requireNonNull(telegramProperties, "telegramProperties must not be null");
        this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
    }

    public TelegramResponsePlan handle(TelegramInteractionContext context, int page) {
        int zeroBasedPage = Math.max(page - 1, 0);
        CustomerServicePageResult result = useCase.list(
                context.telegramUserId(),
                zeroBasedPage,
                serviceProperties.pageSize(),
                CustomerServiceSort.STATUS_THEN_NEWEST,
                CustomerServiceStatusFilter.ALL
        );
        String text = result.items().isEmpty()
                ? catalog.text(context.language(), "telegram.services.empty")
                : formatter.listText(context.language());
        metrics.servicesViewed(result.items().isEmpty() ? "empty" : "success");
        return new TelegramResponsePlan(List.of(TelegramResponseAction.sendMessage(new SendTelegramMessageCommand(
                context.chatId(),
                text,
                TelegramParseMode.HTML,
                keyboard(context, result),
                telegramProperties.disableLinkPreview(),
                null
        ), false)), "telegram:my-services");
    }

    private TelegramInlineKeyboard keyboard(TelegramInteractionContext context, CustomerServicePageResult page) {
        List<TelegramInlineKeyboardRow> rows = new ArrayList<>();
        for (var service : page.items()) {
            rows.add(keyboardFactory.row(keyboardFactory.button(
                    formatter.buttonLabel(service, context.language()),
                    TelegramCallbackAction.SHOW_SERVICE_DETAILS,
                    context.telegramUserId(),
                    service.subscriptionId(),
                    page.page() + 1,
                    null,
                    context.receivedAt()
            )));
        }
        if (page.hasPrevious() || page.hasNext()) {
            List<com.parazit.panel.application.telegram.model.TelegramInlineButton> buttons = new ArrayList<>();
            if (page.hasPrevious()) {
                buttons.add(keyboardFactory.button(catalog.text(context.language(), "telegram.services.previous"), TelegramCallbackAction.MY_SERVICES_PAGE, context.telegramUserId(), page.page(), "", context.receivedAt()));
            }
            if (page.hasNext()) {
                buttons.add(keyboardFactory.button(catalog.text(context.language(), "telegram.services.next"), TelegramCallbackAction.MY_SERVICES_PAGE, context.telegramUserId(), page.page() + 2, "", context.receivedAt()));
            }
            rows.add(new TelegramInlineKeyboardRow(buttons));
        }
        rows.add(keyboardFactory.row(keyboardFactory.button(catalog.text(context.language(), "telegram.services.search"), TelegramCallbackAction.SEARCH_MY_SERVICES, context.telegramUserId(), page.page() + 1, "", context.receivedAt())));
        rows.add(keyboardFactory.row(keyboardFactory.button(catalog.text(context.language(), "telegram.account.title_short"), TelegramCallbackAction.SHOW_ACCOUNT, context.telegramUserId(), 1, "", context.receivedAt())));
        rows.add(keyboardFactory.row(keyboardFactory.button(
                labelProvider.label(context.language(), com.parazit.panel.application.telegram.navigation.TelegramNavigationAction.HOME),
                TelegramCallbackAction.BACK_TO_MAIN,
                context.telegramUserId(),
                null,
                null,
                null,
                context.receivedAt()
        )));
        return keyboardFactory.rows(rows);
    }
}
