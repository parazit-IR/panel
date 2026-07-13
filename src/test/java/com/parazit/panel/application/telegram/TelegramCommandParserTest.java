package com.parazit.panel.application.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.telegram.model.TelegramCommand;
import org.junit.jupiter.api.Test;

class TelegramCommandParserTest {

    private final TelegramCommandParser parser = new TelegramCommandParser(TelegramTestProperties.properties());

    @Test
    void parsesSupportedCommands() {
        assertThat(parser.parse("/start")).isEqualTo(TelegramCommand.START);
        assertThat(parser.parse("/start@PanelBot abc")).isEqualTo(TelegramCommand.START);
        assertThat(parser.parse("/menu")).isEqualTo(TelegramCommand.MENU);
        assertThat(parser.parse("/subscriptions")).isEqualTo(TelegramCommand.MY_SUBSCRIPTIONS);
        assertThat(parser.parse("/help")).isEqualTo(TelegramCommand.HELP);
    }

    @Test
    void rejectsWrongBotMentionAndPlainText() {
        assertThat(parser.parse("/start@OtherBot")).isEqualTo(TelegramCommand.UNKNOWN);
        assertThat(parser.parse("hello")).isEqualTo(TelegramCommand.UNKNOWN);
        assertThat(parser.parse("")).isEqualTo(TelegramCommand.UNKNOWN);
    }
}
