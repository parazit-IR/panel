package com.parazit.panel.application.telegram.wallet;

import com.parazit.panel.application.port.in.wallet.payment.GetWalletOrderPaymentPreviewUseCase;
import com.parazit.panel.application.port.in.wallet.payment.PayOrderWithWalletUseCase;
import com.parazit.panel.application.telegram.TelegramKeyboardFactory;
import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.application.telegram.TelegramSensitiveActionService;
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
import com.parazit.panel.application.wallet.payment.WalletOrderPaymentException;
import com.parazit.panel.application.wallet.payment.command.GetWalletOrderPaymentPreviewCommand;
import com.parazit.panel.application.wallet.payment.command.PayOrderWithWalletCommand;
import com.parazit.panel.application.wallet.payment.result.WalletOrderPaymentPreviewResult;
import com.parazit.panel.application.wallet.payment.result.WalletOrderPaymentResult;
import com.parazit.panel.config.properties.TelegramBotProperties;
import com.parazit.panel.domain.telegram.TelegramSensitiveAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TelegramWalletOrderPaymentHandler {

    private final GetWalletOrderPaymentPreviewUseCase previewUseCase;
    private final PayOrderWithWalletUseCase payUseCase;
    private final TelegramSensitiveActionService sensitiveActionService;
    private final TelegramWalletOrderPaymentFormatter formatter;
    private final TelegramKeyboardFactory keyboardFactory;
    private final TelegramMessageCatalog catalog;
    private final TelegramMenuLabelProvider labelProvider;
    private final TelegramBotProperties telegramProperties;

    public TelegramWalletOrderPaymentHandler(
            GetWalletOrderPaymentPreviewUseCase previewUseCase,
            PayOrderWithWalletUseCase payUseCase,
            TelegramSensitiveActionService sensitiveActionService,
            TelegramWalletOrderPaymentFormatter formatter,
            TelegramKeyboardFactory keyboardFactory,
            TelegramMessageCatalog catalog,
            TelegramMenuLabelProvider labelProvider,
            TelegramBotProperties telegramProperties
    ) {
        this.previewUseCase = Objects.requireNonNull(previewUseCase, "previewUseCase must not be null");
        this.payUseCase = Objects.requireNonNull(payUseCase, "payUseCase must not be null");
        this.sensitiveActionService = Objects.requireNonNull(sensitiveActionService, "sensitiveActionService must not be null");
        this.formatter = Objects.requireNonNull(formatter, "formatter must not be null");
        this.keyboardFactory = Objects.requireNonNull(keyboardFactory, "keyboardFactory must not be null");
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.labelProvider = Objects.requireNonNull(labelProvider, "labelProvider must not be null");
        this.telegramProperties = Objects.requireNonNull(telegramProperties, "telegramProperties must not be null");
    }

    public TelegramResponsePlan preview(TelegramInteractionContext context, UUID orderId) {
        try {
            WalletOrderPaymentPreviewResult result = previewUseCase.preview(new GetWalletOrderPaymentPreviewCommand(
                    context.telegramUserId(),
                    orderId
            ));
            List<TelegramInlineKeyboardRow> rows = new ArrayList<>();
            if (result.walletPaymentAvailable() && result.sufficientBalance()) {
                TelegramSensitiveAction action = sensitiveActionService.createWalletOrderPayment(context.telegramUserId(), orderId);
                rows.add(keyboardFactory.row(keyboardFactory.button(
                        catalog.text(context.language(), "telegram.wallet.payment_confirm"),
                        TelegramCallbackAction.CONFIRM_WALLET_ORDER_PAYMENT,
                        context.telegramUserId(),
                        null,
                        null,
                        action.getId(),
                        context.receivedAt()
                )));
            }
            if (!result.sufficientBalance()) {
                rows.add(keyboardFactory.row(keyboardFactory.button(
                        catalog.text(context.language(), "telegram.wallet.top_up"),
                        TelegramCallbackAction.TOP_UP_FOR_ORDER,
                        context.telegramUserId(),
                        orderId,
                        null,
                        null,
                        context.receivedAt()
                )));
            }
            rows.add(homeRow(context));
            return send(context, formatter.confirmation(result, context.language()), keyboardFactory.rows(rows), "telegram:wallet-order-payment-preview");
        } catch (WalletOrderPaymentException | IllegalArgumentException exception) {
            return send(context, catalog.text(context.language(), "telegram.wallet.payment_unavailable"), backHome(context), "telegram:wallet-order-payment-unavailable");
        }
    }

    public TelegramResponsePlan confirm(TelegramInteractionContext context, UUID actionId) {
        return sensitiveActionService.claimWalletOrderPayment(actionId, context.telegramUserId(), "wallet-payment-" + context.updateId())
                .map(action -> pay(context, action))
                .orElseGet(() -> send(context, formatter.expired(context.language()), backHome(context), "telegram:wallet-order-payment-expired"));
    }

    private TelegramResponsePlan pay(TelegramInteractionContext context, TelegramSensitiveAction action) {
        UUID orderId = action.getResourceId();
        WalletOrderPaymentPreviewResult preview = previewUseCase.preview(new GetWalletOrderPaymentPreviewCommand(context.telegramUserId(), orderId));
        WalletOrderPaymentResult result = payUseCase.pay(new PayOrderWithWalletCommand(
                context.telegramUserId(),
                orderId,
                UUID.nameUUIDFromBytes(("wallet-order-payment:" + context.updateId() + ":" + orderId).getBytes(StandardCharsets.UTF_8))
        ));
        return send(context, formatter.result(result, preview.orderType(), context.language()), resultKeyboard(context, orderId), "telegram:wallet-order-payment-result");
    }

    private TelegramInlineKeyboard resultKeyboard(TelegramInteractionContext context, UUID orderId) {
        return keyboardFactory.rows(List.of(
                keyboardFactory.row(
                        keyboardFactory.button(catalog.text(context.language(), "telegram.purchase.check_status"),
                                TelegramCallbackAction.SHOW_WALLET_PAYMENT_RESULT, context.telegramUserId(), orderId, null, null, context.receivedAt()),
                        keyboardFactory.button(catalog.text(context.language(), "telegram.wallet.title"),
                                TelegramCallbackAction.BACK_TO_WALLET, context.telegramUserId(), null, null, null, context.receivedAt())
                ),
                homeRow(context)
        ));
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

    private TelegramInlineKeyboard backHome(TelegramInteractionContext context) {
        return keyboardFactory.rows(List.of(homeRow(context)));
    }

    private TelegramResponsePlan send(TelegramInteractionContext context, String text, TelegramInlineKeyboard keyboard, String key) {
        return new TelegramResponsePlan(List.of(TelegramResponseAction.sendMessage(new SendTelegramMessageCommand(
                context.chatId(),
                text,
                TelegramParseMode.HTML,
                keyboard,
                telegramProperties.disableLinkPreview(),
                null
        ), false)), key);
    }
}
