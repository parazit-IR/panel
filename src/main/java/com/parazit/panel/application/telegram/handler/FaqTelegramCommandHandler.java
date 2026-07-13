package com.parazit.panel.application.telegram.handler;

import com.parazit.panel.application.telegram.faq.TelegramFaqListHandler;
import com.parazit.panel.application.telegram.model.TelegramCommand;
import com.parazit.panel.application.telegram.model.TelegramInteractionContext;
import com.parazit.panel.application.telegram.model.TelegramResponsePlan;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class FaqTelegramCommandHandler implements TelegramCommandHandler {

    private final TelegramFaqListHandler faqListHandler;

    public FaqTelegramCommandHandler(TelegramFaqListHandler faqListHandler) {
        this.faqListHandler = Objects.requireNonNull(faqListHandler, "faqListHandler must not be null");
    }

    @Override
    public TelegramCommand command() {
        return TelegramCommand.FAQ;
    }

    @Override
    public TelegramResponsePlan handle(TelegramInteractionContext context) {
        return faqListHandler.handle(context, 1);
    }
}
