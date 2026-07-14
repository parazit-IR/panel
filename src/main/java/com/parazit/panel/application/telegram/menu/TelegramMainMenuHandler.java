package com.parazit.panel.application.telegram.menu;

import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.application.telegram.command.SendTelegramMessageCommand;
import com.parazit.panel.application.telegram.support.TelegramSupportMenuHandler;
import com.parazit.panel.application.telegram.tariff.TelegramTariffCatalogHandler;
import com.parazit.panel.application.telegram.tutorial.TelegramTutorialMenuHandler;
import com.parazit.panel.application.telegram.handler.MySubscriptionsTelegramCommandHandler;
import com.parazit.panel.application.telegram.keyboard.TelegramReplyKeyboard;
import com.parazit.panel.application.telegram.model.TelegramInlineKeyboard;
import com.parazit.panel.application.telegram.model.TelegramInteractionContext;
import com.parazit.panel.application.telegram.model.TelegramParseMode;
import com.parazit.panel.application.telegram.model.TelegramResponseAction;
import com.parazit.panel.application.telegram.model.TelegramResponsePlan;
import com.parazit.panel.application.telegram.purchase.TelegramPurchaseFlowHandler;
import com.parazit.panel.application.telegram.renewal.TelegramRenewalFlowHandler;
import com.parazit.panel.application.telegram.wallet.TelegramWalletHandler;
import com.parazit.panel.config.properties.TelegramBotProperties;
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
    private final TelegramMenuMetrics metrics;
    private final TelegramBotProperties properties;
    private final MySubscriptionsTelegramCommandHandler subscriptionsHandler;
    private final TelegramTariffCatalogHandler tariffCatalogHandler;
    private final TelegramTutorialMenuHandler tutorialMenuHandler;
    private final TelegramSupportMenuHandler supportMenuHandler;
    private final TelegramPurchaseFlowHandler purchaseFlowHandler;
    private final TelegramRenewalFlowHandler renewalFlowHandler;
    private final TelegramWalletHandler walletHandler;

    public TelegramMainMenuHandler(
            TelegramMenuFeatureAvailabilityService availabilityService,
            TelegramUnavailableFeatureFormatter unavailableFormatter,
            TelegramMainReplyKeyboardFactory replyKeyboardFactory,
            TelegramMessageCatalog catalog,
            TelegramMenuMetrics metrics,
            TelegramBotProperties properties,
            MySubscriptionsTelegramCommandHandler subscriptionsHandler,
            TelegramTariffCatalogHandler tariffCatalogHandler,
            TelegramTutorialMenuHandler tutorialMenuHandler,
            TelegramSupportMenuHandler supportMenuHandler,
            TelegramPurchaseFlowHandler purchaseFlowHandler,
            TelegramRenewalFlowHandler renewalFlowHandler,
            TelegramWalletHandler walletHandler
    ) {
        this.availabilityService = Objects.requireNonNull(availabilityService, "availabilityService must not be null");
        this.unavailableFormatter = Objects.requireNonNull(unavailableFormatter, "unavailableFormatter must not be null");
        this.replyKeyboardFactory = Objects.requireNonNull(replyKeyboardFactory, "replyKeyboardFactory must not be null");
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.subscriptionsHandler = Objects.requireNonNull(subscriptionsHandler, "subscriptionsHandler must not be null");
        this.tariffCatalogHandler = Objects.requireNonNull(tariffCatalogHandler, "tariffCatalogHandler must not be null");
        this.tutorialMenuHandler = Objects.requireNonNull(tutorialMenuHandler, "tutorialMenuHandler must not be null");
        this.supportMenuHandler = Objects.requireNonNull(supportMenuHandler, "supportMenuHandler must not be null");
        this.purchaseFlowHandler = Objects.requireNonNull(purchaseFlowHandler, "purchaseFlowHandler must not be null");
        this.renewalFlowHandler = Objects.requireNonNull(renewalFlowHandler, "renewalFlowHandler must not be null");
        this.walletHandler = Objects.requireNonNull(walletHandler, "walletHandler must not be null");
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
            case BUY_SUBSCRIPTION -> purchaseFlowHandler.showPlanCatalog(context);
            case RENEW_SERVICE -> renewalFlowHandler.listServices(context, 1);
            case SHOW_TARIFFS -> tariffCatalogHandler.handle(context, 1);
            case MY_SERVICES -> subscriptionsHandler.handle(context);
            case SHOW_TUTORIALS -> tutorialMenuHandler.handle(context);
            case SHOW_SUPPORT -> supportMenuHandler.handle(context);
            case SHOW_WALLET -> walletHandler.show(context);
            case REQUEST_TRIAL -> unavailable(context, availability);
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

    private TelegramReplyKeyboard mainKeyboard(TelegramInteractionContext context) {
        return replyKeyboardFactory.mainKeyboard(context.language());
    }
}
