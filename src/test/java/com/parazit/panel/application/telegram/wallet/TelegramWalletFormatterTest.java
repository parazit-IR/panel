package com.parazit.panel.application.telegram.wallet;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.application.telegram.TelegramMessageFormatter;
import com.parazit.panel.application.telegram.TelegramPersianTextFormatter;
import com.parazit.panel.application.telegram.TelegramHtmlEscaper;
import com.parazit.panel.application.telegram.TelegramTestProperties;
import com.parazit.panel.application.wallet.result.CustomerWalletResult;
import com.parazit.panel.application.wallet.result.WalletTransactionPageResult;
import com.parazit.panel.application.wallet.result.WalletTransactionSummaryResult;
import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.wallet.WalletStatus;
import com.parazit.panel.domain.wallet.WalletTransactionDirection;
import com.parazit.panel.domain.wallet.WalletTransactionType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TelegramWalletFormatterTest {

    @Test
    void formatsWalletSummaryWithoutInternalIds() {
        TelegramWalletFormatter formatter = new TelegramWalletFormatter(
                new TelegramMessageCatalog(),
                new TelegramMessageFormatter(TelegramTestProperties.properties(), new TelegramHtmlEscaper()),
                new TelegramPersianTextFormatter()
        );
        CustomerWalletResult result = new CustomerWalletResult(
                UUID.randomUUID(),
                new Money(350_000, CurrencyCode.IRT),
                WalletStatus.ACTIVE,
                2,
                Optional.of(Instant.parse("2026-07-14T00:00:00Z")),
                false,
                false
        );

        String text = formatter.summary(result, "FA");

        assertThat(text).contains("کیف پول", "۳۵۰٬۰۰۰ تومان", "۲");
        assertThat(text).doesNotContain(result.walletId().toString()).doesNotContain("ACTIVE");
    }

    @Test
    void formatsTransactionHistoryWithoutRawEnumNames() {
        TelegramWalletTransactionsFormatter formatter = new TelegramWalletTransactionsFormatter(
                new TelegramMessageCatalog(),
                new TelegramMessageFormatter(TelegramTestProperties.properties(), new TelegramHtmlEscaper()),
                new TelegramPersianTextFormatter()
        );
        WalletTransactionPageResult page = new WalletTransactionPageResult(List.of(new WalletTransactionSummaryResult(
                UUID.randomUUID(),
                WalletTransactionDirection.CREDIT,
                WalletTransactionType.SYSTEM_CREDIT,
                new Money(100_000, CurrencyCode.IRT),
                new Money(350_000, CurrencyCode.IRT),
                "wallet.test",
                Instant.parse("2026-07-14T00:00:00Z")
        )), 0, 10, false, false, 1);

        String text = formatter.format(page, "FA");

        assertThat(text).contains("افزایش موجودی", "۱۰۰٬۰۰۰ تومان", "۳۵۰٬۰۰۰ تومان");
        assertThat(text).doesNotContain("SYSTEM_CREDIT").doesNotContain(page.items().getFirst().transactionId().toString());
    }
}
