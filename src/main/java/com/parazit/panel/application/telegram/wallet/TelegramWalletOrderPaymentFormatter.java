package com.parazit.panel.application.telegram.wallet;

import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.application.telegram.TelegramPersianTextFormatter;
import com.parazit.panel.application.wallet.payment.result.WalletOrderPaymentOutcome;
import com.parazit.panel.application.wallet.payment.result.WalletOrderPaymentPreviewResult;
import com.parazit.panel.application.wallet.payment.result.WalletOrderPaymentResult;
import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.order.OrderType;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TelegramWalletOrderPaymentFormatter {

    private final TelegramMessageCatalog catalog;
    private final TelegramPersianTextFormatter textFormatter;

    public TelegramWalletOrderPaymentFormatter(TelegramMessageCatalog catalog, TelegramPersianTextFormatter textFormatter) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.textFormatter = Objects.requireNonNull(textFormatter, "textFormatter must not be null");
    }

    public String confirmation(WalletOrderPaymentPreviewResult result, String language) {
        String key = result.sufficientBalance()
                ? "telegram.wallet.payment_confirmation"
                : "telegram.wallet.payment_insufficient";
        Money shortfall = result.sufficientBalance()
                ? new Money(0, result.orderAmount().currency())
                : new Money(result.orderAmount().amount() - result.walletBalance().amount(), result.orderAmount().currency());
        return catalog.text(language, key)
                .replace("{orderAmount}", amount(result.orderAmount(), language))
                .replace("{walletBalance}", amount(result.walletBalance(), language))
                .replace("{projectedBalance}", amount(result.projectedBalance(), language))
                .replace("{shortfall}", amount(shortfall, language));
    }

    public String result(WalletOrderPaymentResult result, OrderType orderType, String language) {
        if (result.outcome() == WalletOrderPaymentOutcome.INSUFFICIENT_BALANCE) {
            Money shortfall = new Money(
                    Math.max(result.paidAmount().amount() - result.balanceBefore().amount(), 0),
                    result.paidAmount().currency()
            );
            return catalog.text(language, "telegram.wallet.payment_insufficient")
                    .replace("{orderAmount}", amount(result.paidAmount(), language))
                    .replace("{walletBalance}", amount(result.balanceBefore(), language))
                    .replace("{projectedBalance}", amount(result.balanceBefore(), language))
                    .replace("{shortfall}", amount(shortfall, language));
        }
        if (result.outcome() == WalletOrderPaymentOutcome.CONFLICTING_PAYMENT_EXISTS) {
            return catalog.text(language, "telegram.wallet.payment_conflict");
        }
        if (result.outcome() == WalletOrderPaymentOutcome.WALLET_UNAVAILABLE
                || result.outcome() == WalletOrderPaymentOutcome.ORDER_NOT_ELIGIBLE) {
            return catalog.text(language, "telegram.wallet.payment_unavailable");
        }
        String key = orderType == OrderType.RENEWAL
                ? "telegram.wallet.payment_success_renewal"
                : "telegram.wallet.payment_success_new";
        return catalog.text(language, key)
                .replace("{amount}", amount(result.paidAmount(), language))
                .replace("{balanceAfter}", result.balanceAfter() == null ? "-" : amount(result.balanceAfter(), language));
    }

    public String expired(String language) {
        return catalog.text(language, "telegram.wallet.payment_expired");
    }

    private String amount(Money money, String language) {
        return textFormatter.formatAmount(money.amount(), money.currency().name(), language);
    }
}
