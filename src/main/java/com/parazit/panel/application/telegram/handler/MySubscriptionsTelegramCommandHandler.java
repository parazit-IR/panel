package com.parazit.panel.application.telegram.handler;

import com.parazit.panel.application.port.in.subscription.ListUserSubscriptionsUseCase;
import com.parazit.panel.application.subscription.result.SubscriptionResult;
import com.parazit.panel.application.telegram.TelegramKeyboardFactory;
import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.application.telegram.command.SendTelegramMessageCommand;
import com.parazit.panel.application.telegram.menu.TelegramMainReplyKeyboardFactory;
import com.parazit.panel.application.telegram.model.TelegramCallbackAction;
import com.parazit.panel.application.telegram.model.TelegramCommand;
import com.parazit.panel.application.telegram.model.TelegramInlineKeyboard;
import com.parazit.panel.application.telegram.model.TelegramInlineKeyboardRow;
import com.parazit.panel.application.telegram.model.TelegramInteractionContext;
import com.parazit.panel.application.telegram.model.TelegramParseMode;
import com.parazit.panel.application.telegram.model.TelegramResponseAction;
import com.parazit.panel.application.telegram.model.TelegramResponsePlan;
import com.parazit.panel.config.properties.TelegramBotProperties;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class MySubscriptionsTelegramCommandHandler implements TelegramCommandHandler {

    private static final int PAGE_SIZE = 5;

    private final ListUserSubscriptionsUseCase listUserSubscriptionsUseCase;
    private final TelegramMessageCatalog catalog;
    private final TelegramKeyboardFactory keyboardFactory;
    private final TelegramMainReplyKeyboardFactory replyKeyboardFactory;
    private final TelegramBotProperties properties;

    public MySubscriptionsTelegramCommandHandler(
            ListUserSubscriptionsUseCase listUserSubscriptionsUseCase,
            TelegramMessageCatalog catalog,
            TelegramKeyboardFactory keyboardFactory,
            TelegramMainReplyKeyboardFactory replyKeyboardFactory,
            TelegramBotProperties properties
    ) {
        this.listUserSubscriptionsUseCase = Objects.requireNonNull(listUserSubscriptionsUseCase, "listUserSubscriptionsUseCase must not be null");
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.keyboardFactory = Objects.requireNonNull(keyboardFactory, "keyboardFactory must not be null");
        this.replyKeyboardFactory = Objects.requireNonNull(replyKeyboardFactory, "replyKeyboardFactory must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public TelegramCommand command() {
        return TelegramCommand.MY_SUBSCRIPTIONS;
    }

    @Override
    public TelegramResponsePlan handle(TelegramInteractionContext context) {
        List<SubscriptionResult> subscriptions = listUserSubscriptionsUseCase.list(context.telegramUserId())
                .stream()
                .sorted(Comparator.comparing((SubscriptionResult result) -> !"ACTIVE".equals(result.status().name()))
                        .thenComparing(SubscriptionResult::activatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(PAGE_SIZE)
                .toList();
        if (subscriptions.isEmpty()) {
            return new TelegramResponsePlan(List.of(TelegramResponseAction.sendMessage(
                    new SendTelegramMessageCommand(
                            context.chatId(),
                            catalog.text(context.language(), "no_subscriptions"),
                            TelegramParseMode.NONE,
                            TelegramInlineKeyboard.empty(),
                            replyKeyboardFactory.mainKeyboard(context.language()),
                            properties.disableLinkPreview(),
                            null
                    ),
                    false
            )), "command:subscriptions");
        }
        List<TelegramInlineKeyboardRow> rows = subscriptions.stream()
                .map(subscription -> keyboardFactory.row(keyboardFactory.button(
                        label(subscription),
                        TelegramCallbackAction.VIEW_SUBSCRIPTION,
                        context.telegramUserId(),
                        subscription.subscriptionId(),
                        null,
                        null,
                        context.receivedAt()
                )))
                .toList();
        return message(context, catalog.text(context.language(), "my_subscriptions"), keyboardFactory.rows(rows));
    }

    private TelegramResponsePlan message(TelegramInteractionContext context, String text, TelegramInlineKeyboard keyboard) {
        return new TelegramResponsePlan(List.of(TelegramResponseAction.sendMessage(
                new SendTelegramMessageCommand(
                        context.chatId(),
                        text,
                        TelegramParseMode.NONE,
                        keyboard,
                        properties.disableLinkPreview(),
                        null
                ),
                false
        )), "command:subscriptions");
    }

    private static String label(SubscriptionResult result) {
        String plan = result.planName() == null || result.planName().isBlank() ? "Subscription" : result.planName();
        return plan;
    }
}
