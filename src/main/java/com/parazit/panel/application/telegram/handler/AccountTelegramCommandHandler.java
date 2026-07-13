package com.parazit.panel.application.telegram.handler;

import com.parazit.panel.application.telegram.account.TelegramCustomerAccountHandler;
import com.parazit.panel.application.telegram.model.TelegramCommand;
import com.parazit.panel.application.telegram.model.TelegramInteractionContext;
import com.parazit.panel.application.telegram.model.TelegramResponsePlan;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class AccountTelegramCommandHandler implements TelegramCommandHandler {

    private final TelegramCustomerAccountHandler accountHandler;

    public AccountTelegramCommandHandler(TelegramCustomerAccountHandler accountHandler) {
        this.accountHandler = Objects.requireNonNull(accountHandler, "accountHandler must not be null");
    }

    @Override
    public TelegramCommand command() {
        return TelegramCommand.ACCOUNT;
    }

    @Override
    public TelegramResponsePlan handle(TelegramInteractionContext context) {
        return accountHandler.handle(context);
    }
}
