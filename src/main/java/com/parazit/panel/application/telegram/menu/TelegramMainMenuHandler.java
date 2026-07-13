package com.parazit.panel.application.telegram.menu;

import com.parazit.panel.application.plan.catalog.query.ListAvailablePlansQuery;
import com.parazit.panel.application.plan.catalog.result.AvailablePlanResult;
import com.parazit.panel.application.port.in.plan.catalog.ListAvailablePlansUseCase;
import com.parazit.panel.application.telegram.TelegramHtmlEscaper;
import com.parazit.panel.application.telegram.TelegramKeyboardFactory;
import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.application.telegram.TelegramPersianTextFormatter;
import com.parazit.panel.application.telegram.command.SendTelegramMessageCommand;
import com.parazit.panel.application.telegram.support.TelegramSupportMenuHandler;
import com.parazit.panel.application.telegram.tariff.TelegramTariffCatalogHandler;
import com.parazit.panel.application.telegram.tutorial.TelegramTutorialMenuHandler;
import com.parazit.panel.application.telegram.handler.MySubscriptionsTelegramCommandHandler;
import com.parazit.panel.application.telegram.keyboard.TelegramReplyKeyboard;
import com.parazit.panel.application.telegram.model.TelegramCallbackAction;
import com.parazit.panel.application.telegram.model.TelegramInlineKeyboard;
import com.parazit.panel.application.telegram.model.TelegramInlineKeyboardRow;
import com.parazit.panel.application.telegram.model.TelegramInteractionContext;
import com.parazit.panel.application.telegram.model.TelegramParseMode;
import com.parazit.panel.application.telegram.model.TelegramResponseAction;
import com.parazit.panel.application.telegram.model.TelegramResponsePlan;
import com.parazit.panel.config.properties.TelegramBotProperties;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TelegramMainMenuHandler {

    private static final Logger log = LoggerFactory.getLogger(TelegramMainMenuHandler.class);

    private final TelegramMenuFeatureAvailabilityService availabilityService;
    private final TelegramUnavailableFeatureFormatter unavailableFormatter;
    private final TelegramMainReplyKeyboardFactory replyKeyboardFactory;
    private final TelegramMessageCatalog catalog;
    private final TelegramHtmlEscaper htmlEscaper;
    private final TelegramKeyboardFactory inlineKeyboardFactory;
    private final TelegramMenuLabelProvider labelProvider;
    private final TelegramPersianTextFormatter persianTextFormatter;
    private final TelegramMenuMetrics metrics;
    private final TelegramBotProperties properties;
    private final ListAvailablePlansUseCase listAvailablePlansUseCase;
    private final MySubscriptionsTelegramCommandHandler subscriptionsHandler;
    private final TelegramTariffCatalogHandler tariffCatalogHandler;
    private final TelegramTutorialMenuHandler tutorialMenuHandler;
    private final TelegramSupportMenuHandler supportMenuHandler;

    public TelegramMainMenuHandler(
            TelegramMenuFeatureAvailabilityService availabilityService,
            TelegramUnavailableFeatureFormatter unavailableFormatter,
            TelegramMainReplyKeyboardFactory replyKeyboardFactory,
            TelegramMessageCatalog catalog,
            TelegramHtmlEscaper htmlEscaper,
            TelegramKeyboardFactory inlineKeyboardFactory,
            TelegramMenuLabelProvider labelProvider,
            TelegramPersianTextFormatter persianTextFormatter,
            TelegramMenuMetrics metrics,
            TelegramBotProperties properties,
            ListAvailablePlansUseCase listAvailablePlansUseCase,
            MySubscriptionsTelegramCommandHandler subscriptionsHandler,
            TelegramTariffCatalogHandler tariffCatalogHandler,
            TelegramTutorialMenuHandler tutorialMenuHandler,
            TelegramSupportMenuHandler supportMenuHandler
    ) {
        this.availabilityService = Objects.requireNonNull(availabilityService, "availabilityService must not be null");
        this.unavailableFormatter = Objects.requireNonNull(unavailableFormatter, "unavailableFormatter must not be null");
        this.replyKeyboardFactory = Objects.requireNonNull(replyKeyboardFactory, "replyKeyboardFactory must not be null");
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.htmlEscaper = Objects.requireNonNull(htmlEscaper, "htmlEscaper must not be null");
        this.inlineKeyboardFactory = Objects.requireNonNull(inlineKeyboardFactory, "inlineKeyboardFactory must not be null");
        this.labelProvider = Objects.requireNonNull(labelProvider, "labelProvider must not be null");
        this.persianTextFormatter = Objects.requireNonNull(persianTextFormatter, "persianTextFormatter must not be null");
        this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.listAvailablePlansUseCase = Objects.requireNonNull(listAvailablePlansUseCase, "listAvailablePlansUseCase must not be null");
        this.subscriptionsHandler = Objects.requireNonNull(subscriptionsHandler, "subscriptionsHandler must not be null");
        this.tariffCatalogHandler = Objects.requireNonNull(tariffCatalogHandler, "tariffCatalogHandler must not be null");
        this.tutorialMenuHandler = Objects.requireNonNull(tutorialMenuHandler, "tutorialMenuHandler must not be null");
        this.supportMenuHandler = Objects.requireNonNull(supportMenuHandler, "supportMenuHandler must not be null");
    }

    public TelegramResponsePlan handle(TelegramInteractionContext context, TelegramMainMenuAction action) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(action, "action must not be null");
        TelegramMenuFeatureAvailability availability = availabilityService.availability(action);
        if (!availability.visible()) {
            metrics.recordMenuAction(action, "hidden");
            return unavailable(context, availability);
        }
        if (!availability.enabled()) {
            metrics.recordMenuAction(action, "unavailable");
            log.atInfo()
                    .addKeyValue("updateId", context.updateId())
                    .addKeyValue("telegramUserId", context.telegramUserId())
                    .addKeyValue("action", action)
                    .addKeyValue("locale", context.language())
                    .log("Unavailable Telegram menu feature selected");
            return unavailable(context, availability);
        }
        metrics.recordMenuAction(action, "routed");
        log.atInfo()
                .addKeyValue("updateId", context.updateId())
                .addKeyValue("telegramUserId", context.telegramUserId())
                .addKeyValue("action", action)
                .addKeyValue("locale", context.language())
                .log("Telegram main menu action selected");
        return switch (action) {
            case BUY_SUBSCRIPTION -> planCatalog(context, action);
            case SHOW_TARIFFS -> tariffCatalogHandler.handle(context, 1);
            case MY_SERVICES -> subscriptionsHandler.handle(context);
            case SHOW_TUTORIALS -> tutorialMenuHandler.handle(context);
            case SHOW_SUPPORT -> supportMenuHandler.handle(context);
            case RENEW_SERVICE, REQUEST_TRIAL, SHOW_WALLET -> unavailable(context, availability);
        };
    }

    public TelegramResponsePlan showHome(TelegramInteractionContext context, String messageKey, String handlerKey) {
        metrics.recordNavigation("unknown", "main_menu");
        return new TelegramResponsePlan(List.of(TelegramResponseAction.sendMessage(
                new SendTelegramMessageCommand(
                        context.chatId(),
                        catalog.text(context.language(), messageKey),
                        TelegramParseMode.NONE,
                        TelegramInlineKeyboard.empty(),
                        mainKeyboard(context),
                        properties.disableLinkPreview(),
                        null
                ),
                false
        )), handlerKey);
    }

    private TelegramResponsePlan unavailable(TelegramInteractionContext context, TelegramMenuFeatureAvailability availability) {
        return new TelegramResponsePlan(List.of(TelegramResponseAction.sendMessage(
                new SendTelegramMessageCommand(
                        context.chatId(),
                        unavailableFormatter.format(context.language(), availability),
                        TelegramParseMode.NONE,
                        TelegramInlineKeyboard.empty(),
                        mainKeyboard(context),
                        properties.disableLinkPreview(),
                        null
                ),
                false
        )), "menu:unavailable");
    }

    private TelegramResponsePlan planCatalog(TelegramInteractionContext context, TelegramMainMenuAction action) {
        List<AvailablePlanResult> plans = listAvailablePlansUseCase.list(new ListAvailablePlansQuery(null))
                .stream()
                .sorted(Comparator.comparingInt(AvailablePlanResult::displayOrder).thenComparing(AvailablePlanResult::code))
                .toList();
        String text = formatPlanCatalog(context.language(), plans, action);
        List<TelegramInlineKeyboardRow> rows = new ArrayList<>();
        rows.add(inlineKeyboardFactory.row(inlineKeyboardFactory.button(
                labelProvider.label(context.language(), com.parazit.panel.application.telegram.navigation.TelegramNavigationAction.HOME),
                TelegramCallbackAction.BACK_TO_MAIN,
                context.telegramUserId(),
                null,
                null,
                null,
                context.receivedAt()
        )));
        return new TelegramResponsePlan(List.of(TelegramResponseAction.sendMessage(
                new SendTelegramMessageCommand(
                        context.chatId(),
                        text,
                        TelegramParseMode.HTML,
                        inlineKeyboardFactory.rows(rows),
                        properties.disableLinkPreview(),
                        null
                ),
                false
        )), "menu:plans");
    }

    private String formatPlanCatalog(String language, List<AvailablePlanResult> plans, TelegramMainMenuAction action) {
        if (plans.isEmpty()) {
            return catalog.text(language, "telegram.plans.empty");
        }
        StringBuilder builder = new StringBuilder();
        builder.append(catalog.text(language, action == TelegramMainMenuAction.BUY_SUBSCRIPTION
                ? "telegram.plans.buy_title"
                : "telegram.plans.tariffs_title"));
        int index = 1;
        for (AvailablePlanResult plan : plans) {
            builder.append("\n\n")
                    .append(persianTextFormatter.formatNumber(index++, language))
                    .append(". ")
                    .append(htmlEscaper.escape(plan.name() == null || plan.name().isBlank() ? plan.code() : plan.name()))
                    .append("\n")
                    .append(catalog.text(language, "telegram.plans.price"))
                    .append(": ")
                    .append(persianTextFormatter.formatAmount(plan.priceAmount(), plan.currency().name(), language))
                    .append("\n")
                    .append(catalog.text(language, "telegram.plans.duration"))
                    .append(": ")
                    .append(persianTextFormatter.formatNumber(plan.durationDays(), language))
                    .append(" ")
                    .append(catalog.text(language, "telegram.plans.days"));
        }
        builder.append("\n\n").append(catalog.text(language, "telegram.plans.purchase_placeholder"));
        return builder.toString();
    }

    private TelegramReplyKeyboard mainKeyboard(TelegramInteractionContext context) {
        return replyKeyboardFactory.mainKeyboard(context.language());
    }
}
