package com.parazit.panel.application.port.out.telegram;

import com.parazit.panel.application.telegram.command.AnswerTelegramCallbackCommand;
import com.parazit.panel.application.telegram.command.EditTelegramMessageCommand;
import com.parazit.panel.application.telegram.command.GetTelegramUpdatesCommand;
import com.parazit.panel.application.telegram.command.SendTelegramMessageCommand;
import com.parazit.panel.application.telegram.command.SendTelegramPhotoCommand;
import com.parazit.panel.application.telegram.result.TelegramEditResult;
import com.parazit.panel.application.telegram.result.TelegramSendResult;
import com.parazit.panel.application.telegram.result.TelegramUpdatesResult;

public interface TelegramBotClient {

    TelegramSendResult sendMessage(SendTelegramMessageCommand command);

    TelegramSendResult sendPhoto(SendTelegramPhotoCommand command);

    TelegramEditResult editMessage(EditTelegramMessageCommand command);

    void answerCallbackQuery(AnswerTelegramCallbackCommand command);

    TelegramUpdatesResult getUpdates(GetTelegramUpdatesCommand command);
}
