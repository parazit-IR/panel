package com.parazit.panel.application.telegram.renewal;

import com.parazit.panel.application.port.in.renewal.CreateRenewalOrderUseCase;
import com.parazit.panel.application.port.in.renewal.GetRenewalPreInvoiceUseCase;
import com.parazit.panel.application.port.in.renewal.GetRenewalStatusUseCase;
import com.parazit.panel.application.port.in.renewal.GetRenewalTargetDetailsUseCase;
import com.parazit.panel.application.port.in.renewal.ListRenewableServicesUseCase;
import com.parazit.panel.application.port.in.renewal.ListRenewalPlansUseCase;
import com.parazit.panel.application.port.in.renewal.SelectRenewalPlanUseCase;
import com.parazit.panel.application.purchase.result.AvailablePaymentMethodResult;
import com.parazit.panel.application.renewal.RenewalFlowException;
import com.parazit.panel.application.renewal.command.CreateRenewalOrderCommand;
import com.parazit.panel.application.renewal.command.GetRenewalPreInvoiceCommand;
import com.parazit.panel.application.renewal.command.GetRenewalStatusCommand;
import com.parazit.panel.application.renewal.command.GetRenewalTargetDetailsCommand;
import com.parazit.panel.application.renewal.command.ListRenewableServicesCommand;
import com.parazit.panel.application.renewal.command.ListRenewalPlansCommand;
import com.parazit.panel.application.renewal.command.SelectRenewalPlanCommand;
import com.parazit.panel.application.renewal.result.CreateRenewalOrderResult;
import com.parazit.panel.application.renewal.result.RenewableServicePageResult;
import com.parazit.panel.application.renewal.result.RenewalPlanPageResult;
import com.parazit.panel.application.renewal.result.RenewalPreInvoiceResult;
import com.parazit.panel.application.sales.SalesAvailabilityService;
import com.parazit.panel.application.telegram.TelegramKeyboardFactory;
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
import com.parazit.panel.config.properties.RenewalProperties;
import com.parazit.panel.config.properties.TelegramBotProperties;
import com.parazit.panel.domain.payment.PaymentMethod;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TelegramRenewalFlowHandler {

    private final ListRenewableServicesUseCase listServicesUseCase;
    private final GetRenewalTargetDetailsUseCase targetDetailsUseCase;
    private final ListRenewalPlansUseCase listPlansUseCase;
    private final SelectRenewalPlanUseCase selectPlanUseCase;
    private final GetRenewalPreInvoiceUseCase preInvoiceUseCase;
    private final CreateRenewalOrderUseCase createOrderUseCase;
    private final GetRenewalStatusUseCase statusUseCase;
    private final SalesAvailabilityService salesAvailabilityService;
    private final TelegramRenewalPreInvoiceFormatter formatter;
    private final TelegramRenewalStatusFormatter statusFormatter;
    private final TelegramKeyboardFactory keyboardFactory;
    private final TelegramMessageCatalog catalog;
    private final TelegramMenuLabelProvider labelProvider;
    private final TelegramBotProperties telegramProperties;
    private final RenewalProperties renewalProperties;

    public TelegramRenewalFlowHandler(
            ListRenewableServicesUseCase listServicesUseCase,
            GetRenewalTargetDetailsUseCase targetDetailsUseCase,
            ListRenewalPlansUseCase listPlansUseCase,
            SelectRenewalPlanUseCase selectPlanUseCase,
            GetRenewalPreInvoiceUseCase preInvoiceUseCase,
            CreateRenewalOrderUseCase createOrderUseCase,
            GetRenewalStatusUseCase statusUseCase,
            SalesAvailabilityService salesAvailabilityService,
            TelegramRenewalPreInvoiceFormatter formatter,
            TelegramRenewalStatusFormatter statusFormatter,
            TelegramKeyboardFactory keyboardFactory,
            TelegramMessageCatalog catalog,
            TelegramMenuLabelProvider labelProvider,
            TelegramBotProperties telegramProperties,
            RenewalProperties renewalProperties
    ) {
        this.listServicesUseCase = Objects.requireNonNull(listServicesUseCase, "listServicesUseCase must not be null");
        this.targetDetailsUseCase = Objects.requireNonNull(targetDetailsUseCase, "targetDetailsUseCase must not be null");
        this.listPlansUseCase = Objects.requireNonNull(listPlansUseCase, "listPlansUseCase must not be null");
        this.selectPlanUseCase = Objects.requireNonNull(selectPlanUseCase, "selectPlanUseCase must not be null");
        this.preInvoiceUseCase = Objects.requireNonNull(preInvoiceUseCase, "preInvoiceUseCase must not be null");
        this.createOrderUseCase = Objects.requireNonNull(createOrderUseCase, "createOrderUseCase must not be null");
        this.statusUseCase = Objects.requireNonNull(statusUseCase, "statusUseCase must not be null");
        this.salesAvailabilityService = Objects.requireNonNull(salesAvailabilityService, "salesAvailabilityService must not be null");
        this.formatter = Objects.requireNonNull(formatter, "formatter must not be null");
        this.statusFormatter = Objects.requireNonNull(statusFormatter, "statusFormatter must not be null");
        this.keyboardFactory = Objects.requireNonNull(keyboardFactory, "keyboardFactory must not be null");
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.labelProvider = Objects.requireNonNull(labelProvider, "labelProvider must not be null");
        this.telegramProperties = Objects.requireNonNull(telegramProperties, "telegramProperties must not be null");
        this.renewalProperties = Objects.requireNonNull(renewalProperties, "renewalProperties must not be null");
    }

    public TelegramResponsePlan listServices(TelegramInteractionContext context, int page) {
        if (!salesAvailabilityService.availability(com.parazit.panel.application.sales.SalesCapability.RENEWAL).enabled()) {
            return message(context, catalog.text(context.language(), "telegram.renewal.not_available"), TelegramInlineKeyboard.empty(), "renewal:disabled");
        }
        RenewableServicePageResult result = listServicesUseCase.list(new ListRenewableServicesCommand(
                context.telegramUserId(),
                Math.max(page - 1, 0),
                renewalProperties.servicePageSize()
        ));
        List<TelegramInlineKeyboardRow> rows = new ArrayList<>();
        for (var service : result.items()) {
            if (service.renewable()) {
                rows.add(keyboardFactory.row(keyboardFactory.button(
                        formatter.serviceButton(context.language(), service),
                        TelegramCallbackAction.SHOW_RENEWAL_TARGET,
                        context.telegramUserId(),
                        service.subscriptionId(),
                        result.page() + 1,
                        null,
                        context.receivedAt()
                )));
            }
        }
        addPagination(context, rows, result.hasPrevious(), result.hasNext(), result.page() + 1, TelegramCallbackAction.RENEWABLE_SERVICES_PAGE, null);
        rows.add(homeRow(context));
        return message(context, formatter.serviceList(context.language(), result), keyboardFactory.rows(rows), "renewal:services");
    }

    public TelegramResponsePlan target(TelegramInteractionContext context, UUID subscriptionId, int backPage) {
        try {
            var result = targetDetailsUseCase.get(new GetRenewalTargetDetailsCommand(context.telegramUserId(), subscriptionId));
            List<TelegramInlineKeyboardRow> rows = new ArrayList<>();
            if (result.renewable()) {
                rows.add(keyboardFactory.row(keyboardFactory.button(
                        catalog.text(context.language(), "telegram.renewal.select_plan"),
                        TelegramCallbackAction.LIST_RENEWAL_PLANS,
                        context.telegramUserId(),
                        subscriptionId,
                        1,
                        null,
                        context.receivedAt()
                )));
            }
            rows.add(keyboardFactory.row(
                    keyboardFactory.button(catalog.text(context.language(), "telegram.navigation.back"), TelegramCallbackAction.BACK_TO_RENEWABLE_SERVICES, context.telegramUserId(), Math.max(1, backPage), "", context.receivedAt()),
                    homeButton(context)
            ));
            return message(context, formatter.targetDetails(context.language(), result), keyboardFactory.rows(rows), "renewal:target");
        } catch (RenewalFlowException exception) {
            return failure(context, exception.messageKey());
        }
    }

    public TelegramResponsePlan plans(TelegramInteractionContext context, UUID subscriptionId, int page) {
        try {
            RenewalPlanPageResult result = listPlansUseCase.list(new ListRenewalPlansCommand(
                    context.telegramUserId(),
                    subscriptionId,
                    Math.max(page - 1, 0),
                    renewalProperties.planPageSize()
            ));
            List<TelegramInlineKeyboardRow> rows = new ArrayList<>();
            for (int index = 0; index < result.items().size(); index++) {
                var plan = result.items().get(index);
                rows.add(keyboardFactory.row(keyboardFactory.button(
                        formatter.planButton(context.language(), plan),
                        TelegramCallbackAction.SELECT_RENEWAL_PLAN,
                        context.telegramUserId(),
                        subscriptionId,
                        result.page() * result.size() + index + 1,
                        null,
                        context.receivedAt()
                )));
            }
            addPagination(context, rows, result.hasPrevious(), result.hasNext(), result.page() + 1, TelegramCallbackAction.RENEWAL_PLANS_PAGE, subscriptionId);
            rows.add(keyboardFactory.row(
                    keyboardFactory.button(catalog.text(context.language(), "telegram.navigation.back"), TelegramCallbackAction.BACK_TO_RENEWAL_TARGET, context.telegramUserId(), subscriptionId, 1, null, context.receivedAt()),
                    homeButton(context)
            ));
            return message(context, formatter.planList(context.language(), result), keyboardFactory.rows(rows), "renewal:plans");
        } catch (RenewalFlowException exception) {
            return failure(context, exception.messageKey());
        }
    }

    public TelegramResponsePlan selectPlan(TelegramInteractionContext context, UUID subscriptionId, UUID planId) {
        try {
            var result = selectPlanUseCase.select(new SelectRenewalPlanCommand(
                    context.telegramUserId(),
                    subscriptionId,
                    planId,
                    deterministic("renewal-selection", context.updateId(), planId)
            ));
            return preInvoice(context, result.purchaseSessionId());
        } catch (RenewalFlowException exception) {
            return failure(context, exception.messageKey());
        }
    }

    public TelegramResponsePlan selectPlanByIndex(TelegramInteractionContext context, UUID subscriptionId, int oneBasedIndex) {
        try {
            RenewalPlanPageResult plans = listPlansUseCase.list(new ListRenewalPlansCommand(
                    context.telegramUserId(),
                    subscriptionId,
                    0,
                    25
            ));
            int index = Math.max(oneBasedIndex, 1) - 1;
            if (index >= plans.items().size()) {
                return failure(context, "telegram.renewal.no_plan");
            }
            return selectPlan(context, subscriptionId, plans.items().get(index).planId());
        } catch (RenewalFlowException exception) {
            return failure(context, exception.messageKey());
        }
    }

    public TelegramResponsePlan preInvoice(TelegramInteractionContext context, UUID purchaseSessionId) {
        try {
            RenewalPreInvoiceResult result = preInvoiceUseCase.get(new GetRenewalPreInvoiceCommand(context.telegramUserId(), purchaseSessionId));
            List<TelegramInlineKeyboardRow> rows = List.of(
                    keyboardFactory.row(keyboardFactory.button(catalog.text(context.language(), "telegram.renewal.pay_and_renew"), TelegramCallbackAction.CONFIRM_RENEWAL_ORDER, context.telegramUserId(), result.purchaseSessionId(), null, null, context.receivedAt())),
                    keyboardFactory.row(keyboardFactory.button(catalog.text(context.language(), "telegram.renewal.choose_other_plan"), TelegramCallbackAction.LIST_RENEWAL_PLANS, context.telegramUserId(), result.targetSubscriptionId(), 1, null, context.receivedAt())),
                    keyboardFactory.row(keyboardFactory.button(catalog.text(context.language(), "telegram.navigation.back"), TelegramCallbackAction.BACK_TO_RENEWAL_TARGET, context.telegramUserId(), result.targetSubscriptionId(), 1, null, context.receivedAt()), homeButton(context))
            );
            return message(context, formatter.preInvoice(context.language(), result), keyboardFactory.rows(rows), "renewal:preinvoice");
        } catch (RenewalFlowException exception) {
            return failure(context, exception.messageKey());
        }
    }

    public TelegramResponsePlan confirm(TelegramInteractionContext context, UUID purchaseSessionId) {
        try {
            CreateRenewalOrderResult result = createOrderUseCase.create(new CreateRenewalOrderCommand(
                    context.telegramUserId(),
                    purchaseSessionId,
                    deterministic("renewal-order", context.updateId(), purchaseSessionId)
            ));
            List<TelegramInlineKeyboardRow> rows = new ArrayList<>();
            for (AvailablePaymentMethodResult method : result.methods()) {
                rows.add(keyboardFactory.row(keyboardFactory.button(
                        catalog.text(context.language(), method.displayLabelKey()),
                        method.method() == PaymentMethod.CARD_TO_CARD ? TelegramCallbackAction.SELECT_MANUAL_PAYMENT : TelegramCallbackAction.SELECT_ONLINE_PAYMENT,
                        context.telegramUserId(),
                        result.purchaseSessionId(),
                        null,
                        null,
                        context.receivedAt()
                )));
            }
            rows.add(keyboardFactory.row(keyboardFactory.button(catalog.text(context.language(), "telegram.purchase.back_to_preinvoice"), TelegramCallbackAction.SHOW_RENEWAL_PRE_INVOICE, context.telegramUserId(), result.purchaseSessionId(), null, null, context.receivedAt())));
            rows.add(homeRow(context));
            return message(context, formatter.paymentMethods(context.language(), result), keyboardFactory.rows(rows), "renewal:payment-methods");
        } catch (RenewalFlowException exception) {
            return failure(context, exception.messageKey());
        }
    }

    public TelegramResponsePlan status(TelegramInteractionContext context, UUID renewalOrderId) {
        try {
            var result = statusUseCase.get(new GetRenewalStatusCommand(context.telegramUserId(), renewalOrderId));
            List<TelegramInlineKeyboardRow> rows = List.of(
                    keyboardFactory.row(keyboardFactory.button(
                            "🔄 بررسی وضعیت تمدید",
                            TelegramCallbackAction.REFRESH_RENEWAL_STATUS,
                            context.telegramUserId(),
                            renewalOrderId,
                            null,
                            null,
                            context.receivedAt()
                    )),
                    keyboardFactory.row(
                            keyboardFactory.button("🛍 سرویس‌های من", TelegramCallbackAction.LIST_MY_SERVICES, context.telegramUserId(), null, null, null, context.receivedAt()),
                            homeButton(context)
                    )
            );
            return message(context, statusFormatter.format(context.language(), result), keyboardFactory.rows(rows), "renewal:status");
        } catch (RenewalFlowException exception) {
            return failure(context, exception.messageKey());
        }
    }

    private TelegramResponsePlan failure(TelegramInteractionContext context, String key) {
        return message(context, catalog.text(context.language(), key), keyboardFactory.rows(List.of(homeRow(context))), "renewal:failure");
    }

    private TelegramResponsePlan message(TelegramInteractionContext context, String text, TelegramInlineKeyboard keyboard, String key) {
        return new TelegramResponsePlan(List.of(TelegramResponseAction.sendMessage(new SendTelegramMessageCommand(
                context.chatId(),
                text,
                TelegramParseMode.HTML,
                keyboard,
                telegramProperties.disableLinkPreview(),
                null
        ), false)), key);
    }

    private void addPagination(
            TelegramInteractionContext context,
            List<TelegramInlineKeyboardRow> rows,
            boolean previous,
            boolean next,
            int currentPage,
            TelegramCallbackAction action,
            UUID subscriptionId
    ) {
        List<TelegramInlineButton> buttons = new ArrayList<>();
        if (previous) {
            buttons.add(keyboardFactory.button(catalog.text(context.language(), "telegram.pagination.previous"), action, context.telegramUserId(), subscriptionId, currentPage - 1, null, context.receivedAt()));
        }
        if (next) {
            buttons.add(keyboardFactory.button(catalog.text(context.language(), "telegram.pagination.next"), action, context.telegramUserId(), subscriptionId, currentPage + 1, null, context.receivedAt()));
        }
        if (!buttons.isEmpty()) {
            rows.add(new TelegramInlineKeyboardRow(buttons));
        }
    }

    private TelegramInlineKeyboardRow homeRow(TelegramInteractionContext context) {
        return keyboardFactory.row(homeButton(context));
    }

    private TelegramInlineButton homeButton(TelegramInteractionContext context) {
        return keyboardFactory.button(labelProvider.label(context.language(), TelegramNavigationAction.HOME), TelegramCallbackAction.BACK_TO_MAIN, context.telegramUserId(), null, null, null, context.receivedAt());
    }

    private static UUID deterministic(String prefix, long updateId, UUID id) {
        return UUID.nameUUIDFromBytes((prefix + ":" + updateId + ":" + id).getBytes(StandardCharsets.UTF_8));
    }
}
