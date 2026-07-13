package com.parazit.panel.application.telegram.tutorial;

import com.parazit.panel.application.content.tutorial.TutorialCatalog;
import com.parazit.panel.application.content.tutorial.TutorialContent;
import com.parazit.panel.application.content.tutorial.TutorialDownloadLink;
import com.parazit.panel.application.content.tutorial.TutorialPlatform;
import com.parazit.panel.application.telegram.TelegramKeyboardFactory;
import com.parazit.panel.application.telegram.TelegramMessageChunker;
import com.parazit.panel.application.telegram.TelegramMessageCatalog;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TelegramTutorialDetailHandler {

    private static final Logger log = LoggerFactory.getLogger(TelegramTutorialDetailHandler.class);

    private final TutorialCatalog tutorialCatalog;
    private final TelegramTutorialMessageFormatter formatter;
    private final TelegramKeyboardFactory keyboardFactory;
    private final TelegramMenuLabelProvider labelProvider;
    private final TelegramMessageCatalog catalog;
    private final TelegramMessageChunker chunker;
    private final TelegramBotProperties properties;

    public TelegramTutorialDetailHandler(
            TutorialCatalog tutorialCatalog,
            TelegramTutorialMessageFormatter formatter,
            TelegramKeyboardFactory keyboardFactory,
            TelegramMenuLabelProvider labelProvider,
            TelegramMessageCatalog catalog,
            TelegramMessageChunker chunker,
            TelegramBotProperties properties
    ) {
        this.tutorialCatalog = Objects.requireNonNull(tutorialCatalog, "tutorialCatalog must not be null");
        this.formatter = Objects.requireNonNull(formatter, "formatter must not be null");
        this.keyboardFactory = Objects.requireNonNull(keyboardFactory, "keyboardFactory must not be null");
        this.labelProvider = Objects.requireNonNull(labelProvider, "labelProvider must not be null");
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.chunker = Objects.requireNonNull(chunker, "chunker must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public TelegramResponsePlan handle(TelegramInteractionContext context, String platformCode) {
        TutorialPlatform platform = TutorialPlatform.fromCode(platformCode);
        return tutorialCatalog.findEnabled(platform)
                .map(content -> detail(context, content))
                .orElseGet(() -> unavailable(context));
    }

    private TelegramResponsePlan detail(TelegramInteractionContext context, TutorialContent content) {
        log.atInfo()
                .addKeyValue("updateId", context.updateId())
                .addKeyValue("telegramUserId", context.telegramUserId())
                .addKeyValue("platform", content.platform())
                .addKeyValue("locale", context.language())
                .log("Telegram tutorial viewed");
        List<String> chunks = chunker.split(formatter.detail(context.language(), content));
        List<TelegramResponseAction> actions = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            TelegramInlineKeyboard keyboard = i == chunks.size() - 1 ? keyboard(context, content.downloadLinks()) : TelegramInlineKeyboard.empty();
            actions.add(TelegramResponseAction.sendMessage(new SendTelegramMessageCommand(
                    context.chatId(),
                    chunks.get(i),
                    TelegramParseMode.HTML,
                    keyboard,
                    properties.disableLinkPreview(),
                    null
            ), false));
        }
        return new TelegramResponsePlan(actions, "tutorial:detail");
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

    private TelegramResponsePlan unavailable(TelegramInteractionContext context) {
        return new TelegramResponsePlan(List.of(TelegramResponseAction.sendMessage(new SendTelegramMessageCommand(
                context.chatId(),
                catalog.text(context.language(), "telegram.tutorials.empty"),
                TelegramParseMode.NONE,
                keyboardFactory.rows(List.of(keyboardFactory.row(
                        keyboardFactory.button(labelProvider.label(context.language(), TelegramNavigationAction.BACK), TelegramCallbackAction.BACK_TO_TUTORIALS, context.telegramUserId(), null, null, context.receivedAt()),
                        keyboardFactory.button(labelProvider.label(context.language(), TelegramNavigationAction.HOME), TelegramCallbackAction.BACK_TO_MAIN, context.telegramUserId(), null, null, context.receivedAt())
                ))),
                properties.disableLinkPreview(),
                null
        ), false)), "tutorial:unavailable");
    }
}
