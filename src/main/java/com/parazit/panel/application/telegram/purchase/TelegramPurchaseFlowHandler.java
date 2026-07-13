package com.parazit.panel.application.telegram.purchase;

import com.parazit.panel.application.plan.catalog.query.ListAvailablePlansQuery;
import com.parazit.panel.application.plan.catalog.result.AvailablePlanResult;
import com.parazit.panel.application.port.in.plan.catalog.ListAvailablePlansUseCase;
import com.parazit.panel.application.port.in.purchase.ContinuePurchaseToPaymentUseCase;
import com.parazit.panel.application.port.in.purchase.GetPurchasePreInvoiceUseCase;
import com.parazit.panel.application.port.in.purchase.SelectPurchasePaymentMethodUseCase;
import com.parazit.panel.application.port.in.purchase.SelectPurchasePlanUseCase;
import com.parazit.panel.application.purchase.PurchaseFlowException;
import com.parazit.panel.application.purchase.result.AvailablePaymentMethodResult;
import com.parazit.panel.application.purchase.result.PurchaseManualPaymentResult;
import com.parazit.panel.application.purchase.result.PurchaseOnlinePaymentResult;
import com.parazit.panel.application.purchase.result.PurchasePaymentMethodsResult;
import com.parazit.panel.application.purchase.result.PurchasePreInvoiceResult;
import com.parazit.panel.application.sales.SalesAvailabilityService;
import com.parazit.panel.application.telegram.TelegramKeyboardFactory;
import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.application.telegram.command.SendTelegramMessageCommand;
import com.parazit.panel.application.telegram.menu.TelegramMainReplyKeyboardFactory;
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
import com.parazit.panel.domain.payment.PaymentMethod;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TelegramPurchaseFlowHandler {

    private static final Logger log = LoggerFactory.getLogger(TelegramPurchaseFlowHandler.class);

    private final ListAvailablePlansUseCase listAvailablePlansUseCase;
    private final SelectPurchasePlanUseCase selectPurchasePlanUseCase;
    private final GetPurchasePreInvoiceUseCase getPurchasePreInvoiceUseCase;
    private final ContinuePurchaseToPaymentUseCase continuePurchaseToPaymentUseCase;
    private final SelectPurchasePaymentMethodUseCase selectPurchasePaymentMethodUseCase;
    private final SalesAvailabilityService salesAvailabilityService;
    private final TelegramPurchaseUxFormatter formatter;
    private final TelegramKeyboardFactory keyboardFactory;
    private final TelegramMenuLabelProvider labelProvider;
    private final TelegramMessageCatalog catalog;
    private final TelegramBotProperties properties;
    private final TelegramMainReplyKeyboardFactory replyKeyboardFactory;

    public TelegramPurchaseFlowHandler(
            ListAvailablePlansUseCase listAvailablePlansUseCase,
            SelectPurchasePlanUseCase selectPurchasePlanUseCase,
            GetPurchasePreInvoiceUseCase getPurchasePreInvoiceUseCase,
            ContinuePurchaseToPaymentUseCase continuePurchaseToPaymentUseCase,
            SelectPurchasePaymentMethodUseCase selectPurchasePaymentMethodUseCase,
            SalesAvailabilityService salesAvailabilityService,
            TelegramPurchaseUxFormatter formatter,
            TelegramKeyboardFactory keyboardFactory,
            TelegramMenuLabelProvider labelProvider,
            TelegramMessageCatalog catalog,
            TelegramBotProperties properties,
            TelegramMainReplyKeyboardFactory replyKeyboardFactory
    ) {
        this.listAvailablePlansUseCase = Objects.requireNonNull(listAvailablePlansUseCase, "listAvailablePlansUseCase must not be null");
        this.selectPurchasePlanUseCase = Objects.requireNonNull(selectPurchasePlanUseCase, "selectPurchasePlanUseCase must not be null");
        this.getPurchasePreInvoiceUseCase = Objects.requireNonNull(getPurchasePreInvoiceUseCase, "getPurchasePreInvoiceUseCase must not be null");
        this.continuePurchaseToPaymentUseCase = Objects.requireNonNull(continuePurchaseToPaymentUseCase, "continuePurchaseToPaymentUseCase must not be null");
        this.selectPurchasePaymentMethodUseCase = Objects.requireNonNull(selectPurchasePaymentMethodUseCase, "selectPurchasePaymentMethodUseCase must not be null");
        this.salesAvailabilityService = Objects.requireNonNull(salesAvailabilityService, "salesAvailabilityService must not be null");
        this.formatter = Objects.requireNonNull(formatter, "formatter must not be null");
        this.keyboardFactory = Objects.requireNonNull(keyboardFactory, "keyboardFactory must not be null");
        this.labelProvider = Objects.requireNonNull(labelProvider, "labelProvider must not be null");
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.replyKeyboardFactory = Objects.requireNonNull(replyKeyboardFactory, "replyKeyboardFactory must not be null");
    }

    public TelegramResponsePlan showPlanCatalog(TelegramInteractionContext context) {
        if (!salesAvailabilityService.newPurchaseAvailable()) {
            return disabled(context);
        }
        List<AvailablePlanResult> plans = plans();
        List<TelegramInlineKeyboardRow> rows = new ArrayList<>();
        for (AvailablePlanResult plan : plans) {
            rows.add(keyboardFactory.row(keyboardFactory.button(
                    plan.name() == null || plan.name().isBlank() ? plan.code() : plan.name(),
                    TelegramCallbackAction.SHOW_PLAN_DETAILS,
                    context.telegramUserId(),
                    plan.id(),
                    null,
                    null,
                    context.receivedAt()
            )));
        }
        rows.add(homeRow(context));
        log.atInfo()
                .addKeyValue("updateId", context.updateId())
                .addKeyValue("telegramUserId", context.telegramUserId())
                .log("Telegram purchase plan catalog shown");
        return message(context, formatter.planCatalog(context.language(), plans), keyboardFactory.rows(rows), "purchase:catalog");
    }

    public TelegramResponsePlan showPlanDetails(TelegramInteractionContext context, UUID planId) {
        AvailablePlanResult plan = plans().stream()
                .filter(candidate -> candidate.id().equals(planId))
                .findFirst()
                .orElseThrow(() -> new PurchaseFlowException("telegram.purchase.plan_unavailable"));
        List<TelegramInlineKeyboardRow> rows = List.of(
                keyboardFactory.row(keyboardFactory.button(
                        catalog.text(context.language(), "telegram.purchase.select_plan"),
                        TelegramCallbackAction.SELECT_PLAN,
                        context.telegramUserId(),
                        plan.id(),
                        null,
                        null,
                        context.receivedAt()
                )),
                keyboardFactory.row(keyboardFactory.button(
                        catalog.text(context.language(), "telegram.purchase.choose_other_plan"),
                        TelegramCallbackAction.BACK_TO_PLAN_CATALOG,
                        context.telegramUserId(),
                        null,
                        null,
                        null,
                        context.receivedAt()
                )),
                homeRow(context)
        );
        return message(context, formatter.planDetails(context.language(), plan), keyboardFactory.rows(rows), "purchase:plan-details");
    }

    public TelegramResponsePlan selectPlan(TelegramInteractionContext context, UUID planId) {
        try {
            PurchasePreInvoiceResult result = selectPurchasePlanUseCase.select(context.telegramUserId(), planId);
            return preInvoice(context, result, "purchase:pre-invoice");
        } catch (PurchaseFlowException exception) {
            return safeFailure(context, exception.messageKey());
        }
    }

    public TelegramResponsePlan showPreInvoice(TelegramInteractionContext context, UUID purchaseSessionId) {
        try {
            return preInvoice(context, getPurchasePreInvoiceUseCase.get(context.telegramUserId(), purchaseSessionId), "purchase:pre-invoice");
        } catch (PurchaseFlowException exception) {
            return safeFailure(context, exception.messageKey());
        }
    }

    public TelegramResponsePlan continueToPayment(TelegramInteractionContext context, UUID purchaseSessionId) {
        try {
            PurchasePaymentMethodsResult result = continuePurchaseToPaymentUseCase.continueToPayment(context.telegramUserId(), purchaseSessionId);
            List<TelegramInlineKeyboardRow> rows = new ArrayList<>();
            for (AvailablePaymentMethodResult method : result.methods()) {
                rows.add(keyboardFactory.row(keyboardFactory.button(
                        catalog.text(context.language(), method.displayLabelKey()),
                        method.method() == PaymentMethod.CARD_TO_CARD
                                ? TelegramCallbackAction.SELECT_MANUAL_PAYMENT
                                : TelegramCallbackAction.SELECT_ONLINE_PAYMENT,
                        context.telegramUserId(),
                        result.purchaseSessionId(),
                        null,
                        null,
                        context.receivedAt()
                )));
            }
            rows.add(keyboardFactory.row(keyboardFactory.button(
                    catalog.text(context.language(), "telegram.purchase.back_to_preinvoice"),
                    TelegramCallbackAction.BACK_TO_PRE_INVOICE,
                    context.telegramUserId(),
                    result.purchaseSessionId(),
                    null,
                    null,
                    context.receivedAt()
            )));
            rows.add(homeRow(context));
            return message(context, formatter.paymentMethods(context.language(), result), keyboardFactory.rows(rows), "purchase:payment-methods");
        } catch (PurchaseFlowException exception) {
            return safeFailure(context, exception.messageKey());
        }
    }

    public TelegramResponsePlan selectManual(TelegramInteractionContext context, UUID purchaseSessionId) {
        try {
            PurchaseManualPaymentResult result = selectPurchasePaymentMethodUseCase.selectManual(context.telegramUserId(), purchaseSessionId);
            List<TelegramInlineKeyboardRow> rows = new ArrayList<>();
            rows.add(keyboardFactory.row(keyboardFactory.copyText(
                    catalog.text(context.language(), "telegram.purchase.copy_amount"),
                    String.valueOf(result.instruction().payableAmount())
            )));
            rows.add(keyboardFactory.row(keyboardFactory.copyText(
                    catalog.text(context.language(), "telegram.purchase.copy_card"),
                    result.instruction().cardNumber()
            )));
            rows.add(keyboardFactory.row(keyboardFactory.button(
                    catalog.text(context.language(), "telegram.purchase.upload_receipt"),
                    TelegramCallbackAction.UPLOAD_MANUAL_RECEIPT,
                    context.telegramUserId(),
                    result.instruction().paymentId(),
                    null,
                    null,
                    context.receivedAt()
            )));
            rows.add(keyboardFactory.row(
                    keyboardFactory.button(catalog.text(context.language(), "telegram.purchase.check_status"), TelegramCallbackAction.REFRESH_PAYMENT_STATUS, context.telegramUserId(), result.instruction().paymentId(), null, null, context.receivedAt()),
                    keyboardFactory.button(catalog.text(context.language(), "telegram.purchase.cancel_payment"), TelegramCallbackAction.REQUEST_CANCEL_PAYMENT, context.telegramUserId(), result.instruction().paymentId(), null, null, context.receivedAt())
            ));
            rows.add(homeRow(context));
            return message(context, formatter.manualPayment(context.language(), result.instruction()), keyboardFactory.rows(rows), "purchase:manual-payment");
        } catch (PurchaseFlowException exception) {
            return safeFailure(context, exception.messageKey());
        }
    }

    public TelegramResponsePlan selectOnline(TelegramInteractionContext context, UUID purchaseSessionId) {
        try {
            PurchaseOnlinePaymentResult result = selectPurchasePaymentMethodUseCase.selectOnline(context.telegramUserId(), purchaseSessionId);
            List<TelegramInlineKeyboardRow> rows = new ArrayList<>();
            if (result.payment().paymentUrl() != null && !result.payment().paymentUrl().isBlank()) {
                rows.add(keyboardFactory.row(TelegramInlineButton.url(
                        catalog.text(context.language(), "telegram.purchase.open_payment_page"),
                        result.payment().paymentUrl()
                )));
            }
            rows.add(keyboardFactory.row(
                    keyboardFactory.button(catalog.text(context.language(), "telegram.purchase.check_status"), TelegramCallbackAction.REFRESH_PAYMENT_STATUS, context.telegramUserId(), result.payment().paymentId(), null, null, context.receivedAt()),
                    keyboardFactory.button(catalog.text(context.language(), "telegram.purchase.cancel_payment"), TelegramCallbackAction.REQUEST_CANCEL_PAYMENT, context.telegramUserId(), result.payment().paymentId(), null, null, context.receivedAt())
            ));
            rows.add(homeRow(context));
            return message(context, formatter.onlinePayment(context.language(), result.payment(), result.finalAmount(), result.currency()), keyboardFactory.rows(rows), "purchase:online-payment");
        } catch (PurchaseFlowException exception) {
            return safeFailure(context, exception.messageKey());
        }
    }

    public TelegramResponsePlan discountUnavailable(TelegramInteractionContext context) {
        return message(context, catalog.text(context.language(), "telegram.purchase.discount_unavailable"), TelegramInlineKeyboard.empty(), "purchase:discount-unavailable");
    }

    public TelegramResponsePlan paymentActionUnavailable(TelegramInteractionContext context) {
        return message(context, catalog.text(context.language(), "telegram.purchase.payment_action_unavailable"), TelegramInlineKeyboard.empty(), "purchase:payment-action-unavailable");
    }

    private TelegramResponsePlan preInvoice(TelegramInteractionContext context, PurchasePreInvoiceResult result, String key) {
        List<TelegramInlineKeyboardRow> rows = new ArrayList<>();
        rows.add(keyboardFactory.row(keyboardFactory.button(
                catalog.text(context.language(), "telegram.purchase.pay_and_receive"),
                TelegramCallbackAction.CONTINUE_TO_PAYMENT,
                context.telegramUserId(),
                result.purchaseSessionId(),
                null,
                null,
                context.receivedAt()
        )));
        rows.add(keyboardFactory.row(keyboardFactory.button(
                catalog.text(context.language(), "telegram.purchase.discount"),
                TelegramCallbackAction.APPLY_DISCOUNT_PLACEHOLDER,
                context.telegramUserId(),
                result.purchaseSessionId(),
                null,
                null,
                context.receivedAt()
        )));
        rows.add(keyboardFactory.row(keyboardFactory.button(
                catalog.text(context.language(), "telegram.purchase.choose_other_plan"),
                TelegramCallbackAction.BACK_TO_PLAN_CATALOG,
                context.telegramUserId(),
                null,
                null,
                null,
                context.receivedAt()
        )));
        rows.add(homeRow(context));
        return message(context, formatter.preInvoice(context.language(), result), keyboardFactory.rows(rows), key);
    }

    private TelegramResponsePlan disabled(TelegramInteractionContext context) {
        return new TelegramResponsePlan(List.of(TelegramResponseAction.sendMessage(new SendTelegramMessageCommand(
                context.chatId(),
                formatter.disabled(context.language()),
                TelegramParseMode.HTML,
                TelegramInlineKeyboard.empty(),
                replyKeyboardFactory.mainKeyboard(context.language()),
                properties.disableLinkPreview(),
                null
        ), false)), "purchase:disabled");
    }

    private TelegramResponsePlan safeFailure(TelegramInteractionContext context, String messageKey) {
        List<TelegramInlineKeyboardRow> rows = List.of(
                keyboardFactory.row(keyboardFactory.button(
                        catalog.text(context.language(), "telegram.purchase.show_plans"),
                        TelegramCallbackAction.SHOW_PLAN_CATALOG,
                        context.telegramUserId(),
                        null,
                        null,
                        null,
                        context.receivedAt()
                )),
                homeRow(context)
        );
        return message(context, catalog.text(context.language(), messageKey), keyboardFactory.rows(rows), "purchase:failure");
    }

    private TelegramResponsePlan message(TelegramInteractionContext context, String text, TelegramInlineKeyboard keyboard, String key) {
        return new TelegramResponsePlan(List.of(TelegramResponseAction.sendMessage(new SendTelegramMessageCommand(
                context.chatId(),
                text,
                TelegramParseMode.HTML,
                keyboard,
                properties.disableLinkPreview(),
                null
        ), false)), key);
    }

    private TelegramInlineKeyboardRow homeRow(TelegramInteractionContext context) {
        return keyboardFactory.row(keyboardFactory.button(
                labelProvider.label(context.language(), TelegramNavigationAction.HOME),
                TelegramCallbackAction.BACK_TO_MAIN,
                context.telegramUserId(),
                null,
                null,
                null,
                context.receivedAt()
        ));
    }

    private List<AvailablePlanResult> plans() {
        return listAvailablePlansUseCase.list(new ListAvailablePlansQuery(null))
                .stream()
                .sorted(Comparator.comparingInt(AvailablePlanResult::displayOrder).thenComparing(AvailablePlanResult::code))
                .toList();
    }
}
