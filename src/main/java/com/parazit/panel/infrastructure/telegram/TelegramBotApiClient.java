package com.parazit.panel.infrastructure.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.parazit.panel.application.port.out.telegram.TelegramBotClient;
import com.parazit.panel.application.telegram.TelegramClientException;
import com.parazit.panel.application.telegram.command.AnswerTelegramCallbackCommand;
import com.parazit.panel.application.telegram.command.EditTelegramMessageCommand;
import com.parazit.panel.application.telegram.command.GetTelegramUpdatesCommand;
import com.parazit.panel.application.telegram.command.SendTelegramMessageCommand;
import com.parazit.panel.application.telegram.command.SendTelegramPhotoCommand;
import com.parazit.panel.application.telegram.model.TelegramActor;
import com.parazit.panel.application.telegram.model.TelegramCallbackQuery;
import com.parazit.panel.application.telegram.model.TelegramChat;
import com.parazit.panel.application.telegram.model.TelegramChatType;
import com.parazit.panel.application.telegram.model.TelegramInlineButton;
import com.parazit.panel.application.telegram.model.TelegramInlineButtonType;
import com.parazit.panel.application.telegram.model.TelegramInlineKeyboard;
import com.parazit.panel.application.telegram.model.TelegramInlineKeyboardRow;
import com.parazit.panel.application.telegram.model.TelegramMessage;
import com.parazit.panel.application.telegram.model.TelegramParseMode;
import com.parazit.panel.application.telegram.model.TelegramUpdate;
import com.parazit.panel.application.telegram.model.TelegramUpdateType;
import com.parazit.panel.application.telegram.result.TelegramEditResult;
import com.parazit.panel.application.telegram.result.TelegramMessageKind;
import com.parazit.panel.application.telegram.result.TelegramSendResult;
import com.parazit.panel.application.telegram.result.TelegramUpdatesResult;
import com.parazit.panel.config.properties.TelegramBotProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class TelegramBotApiClient implements TelegramBotClient {

    private final RestClient restClient;
    private final TelegramBotProperties properties;

    public TelegramBotApiClient(
            @Qualifier("telegramRestClient") RestClient restClient,
            TelegramBotProperties properties
    ) {
        this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public TelegramSendResult sendMessage(SendTelegramMessageCommand command) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("chat_id", command.chatId());
        body.put("text", command.text());
        putParseMode(body, command.parseMode());
        body.put("disable_web_page_preview", command.disableLinkPreview());
        putKeyboard(body, command.keyboard());
        JsonNode result = postJson("sendMessage", body);
        JsonNode message = result.path("result");
        return new TelegramSendResult(command.chatId(), message.path("message_id").asLong(), Instant.now(), TelegramMessageKind.TEXT, true);
    }

    @Override
    public TelegramSendResult sendPhoto(SendTelegramPhotoCommand command) {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("chat_id", command.chatId());
        parts.add("photo", new ByteArrayResource(command.photoBytes()) {
            @Override
            public String getFilename() {
                return command.filename();
            }
        });
        if (command.caption() != null) {
            parts.add("caption", command.caption());
            if (command.parseMode() == TelegramParseMode.HTML) {
                parts.add("parse_mode", "HTML");
            }
        }
        JsonNode result = call(() -> restClient.post()
                .uri(path("sendPhoto"))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parts)
                .retrieve()
                .body(JsonNode.class));
        ensureOk(result);
        JsonNode message = result.path("result");
        return new TelegramSendResult(command.chatId(), message.path("message_id").asLong(), Instant.now(), TelegramMessageKind.PHOTO, true);
    }

    @Override
    public TelegramEditResult editMessage(EditTelegramMessageCommand command) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("chat_id", command.chatId());
        body.put("message_id", command.messageId());
        body.put("text", command.text());
        putParseMode(body, command.parseMode());
        putKeyboard(body, command.keyboard());
        JsonNode result = postJson("editMessageText", body);
        ensureOk(result);
        return new TelegramEditResult(command.chatId(), command.messageId(), true);
    }

    @Override
    public void answerCallbackQuery(AnswerTelegramCallbackCommand command) {
        postJson("answerCallbackQuery", Map.of(
                "callback_query_id", command.callbackQueryId(),
                "text", command.text(),
                "show_alert", command.showAlert()
        ));
    }

    @Override
    public TelegramUpdatesResult getUpdates(GetTelegramUpdatesCommand command) {
        JsonNode response = postJson("getUpdates", Map.of(
                "offset", command.offset(),
                "limit", command.limit(),
                "timeout", command.timeout().toSeconds(),
                "allowed_updates", command.allowedUpdates()
        ));
        List<TelegramUpdate> updates = new ArrayList<>();
        for (JsonNode update : response.path("result")) {
            updates.add(mapUpdate(update));
        }
        return new TelegramUpdatesResult(updates);
    }

    private JsonNode postJson(String method, Map<String, ?> body) {
        JsonNode response = call(() -> restClient.post()
                .uri(path(method))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class));
        ensureOk(response);
        return response;
    }

    private JsonNode call(SupplierWithRestException<JsonNode> supplier) {
        try {
            JsonNode response = supplier.get();
            if (response == null) {
                throw new TelegramClientException("Telegram response is empty", 502, "TELEGRAM_EMPTY_RESPONSE", null, false);
            }
            return response;
        } catch (RestClientResponseException exception) {
            throw new TelegramClientException(
                    "Telegram HTTP request failed",
                    exception.getStatusCode().value(),
                    "TELEGRAM_HTTP_" + exception.getStatusCode().value(),
                    null,
                    false
            );
        } catch (RestClientException exception) {
            throw new TelegramClientException("Telegram request result is unknown", 0, "TELEGRAM_REQUEST_UNKNOWN", null, true);
        }
    }

    private void ensureOk(JsonNode response) {
        if (response.path("ok").asBoolean(false)) {
            return;
        }
        int status = response.path("error_code").asInt(502);
        Duration retryAfter = response.path("parameters").has("retry_after")
                ? Duration.ofSeconds(response.path("parameters").path("retry_after").asLong())
                : null;
        throw new TelegramClientException("Telegram logical API error", status, "TELEGRAM_API_" + status, retryAfter, false);
    }

    private TelegramUpdate mapUpdate(JsonNode node) {
        long updateId = node.path("update_id").asLong();
        Instant receivedAt = Instant.now();
        if (node.has("message")) {
            JsonNode message = node.path("message");
            return new TelegramUpdate(
                    updateId,
                    TelegramUpdateType.MESSAGE,
                    actor(message.path("from")),
                    chat(message.path("chat")),
                    message(message),
                    null,
                    receivedAt
            );
        }
        if (node.has("callback_query")) {
            JsonNode callback = node.path("callback_query");
            JsonNode message = callback.path("message");
            return new TelegramUpdate(
                    updateId,
                    TelegramUpdateType.CALLBACK_QUERY,
                    actor(callback.path("from")),
                    chat(message.path("chat")),
                    null,
                    new TelegramCallbackQuery(
                            callback.path("id").asText(),
                            message.path("message_id").asLong(),
                            callback.path("data").asText("")
                    ),
                    receivedAt
            );
        }
        return new TelegramUpdate(updateId, TelegramUpdateType.UNSUPPORTED, null, null, null, null, receivedAt);
    }

    private TelegramActor actor(JsonNode node) {
        return new TelegramActor(
                node.path("id").asLong(),
                text(node, "username"),
                text(node, "first_name"),
                text(node, "last_name"),
                text(node, "language_code"),
                node.path("is_bot").asBoolean(false)
        );
    }

    private TelegramChat chat(JsonNode node) {
        return new TelegramChat(node.path("id").asLong(), chatType(node.path("type").asText("")), text(node, "title"));
    }

    private TelegramMessage message(JsonNode node) {
        return new TelegramMessage(
                node.path("message_id").asLong(),
                node.path("text").asText(""),
                Instant.ofEpochSecond(node.path("date").asLong(Instant.now().getEpochSecond()))
        );
    }

    private static TelegramChatType chatType(String value) {
        return switch (value) {
            case "private" -> TelegramChatType.PRIVATE;
            case "group" -> TelegramChatType.GROUP;
            case "supergroup" -> TelegramChatType.SUPERGROUP;
            case "channel" -> TelegramChatType.CHANNEL;
            default -> TelegramChatType.UNKNOWN;
        };
    }

    private static String text(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.path(field).asText() : null;
    }

    private void putParseMode(Map<String, Object> body, TelegramParseMode parseMode) {
        if (parseMode == TelegramParseMode.HTML) {
            body.put("parse_mode", "HTML");
        }
    }

    private void putKeyboard(Map<String, Object> body, TelegramInlineKeyboard keyboard) {
        Object value = keyboard(keyboard);
        if (value != null) {
            body.put("reply_markup", value);
        }
    }

    private Object keyboard(TelegramInlineKeyboard keyboard) {
        if (keyboard == null || keyboard.rows().isEmpty()) {
            return null;
        }
        List<List<Map<String, String>>> rows = new ArrayList<>();
        for (TelegramInlineKeyboardRow row : keyboard.rows()) {
            List<Map<String, String>> buttons = new ArrayList<>();
            for (TelegramInlineButton button : row.buttons()) {
                if (button.type() == TelegramInlineButtonType.URL) {
                    buttons.add(Map.of("text", button.text(), "url", button.value()));
                } else {
                    buttons.add(Map.of("text", button.text(), "callback_data", button.value()));
                }
            }
            rows.add(buttons);
        }
        return Map.of("inline_keyboard", rows);
    }

    private String path(String method) {
        return "/bot" + properties.token() + "/" + method;
    }

    @FunctionalInterface
    private interface SupplierWithRestException<T> {
        T get();
    }
}
