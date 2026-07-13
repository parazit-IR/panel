package com.parazit.panel.application.port.in.telegram;

import com.parazit.panel.application.telegram.model.TelegramUpdate;

public interface ProcessTelegramUpdateUseCase {

    void process(TelegramUpdate update);
}
