package com.parazit.panel.application.telegram.handler;

import com.parazit.panel.application.telegram.menu.TelegramMainMenuHandler;
import com.parazit.panel.application.telegram.model.TelegramCommand;
import com.parazit.panel.application.telegram.model.TelegramInteractionContext;
import com.parazit.panel.application.telegram.model.TelegramResponsePlan;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class MainMenuTelegramCommandHandler implements TelegramCommandHandler {

    private final TelegramMainMenuHandler mainMenuHandler;

    public MainMenuTelegramCommandHandler(TelegramMainMenuHandler mainMenuHandler) {
        this.mainMenuHandler = Objects.requireNonNull(mainMenuHandler, "mainMenuHandler must not be null");
    }

    @Override
    public TelegramCommand command() {
        return TelegramCommand.MENU;
    }

    @Override
    public TelegramResponsePlan handle(TelegramInteractionContext context) {
        return mainMenuHandler.showHome(context, "telegram.menu.returned", "command:menu");
    }
}
