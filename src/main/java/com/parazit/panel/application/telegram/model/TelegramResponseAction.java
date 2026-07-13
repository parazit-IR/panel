package com.parazit.panel.application.telegram.model;

import com.parazit.panel.application.telegram.command.AnswerTelegramCallbackCommand;
import com.parazit.panel.application.telegram.command.EditTelegramMessageCommand;
import com.parazit.panel.application.telegram.command.SendTelegramMessageCommand;
import com.parazit.panel.application.telegram.command.SendTelegramPhotoCommand;

public record TelegramResponseAction(
        TelegramResponseActionType type,
        SendTelegramMessageCommand message,
        SendTelegramPhotoCommand photo,
        EditTelegramMessageCommand edit,
        AnswerTelegramCallbackCommand callbackAnswer,
        boolean sensitive
) {

    public static TelegramResponseAction sendMessage(SendTelegramMessageCommand message, boolean sensitive) {
        return new TelegramResponseAction(TelegramResponseActionType.SEND_MESSAGE, message, null, null, null, sensitive);
    }

    public static TelegramResponseAction sendPhoto(SendTelegramPhotoCommand photo, boolean sensitive) {
        return new TelegramResponseAction(TelegramResponseActionType.SEND_PHOTO, null, photo, null, null, sensitive);
    }

    public static TelegramResponseAction edit(EditTelegramMessageCommand edit) {
        return new TelegramResponseAction(TelegramResponseActionType.EDIT_MESSAGE, null, null, edit, null, false);
    }

    public static TelegramResponseAction answerCallback(AnswerTelegramCallbackCommand callbackAnswer) {
        return new TelegramResponseAction(TelegramResponseActionType.ANSWER_CALLBACK, null, null, null, callbackAnswer, false);
    }

    @Override
    public String toString() {
        return "TelegramResponseAction[type=%s,sensitive=%s]".formatted(type, sensitive);
    }
}
