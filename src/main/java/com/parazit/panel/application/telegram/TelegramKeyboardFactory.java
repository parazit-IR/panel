package com.parazit.panel.application.telegram;

import com.parazit.panel.application.telegram.model.TelegramCallbackAction;
import com.parazit.panel.application.telegram.model.TelegramCallbackPayload;
import com.parazit.panel.application.telegram.model.TelegramInlineButton;
import com.parazit.panel.application.telegram.model.TelegramInlineKeyboard;
import com.parazit.panel.application.telegram.model.TelegramInlineKeyboardRow;
import com.parazit.panel.config.properties.TelegramBotProperties;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TelegramKeyboardFactory {

    private final TelegramCallbackDataCodec codec;
    private final TelegramBotProperties properties;

    public TelegramKeyboardFactory(TelegramCallbackDataCodec codec, TelegramBotProperties properties) {
        this.codec = Objects.requireNonNull(codec, "codec must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public TelegramInlineKeyboard mainMenu(long telegramUserId, Instant now) {
        return TelegramInlineKeyboard.ofRows(List.of(
                row(button("My subscriptions", TelegramCallbackAction.MY_SUBSCRIPTIONS, telegramUserId, null, null, null, now)),
                row(button("Help", TelegramCallbackAction.HELP, telegramUserId, null, null, null, now))
        ));
    }

    public TelegramInlineButton button(
            String text,
            TelegramCallbackAction action,
            long telegramUserId,
            UUID subscriptionId,
            Integer configIndex,
            UUID actionId,
            Instant now
    ) {
        TelegramCallbackPayload payload = new TelegramCallbackPayload(
                action,
                subscriptionId,
                configIndex,
                actionId,
                now.plus(properties.callbackTtl())
        );
        return TelegramInlineButton.callback(text, codec.encode(payload, telegramUserId));
    }

    public TelegramInlineButton button(
            String text,
            TelegramCallbackAction action,
            long telegramUserId,
            Integer page,
            String reference,
            Instant now
    ) {
        TelegramCallbackPayload payload = new TelegramCallbackPayload(
                action,
                null,
                page,
                null,
                reference,
                now.plus(properties.callbackTtl())
        );
        return TelegramInlineButton.callback(text, codec.encode(payload, telegramUserId));
    }

    public TelegramInlineKeyboard rows(List<TelegramInlineKeyboardRow> rows) {
        return TelegramInlineKeyboard.ofRows(rows);
    }

    public TelegramInlineKeyboardRow row(TelegramInlineButton... buttons) {
        return new TelegramInlineKeyboardRow(List.of(buttons));
    }
}
