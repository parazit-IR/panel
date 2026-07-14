package com.parazit.panel.application.telegram.account;

import com.parazit.panel.application.customer.result.CustomerAccountSummaryResult;
import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.application.telegram.TelegramMessageFormatter;
import com.parazit.panel.application.telegram.TelegramPersianTextFormatter;
import com.parazit.panel.application.telegram.service.TelegramCustomerTextFormatter;
import com.parazit.panel.config.properties.CustomerAccountTelegramProperties;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TelegramCustomerAccountSummaryFormatter {

    private final TelegramMessageCatalog catalog;
    private final TelegramMessageFormatter formatter;
    private final TelegramPersianTextFormatter persianTextFormatter;
    private final TelegramCustomerTextFormatter textFormatter;
    private final CustomerAccountTelegramProperties properties;

    public TelegramCustomerAccountSummaryFormatter(
            TelegramMessageCatalog catalog,
            TelegramMessageFormatter formatter,
            TelegramPersianTextFormatter persianTextFormatter,
            TelegramCustomerTextFormatter textFormatter,
            CustomerAccountTelegramProperties properties
    ) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.formatter = Objects.requireNonNull(formatter, "formatter must not be null");
        this.persianTextFormatter = Objects.requireNonNull(persianTextFormatter, "persianTextFormatter must not be null");
        this.textFormatter = Objects.requireNonNull(textFormatter, "textFormatter must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public String format(CustomerAccountSummaryResult result, String language) {
        StringBuilder text = new StringBuilder(catalog.text(language, "telegram.account.title")).append("\n\n");
        if (properties.showTelegramId()) {
            append(text, catalog.text(language, "telegram.account.telegram_id"), Long.toString(result.telegramUserId()));
        }
        append(text, catalog.text(language, "telegram.account.name"), formatter.html(result.displayName()));
        if (properties.showRegistrationDate()) {
            append(text, catalog.text(language, "telegram.account.registered_at"), formatter.formatDate(result.registeredAt()));
        }
        if (properties.showServiceCounts()) {
            append(text, catalog.text(language, "telegram.account.total_services"), textFormatter.number(result.totalServiceCount(), language));
            append(text, catalog.text(language, "telegram.account.active_services"), textFormatter.number(result.activeServiceCount(), language));
            append(text, catalog.text(language, "telegram.account.expired_services"), textFormatter.number(result.expiredServiceCount(), language));
        }
        if (properties.showPaidOrderCount()) {
            append(text, catalog.text(language, "telegram.account.paid_orders"), textFormatter.number(result.paidOrderCount(), language));
        }
        if (properties.showPendingPaymentCount()) {
            append(text, catalog.text(language, "telegram.account.pending_payments"), textFormatter.number(result.pendingPaymentCount(), language));
        }
        result.phoneNumberMasked().ifPresent(value -> append(text, catalog.text(language, "telegram.account.phone"), formatter.html(value)));
        result.walletBalance().ifPresent(value -> append(text, catalog.text(language, "telegram.account.wallet_balance"),
                persianTextFormatter.formatAmount(value.amount(), value.currency().name(), language)));
        result.customerGroup().ifPresent(value -> append(text, catalog.text(language, "telegram.account.customer_group"), formatter.html(value)));
        return text.toString().trim();
    }

    private static void append(StringBuilder text, String label, String value) {
        text.append(label).append(":\n").append(value).append("\n\n");
    }
}
