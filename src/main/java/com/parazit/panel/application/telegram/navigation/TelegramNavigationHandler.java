package com.parazit.panel.application.telegram.navigation;

import com.parazit.panel.application.telegram.menu.TelegramMainMenuHandler;
import com.parazit.panel.application.telegram.model.TelegramInteractionContext;
import com.parazit.panel.application.telegram.model.TelegramResponsePlan;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TelegramNavigationHandler {

    private final TelegramMainMenuHandler mainMenuHandler;

    public TelegramNavigationHandler(TelegramMainMenuHandler mainMenuHandler) {
        this.mainMenuHandler = Objects.requireNonNull(mainMenuHandler, "mainMenuHandler must not be null");
    }

    public TelegramResponsePlan home(TelegramInteractionContext context) {
        return mainMenuHandler.showHome(context, "telegram.menu.returned", "navigation:home");
    }
}
