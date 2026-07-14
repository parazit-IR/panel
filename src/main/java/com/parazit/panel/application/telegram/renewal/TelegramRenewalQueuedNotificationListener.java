package com.parazit.panel.application.telegram.renewal;

import com.parazit.panel.application.port.out.telegram.TelegramBotClient;
import com.parazit.panel.application.renewal.RenewalMetrics;
import com.parazit.panel.application.renewal.RenewalQueuedNotificationEvent;
import com.parazit.panel.application.telegram.TelegramKeyboardFactory;
import com.parazit.panel.application.telegram.command.SendTelegramMessageCommand;
import com.parazit.panel.application.telegram.model.TelegramCallbackAction;
import com.parazit.panel.application.telegram.model.TelegramInlineKeyboard;
import com.parazit.panel.application.telegram.model.TelegramParseMode;
import com.parazit.panel.config.properties.TelegramBotProperties;
import java.util.List;
import java.util.Objects;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TelegramRenewalQueuedNotificationListener {

    private final TelegramBotClient telegramBotClient;
    private final TelegramKeyboardFactory keyboardFactory;
    private final TelegramBotProperties properties;
    private final RenewalMetrics metrics;

    public TelegramRenewalQueuedNotificationListener(
            TelegramBotClient telegramBotClient,
            TelegramKeyboardFactory keyboardFactory,
            TelegramBotProperties properties,
            RenewalMetrics metrics
    ) {
        this.telegramBotClient = Objects.requireNonNull(telegramBotClient, "telegramBotClient must not be null");
        this.keyboardFactory = Objects.requireNonNull(keyboardFactory, "keyboardFactory must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
    }

    @EventListener
    public void on(RenewalQueuedNotificationEvent event) {
        String service = event.serviceUsername() == null || event.serviceUsername().isBlank()
                ? event.serviceDisplayName()
                : event.serviceUsername();
        TelegramInlineKeyboard keyboard = keyboardFactory.rows(List.of(
                keyboardFactory.row(keyboardFactory.button(
                        "🔄 بررسی وضعیت تمدید",
                        TelegramCallbackAction.REFRESH_RENEWAL_STATUS,
                        event.telegramUserId(),
                        event.renewalOrderId(),
                        null,
                        null,
                        event.queuedAt()
                )),
                keyboardFactory.row(
                        keyboardFactory.button("🛍 سرویس‌های من", TelegramCallbackAction.LIST_MY_SERVICES, event.telegramUserId(), null, null, null, event.queuedAt()),
                        keyboardFactory.button("🏠 منوی اصلی", TelegramCallbackAction.BACK_TO_MAIN, event.telegramUserId(), null, null, null, event.queuedAt())
                )
        ));
        telegramBotClient.sendMessage(new SendTelegramMessageCommand(
                event.telegramUserId(),
                """
                        ✅ پرداخت تمدید شما تأیید شد.

                        ♻️ تمدید سرویس در حال انجام است.

                        نام سرویس:
                        %s

                        پس از اعمال تمدید، نتیجه از طریق بات اطلاع داده می‌شود.
                        """.formatted(service == null || service.isBlank() ? "-" : service).strip(),
                TelegramParseMode.NONE,
                keyboard,
                properties.disableLinkPreview(),
                null
        ));
        metrics.renewalQueuedNotification("sent");
    }
}
