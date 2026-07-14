package com.parazit.panel.application.telegram.renewal;

import com.parazit.panel.application.port.out.telegram.TelegramBotClient;
import com.parazit.panel.application.renewal.RenewalCompletedNotificationEvent;
import com.parazit.panel.application.renewal.RenewalMetrics;
import com.parazit.panel.application.telegram.TelegramKeyboardFactory;
import com.parazit.panel.application.telegram.TelegramMessageFormatter;
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
public class TelegramRenewalCompletionNotificationListener {

    private final TelegramBotClient telegramBotClient;
    private final TelegramKeyboardFactory keyboardFactory;
    private final TelegramMessageFormatter formatter;
    private final TelegramBotProperties properties;
    private final RenewalMetrics metrics;

    public TelegramRenewalCompletionNotificationListener(
            TelegramBotClient telegramBotClient,
            TelegramKeyboardFactory keyboardFactory,
            TelegramMessageFormatter formatter,
            TelegramBotProperties properties,
            RenewalMetrics metrics
    ) {
        this.telegramBotClient = Objects.requireNonNull(telegramBotClient, "telegramBotClient must not be null");
        this.keyboardFactory = Objects.requireNonNull(keyboardFactory, "keyboardFactory must not be null");
        this.formatter = Objects.requireNonNull(formatter, "formatter must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
    }

    @EventListener
    public void on(RenewalCompletedNotificationEvent event) {
        String service = event.serviceUsername() == null || event.serviceUsername().isBlank()
                ? event.serviceDisplayName()
                : event.serviceUsername();
        TelegramInlineKeyboard keyboard = keyboardFactory.rows(List.of(
                keyboardFactory.row(
                        keyboardFactory.button("🛍 مشاهده سرویس", TelegramCallbackAction.SHOW_SERVICE_DETAILS, event.telegramUserId(), event.subscriptionId(), 1, null, event.completedAt())
                ),
                keyboardFactory.row(
                        keyboardFactory.button("🔗 دریافت کانفیگ", TelegramCallbackAction.SHOW_VLESS_CONFIG, event.telegramUserId(), event.subscriptionId(), 1, null, event.completedAt()),
                        keyboardFactory.button("📱 دریافت QR Code", TelegramCallbackAction.SHOW_SUBSCRIPTION_QR, event.telegramUserId(), event.subscriptionId(), 1, null, event.completedAt())
                ),
                keyboardFactory.row(keyboardFactory.button("🏠 منوی اصلی", TelegramCallbackAction.BACK_TO_MAIN, event.telegramUserId(), null, null, null, event.completedAt()))
        ));
        telegramBotClient.sendMessage(new SendTelegramMessageCommand(
                event.telegramUserId(),
                """
                        ✅ تمدید سرویس با موفقیت انجام شد.

                        👤 نام سرویس:
                        %s

                        📅 تاریخ انقضای جدید:
                        %s

                        📊 حجم جدید:
                        %s

                        اکنون می‌توانید از بخش «سرویس‌های من» اطلاعات به‌روز سرویس را مشاهده کنید.
                        """.formatted(
                        formatter.html(service == null || service.isBlank() ? "-" : service),
                        formatter.formatDate(event.newExpiryAt()),
                        formatTraffic(event.newTrafficLimitBytes())
                ).strip(),
                TelegramParseMode.HTML,
                keyboard,
                properties.disableLinkPreview(),
                null
        ));
        metrics.renewalCompletionNotification("sent");
    }

    private static String formatTraffic(long bytes) {
        if (bytes == 0) {
            return "نامحدود";
        }
        long gib = bytes / 1_073_741_824L;
        if (gib > 0 && bytes % 1_073_741_824L == 0) {
            return gib + " گیگابایت";
        }
        long mib = bytes / 1_048_576L;
        if (mib > 0) {
            return mib + " مگابایت";
        }
        return bytes + " بایت";
    }
}
