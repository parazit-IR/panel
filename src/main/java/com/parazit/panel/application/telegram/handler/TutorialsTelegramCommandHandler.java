package com.parazit.panel.application.telegram.handler;

import com.parazit.panel.application.telegram.menu.TelegramMainMenuAction;
import com.parazit.panel.application.telegram.menu.TelegramMainMenuHandler;
import com.parazit.panel.application.telegram.model.TelegramCommand;
import com.parazit.panel.application.telegram.model.TelegramInteractionContext;
import com.parazit.panel.application.telegram.model.TelegramResponsePlan;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TutorialsTelegramCommandHandler implements TelegramCommandHandler {

    private final TelegramMainMenuHandler mainMenuHandler;

    public TutorialsTelegramCommandHandler(TelegramMainMenuHandler mainMenuHandler) {
        this.mainMenuHandler = Objects.requireNonNull(mainMenuHandler, "mainMenuHandler must not be null");
    }

    @Override
    public TelegramCommand command() {
        return TelegramCommand.TUTORIALS;
    }

    @Override
    public TelegramResponsePlan handle(TelegramInteractionContext context) {
        return mainMenuHandler.handle(context, TelegramMainMenuAction.SHOW_TUTORIALS);
    }
}
