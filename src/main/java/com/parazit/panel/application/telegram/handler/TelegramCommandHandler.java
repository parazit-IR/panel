package com.parazit.panel.application.telegram.handler;

import com.parazit.panel.application.telegram.model.TelegramCommand;
import com.parazit.panel.application.telegram.model.TelegramInteractionContext;
import com.parazit.panel.application.telegram.model.TelegramResponsePlan;

public interface TelegramCommandHandler {

    TelegramCommand command();

    TelegramResponsePlan handle(TelegramInteractionContext context);
}
