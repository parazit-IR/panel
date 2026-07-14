package com.parazit.panel.application.telegram.wallet;

import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.application.telegram.TelegramMessageFormatter;
import com.parazit.panel.application.telegram.TelegramPersianTextFormatter;
import com.parazit.panel.application.wallet.result.CustomerWalletResult;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TelegramWalletFormatter {

    private final TelegramMessageCatalog catalog;
    private final TelegramMessageFormatter dateFormatter;
    private final TelegramPersianTextFormatter textFormatter;

    public TelegramWalletFormatter(
            TelegramMessageCatalog catalog,
            TelegramMessageFormatter dateFormatter,
            TelegramPersianTextFormatter textFormatter
    ) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.dateFormatter = Objects.requireNonNull(dateFormatter, "dateFormatter must not be null");
        this.textFormatter = Objects.requireNonNull(textFormatter, "textFormatter must not be null");
    }

    public String summary(CustomerWalletResult result, String language) {
        Objects.requireNonNull(result, "result must not be null");
        StringBuilder text = new StringBuilder(catalog.text(language, "telegram.wallet.title")).append("\n\n");
        append(text, catalog.text(language, "telegram.wallet.balance"), amount(result.balance(), language));
        append(text, catalog.text(language, "telegram.wallet.transaction_count"), textFormatter.formatNumber(result.transactionCount(), language));
        append(text, catalog.text(language, "telegram.wallet.last_transaction"),
                result.lastTransactionAt()
                        .map(dateFormatter::formatDate)
                        .orElse(catalog.text(language, "telegram.wallet.no_transaction")));
        if (!result.topUpAvailable()) {
            text.append("\n").append(catalog.text(language, "telegram.wallet.top_up_unavailable"));
        }
        return text.toString().trim();
    }

    private String amount(com.parazit.panel.domain.order.Money money, String language) {
        return textFormatter.formatAmount(money.amount(), money.currency().name(), language);
    }

    private static void append(StringBuilder text, String label, String value) {
        text.append(label).append(":\n").append(value).append("\n\n");
    }
}
