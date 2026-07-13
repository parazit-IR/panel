package com.parazit.panel.application.telegram.command;

import com.parazit.panel.application.telegram.model.TelegramInlineKeyboard;
import com.parazit.panel.application.telegram.model.TelegramParseMode;
import java.util.Arrays;

public record SendTelegramPhotoCommand(
        long chatId,
        byte[] photoBytes,
        String filename,
        String caption,
        TelegramParseMode parseMode,
        TelegramInlineKeyboard keyboard
) {

    public SendTelegramPhotoCommand {
        if (photoBytes == null || photoBytes.length == 0) {
            throw new IllegalArgumentException("photoBytes must not be empty");
        }
        photoBytes = Arrays.copyOf(photoBytes, photoBytes.length);
        filename = safeFilename(filename);
        caption = caption == null || caption.isBlank() ? null : caption.trim();
        parseMode = parseMode == null ? TelegramParseMode.NONE : parseMode;
        keyboard = keyboard == null ? TelegramInlineKeyboard.empty() : keyboard;
    }

    @Override
    public byte[] photoBytes() {
        return Arrays.copyOf(photoBytes, photoBytes.length);
    }

    @Override
    public String toString() {
        return "SendTelegramPhotoCommand[chatId=%d,filename=%s]".formatted(chatId, filename);
    }

    private static String safeFilename(String filename) {
        String candidate = filename == null || filename.isBlank() ? "subscription-qr.png" : filename.trim();
        candidate = candidate.replaceAll("[^A-Za-z0-9._-]", "_");
        if (!candidate.endsWith(".png")) {
            candidate = candidate + ".png";
        }
        return candidate.length() <= 80 ? candidate : candidate.substring(0, 76) + ".png";
    }
}
