package com.parazit.panel.application.telegram.handler;

import com.parazit.panel.application.telegram.model.TelegramCommand;
import com.parazit.panel.application.telegram.model.TelegramInteractionContext;
import com.parazit.panel.application.telegram.model.TelegramResponsePlan;
import com.parazit.panel.application.telegram.service.TelegramMyServicesHandler;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class MySubscriptionsTelegramCommandHandler implements TelegramCommandHandler {

    private final TelegramMyServicesHandler myServicesHandler;

    public MySubscriptionsTelegramCommandHandler(TelegramMyServicesHandler myServicesHandler) {
        this.myServicesHandler = Objects.requireNonNull(myServicesHandler, "myServicesHandler must not be null");
    }

    @Override
    public TelegramCommand command() {
        return TelegramCommand.MY_SUBSCRIPTIONS;
    }

    @Override
    public TelegramResponsePlan handle(TelegramInteractionContext context) {
        return myServicesHandler.handle(context, 1);
    }
}
