package com.parazit.panel.application.telegram.account;

import com.parazit.panel.application.port.in.customer.GetCustomerAccountSummaryUseCase;
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
import com.parazit.panel.application.telegram.service.TelegramCustomerServiceMetrics;
import com.parazit.panel.config.properties.TelegramBotProperties;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TelegramCustomerAccountHandler {

    private final GetCustomerAccountSummaryUseCase useCase;
    private final TelegramCustomerAccountSummaryFormatter formatter;
    private final TelegramKeyboardFactory keyboardFactory;
    private final TelegramMessageCatalog catalog;
    private final TelegramMenuLabelProvider labelProvider;
    private final TelegramBotProperties properties;
    private final TelegramCustomerServiceMetrics metrics;

    public TelegramCustomerAccountHandler(
            GetCustomerAccountSummaryUseCase useCase,
            TelegramCustomerAccountSummaryFormatter formatter,
            TelegramKeyboardFactory keyboardFactory,
            TelegramMessageCatalog catalog,
            TelegramMenuLabelProvider labelProvider,
            TelegramBotProperties properties,
            TelegramCustomerServiceMetrics metrics
    ) {
        this.useCase = Objects.requireNonNull(useCase, "useCase must not be null");
        this.formatter = Objects.requireNonNull(formatter, "formatter must not be null");
        this.keyboardFactory = Objects.requireNonNull(keyboardFactory, "keyboardFactory must not be null");
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.labelProvider = Objects.requireNonNull(labelProvider, "labelProvider must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
    }

    public TelegramResponsePlan handle(TelegramInteractionContext context) {
        String text = formatter.format(useCase.get(context.telegramUserId()), context.language());
        metrics.accountViewed("success");
        TelegramInlineKeyboard keyboard = keyboardFactory.rows(List.of(
                keyboardFactory.row(keyboardFactory.button(
                        catalog.text(context.language(), "telegram.main.my_services"),
                        TelegramCallbackAction.LIST_MY_SERVICES,
                        context.telegramUserId(),
                        1,
                        "",
                        context.receivedAt()
                )),
                keyboardFactory.row(
                        keyboardFactory.button(catalog.text(context.language(), "telegram.account.payments"), TelegramCallbackAction.SHOW_PAYMENTS, context.telegramUserId(), 1, "", context.receivedAt()),
                        keyboardFactory.button(catalog.text(context.language(), "telegram.account.settings"), TelegramCallbackAction.SHOW_NOTIFICATION_SETTINGS, context.telegramUserId(), 1, "", context.receivedAt())
                ),
                keyboardFactory.row(keyboardFactory.button(
                        labelProvider.label(context.language(), com.parazit.panel.application.telegram.navigation.TelegramNavigationAction.HOME),
                        TelegramCallbackAction.BACK_TO_MAIN,
                        context.telegramUserId(),
                        null,
                        null,
                        null,
                        context.receivedAt()
                ))
        ));
        return new TelegramResponsePlan(List.of(TelegramResponseAction.sendMessage(new SendTelegramMessageCommand(
                context.chatId(),
                text,
                TelegramParseMode.HTML,
                keyboard,
                properties.disableLinkPreview(),
                null
        ), false)), "telegram:account");
    }
}
