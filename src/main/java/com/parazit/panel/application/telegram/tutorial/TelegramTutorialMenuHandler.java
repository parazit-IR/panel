package com.parazit.panel.application.telegram.tutorial;

import com.parazit.panel.application.content.tutorial.TutorialCatalog;
import com.parazit.panel.application.content.tutorial.TutorialContent;
import com.parazit.panel.application.content.tutorial.TutorialPlatform;
import com.parazit.panel.application.telegram.TelegramKeyboardFactory;
import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.application.telegram.command.SendTelegramMessageCommand;
import com.parazit.panel.application.telegram.menu.TelegramMenuLabelProvider;
import com.parazit.panel.application.telegram.model.TelegramCallbackAction;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TelegramTutorialMenuHandler {

    private static final Logger log = LoggerFactory.getLogger(TelegramTutorialMenuHandler.class);

    private final TutorialCatalog tutorialCatalog;
    private final TelegramTutorialMessageFormatter formatter;
    private final TelegramKeyboardFactory keyboardFactory;
    private final TelegramMenuLabelProvider labelProvider;
    private final TelegramMessageCatalog catalog;
    private final TelegramBotProperties properties;

    public TelegramTutorialMenuHandler(
            TutorialCatalog tutorialCatalog,
            TelegramTutorialMessageFormatter formatter,
            TelegramKeyboardFactory keyboardFactory,
            TelegramMenuLabelProvider labelProvider,
            TelegramMessageCatalog catalog,
            TelegramBotProperties properties
    ) {
        this.tutorialCatalog = Objects.requireNonNull(tutorialCatalog, "tutorialCatalog must not be null");
        this.formatter = Objects.requireNonNull(formatter, "formatter must not be null");
        this.keyboardFactory = Objects.requireNonNull(keyboardFactory, "keyboardFactory must not be null");
        this.labelProvider = Objects.requireNonNull(labelProvider, "labelProvider must not be null");
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public TelegramResponsePlan handle(TelegramInteractionContext context) {
        List<TutorialContent> platforms = tutorialCatalog.enabledPlatforms();
        log.atInfo()
                .addKeyValue("updateId", context.updateId())
                .addKeyValue("telegramUserId", context.telegramUserId())
                .addKeyValue("enabledPlatformCount", platforms.size())
                .addKeyValue("locale", context.language())
                .log("Telegram tutorial menu viewed");
        String text = platforms.isEmpty() && tutorialCatalog.downloads().isEmpty()
                ? formatter.empty(context.language())
                : formatter.menu(context.language());
        return new TelegramResponsePlan(List.of(TelegramResponseAction.sendMessage(
                new SendTelegramMessageCommand(
                        context.chatId(),
                        text,
                        TelegramParseMode.HTML,
                        keyboard(context, platforms),
                        properties.disableLinkPreview(),
                        null
                ),
                false
        )), "tutorials");
    }

    private TelegramInlineKeyboard keyboard(TelegramInteractionContext context, List<TutorialContent> platforms) {
        List<TelegramInlineKeyboardRow> rows = new ArrayList<>();
        for (TutorialContent content : platforms) {
            rows.add(keyboardFactory.row(keyboardFactory.button(
                    platformLabel(context.language(), content.platform()),
                    TelegramCallbackAction.SHOW_TUTORIAL_PLATFORM,
                    context.telegramUserId(),
                    null,
                    content.platform().code(),
                    context.receivedAt()
            )));
        }
        tutorialCatalog.downloads().ifPresent(content -> rows.add(keyboardFactory.row(keyboardFactory.button(
                catalog.text(context.language(), "telegram.tutorials.downloads"),
                TelegramCallbackAction.SHOW_DOWNLOAD_LINKS,
                context.telegramUserId(),
                null,
                TutorialPlatform.GENERAL_DOWNLOADS.code(),
                context.receivedAt()
        ))));
        rows.add(keyboardFactory.row(
                keyboardFactory.button(labelProvider.label(context.language(), TelegramNavigationAction.BACK), TelegramCallbackAction.BACK_TO_MAIN, context.telegramUserId(), null, null, context.receivedAt()),
                keyboardFactory.button(labelProvider.label(context.language(), TelegramNavigationAction.HOME), TelegramCallbackAction.BACK_TO_MAIN, context.telegramUserId(), null, null, context.receivedAt())
        ));
        return keyboardFactory.rows(rows);
    }

    private String platformLabel(String language, TutorialPlatform platform) {
        return switch (platform) {
            case ANDROID -> catalog.text(language, "telegram.tutorials.android");
            case IOS -> catalog.text(language, "telegram.tutorials.ios");
            case WINDOWS -> catalog.text(language, "telegram.tutorials.windows");
            case LINUX -> catalog.text(language, "telegram.tutorials.linux");
            case MACOS -> catalog.text(language, "telegram.tutorials.macos");
            case GENERAL_DOWNLOADS -> catalog.text(language, "telegram.tutorials.downloads");
        };
    }
}
