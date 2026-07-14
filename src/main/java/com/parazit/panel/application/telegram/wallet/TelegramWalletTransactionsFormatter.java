package com.parazit.panel.application.telegram.wallet;

import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.application.telegram.TelegramMessageFormatter;
import com.parazit.panel.application.telegram.TelegramPersianTextFormatter;
import com.parazit.panel.application.wallet.result.WalletTransactionPageResult;
import com.parazit.panel.application.wallet.result.WalletTransactionSummaryResult;
import com.parazit.panel.domain.wallet.WalletTransactionDirection;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TelegramWalletTransactionsFormatter {

    private final TelegramMessageCatalog catalog;
    private final TelegramMessageFormatter dateFormatter;
    private final TelegramPersianTextFormatter textFormatter;

    public TelegramWalletTransactionsFormatter(
            TelegramMessageCatalog catalog,
            TelegramMessageFormatter dateFormatter,
            TelegramPersianTextFormatter textFormatter
    ) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.dateFormatter = Objects.requireNonNull(dateFormatter, "dateFormatter must not be null");
        this.textFormatter = Objects.requireNonNull(textFormatter, "textFormatter must not be null");
    }

    public String format(WalletTransactionPageResult page, String language) {
        Objects.requireNonNull(page, "page must not be null");
        if (page.items().isEmpty()) {
            return catalog.text(language, "telegram.wallet.history_empty");
        }
        StringBuilder text = new StringBuilder(catalog.text(language, "telegram.wallet.history")).append("\n\n");
        for (WalletTransactionSummaryResult item : page.items()) {
            text.append(title(item.direction(), language)).append("\n")
                    .append(amount(item.amount(), language)).append("\n")
                    .append(catalog.text(language, "telegram.wallet.balance_after")).append(": ")
                    .append(amount(item.balanceAfter(), language)).append("\n")
                    .append(dateFormatter.formatDate(item.occurredAt()))
                    .append("\n\n");
        }
        return text.toString().trim();
    }

    private String title(WalletTransactionDirection direction, String language) {
        return direction == WalletTransactionDirection.CREDIT
                ? catalog.text(language, "telegram.wallet.credit")
                : catalog.text(language, "telegram.wallet.debit");
    }

    private String amount(com.parazit.panel.domain.order.Money money, String language) {
        return textFormatter.formatAmount(money.amount(), money.currency().name(), language);
    }
}
