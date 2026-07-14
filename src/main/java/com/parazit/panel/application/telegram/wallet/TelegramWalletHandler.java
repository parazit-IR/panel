package com.parazit.panel.application.telegram.wallet;

import com.parazit.panel.application.port.in.wallet.GetCustomerWalletUseCase;
import com.parazit.panel.application.port.in.wallet.ListWalletTransactionsUseCase;
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
import com.parazit.panel.config.properties.TelegramBotProperties;
import com.parazit.panel.config.properties.WalletProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TelegramWalletHandler {

    private final GetCustomerWalletUseCase getWalletUseCase;
    private final ListWalletTransactionsUseCase listTransactionsUseCase;
    private final TelegramWalletFormatter walletFormatter;
    private final TelegramWalletTransactionsFormatter transactionsFormatter;
    private final TelegramKeyboardFactory keyboardFactory;
    private final TelegramMessageCatalog catalog;
    private final TelegramMenuLabelProvider labelProvider;
    private final TelegramBotProperties telegramProperties;
    private final WalletProperties walletProperties;

    public TelegramWalletHandler(
            GetCustomerWalletUseCase getWalletUseCase,
            ListWalletTransactionsUseCase listTransactionsUseCase,
            TelegramWalletFormatter walletFormatter,
            TelegramWalletTransactionsFormatter transactionsFormatter,
            TelegramKeyboardFactory keyboardFactory,
            TelegramMessageCatalog catalog,
            TelegramMenuLabelProvider labelProvider,
            TelegramBotProperties telegramProperties,
            WalletProperties walletProperties
    ) {
        this.getWalletUseCase = Objects.requireNonNull(getWalletUseCase, "getWalletUseCase must not be null");
        this.listTransactionsUseCase = Objects.requireNonNull(listTransactionsUseCase, "listTransactionsUseCase must not be null");
        this.walletFormatter = Objects.requireNonNull(walletFormatter, "walletFormatter must not be null");
        this.transactionsFormatter = Objects.requireNonNull(transactionsFormatter, "transactionsFormatter must not be null");
        this.keyboardFactory = Objects.requireNonNull(keyboardFactory, "keyboardFactory must not be null");
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.labelProvider = Objects.requireNonNull(labelProvider, "labelProvider must not be null");
        this.telegramProperties = Objects.requireNonNull(telegramProperties, "telegramProperties must not be null");
        this.walletProperties = Objects.requireNonNull(walletProperties, "walletProperties must not be null");
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
                    TelegramCallbackAction.WALLET_TOP_UP,
                    context.telegramUserId(),
                    1,
                    "",
                    context.receivedAt()
            )));
        }
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
