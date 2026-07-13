package com.parazit.panel.application.telegram;

import com.parazit.panel.application.port.out.telegram.TelegramBotClient;
import com.parazit.panel.application.telegram.model.TelegramResponseAction;
import com.parazit.panel.application.telegram.model.TelegramResponsePlan;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TelegramResponseExecutor {

    private final TelegramBotClient client;

    public TelegramResponseExecutor(TelegramBotClient client) {
        this.client = Objects.requireNonNull(client, "client must not be null");
    }

    public String execute(TelegramResponsePlan plan, long updateId) {
        Objects.requireNonNull(plan, "plan must not be null");
        for (TelegramResponseAction action : plan.actions()) {
            switch (action.type()) {
                case SEND_MESSAGE -> client.sendMessage(action.message());
                case SEND_PHOTO -> client.sendPhoto(action.photo());
                case EDIT_MESSAGE -> client.editMessage(action.edit());
                case ANSWER_CALLBACK -> client.answerCallbackQuery(action.callbackAnswer());
            }
        }
        return fingerprint(updateId, plan);
    }

    private static String fingerprint(long updateId, TelegramResponsePlan plan) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update((updateId + "|" + plan.handlerKey() + "|" + plan.actions().size()).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
