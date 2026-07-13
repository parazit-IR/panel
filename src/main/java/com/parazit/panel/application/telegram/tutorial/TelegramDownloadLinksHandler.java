package com.parazit.panel.application.telegram.tutorial;

import com.parazit.panel.application.content.tutorial.TutorialCatalog;
import com.parazit.panel.application.content.tutorial.TutorialContent;
import com.parazit.panel.application.content.tutorial.TutorialDownloadLink;
import com.parazit.panel.application.telegram.TelegramKeyboardFactory;
import com.parazit.panel.application.telegram.command.SendTelegramMessageCommand;
import com.parazit.panel.application.telegram.menu.TelegramMenuLabelProvider;
import com.parazit.panel.application.telegram.model.TelegramCallbackAction;
import com.parazit.panel.application.telegram.model.TelegramInlineButton;
import com.parazit.panel.application.telegram.model.TelegramInlineKeyboard;
import com.parazit.panel.application.telegram.model.TelegramInlineKeyboardRow;
import com.parazit.panel.application.telegram.model.TelegramInteractionContext;
import com.parazit.panel.application.telegram.model.TelegramParseMode;
import com.parazit.panel.application.telegram.model.TelegramResponseAction;
import com.parazit.panel.application.telegram.model.TelegramResponsePlan;
import com.parazit.panel.application.telegram.navigation.TelegramNavigationAction;
import com.parazit.panel.config.properties.TelegramBotProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TelegramDownloadLinksHandler {

    private final TutorialCatalog tutorialCatalog;
    private final TelegramTutorialMessageFormatter formatter;
    private final TelegramKeyboardFactory keyboardFactory;
    private final TelegramMenuLabelProvider labelProvider;
    private final TelegramBotProperties properties;

    public TelegramDownloadLinksHandler(
            TutorialCatalog tutorialCatalog,
            TelegramTutorialMessageFormatter formatter,
            TelegramKeyboardFactory keyboardFactory,
            TelegramMenuLabelProvider labelProvider,
            TelegramBotProperties properties
    ) {
        this.tutorialCatalog = Objects.requireNonNull(tutorialCatalog, "tutorialCatalog must not be null");
        this.formatter = Objects.requireNonNull(formatter, "formatter must not be null");
        this.keyboardFactory = Objects.requireNonNull(keyboardFactory, "keyboardFactory must not be null");
        this.labelProvider = Objects.requireNonNull(labelProvider, "labelProvider must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public TelegramResponsePlan handle(TelegramInteractionContext context) {
        TutorialContent content = tutorialCatalog.downloads().orElse(null);
        String text = content == null ? formatter.empty(context.language()) : formatter.downloads(context.language(), content);
        List<TutorialDownloadLink> links = content == null ? List.of() : content.downloadLinks();
        return new TelegramResponsePlan(List.of(TelegramResponseAction.sendMessage(new SendTelegramMessageCommand(
                context.chatId(),
                text,
                TelegramParseMode.HTML,
                keyboard(context, links),
                properties.disableLinkPreview(),
                null
        ), false)), "tutorial:downloads");
    }

    private TelegramInlineKeyboard keyboard(TelegramInteractionContext context, List<TutorialDownloadLink> links) {
        List<TelegramInlineKeyboardRow> rows = new ArrayList<>();
        for (TutorialDownloadLink link : links) {
            rows.add(keyboardFactory.row(TelegramInlineButton.url(link.label(), link.url().toString())));
        }
        rows.add(keyboardFactory.row(
                keyboardFactory.button(labelProvider.label(context.language(), TelegramNavigationAction.BACK), TelegramCallbackAction.BACK_TO_TUTORIALS, context.telegramUserId(), null, null, context.receivedAt()),
                keyboardFactory.button(labelProvider.label(context.language(), TelegramNavigationAction.HOME), TelegramCallbackAction.BACK_TO_MAIN, context.telegramUserId(), null, null, context.receivedAt())
        ));
        return keyboardFactory.rows(rows);
    }
}
