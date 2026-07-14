package com.parazit.panel.application.telegram.wallet;

import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.application.telegram.TelegramMessageFormatter;
import com.parazit.panel.application.telegram.TelegramPersianTextFormatter;
import com.parazit.panel.application.wallet.result.CustomerWalletResult;
import com.parazit.panel.application.wallet.topup.result.WalletTopUpPaymentResult;
import com.parazit.panel.application.wallet.topup.result.WalletTopUpRequestResult;
import com.parazit.panel.application.wallet.topup.result.WalletTopUpStatusResult;
import com.parazit.panel.config.properties.WalletTopUpProperties;
import com.parazit.panel.domain.order.Money;
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

    public String topUpPrompt(WalletTopUpProperties properties, String language) {
        return catalog.text(language, "telegram.wallet.top_up_prompt")
                .replace("{minimumAmount}", amount(properties.minimumMoney(), language))
                .replace("{maximumAmount}", amount(properties.maximumMoney(), language));
    }

    public String topUpConfirmation(WalletTopUpRequestResult result, String language) {
        return catalog.text(language, "telegram.wallet.top_up_invoice")
                .replace("{amount}", amount(result.amount(), language))
                .replace("{expiresAt}", dateFormatter.formatDate(result.expiresAt()));
    }

    public String manualTopUp(WalletTopUpPaymentResult result, String language) {
        var instruction = result.manualPayment().instruction();
        return catalog.text(language, "telegram.wallet.top_up_manual")
                .replace("{requestedAmount}", amount(result.amount(), language))
                .replace("{payableAmount}", textFormatter.formatAmount(instruction.payableAmount(), instruction.currency(), language))
                .replace("{card}", instruction.cardNumberFormatted())
                .replace("{expiresAt}", dateFormatter.formatDate(instruction.expiresAt()));
    }

    public String onlineTopUp(WalletTopUpPaymentResult result, String language) {
        return catalog.text(language, "telegram.wallet.top_up_online")
                .replace("{amount}", amount(result.amount(), language))
                .replace("{expiresAt}", dateFormatter.formatDate(result.expiresAt()));
    }

    public String topUpStatus(WalletTopUpStatusResult result, String language) {
        String balance = result.balanceAfter() == null ? "-" : amount(result.balanceAfter(), language);
        String paymentStatus = result.paymentStatus() == null ? "-" : result.paymentStatus().name();
        return catalog.text(language, "telegram.wallet.top_up_status")
                .replace("{amount}", amount(result.amount(), language))
                .replace("{topUpStatus}", statusLabel(result.topUpStatus().name(), language))
                .replace("{paymentStatus}", statusLabel(paymentStatus, language))
                .replace("{balanceAfter}", balance);
    }

    private String amount(Money money, String language) {
        return textFormatter.formatAmount(money.amount(), money.currency().name(), language);
    }

    private String statusLabel(String status, String language) {
        boolean fa = language != null && language.toUpperCase(java.util.Locale.ROOT).startsWith("FA");
        if (!fa) {
            return status.replace('_', ' ').toLowerCase(java.util.Locale.ROOT);
        }
        return switch (status) {
            case "AWAITING_PAYMENT_METHOD" -> "در انتظار انتخاب روش پرداخت";
            case "PENDING_PAYMENT", "WAITING_FOR_PAYMENT" -> "در انتظار پرداخت";
            case "WAITING_FOR_REVIEW", "RECEIPT_SUBMITTED" -> "در انتظار بررسی رسید";
            case "PAYMENT_APPROVED", "APPROVED" -> "پرداخت تأیید شد";
            case "CREDITED" -> "کیف پول شارژ شد";
            case "CANCELLED" -> "لغوشده";
            case "EXPIRED" -> "منقضی‌شده";
            case "FAILED" -> "نیازمند بررسی";
            case "-" -> "-";
            default -> "در حال بررسی";
        };
    }

    private static void append(StringBuilder text, String label, String value) {
        text.append(label).append(":\n").append(value).append("\n\n");
    }
}
