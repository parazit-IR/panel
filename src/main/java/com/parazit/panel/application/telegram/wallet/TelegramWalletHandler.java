package com.parazit.panel.application.telegram.wallet;

import com.parazit.panel.application.port.in.wallet.GetCustomerWalletUseCase;
import com.parazit.panel.application.port.in.wallet.ListWalletTransactionsUseCase;
import com.parazit.panel.application.port.in.wallet.topup.CreateWalletTopUpPaymentUseCase;
import com.parazit.panel.application.port.in.wallet.topup.CreateWalletTopUpRequestUseCase;
import com.parazit.panel.application.port.in.wallet.topup.GetWalletTopUpStatusUseCase;
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
import com.parazit.panel.application.wallet.command.GetCustomerWalletCommand;
import com.parazit.panel.application.wallet.command.ListWalletTransactionsCommand;
import com.parazit.panel.application.wallet.result.CustomerWalletResult;
import com.parazit.panel.application.wallet.result.WalletTransactionPageResult;
import com.parazit.panel.application.wallet.topup.WalletTopUpAmountPolicy;
import com.parazit.panel.application.wallet.topup.WalletTopUpException;
import com.parazit.panel.application.wallet.topup.command.CreateWalletTopUpPaymentCommand;
import com.parazit.panel.application.wallet.topup.command.CreateWalletTopUpRequestCommand;
import com.parazit.panel.application.wallet.topup.command.GetWalletTopUpStatusCommand;
import com.parazit.panel.application.wallet.topup.result.WalletTopUpRequestResult;
import com.parazit.panel.application.wallet.topup.result.WalletTopUpStatusResult;
import com.parazit.panel.config.properties.TelegramBotProperties;
import com.parazit.panel.config.properties.WalletProperties;
import com.parazit.panel.config.properties.WalletTopUpProperties;
import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.payment.PaymentMethod;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TelegramWalletHandler {

    private final GetCustomerWalletUseCase getWalletUseCase;
    private final ListWalletTransactionsUseCase listTransactionsUseCase;
    private final CreateWalletTopUpRequestUseCase createTopUpRequestUseCase;
    private final CreateWalletTopUpPaymentUseCase createTopUpPaymentUseCase;
    private final GetWalletTopUpStatusUseCase getTopUpStatusUseCase;
    private final WalletTopUpAmountPolicy amountPolicy;
    private final TelegramWalletTopUpAmountSessionStore topUpSessionStore;
    private final TelegramWalletFormatter walletFormatter;
    private final TelegramWalletTransactionsFormatter transactionsFormatter;
    private final TelegramKeyboardFactory keyboardFactory;
    private final TelegramMessageCatalog catalog;
    private final TelegramMenuLabelProvider labelProvider;
    private final TelegramBotProperties telegramProperties;
    private final WalletProperties walletProperties;
    private final WalletTopUpProperties topUpProperties;

    public TelegramWalletHandler(
            GetCustomerWalletUseCase getWalletUseCase,
            ListWalletTransactionsUseCase listTransactionsUseCase,
            CreateWalletTopUpRequestUseCase createTopUpRequestUseCase,
            CreateWalletTopUpPaymentUseCase createTopUpPaymentUseCase,
            GetWalletTopUpStatusUseCase getTopUpStatusUseCase,
            WalletTopUpAmountPolicy amountPolicy,
            TelegramWalletTopUpAmountSessionStore topUpSessionStore,
            TelegramWalletFormatter walletFormatter,
            TelegramWalletTransactionsFormatter transactionsFormatter,
            TelegramKeyboardFactory keyboardFactory,
            TelegramMessageCatalog catalog,
            TelegramMenuLabelProvider labelProvider,
            TelegramBotProperties telegramProperties,
            WalletProperties walletProperties,
            WalletTopUpProperties topUpProperties
    ) {
        this.getWalletUseCase = Objects.requireNonNull(getWalletUseCase, "getWalletUseCase must not be null");
        this.listTransactionsUseCase = Objects.requireNonNull(listTransactionsUseCase, "listTransactionsUseCase must not be null");
        this.createTopUpRequestUseCase = Objects.requireNonNull(createTopUpRequestUseCase, "createTopUpRequestUseCase must not be null");
        this.createTopUpPaymentUseCase = Objects.requireNonNull(createTopUpPaymentUseCase, "createTopUpPaymentUseCase must not be null");
        this.getTopUpStatusUseCase = Objects.requireNonNull(getTopUpStatusUseCase, "getTopUpStatusUseCase must not be null");
        this.amountPolicy = Objects.requireNonNull(amountPolicy, "amountPolicy must not be null");
        this.topUpSessionStore = Objects.requireNonNull(topUpSessionStore, "topUpSessionStore must not be null");
        this.walletFormatter = Objects.requireNonNull(walletFormatter, "walletFormatter must not be null");
        this.transactionsFormatter = Objects.requireNonNull(transactionsFormatter, "transactionsFormatter must not be null");
        this.keyboardFactory = Objects.requireNonNull(keyboardFactory, "keyboardFactory must not be null");
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.labelProvider = Objects.requireNonNull(labelProvider, "labelProvider must not be null");
        this.telegramProperties = Objects.requireNonNull(telegramProperties, "telegramProperties must not be null");
        this.walletProperties = Objects.requireNonNull(walletProperties, "walletProperties must not be null");
        this.topUpProperties = Objects.requireNonNull(topUpProperties, "topUpProperties must not be null");
    }

    public TelegramResponsePlan show(TelegramInteractionContext context) {
        Objects.requireNonNull(context, "context must not be null");
        CustomerWalletResult result = getWalletUseCase.get(new GetCustomerWalletCommand(context.telegramUserId()));
        return new TelegramResponsePlan(List.of(TelegramResponseAction.sendMessage(new SendTelegramMessageCommand(
                context.chatId(),
                walletFormatter.summary(result, context.language()),
                TelegramParseMode.HTML,
                walletKeyboard(context),
                telegramProperties.disableLinkPreview(),
                null
        ), false)), "telegram:wallet");
    }

    public TelegramResponsePlan history(TelegramInteractionContext context, int page) {
        Objects.requireNonNull(context, "context must not be null");
        int oneBasedPage = Math.max(1, page);
        WalletTransactionPageResult result = listTransactionsUseCase.list(new ListWalletTransactionsCommand(
                context.telegramUserId(),
                oneBasedPage - 1,
                walletProperties.historyPageSize()
        ));
        return new TelegramResponsePlan(List.of(TelegramResponseAction.sendMessage(new SendTelegramMessageCommand(
                context.chatId(),
                transactionsFormatter.format(result, context.language()),
                TelegramParseMode.HTML,
                historyKeyboard(context, result),
                telegramProperties.disableLinkPreview(),
                null
        ), false)), "telegram:wallet-history");
    }

    public TelegramResponsePlan topUpUnavailable(TelegramInteractionContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return new TelegramResponsePlan(List.of(TelegramResponseAction.sendMessage(new SendTelegramMessageCommand(
                context.chatId(),
                catalog.text(context.language(), "telegram.wallet.top_up_unavailable"),
                TelegramParseMode.NONE,
                backToWalletKeyboard(context),
                telegramProperties.disableLinkPreview(),
                null
        ), false)), "telegram:wallet-top-up-unavailable");
    }

    public TelegramResponsePlan startTopUp(TelegramInteractionContext context) {
        Objects.requireNonNull(context, "context must not be null");
        if (!topUpProperties.enabled()) {
            return topUpUnavailable(context);
        }
        topUpSessionStore.start(context.telegramUserId());
        return send(context, walletFormatter.topUpPrompt(topUpProperties, context.language()), TelegramParseMode.NONE, backToWalletKeyboard(context), "telegram:wallet-top-up-prompt");
    }

    public Optional<TelegramResponsePlan> handleAmountIfAwaiting(TelegramInteractionContext context, String text) {
        Objects.requireNonNull(context, "context must not be null");
        if (topUpSessionStore.active(context.telegramUserId()).isEmpty()) {
            return Optional.empty();
        }
        try {
            Money amount = amountPolicy.parseCustomerInput(text);
            WalletTopUpRequestResult result = createTopUpRequestUseCase.create(new CreateWalletTopUpRequestCommand(
                    context.telegramUserId(),
                    amount,
                    UUID.nameUUIDFromBytes(("wallet-top-up-message:" + context.updateId()).getBytes(java.nio.charset.StandardCharsets.UTF_8))
            ));
            topUpSessionStore.clear(context.telegramUserId());
            return Optional.of(showTopUpConfirmation(context, result));
        } catch (WalletTopUpException | IllegalArgumentException exception) {
            return Optional.of(send(context, catalog.text(context.language(), "telegram.wallet.top_up_invalid_amount"),
                    TelegramParseMode.NONE, backToWalletKeyboard(context), "telegram:wallet-top-up-invalid"));
        }
    }

    public TelegramResponsePlan selectTopUpPayment(TelegramInteractionContext context, UUID topUpRequestId, PaymentMethod method) {
        var result = createTopUpPaymentUseCase.create(new CreateWalletTopUpPaymentCommand(
                context.telegramUserId(),
                topUpRequestId,
                method,
                UUID.nameUUIDFromBytes(("wallet-top-up-payment:" + context.updateId()).getBytes(java.nio.charset.StandardCharsets.UTF_8))
        ));
        String text = method == PaymentMethod.CARD_TO_CARD
                ? walletFormatter.manualTopUp(result, context.language())
                : walletFormatter.onlineTopUp(result, context.language());
        return send(context, text, TelegramParseMode.HTML, topUpStatusKeyboard(context, topUpRequestId), "telegram:wallet-top-up-payment");
    }

    public TelegramResponsePlan topUpStatus(TelegramInteractionContext context, UUID topUpRequestId) {
        WalletTopUpStatusResult result = getTopUpStatusUseCase.get(new GetWalletTopUpStatusCommand(context.telegramUserId(), topUpRequestId));
        return send(context, walletFormatter.topUpStatus(result, context.language()), TelegramParseMode.HTML,
                topUpStatusKeyboard(context, topUpRequestId), "telegram:wallet-top-up-status");
    }

    public void clearTopUpSession(long telegramUserId) {
        topUpSessionStore.clear(telegramUserId);
    }

    private TelegramInlineKeyboard walletKeyboard(TelegramInteractionContext context) {
        List<TelegramInlineKeyboardRow> rows = new ArrayList<>();
        if (walletProperties.showTransactionHistory()) {
            rows.add(keyboardFactory.row(keyboardFactory.button(
                    catalog.text(context.language(), "telegram.wallet.history"),
                    TelegramCallbackAction.WALLET_HISTORY,
                    context.telegramUserId(),
                    1,
                    "",
                    context.receivedAt()
            )));
        }
        if (walletProperties.showTopUpPlaceholder()) {
            rows.add(keyboardFactory.row(keyboardFactory.button(
                    catalog.text(context.language(), "telegram.wallet.top_up"),
                    topUpProperties.enabled() ? TelegramCallbackAction.START_WALLET_TOP_UP : TelegramCallbackAction.WALLET_TOP_UP,
                    context.telegramUserId(),
                    1,
                    "",
                    context.receivedAt()
            )));
        }
        rows.add(keyboardFactory.row(keyboardFactory.button(
                catalog.text(context.language(), "telegram.wallet.gift_code"),
                TelegramCallbackAction.START_GIFT_CODE_ENTRY,
                context.telegramUserId(),
                null,
                null,
                null,
                context.receivedAt()
        )));
        rows.add(keyboardFactory.row(keyboardFactory.button(
                labelProvider.label(context.language(), TelegramNavigationAction.HOME),
                TelegramCallbackAction.BACK_TO_MAIN,
                context.telegramUserId(),
                null,
                null,
                null,
                context.receivedAt()
        )));
        return keyboardFactory.rows(rows);
    }

    private TelegramResponsePlan showTopUpConfirmation(TelegramInteractionContext context, WalletTopUpRequestResult result) {
        List<TelegramInlineKeyboardRow> rows = new ArrayList<>();
        if (result.manualPaymentAvailable()) {
            rows.add(keyboardFactory.row(keyboardFactory.button(
                    catalog.text(context.language(), "telegram.purchase.manual_payment"),
                    TelegramCallbackAction.SELECT_WALLET_TOP_UP_MANUAL_PAYMENT,
                    context.telegramUserId(),
                    result.topUpRequestId(),
                    null,
                    null,
                    context.receivedAt()
            )));
        }
        if (result.onlinePaymentAvailable()) {
            rows.add(keyboardFactory.row(keyboardFactory.button(
                    catalog.text(context.language(), "telegram.purchase.online_payment"),
                    TelegramCallbackAction.SELECT_WALLET_TOP_UP_ONLINE_PAYMENT,
                    context.telegramUserId(),
                    result.topUpRequestId(),
                    null,
                    null,
                    context.receivedAt()
            )));
        }
        rows.add(keyboardFactory.row(
                keyboardFactory.button(catalog.text(context.language(), "telegram.wallet.change_amount"),
                        TelegramCallbackAction.CHANGE_WALLET_TOP_UP_AMOUNT, context.telegramUserId(), null, null, null, context.receivedAt()),
                keyboardFactory.button(labelProvider.label(context.language(), TelegramNavigationAction.HOME),
                        TelegramCallbackAction.BACK_TO_MAIN, context.telegramUserId(), null, null, null, context.receivedAt())
        ));
        return send(context, walletFormatter.topUpConfirmation(result, context.language()), TelegramParseMode.HTML,
                keyboardFactory.rows(rows), "telegram:wallet-top-up-confirmation");
    }

    private TelegramInlineKeyboard topUpStatusKeyboard(TelegramInteractionContext context, UUID topUpRequestId) {
        return keyboardFactory.rows(List.of(
                keyboardFactory.row(keyboardFactory.button(
                        catalog.text(context.language(), "telegram.purchase.check_status"),
                        TelegramCallbackAction.REFRESH_WALLET_TOP_UP_STATUS,
                        context.telegramUserId(),
                        topUpRequestId,
                        null,
                        null,
                        context.receivedAt()
                )),
                keyboardFactory.row(
                        keyboardFactory.button(catalog.text(context.language(), "telegram.wallet.title"),
                                TelegramCallbackAction.BACK_TO_WALLET, context.telegramUserId(), null, null, null, context.receivedAt()),
                        keyboardFactory.button(labelProvider.label(context.language(), TelegramNavigationAction.HOME),
                                TelegramCallbackAction.BACK_TO_MAIN, context.telegramUserId(), null, null, null, context.receivedAt())
                )
        ));
    }

    private TelegramResponsePlan send(
            TelegramInteractionContext context,
            String text,
            TelegramParseMode parseMode,
            TelegramInlineKeyboard keyboard,
            String handlerKey
    ) {
        return new TelegramResponsePlan(List.of(TelegramResponseAction.sendMessage(new SendTelegramMessageCommand(
                context.chatId(),
                text,
                parseMode,
                keyboard,
                telegramProperties.disableLinkPreview(),
                null
        ), false)), handlerKey);
    }

    private TelegramInlineKeyboard historyKeyboard(TelegramInteractionContext context, WalletTransactionPageResult page) {
        List<TelegramInlineKeyboardRow> rows = new ArrayList<>();
        List<TelegramInlineButton> navigation = new ArrayList<>();
        if (page.hasPrevious()) {
            navigation.add(keyboardFactory.button(
                    catalog.text(context.language(), "telegram.wallet.previous"),
                    TelegramCallbackAction.WALLET_HISTORY_PAGE,
                    context.telegramUserId(),
                    page.page(),
                    "",
                    context.receivedAt()
            ));
        }
        if (page.hasNext()) {
            navigation.add(keyboardFactory.button(
                    catalog.text(context.language(), "telegram.wallet.next"),
                    TelegramCallbackAction.WALLET_HISTORY_PAGE,
                    context.telegramUserId(),
                    page.page() + 2,
                    "",
                    context.receivedAt()
            ));
        }
        if (!navigation.isEmpty()) {
            rows.add(new TelegramInlineKeyboardRow(navigation));
        }
        rows.add(keyboardFactory.row(
                keyboardFactory.button(
                        labelProvider.label(context.language(), TelegramNavigationAction.BACK),
                        TelegramCallbackAction.BACK_TO_WALLET,
                        context.telegramUserId(),
                        1,
                        "",
                        context.receivedAt()
                ),
                keyboardFactory.button(
                        labelProvider.label(context.language(), TelegramNavigationAction.HOME),
                        TelegramCallbackAction.BACK_TO_MAIN,
                        context.telegramUserId(),
                        null,
                        null,
                        null,
                        context.receivedAt()
                )
        ));
        return keyboardFactory.rows(rows);
    }

    private TelegramInlineKeyboard backToWalletKeyboard(TelegramInteractionContext context) {
        return keyboardFactory.rows(List.of(keyboardFactory.row(
                keyboardFactory.button(
                        labelProvider.label(context.language(), TelegramNavigationAction.BACK),
                        TelegramCallbackAction.BACK_TO_WALLET,
                        context.telegramUserId(),
                        1,
                        "",
                        context.receivedAt()
                ),
                keyboardFactory.button(
                        labelProvider.label(context.language(), TelegramNavigationAction.HOME),
                        TelegramCallbackAction.BACK_TO_MAIN,
                        context.telegramUserId(),
                        null,
                        null,
                        null,
                        context.receivedAt()
                )
        )));
    }
}
