package com.parazit.panel.infrastructure.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parazit.panel.application.telegram.TelegramTestProperties;
import com.parazit.panel.application.telegram.command.GetTelegramUpdatesCommand;
import com.parazit.panel.application.telegram.command.SendTelegramMessageCommand;
import com.parazit.panel.application.telegram.keyboard.TelegramReplyKeyboard;
import com.parazit.panel.application.telegram.keyboard.TelegramReplyKeyboardButton;
import com.parazit.panel.application.telegram.keyboard.TelegramReplyKeyboardRow;
import com.parazit.panel.application.telegram.model.TelegramInlineKeyboard;
import com.parazit.panel.application.telegram.model.TelegramInlineKeyboardRow;
import com.parazit.panel.application.telegram.model.TelegramInlineButton;
import com.parazit.panel.application.telegram.model.TelegramParseMode;
import com.parazit.panel.application.telegram.model.TelegramUpdateType;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

class TelegramBotApiClientTest {

    private MockWebServer server;
    private TelegramBotApiClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        RestClient restClient = RestClient.builder()
                .baseUrl(server.url("/").toString())
                .messageConverters(converters -> {
                    converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
                    converters.add(new MappingJackson2HttpMessageConverter(new ObjectMapper()));
                })
                .build();
        client = new TelegramBotApiClient(restClient, TelegramTestProperties.properties());
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void sendsMessageWithoutLoggingOrExposingTokenInModel() throws Exception {
        server.enqueue(json("""
                {"ok":true,"result":{"message_id":77}}
                """));

        var result = client.sendMessage(new SendTelegramMessageCommand(
                42L,
                "hello",
                TelegramParseMode.NONE,
                TelegramInlineKeyboard.empty(),
                true,
                null
        ));

        assertThat(result.messageId()).isEqualTo(77L);
        assertThat(server.takeRequest().getPath()).isEqualTo("/bot123456:test-token/sendMessage");
    }

    @Test
    void sendsPersistentReplyKeyboardMarkup() throws Exception {
        server.enqueue(json("""
                {"ok":true,"result":{"message_id":78}}
                """));

        client.sendMessage(new SendTelegramMessageCommand(
                42L,
                "menu",
                TelegramParseMode.NONE,
                TelegramInlineKeyboard.empty(),
                new TelegramReplyKeyboard(
                        List.of(new TelegramReplyKeyboardRow(List.of(
                                new TelegramReplyKeyboardButton("🔐 خرید اشتراک"),
                                new TelegramReplyKeyboardButton("♻️ تمدید سرویس")
                        ))),
                        true,
                        false,
                        true,
                        false,
                        "یکی از گزینه‌های منو را انتخاب کنید"
                ),
                true,
                null
        ));

        String body = server.takeRequest().getBody().readUtf8();
        assertThat(body).contains("\"reply_markup\"");
        assertThat(body).contains("\"keyboard\"");
        assertThat(body).contains("\"resize_keyboard\":true");
        assertThat(body).contains("\"one_time_keyboard\":false");
        assertThat(body).contains("\"is_persistent\":true");
        assertThat(body).contains("\"input_field_placeholder\":\"یکی از گزینه‌های منو را انتخاب کنید\"");
        assertThat(body).doesNotContain("inline_keyboard");
    }

    @Test
    void serializesUrlButtonsWithoutCallbackData() throws Exception {
        server.enqueue(json("""
                {"ok":true,"result":{"message_id":79}}
                """));

        client.sendMessage(new SendTelegramMessageCommand(
                42L,
                "support",
                TelegramParseMode.HTML,
                TelegramInlineKeyboard.ofRows(List.of(new TelegramInlineKeyboardRow(List.of(
                        TelegramInlineButton.url("☎️ ارسال پیام به پشتیبانی", "https://t.me/SupportUser")
                )))),
                true,
                null
        ));

        String body = server.takeRequest().getBody().readUtf8();
        assertThat(body).contains("\"inline_keyboard\"");
        assertThat(body).contains("\"url\":\"https://t.me/SupportUser\"");
        assertThat(body).doesNotContain("callback_data");
    }

    @Test
    void serializesCopyTextButtonsWithoutCallbackData() throws Exception {
        server.enqueue(json("""
                {"ok":true,"result":{"message_id":80}}
                """));

        client.sendMessage(new SendTelegramMessageCommand(
                42L,
                "manual",
                TelegramParseMode.HTML,
                TelegramInlineKeyboard.ofRows(List.of(new TelegramInlineKeyboardRow(List.of(
                        TelegramInlineButton.copyText("📋 کپی مبلغ", "501596")
                )))),
                true,
                null
        ));

        String body = server.takeRequest().getBody().readUtf8();
        assertThat(body).contains("\"copy_text\":{\"text\":\"501596\"}");
        assertThat(body).doesNotContain("callback_data");
    }

    @Test
    void mapsMessageAndCallbackUpdates() {
        server.enqueue(json("""
                {"ok":true,"result":[
                  {"update_id":10,"message":{"message_id":1,"date":1783946400,"text":"/start",
                    "from":{"id":42,"is_bot":false,"first_name":"A","username":"u","language_code":"en"},
                    "chat":{"id":42,"type":"private"}}},
                  {"update_id":11,"callback_query":{"id":"cb1","data":"payload",
                    "from":{"id":42,"is_bot":false,"first_name":"A"},
                    "message":{"message_id":2,"chat":{"id":42,"type":"private"}}}}
                ]}
                """));

        var result = client.getUpdates(new GetTelegramUpdatesCommand(0, 50, Duration.ZERO, Set.of("message", "callback_query")));

        assertThat(result.updates()).hasSize(2);
        assertThat(result.updates().get(0).type()).isEqualTo(TelegramUpdateType.MESSAGE);
        assertThat(result.updates().get(1).type()).isEqualTo(TelegramUpdateType.CALLBACK_QUERY);
    }

    private static MockResponse json(String body) {
        return new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }
}
