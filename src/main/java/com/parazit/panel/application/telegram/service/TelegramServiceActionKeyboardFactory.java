package com.parazit.panel.application.telegram.service;

import com.parazit.panel.application.customer.result.CustomerServiceDetailsResult;
import com.parazit.panel.application.telegram.TelegramKeyboardFactory;
import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.application.telegram.menu.TelegramMenuLabelProvider;
import com.parazit.panel.application.telegram.model.TelegramCallbackAction;
import com.parazit.panel.application.telegram.model.TelegramInlineKeyboard;
import com.parazit.panel.application.telegram.model.TelegramInlineKeyboardRow;
import com.parazit.panel.application.telegram.model.TelegramInteractionContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TelegramServiceActionKeyboardFactory {

    private final TelegramKeyboardFactory keyboardFactory;
    private final TelegramMessageCatalog catalog;
    private final TelegramMenuLabelProvider labelProvider;

    public TelegramServiceActionKeyboardFactory(
            TelegramKeyboardFactory keyboardFactory,
            TelegramMessageCatalog catalog,
            TelegramMenuLabelProvider labelProvider
    ) {
        this.keyboardFactory = Objects.requireNonNull(keyboardFactory, "keyboardFactory must not be null");
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.labelProvider = Objects.requireNonNull(labelProvider, "labelProvider must not be null");
    }

    public TelegramInlineKeyboard serviceDetails(TelegramInteractionContext context, CustomerServiceDetailsResult service, int backPage) {
        List<TelegramInlineKeyboardRow> rows = new ArrayList<>();
        if (service.contentAvailable()) {
            rows.add(keyboardFactory.row(keyboardFactory.button(
                    catalog.text(context.language(), "telegram.service.subscription_link"),
                    TelegramCallbackAction.SHOW_SUBSCRIPTION_LINK,
                    context.telegramUserId(),
                    service.subscriptionId(),
                    null,
                    null,
                    context.receivedAt()
            )));
        }
        List<com.parazit.panel.application.telegram.model.TelegramInlineButton> configButtons = new ArrayList<>();
        if (service.qrAvailable()) {
            configButtons.add(keyboardFactory.button(catalog.text(context.language(), "telegram.service.qr"), TelegramCallbackAction.SHOW_SUBSCRIPTION_QR, context.telegramUserId(), service.subscriptionId(), 1, null, context.receivedAt()));
        }
        if (service.vlessAvailable()) {
            configButtons.add(keyboardFactory.button(catalog.text(context.language(), "telegram.service.vless"), TelegramCallbackAction.SHOW_VLESS_CONFIG, context.telegramUserId(), service.subscriptionId(), 1, null, context.receivedAt()));
        }
        if (!configButtons.isEmpty()) {
            rows.add(new TelegramInlineKeyboardRow(configButtons));
        }
        if (service.refreshAvailable()) {
            rows.add(keyboardFactory.row(keyboardFactory.button(catalog.text(context.language(), "telegram.service.refresh"), TelegramCallbackAction.REFRESH_SERVICE_STATUS, context.telegramUserId(), service.subscriptionId(), backPage, null, context.receivedAt())));
        }
        rows.add(keyboardFactory.row(
                keyboardFactory.button(catalog.text(context.language(), "telegram.service.renewal"), TelegramCallbackAction.REQUEST_RENEWAL, context.telegramUserId(), service.subscriptionId(), null, null, context.receivedAt()),
                keyboardFactory.button(catalog.text(context.language(), "telegram.main.tutorials"), TelegramCallbackAction.SHOW_TUTORIALS, context.telegramUserId(), null, null, null, context.receivedAt())
        ));
        rows.add(keyboardFactory.row(
                keyboardFactory.button(labelProvider.label(context.language(), com.parazit.panel.application.telegram.navigation.TelegramNavigationAction.BACK), TelegramCallbackAction.BACK_TO_MY_SERVICES, context.telegramUserId(), backPage, "", context.receivedAt()),
                keyboardFactory.button(labelProvider.label(context.language(), com.parazit.panel.application.telegram.navigation.TelegramNavigationAction.HOME), TelegramCallbackAction.BACK_TO_MAIN, context.telegramUserId(), null, null, null, context.receivedAt())
        ));
        return keyboardFactory.rows(rows);
    }
}
