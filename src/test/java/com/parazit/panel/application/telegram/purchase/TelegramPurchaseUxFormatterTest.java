package com.parazit.panel.application.telegram.purchase;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.purchase.result.PurchasePreInvoiceResult;
import com.parazit.panel.application.telegram.TelegramHtmlEscaper;
import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.application.telegram.TelegramPersianTextFormatter;
import com.parazit.panel.domain.plan.CurrencyCode;
import java.time.Instant;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TelegramPurchaseUxFormatterTest {

    @Test
    void formatsPreInvoiceWithoutInternalIdsOrFakeFutureFields() {
        TelegramPurchaseUxFormatter formatter = new TelegramPurchaseUxFormatter(
                new TelegramMessageCatalog(),
                new TelegramHtmlEscaper(),
                new TelegramPersianTextFormatter()
        );
        UUID sessionId = UUID.randomUUID();
        UUID selectionId = UUID.randomUUID();

        String text = formatter.preInvoice("fa", new PurchasePreInvoiceResult(
                sessionId,
                selectionId,
                "<Ali>",
                "Monthly <30>",
                "یک ماهه",
                "توضیح <پلن>",
                30,
                OptionalLong.of(30L * 1024L * 1024L * 1024L),
                OptionalInt.of(2),
                100_000L,
                0L,
                100_000L,
                CurrencyCode.IRT,
                false,
                false,
                true,
                false,
                Instant.parse("2026-07-13T12:30:00Z"),
                Instant.parse("2026-07-13T12:00:00Z")
        ));

        assertThat(text).contains("🧾 پیش‌فاکتور شما");
        assertThat(text).contains("&lt;Ali&gt;");
        assertThat(text).contains("۱۰۰٬۰۰۰ تومان");
        assertThat(text).contains("۳۰ گیگابایت");
        assertThat(text).doesNotContain(sessionId.toString());
        assertThat(text).doesNotContain(selectionId.toString());
        assertThat(text).doesNotContain("کیف پول");
    }
}
