package com.parazit.panel.application.telegram.handler;

import com.parazit.panel.application.port.in.subscription.RotateSubscriptionTokenUseCase;
import com.parazit.panel.application.port.in.subscription.delivery.BuildSubscriptionUrlUseCase;
import com.parazit.panel.application.port.in.subscription.delivery.GenerateSubscriptionUrlQrCodeUseCase;
import com.parazit.panel.application.port.in.subscription.delivery.GenerateVlessConfigQrCodeUseCase;
import com.parazit.panel.application.port.in.subscription.delivery.GetSubscriptionConfigEntryUseCase;
import com.parazit.panel.application.port.in.subscription.delivery.GetSubscriptionDeliverySummaryUseCase;
import com.parazit.panel.application.qrcode.model.QrImageFormat;
import com.parazit.panel.application.qrcode.model.QrRenderOptions;
import com.parazit.panel.application.subscription.command.RotateSubscriptionTokenCommand;
import com.parazit.panel.application.subscription.delivery.BuildSubscriptionUrlCommand;
import com.parazit.panel.application.subscription.delivery.BuildSubscriptionUrlResult;
import com.parazit.panel.application.subscription.delivery.GenerateSubscriptionUrlQrCommand;
import com.parazit.panel.application.subscription.delivery.GenerateVlessConfigQrCommand;
import com.parazit.panel.application.subscription.delivery.QrCodeImageResult;
import com.parazit.panel.application.subscription.delivery.SubscriptionConfigEntryResult;
import com.parazit.panel.application.subscription.delivery.SubscriptionDeliveryEntry;
import com.parazit.panel.application.subscription.delivery.SubscriptionDeliverySummary;
import com.parazit.panel.application.subscription.result.CreateSubscriptionResult;
import com.parazit.panel.application.telegram.TelegramCallbackDataCodec;
import com.parazit.panel.application.telegram.TelegramKeyboardFactory;
import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.application.telegram.TelegramMessageFormatter;
import com.parazit.panel.application.telegram.TelegramSensitiveActionService;
import com.parazit.panel.application.telegram.command.AnswerTelegramCallbackCommand;
import com.parazit.panel.application.telegram.command.SendTelegramMessageCommand;
import com.parazit.panel.application.telegram.command.SendTelegramPhotoCommand;
import com.parazit.panel.application.telegram.menu.TelegramMainReplyKeyboardFactory;
import com.parazit.panel.application.telegram.menu.TelegramMenuLabelProvider;
import com.parazit.panel.application.telegram.model.TelegramCallbackAction;
import com.parazit.panel.application.telegram.model.TelegramCallbackPayload;
import com.parazit.panel.application.telegram.model.TelegramInlineKeyboard;
import com.parazit.panel.application.telegram.model.TelegramInlineKeyboardRow;
import com.parazit.panel.application.telegram.model.TelegramInteractionContext;
import com.parazit.panel.application.telegram.model.TelegramParseMode;
import com.parazit.panel.application.telegram.model.TelegramResponseAction;
import com.parazit.panel.application.telegram.model.TelegramResponsePlan;
import com.parazit.panel.application.telegram.faq.TelegramFaqDetailHandler;
import com.parazit.panel.application.telegram.faq.TelegramFaqListHandler;
import com.parazit.panel.application.telegram.account.TelegramCustomerAccountHandler;
import com.parazit.panel.application.telegram.menu.TelegramMainMenuAction;
import com.parazit.panel.application.telegram.menu.TelegramMainMenuHandler;
import com.parazit.panel.application.telegram.purchase.TelegramPurchaseFlowHandler;
import com.parazit.panel.application.telegram.renewal.TelegramRenewalFlowHandler;
import com.parazit.panel.application.telegram.service.TelegramMyServicesHandler;
import com.parazit.panel.application.telegram.service.TelegramServiceDetailsHandler;
import com.parazit.panel.application.telegram.service.TelegramServiceSearchHandler;
import com.parazit.panel.application.telegram.support.TelegramSupportMenuHandler;
import com.parazit.panel.application.telegram.tariff.TelegramTariffCatalogHandler;
import com.parazit.panel.application.telegram.tutorial.TelegramDownloadLinksHandler;
import com.parazit.panel.application.telegram.tutorial.TelegramTutorialDetailHandler;
import com.parazit.panel.application.telegram.tutorial.TelegramTutorialMenuHandler;
import com.parazit.panel.config.properties.QrCodeProperties;
import com.parazit.panel.config.properties.TelegramBotProperties;
import com.parazit.panel.domain.telegram.TelegramSensitiveAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TelegramCallbackHandler {

    private final TelegramCallbackDataCodec callbackDataCodec;
    private final TelegramKeyboardFactory keyboardFactory;
    private final TelegramMessageCatalog catalog;
    private final TelegramMessageFormatter formatter;
    private final TelegramMainReplyKeyboardFactory replyKeyboardFactory;
    private final TelegramMenuLabelProvider labelProvider;
    private final TelegramBotProperties telegramProperties;
    private final QrCodeProperties qrProperties;
    private final GetSubscriptionDeliverySummaryUseCase getSummaryUseCase;
    private final GetSubscriptionConfigEntryUseCase getConfigEntryUseCase;
    private final GenerateVlessConfigQrCodeUseCase generateVlessQrUseCase;
    private final GenerateSubscriptionUrlQrCodeUseCase generateSubscriptionUrlQrUseCase;
    private final RotateSubscriptionTokenUseCase rotateSubscriptionTokenUseCase;
    private final BuildSubscriptionUrlUseCase buildSubscriptionUrlUseCase;
    private final TelegramSensitiveActionService sensitiveActionService;
    private final MySubscriptionsTelegramCommandHandler mySubscriptionsTelegramCommandHandler;
    private final TelegramMainMenuHandler mainMenuHandler;
    private final TelegramTariffCatalogHandler tariffCatalogHandler;
    private final TelegramTutorialMenuHandler tutorialMenuHandler;
    private final TelegramTutorialDetailHandler tutorialDetailHandler;
    private final TelegramDownloadLinksHandler downloadLinksHandler;
    private final TelegramSupportMenuHandler supportMenuHandler;
    private final TelegramFaqListHandler faqListHandler;
    private final TelegramFaqDetailHandler faqDetailHandler;
    private final TelegramCustomerAccountHandler accountHandler;
    private final TelegramMyServicesHandler myServicesHandler;
    private final TelegramServiceDetailsHandler serviceDetailsHandler;
    private final TelegramServiceSearchHandler serviceSearchHandler;
    private final PaymentsTelegramCommandHandler paymentsTelegramCommandHandler;
    private final SettingsTelegramCommandHandler settingsTelegramCommandHandler;
    private final TelegramPurchaseFlowHandler purchaseFlowHandler;
    private final TelegramRenewalFlowHandler renewalFlowHandler;

    public TelegramCallbackHandler(
            TelegramCallbackDataCodec callbackDataCodec,
            TelegramKeyboardFactory keyboardFactory,
            TelegramMessageCatalog catalog,
            TelegramMessageFormatter formatter,
            TelegramMainReplyKeyboardFactory replyKeyboardFactory,
            TelegramMenuLabelProvider labelProvider,
            TelegramBotProperties telegramProperties,
            QrCodeProperties qrProperties,
            GetSubscriptionDeliverySummaryUseCase getSummaryUseCase,
            GetSubscriptionConfigEntryUseCase getConfigEntryUseCase,
            GenerateVlessConfigQrCodeUseCase generateVlessQrUseCase,
            GenerateSubscriptionUrlQrCodeUseCase generateSubscriptionUrlQrUseCase,
            RotateSubscriptionTokenUseCase rotateSubscriptionTokenUseCase,
            BuildSubscriptionUrlUseCase buildSubscriptionUrlUseCase,
            TelegramSensitiveActionService sensitiveActionService,
            MySubscriptionsTelegramCommandHandler mySubscriptionsTelegramCommandHandler,
            TelegramMainMenuHandler mainMenuHandler,
            TelegramTariffCatalogHandler tariffCatalogHandler,
            TelegramTutorialMenuHandler tutorialMenuHandler,
            TelegramTutorialDetailHandler tutorialDetailHandler,
            TelegramDownloadLinksHandler downloadLinksHandler,
            TelegramSupportMenuHandler supportMenuHandler,
            TelegramFaqListHandler faqListHandler,
            TelegramFaqDetailHandler faqDetailHandler,
            TelegramCustomerAccountHandler accountHandler,
            TelegramMyServicesHandler myServicesHandler,
            TelegramServiceDetailsHandler serviceDetailsHandler,
            TelegramServiceSearchHandler serviceSearchHandler,
            PaymentsTelegramCommandHandler paymentsTelegramCommandHandler,
            SettingsTelegramCommandHandler settingsTelegramCommandHandler,
            TelegramPurchaseFlowHandler purchaseFlowHandler,
            TelegramRenewalFlowHandler renewalFlowHandler
    ) {
        this.callbackDataCodec = Objects.requireNonNull(callbackDataCodec, "callbackDataCodec must not be null");
        this.keyboardFactory = Objects.requireNonNull(keyboardFactory, "keyboardFactory must not be null");
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.formatter = Objects.requireNonNull(formatter, "formatter must not be null");
        this.replyKeyboardFactory = Objects.requireNonNull(replyKeyboardFactory, "replyKeyboardFactory must not be null");
        this.labelProvider = Objects.requireNonNull(labelProvider, "labelProvider must not be null");
        this.telegramProperties = Objects.requireNonNull(telegramProperties, "telegramProperties must not be null");
        this.qrProperties = Objects.requireNonNull(qrProperties, "qrProperties must not be null");
        this.getSummaryUseCase = Objects.requireNonNull(getSummaryUseCase, "getSummaryUseCase must not be null");
        this.getConfigEntryUseCase = Objects.requireNonNull(getConfigEntryUseCase, "getConfigEntryUseCase must not be null");
        this.generateVlessQrUseCase = Objects.requireNonNull(generateVlessQrUseCase, "generateVlessQrUseCase must not be null");
        this.generateSubscriptionUrlQrUseCase = Objects.requireNonNull(generateSubscriptionUrlQrUseCase, "generateSubscriptionUrlQrUseCase must not be null");
        this.rotateSubscriptionTokenUseCase = Objects.requireNonNull(rotateSubscriptionTokenUseCase, "rotateSubscriptionTokenUseCase must not be null");
        this.buildSubscriptionUrlUseCase = Objects.requireNonNull(buildSubscriptionUrlUseCase, "buildSubscriptionUrlUseCase must not be null");
        this.sensitiveActionService = Objects.requireNonNull(sensitiveActionService, "sensitiveActionService must not be null");
        this.mySubscriptionsTelegramCommandHandler = Objects.requireNonNull(mySubscriptionsTelegramCommandHandler, "mySubscriptionsTelegramCommandHandler must not be null");
        this.mainMenuHandler = Objects.requireNonNull(mainMenuHandler, "mainMenuHandler must not be null");
        this.tariffCatalogHandler = Objects.requireNonNull(tariffCatalogHandler, "tariffCatalogHandler must not be null");
        this.tutorialMenuHandler = Objects.requireNonNull(tutorialMenuHandler, "tutorialMenuHandler must not be null");
        this.tutorialDetailHandler = Objects.requireNonNull(tutorialDetailHandler, "tutorialDetailHandler must not be null");
        this.downloadLinksHandler = Objects.requireNonNull(downloadLinksHandler, "downloadLinksHandler must not be null");
        this.supportMenuHandler = Objects.requireNonNull(supportMenuHandler, "supportMenuHandler must not be null");
        this.faqListHandler = Objects.requireNonNull(faqListHandler, "faqListHandler must not be null");
        this.faqDetailHandler = Objects.requireNonNull(faqDetailHandler, "faqDetailHandler must not be null");
        this.accountHandler = Objects.requireNonNull(accountHandler, "accountHandler must not be null");
        this.myServicesHandler = Objects.requireNonNull(myServicesHandler, "myServicesHandler must not be null");
        this.serviceDetailsHandler = Objects.requireNonNull(serviceDetailsHandler, "serviceDetailsHandler must not be null");
        this.serviceSearchHandler = Objects.requireNonNull(serviceSearchHandler, "serviceSearchHandler must not be null");
        this.paymentsTelegramCommandHandler = Objects.requireNonNull(paymentsTelegramCommandHandler, "paymentsTelegramCommandHandler must not be null");
        this.settingsTelegramCommandHandler = Objects.requireNonNull(settingsTelegramCommandHandler, "settingsTelegramCommandHandler must not be null");
        this.purchaseFlowHandler = Objects.requireNonNull(purchaseFlowHandler, "purchaseFlowHandler must not be null");
        this.renewalFlowHandler = Objects.requireNonNull(renewalFlowHandler, "renewalFlowHandler must not be null");
    }

    public TelegramResponsePlan handle(TelegramInteractionContext context, String callbackData) {
        TelegramCallbackPayload payload;
        try {
            payload = callbackDataCodec.decode(callbackData, context.telegramUserId(), context.receivedAt());
        } catch (IllegalArgumentException exception) {
            return new TelegramResponsePlan(List.of(
                    answer(context, catalog.text(context.language(), "not_available"))
            ), "callback:invalid");
        }
        List<TelegramResponseAction> actions = new ArrayList<>();
        actions.add(answer(context, ""));
        switch (payload.action()) {
            case MAIN_MENU, BACK_TO_MAIN -> actions.add(menuMessage(context));
            case HELP -> actions.add(helpMessage(context));
            case MY_SUBSCRIPTIONS, BACK_TO_SUBSCRIPTIONS, LIST_MY_SERVICES -> actions.addAll(myServicesHandler.handle(context, 1).actions());
            case MY_SERVICES_PAGE, BACK_TO_MY_SERVICES -> actions.addAll(myServicesHandler.handle(context, configIndex(payload)).actions());
            case VIEW_SUBSCRIPTION, SHOW_SERVICE_DETAILS -> actions.addAll(serviceDetailsHandler.handle(context, requireSubscription(payload), configIndex(payload)).actions());
            case REFRESH_SERVICE_STATUS -> actions.addAll(serviceDetailsHandler.refresh(context, requireSubscription(payload), configIndex(payload)).actions());
            case SHOW_CONFIG, SHOW_VLESS_CONFIG -> actions.add(configText(context, requireSubscription(payload), configIndex(payload)));
            case SHOW_CONFIG_QR, SHOW_SUBSCRIPTION_QR -> actions.add(configQr(context, requireSubscription(payload), configIndex(payload)));
            case REQUEST_SUBSCRIPTION_LINK, SHOW_SUBSCRIPTION_LINK -> actions.add(rotationWarning(context, requireSubscription(payload)));
            case CONFIRM_ROTATE_SUBSCRIPTION_TOKEN -> actions.addAll(confirmRotation(context, requireAction(payload)));
            case CANCEL_ROTATION -> actions.add(cancelRotation(context, requireAction(payload)));
            case SHOW_ACCOUNT, BACK_TO_ACCOUNT -> actions.addAll(accountHandler.handle(context).actions());
            case SEARCH_MY_SERVICES -> actions.addAll(serviceSearchHandler.begin(context, configIndex(payload)).actions());
            case REQUEST_RENEWAL -> actions.addAll(renewalFlowHandler.target(context, requireSubscription(payload), 1).actions());
            case SHOW_PAYMENTS -> actions.addAll(paymentsTelegramCommandHandler.handle(context).actions());
            case SHOW_NOTIFICATION_SETTINGS -> actions.addAll(settingsTelegramCommandHandler.handle(context).actions());
            case SHOW_TARIFFS -> actions.addAll(tariffCatalogHandler.handle(context, 1).actions());
            case SHOW_TARIFF_PAGE -> actions.addAll(tariffCatalogHandler.handle(context, configIndex(payload)).actions());
            case BUY_SUBSCRIPTION -> actions.addAll(mainMenuHandler.handle(context, TelegramMainMenuAction.BUY_SUBSCRIPTION).actions());
            case SHOW_PLAN_CATALOG, BACK_TO_PLAN_CATALOG -> actions.addAll(purchaseFlowHandler.showPlanCatalog(context).actions());
            case SHOW_PLAN_DETAILS, BACK_TO_PLAN_DETAILS -> actions.addAll(purchaseFlowHandler.showPlanDetails(context, requireSubscription(payload)).actions());
            case SELECT_PLAN -> actions.addAll(purchaseFlowHandler.selectPlan(context, requireSubscription(payload)).actions());
            case SHOW_PRE_INVOICE, BACK_TO_PRE_INVOICE -> actions.addAll(purchaseFlowHandler.showPreInvoice(context, requireSubscription(payload)).actions());
            case CONTINUE_TO_PAYMENT, SHOW_PAYMENT_METHODS -> actions.addAll(purchaseFlowHandler.continueToPayment(context, requireSubscription(payload)).actions());
            case SELECT_MANUAL_PAYMENT -> actions.addAll(purchaseFlowHandler.selectManual(context, requireSubscription(payload)).actions());
            case SELECT_ONLINE_PAYMENT -> actions.addAll(purchaseFlowHandler.selectOnline(context, requireSubscription(payload)).actions());
            case APPLY_DISCOUNT_PLACEHOLDER -> actions.addAll(purchaseFlowHandler.discountUnavailable(context).actions());
            case UPLOAD_MANUAL_RECEIPT, REFRESH_PAYMENT_STATUS, REQUEST_CANCEL_PAYMENT, VIEW_PAYMENT -> actions.addAll(purchaseFlowHandler.paymentActionUnavailable(context).actions());
            case LIST_RENEWABLE_SERVICES -> actions.addAll(renewalFlowHandler.listServices(context, 1).actions());
            case RENEWABLE_SERVICES_PAGE, BACK_TO_RENEWABLE_SERVICES -> actions.addAll(renewalFlowHandler.listServices(context, configIndex(payload)).actions());
            case SHOW_RENEWAL_TARGET, BACK_TO_RENEWAL_TARGET -> actions.addAll(renewalFlowHandler.target(context, requireSubscription(payload), configIndex(payload)).actions());
            case LIST_RENEWAL_PLANS, RENEWAL_PLANS_PAGE -> actions.addAll(renewalFlowHandler.plans(context, requireSubscription(payload), configIndex(payload)).actions());
            case SELECT_RENEWAL_PLAN -> actions.addAll(renewalFlowHandler.selectPlanByIndex(context, requireSubscription(payload), configIndex(payload)).actions());
            case SHOW_RENEWAL_PRE_INVOICE -> actions.addAll(renewalFlowHandler.preInvoice(context, requireSubscription(payload)).actions());
            case CONFIRM_RENEWAL_ORDER -> actions.addAll(renewalFlowHandler.confirm(context, requireSubscription(payload)).actions());
            case SHOW_TUTORIALS, BACK_TO_TUTORIALS -> actions.addAll(tutorialMenuHandler.handle(context).actions());
            case SHOW_TUTORIAL_PLATFORM -> actions.addAll(tutorialDetailHandler.handle(context, requireReference(payload)).actions());
            case SHOW_DOWNLOAD_LINKS -> actions.addAll(downloadLinksHandler.handle(context).actions());
            case SHOW_SUPPORT, BACK_TO_SUPPORT -> actions.addAll(supportMenuHandler.handle(context).actions());
            case SHOW_FAQ, BACK_TO_FAQ, SHOW_FAQ_PAGE -> actions.addAll(faqListHandler.handle(context, configIndex(payload)).actions());
            case SHOW_FAQ_ITEM -> actions.addAll(faqDetailHandler.handle(context, requireReference(payload), configIndex(payload)).actions());
        }
        return new TelegramResponsePlan(actions, "callback:" + payload.action().name().toLowerCase());
    }

    private TelegramResponseAction subscriptionsMessage(TelegramInteractionContext context) {
        return mySubscriptionsTelegramCommandHandler.handle(context).actions().getFirst();
    }

    private TelegramResponseAction unavailable(TelegramInteractionContext context, String key) {
        return TelegramResponseAction.sendMessage(new SendTelegramMessageCommand(
                context.chatId(),
                catalog.text(context.language(), key),
                TelegramParseMode.NONE,
                TelegramInlineKeyboard.empty(),
                telegramProperties.disableLinkPreview(),
                null
        ), false);
    }

    private TelegramResponseAction subscriptionDetails(TelegramInteractionContext context, UUID subscriptionId) {
        SubscriptionDeliverySummary summary = getSummaryUseCase.get(context.telegramUserId(), subscriptionId);
        List<TelegramInlineKeyboardRow> rows = new ArrayList<>();
        for (SubscriptionDeliveryEntry entry : summary.entries()) {
            rows.add(keyboardFactory.row(
                    keyboardFactory.button(catalog.text(context.language(), "telegram.subscription.config") + " " + entry.index(), TelegramCallbackAction.SHOW_CONFIG, context.telegramUserId(), subscriptionId, entry.index(), null, context.receivedAt()),
                    keyboardFactory.button(catalog.text(context.language(), "telegram.subscription.config_qr") + " " + entry.index(), TelegramCallbackAction.SHOW_CONFIG_QR, context.telegramUserId(), subscriptionId, entry.index(), null, context.receivedAt())
            ));
        }
        rows.add(keyboardFactory.row(keyboardFactory.button(catalog.text(context.language(), "telegram.subscription.new_link"), TelegramCallbackAction.REQUEST_SUBSCRIPTION_LINK, context.telegramUserId(), subscriptionId, null, null, context.receivedAt())));
        rows.add(keyboardFactory.row(
                keyboardFactory.button(labelProvider.label(context.language(), com.parazit.panel.application.telegram.navigation.TelegramNavigationAction.BACK), TelegramCallbackAction.BACK_TO_SUBSCRIPTIONS, context.telegramUserId(), null, null, null, context.receivedAt()),
                keyboardFactory.button(labelProvider.label(context.language(), com.parazit.panel.application.telegram.navigation.TelegramNavigationAction.HOME), TelegramCallbackAction.BACK_TO_MAIN, context.telegramUserId(), null, null, null, context.receivedAt())
        ));
        String text = catalog.text(context.language(), "telegram.subscription.details_title") + "\n"
                + catalog.text(context.language(), "telegram.subscription.plan") + ": " + formatter.html(summary.planName()) + "\n"
                + catalog.text(context.language(), "telegram.subscription.status") + ": " + statusLabel(context.language(), summary.status().name()) + "\n"
                + catalog.text(context.language(), "telegram.subscription.expires") + ": " + formatter.formatDate(summary.expiresAt()) + "\n"
                + catalog.text(context.language(), "telegram.subscription.configs") + ": " + summary.configCount();
        return TelegramResponseAction.sendMessage(new SendTelegramMessageCommand(
                context.chatId(),
                text,
                TelegramParseMode.HTML,
                keyboardFactory.rows(rows),
                telegramProperties.disableLinkPreview(),
                null
        ), false);
    }

    private TelegramResponseAction configText(TelegramInteractionContext context, UUID subscriptionId, int configIndex) {
        SubscriptionConfigEntryResult result = getConfigEntryUseCase.get(context.telegramUserId(), subscriptionId, configIndex);
        String text = catalog.text(context.language(), "telegram.subscription.config") + " " + configIndex + "\n<code>" + formatter.html(result.uri()) + "</code>";
        return TelegramResponseAction.sendMessage(new SendTelegramMessageCommand(
                context.chatId(),
                text,
                TelegramParseMode.HTML,
                TelegramInlineKeyboard.empty(),
                true,
                null
        ), true);
    }

    private TelegramResponseAction configQr(TelegramInteractionContext context, UUID subscriptionId, int configIndex) {
        QrCodeImageResult result = generateVlessQrUseCase.generate(new GenerateVlessConfigQrCommand(
                context.telegramUserId(),
                subscriptionId,
                configIndex,
                defaultQrOptions(),
                false
        ));
        return TelegramResponseAction.sendPhoto(new SendTelegramPhotoCommand(
                context.chatId(),
                result.bytes(),
                result.filename(),
                catalog.text(context.language(), "telegram.subscription.config_qr") + " " + configIndex,
                TelegramParseMode.NONE,
                TelegramInlineKeyboard.empty()
        ), true);
    }

    private TelegramResponseAction rotationWarning(TelegramInteractionContext context, UUID subscriptionId) {
        TelegramSensitiveAction action = sensitiveActionService.createRotation(context.telegramUserId(), subscriptionId);
        TelegramInlineKeyboard keyboard = keyboardFactory.rows(List.of(
                keyboardFactory.row(keyboardFactory.button(
                        catalog.text(context.language(), "telegram.action.confirm"),
                        TelegramCallbackAction.CONFIRM_ROTATE_SUBSCRIPTION_TOKEN,
                        context.telegramUserId(),
                        null,
                        null,
                        action.getId(),
                        context.receivedAt()
                )),
                keyboardFactory.row(keyboardFactory.button(
                        catalog.text(context.language(), "telegram.navigation.close"),
                        TelegramCallbackAction.CANCEL_ROTATION,
                        context.telegramUserId(),
                        null,
                        null,
                        action.getId(),
                        context.receivedAt()
                ))
        ));
        return TelegramResponseAction.sendMessage(new SendTelegramMessageCommand(
                context.chatId(),
                catalog.text(context.language(), "rotation_warning"),
                TelegramParseMode.NONE,
                keyboard,
                telegramProperties.disableLinkPreview(),
                null
        ), false);
    }

    private List<TelegramResponseAction> confirmRotation(TelegramInteractionContext context, UUID actionId) {
        return sensitiveActionService.claimRotation(actionId, context.telegramUserId(), "telegram-rotation-" + context.updateId())
                .map(action -> {
                    CreateSubscriptionResult rotated = rotateSubscriptionTokenUseCase.rotate(new RotateSubscriptionTokenCommand(
                            context.telegramUserId(),
                            action.getSubscriptionId(),
                            "telegram-delivery"
                    ));
                    BuildSubscriptionUrlResult url = buildSubscriptionUrlUseCase.build(new BuildSubscriptionUrlCommand(
                            context.telegramUserId(),
                            action.getSubscriptionId(),
                            rotated.rawAccessToken()
                    ));
                    QrCodeImageResult qr = generateSubscriptionUrlQrUseCase.generate(new GenerateSubscriptionUrlQrCommand(
                            context.telegramUserId(),
                            action.getSubscriptionId(),
                            rotated.rawAccessToken(),
                            defaultQrOptions(),
                            false
                    ));
                    return List.of(
                            TelegramResponseAction.sendMessage(new SendTelegramMessageCommand(
                                    context.chatId(),
                                    formatter.rawSensitive(url.subscriptionUrl()),
                                    TelegramParseMode.NONE,
                                    TelegramInlineKeyboard.empty(),
                                    true,
                                    null
                            ), true),
                            TelegramResponseAction.sendPhoto(new SendTelegramPhotoCommand(
                                    context.chatId(),
                                    qr.bytes(),
                                    qr.filename(),
                                    "Subscription QR",
                                    TelegramParseMode.NONE,
                                    TelegramInlineKeyboard.empty()
                            ), true)
                    );
                })
                .orElseGet(() -> List.of(TelegramResponseAction.sendMessage(new SendTelegramMessageCommand(
                        context.chatId(),
                        catalog.text(context.language(), "not_available"),
                        TelegramParseMode.NONE,
                        TelegramInlineKeyboard.empty(),
                        telegramProperties.disableLinkPreview(),
                        null
                ), false)));
    }

    private TelegramResponseAction cancelRotation(TelegramInteractionContext context, UUID actionId) {
        sensitiveActionService.cancel(actionId, context.telegramUserId());
        return TelegramResponseAction.sendMessage(new SendTelegramMessageCommand(
                context.chatId(),
                catalog.text(context.language(), "rotation_cancelled"),
                TelegramParseMode.NONE,
                TelegramInlineKeyboard.empty(),
                telegramProperties.disableLinkPreview(),
                null
        ), false);
    }

    private TelegramResponseAction menuMessage(TelegramInteractionContext context) {
        return TelegramResponseAction.sendMessage(new SendTelegramMessageCommand(
                context.chatId(),
                catalog.text(context.language(), "telegram.menu.returned"),
                TelegramParseMode.NONE,
                TelegramInlineKeyboard.empty(),
                replyKeyboardFactory.mainKeyboard(context.language()),
                telegramProperties.disableLinkPreview(),
                null
        ), false);
    }

    private TelegramResponseAction helpMessage(TelegramInteractionContext context) {
        return TelegramResponseAction.sendMessage(new SendTelegramMessageCommand(
                context.chatId(),
                catalog.text(context.language(), "help"),
                TelegramParseMode.NONE,
                TelegramInlineKeyboard.empty(),
                replyKeyboardFactory.mainKeyboard(context.language()),
                telegramProperties.disableLinkPreview(),
                null
        ), false);
    }

    private TelegramResponseAction answer(TelegramInteractionContext context, String text) {
        return TelegramResponseAction.answerCallback(new AnswerTelegramCallbackCommand(context.callbackQueryId(), text, false));
    }

    private QrRenderOptions defaultQrOptions() {
        return new QrRenderOptions(
                qrProperties.defaultSize(),
                qrProperties.defaultSize(),
                qrProperties.defaultMarginModules(),
                qrProperties.defaultErrorCorrection(),
                QrImageFormat.PNG,
                false
        );
    }

    private static UUID requireSubscription(TelegramCallbackPayload payload) {
        if (payload.subscriptionId() == null) {
            throw new IllegalArgumentException("subscription callback reference is missing");
        }
        return payload.subscriptionId();
    }

    private static UUID requireAction(TelegramCallbackPayload payload) {
        if (payload.actionId() == null) {
            throw new IllegalArgumentException("sensitive action callback reference is missing");
        }
        return payload.actionId();
    }

    private static int configIndex(TelegramCallbackPayload payload) {
        return payload.configIndex() == null ? 1 : payload.configIndex();
    }

    private static String requireReference(TelegramCallbackPayload payload) {
        if (payload.reference() == null || payload.reference().isBlank()) {
            throw new IllegalArgumentException("callback reference is missing");
        }
        return payload.reference();
    }

    private String statusLabel(String language, String status) {
        return catalog.text(language, "telegram.subscription.status." + status);
    }
}
