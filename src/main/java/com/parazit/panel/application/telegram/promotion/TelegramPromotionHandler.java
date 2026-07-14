package com.parazit.panel.application.telegram.promotion;

import com.parazit.panel.application.port.in.promotion.ApplyDiscountCodeUseCase;
import com.parazit.panel.application.port.in.promotion.RedeemGiftCodeUseCase;
import com.parazit.panel.application.port.in.purchase.ContinuePurchaseToPaymentUseCase;
import com.parazit.panel.application.port.in.renewal.CreateRenewalOrderUseCase;
import com.parazit.panel.application.promotion.PromotionException;
import com.parazit.panel.application.promotion.command.ApplyDiscountCodeCommand;
import com.parazit.panel.application.promotion.command.RedeemGiftCodeCommand;
import com.parazit.panel.application.renewal.command.CreateRenewalOrderCommand;
import com.parazit.panel.application.telegram.TelegramKeyboardFactory;
import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.application.telegram.command.SendTelegramMessageCommand;
import com.parazit.panel.application.telegram.model.TelegramCallbackAction;
import com.parazit.panel.application.telegram.model.TelegramInlineKeyboard;
import com.parazit.panel.application.telegram.model.TelegramInteractionContext;
import com.parazit.panel.application.telegram.model.TelegramParseMode;
import com.parazit.panel.application.telegram.model.TelegramResponseAction;
import com.parazit.panel.application.telegram.model.TelegramResponsePlan;
import com.parazit.panel.config.properties.TelegramBotProperties;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TelegramPromotionHandler {

    private final ContinuePurchaseToPaymentUseCase continuePurchaseToPaymentUseCase;
    private final CreateRenewalOrderUseCase createRenewalOrderUseCase;
    private final ApplyDiscountCodeUseCase applyDiscountCodeUseCase;
    private final RedeemGiftCodeUseCase redeemGiftCodeUseCase;
    private final TelegramPromotionCodeSessionStore sessionStore;
    private final TelegramPromotionFormatter formatter;
    private final TelegramKeyboardFactory keyboardFactory;
    private final TelegramMessageCatalog catalog;
    private final TelegramBotProperties telegramProperties;

    public TelegramPromotionHandler(
            ContinuePurchaseToPaymentUseCase continuePurchaseToPaymentUseCase,
            CreateRenewalOrderUseCase createRenewalOrderUseCase,
            ApplyDiscountCodeUseCase applyDiscountCodeUseCase,
            RedeemGiftCodeUseCase redeemGiftCodeUseCase,
            TelegramPromotionCodeSessionStore sessionStore,
            TelegramPromotionFormatter formatter,
            TelegramKeyboardFactory keyboardFactory,
            TelegramMessageCatalog catalog,
            TelegramBotProperties telegramProperties
    ) {
        this.continuePurchaseToPaymentUseCase = Objects.requireNonNull(continuePurchaseToPaymentUseCase, "continuePurchaseToPaymentUseCase must not be null");
        this.createRenewalOrderUseCase = Objects.requireNonNull(createRenewalOrderUseCase, "createRenewalOrderUseCase must not be null");
        this.applyDiscountCodeUseCase = Objects.requireNonNull(applyDiscountCodeUseCase, "applyDiscountCodeUseCase must not be null");
        this.redeemGiftCodeUseCase = Objects.requireNonNull(redeemGiftCodeUseCase, "redeemGiftCodeUseCase must not be null");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore must not be null");
        this.formatter = Objects.requireNonNull(formatter, "formatter must not be null");
        this.keyboardFactory = Objects.requireNonNull(keyboardFactory, "keyboardFactory must not be null");
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.telegramProperties = Objects.requireNonNull(telegramProperties, "telegramProperties must not be null");
    }

    public TelegramResponsePlan startPurchaseDiscount(TelegramInteractionContext context, UUID purchaseSessionId) {
        var result = continuePurchaseToPaymentUseCase.continueToPayment(context.telegramUserId(), purchaseSessionId);
        sessionStore.startDiscount(context.telegramUserId(), result.orderId());
        return send(context, formatter.discountPrompt(context.language()), backToPreInvoice(context, purchaseSessionId), "promotion:discount-prompt");
    }

    public TelegramResponsePlan startRenewalDiscount(TelegramInteractionContext context, UUID purchaseSessionId) {
        var result = createRenewalOrderUseCase.create(new CreateRenewalOrderCommand(
                context.telegramUserId(),
                purchaseSessionId,
                deterministic("renewal-discount-order", context.updateId(), purchaseSessionId)
        ));
        sessionStore.startDiscount(context.telegramUserId(), result.orderId());
        return send(context, formatter.discountPrompt(context.language()), backToRenewalPreInvoice(context, purchaseSessionId), "promotion:renewal-discount-prompt");
    }

    public TelegramResponsePlan startGift(TelegramInteractionContext context) {
        sessionStore.startGift(context.telegramUserId());
        return send(context, formatter.giftPrompt(context.language()), backToWallet(context), "promotion:gift-prompt");
    }

    public Optional<TelegramResponsePlan> handleCodeIfAwaiting(TelegramInteractionContext context, String text) {
        Optional<TelegramPromotionCodeSessionStore.Session> active = sessionStore.active(context.telegramUserId());
        if (active.isEmpty()) {
            return Optional.empty();
        }
        try {
            TelegramPromotionCodeSessionStore.Session session = active.get();
            if (session.type() == TelegramPromotionCodeSessionStore.SessionType.DISCOUNT) {
                var result = applyDiscountCodeUseCase.apply(new ApplyDiscountCodeCommand(
                        context.telegramUserId(),
                        session.orderId(),
                        text,
                        deterministic("discount-code", context.updateId(), session.orderId())
                ));
                sessionStore.clear(context.telegramUserId());
                return Optional.of(send(context, formatter.discountSuccess(context.language(), result), discountSuccessKeyboard(context, result.orderId()), "promotion:discount-success"));
            }
            var result = redeemGiftCodeUseCase.redeem(new RedeemGiftCodeCommand(
                    context.telegramUserId(),
                    text,
                    deterministic("gift-code", context.updateId(), null)
            ));
            sessionStore.clear(context.telegramUserId());
            return Optional.of(send(context, formatter.giftSuccess(context.language(), result), giftSuccessKeyboard(context), "promotion:gift-success"));
        } catch (PromotionException exception) {
            sessionStore.clear(context.telegramUserId());
            return Optional.of(send(context, catalog.text(context.language(), exception.messageKey()), backToWallet(context), "promotion:failure"));
        }
    }

    public void clear(long telegramUserId) {
        sessionStore.clear(telegramUserId);
    }

    private TelegramInlineKeyboard backToPreInvoice(TelegramInteractionContext context, UUID purchaseSessionId) {
        return keyboardFactory.rows(List.of(keyboardFactory.row(
                keyboardFactory.button(catalog.text(context.language(), "telegram.purchase.back_to_preinvoice"), TelegramCallbackAction.BACK_TO_PRE_INVOICE, context.telegramUserId(), purchaseSessionId, null, null, context.receivedAt()),
                keyboardFactory.button(catalog.text(context.language(), "telegram.navigation.home"), TelegramCallbackAction.BACK_TO_MAIN, context.telegramUserId(), null, null, null, context.receivedAt())
        )));
    }

    private TelegramInlineKeyboard backToRenewalPreInvoice(TelegramInteractionContext context, UUID purchaseSessionId) {
        return keyboardFactory.rows(List.of(keyboardFactory.row(
                keyboardFactory.button(catalog.text(context.language(), "telegram.purchase.back_to_preinvoice"), TelegramCallbackAction.SHOW_RENEWAL_PRE_INVOICE, context.telegramUserId(), purchaseSessionId, null, null, context.receivedAt()),
                keyboardFactory.button(catalog.text(context.language(), "telegram.navigation.home"), TelegramCallbackAction.BACK_TO_MAIN, context.telegramUserId(), null, null, null, context.receivedAt())
        )));
    }

    private TelegramInlineKeyboard discountSuccessKeyboard(TelegramInteractionContext context, UUID orderId) {
        return keyboardFactory.rows(List.of(keyboardFactory.row(
                keyboardFactory.button(catalog.text(context.language(), "telegram.purchase.wallet_payment"), TelegramCallbackAction.PAY_ORDER_WITH_WALLET, context.telegramUserId(), orderId, null, null, context.receivedAt()),
                keyboardFactory.button(catalog.text(context.language(), "telegram.navigation.home"), TelegramCallbackAction.BACK_TO_MAIN, context.telegramUserId(), null, null, null, context.receivedAt())
        )));
    }

    private TelegramInlineKeyboard giftSuccessKeyboard(TelegramInteractionContext context) {
        return keyboardFactory.rows(List.of(
                keyboardFactory.row(keyboardFactory.button(catalog.text(context.language(), "telegram.wallet.title"), TelegramCallbackAction.BACK_TO_WALLET, context.telegramUserId(), null, null, null, context.receivedAt())),
                keyboardFactory.row(keyboardFactory.button(catalog.text(context.language(), "telegram.wallet.history"), TelegramCallbackAction.WALLET_HISTORY, context.telegramUserId(), 1, "", context.receivedAt()))
        ));
    }

    private TelegramInlineKeyboard backToWallet(TelegramInteractionContext context) {
        return keyboardFactory.rows(List.of(keyboardFactory.row(
                keyboardFactory.button(catalog.text(context.language(), "telegram.wallet.title"), TelegramCallbackAction.BACK_TO_WALLET, context.telegramUserId(), null, null, null, context.receivedAt()),
                keyboardFactory.button(catalog.text(context.language(), "telegram.navigation.home"), TelegramCallbackAction.BACK_TO_MAIN, context.telegramUserId(), null, null, null, context.receivedAt())
        )));
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

    private static UUID deterministic(String prefix, long updateId, UUID id) {
        String raw = prefix + ":" + updateId + ":" + (id == null ? "" : id);
        return UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8));
    }
}
