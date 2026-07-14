package com.parazit.panel.application.telegram.renewal;

import com.parazit.panel.application.port.out.telegram.TelegramBotClient;
import com.parazit.panel.application.renewal.RenewalFailedNotificationEvent;
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
public class TelegramRenewalFailureNotificationListener {

    private final TelegramBotClient telegramBotClient;
    private final TelegramKeyboardFactory keyboardFactory;
    private final TelegramBotProperties properties;

    public TelegramRenewalFailureNotificationListener(
            TelegramBotClient telegramBotClient,
            TelegramKeyboardFactory keyboardFactory,
            TelegramBotProperties properties
    ) {
        this.telegramBotClient = Objects.requireNonNull(telegramBotClient, "telegramBotClient must not be null");
        this.keyboardFactory = Objects.requireNonNull(keyboardFactory, "keyboardFactory must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @EventListener
    public void on(RenewalFailedNotificationEvent event) {
        TelegramInlineKeyboard keyboard = keyboardFactory.rows(List.of(
                keyboardFactory.row(keyboardFactory.button("☎️ پشتیبانی", TelegramCallbackAction.SHOW_SUPPORT, event.telegramUserId(), null, null, null, event.failedAt())),
                keyboardFactory.row(keyboardFactory.button("🔄 بررسی وضعیت تمدید", TelegramCallbackAction.REFRESH_RENEWAL_STATUS, event.telegramUserId(), event.renewalOrderId(), null, null, event.failedAt())),
                keyboardFactory.row(keyboardFactory.button("🏠 منوی اصلی", TelegramCallbackAction.BACK_TO_MAIN, event.telegramUserId(), null, null, null, event.failedAt()))
        ));
        telegramBotClient.sendMessage(new SendTelegramMessageCommand(
                event.telegramUserId(),
                """
                        ⚠️ پرداخت تمدید تأیید شده است، اما اعمال تمدید با مشکل مواجه شد.

                        سفارش شما ثبت شده و نیازمند بررسی است.
                        لطفاً با پشتیبانی تماس بگیرید.
                        """.strip(),
                TelegramParseMode.NONE,
                keyboard,
                properties.disableLinkPreview(),
                null
        ));
    }
}
