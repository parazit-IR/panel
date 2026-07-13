package com.parazit.panel.application.telegram.service;

import com.parazit.panel.application.customer.result.CustomerServiceDetailsResult;
import com.parazit.panel.application.port.in.customer.GetCustomerServiceDetailsUseCase;
import com.parazit.panel.application.port.in.customer.RefreshSubscriptionUsageUseCase;
import com.parazit.panel.application.telegram.command.SendTelegramMessageCommand;
import com.parazit.panel.application.telegram.model.TelegramInteractionContext;
import com.parazit.panel.application.telegram.model.TelegramParseMode;
import com.parazit.panel.application.telegram.model.TelegramResponseAction;
import com.parazit.panel.application.telegram.model.TelegramResponsePlan;
import com.parazit.panel.config.properties.TelegramBotProperties;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TelegramServiceDetailsHandler {

    private final GetCustomerServiceDetailsUseCase detailsUseCase;
    private final RefreshSubscriptionUsageUseCase refreshUseCase;
    private final TelegramCustomerServiceDetailsFormatter formatter;
    private final TelegramServiceActionKeyboardFactory keyboardFactory;
    private final TelegramBotProperties properties;
    private final TelegramCustomerServiceMetrics metrics;

    public TelegramServiceDetailsHandler(
            GetCustomerServiceDetailsUseCase detailsUseCase,
            RefreshSubscriptionUsageUseCase refreshUseCase,
            TelegramCustomerServiceDetailsFormatter formatter,
            TelegramServiceActionKeyboardFactory keyboardFactory,
            TelegramBotProperties properties,
            TelegramCustomerServiceMetrics metrics
    ) {
        this.detailsUseCase = Objects.requireNonNull(detailsUseCase, "detailsUseCase must not be null");
        this.refreshUseCase = Objects.requireNonNull(refreshUseCase, "refreshUseCase must not be null");
        this.formatter = Objects.requireNonNull(formatter, "formatter must not be null");
        this.keyboardFactory = Objects.requireNonNull(keyboardFactory, "keyboardFactory must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
    }

    public TelegramResponsePlan handle(TelegramInteractionContext context, UUID subscriptionId, int backPage) {
        CustomerServiceDetailsResult result = detailsUseCase.get(context.telegramUserId(), subscriptionId, false);
        metrics.serviceDetailsViewed(result.status().name(), result.usageFreshness().name());
        return message(context, result, backPage, "telegram:service-details");
    }

    public TelegramResponsePlan refresh(TelegramInteractionContext context, UUID subscriptionId, int backPage) {
        CustomerServiceDetailsResult result = refreshUseCase.refresh(context.telegramUserId(), subscriptionId, UUID.nameUUIDFromBytes(("telegram-refresh-" + context.updateId()).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        metrics.serviceRefresh("success", result.usageFreshness().name());
        return message(context, result, backPage, "telegram:service-refresh");
    }

    private TelegramResponsePlan message(TelegramInteractionContext context, CustomerServiceDetailsResult result, int backPage, String key) {
        return new TelegramResponsePlan(List.of(TelegramResponseAction.sendMessage(new SendTelegramMessageCommand(
                context.chatId(),
                formatter.format(result, context.language()),
                TelegramParseMode.HTML,
                keyboardFactory.serviceDetails(context, result, Math.max(1, backPage)),
                properties.disableLinkPreview(),
                null
        ), false)), key);
    }
}
