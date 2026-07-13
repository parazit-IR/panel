package com.parazit.panel.application.telegram.tariff;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.plan.catalog.result.AvailablePlanResult;
import com.parazit.panel.application.telegram.TelegramHtmlEscaper;
import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.application.telegram.TelegramPersianTextFormatter;
import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.plan.PlanType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TelegramTariffMessageFormatterTest {

    private final TelegramTariffMessageFormatter formatter = new TelegramTariffMessageFormatter(
            new TelegramMessageCatalog(),
            new TelegramHtmlEscaper(),
            new TelegramPersianTextFormatter()
    );

    @Test
    void formatsTrustedPlanDataWithoutInternalIds() {
        AvailablePlanResult plan = new AvailablePlanResult(
                UUID.fromString("11111111-1111-4111-8111-111111111111"),
                "monthly-20",
                "یک ماهه <۲۰>",
                "",
                PlanType.TRAFFIC_LIMITED,
                100_000L,
                CurrencyCode.IRT,
                30,
                20L * 1024L * 1024L * 1024L,
                2,
                10
        );

        String text = formatter.format("FA", new TelegramTariffPage(List.of(plan), 1, 1, false, false));

        assertThat(text).contains("💵 تعرفه اشتراک‌ها");
        assertThat(text).contains("یک ماهه &lt;۲۰&gt;");
        assertThat(text).contains("۲۰ گیگابایت");
        assertThat(text).contains("۱۰۰٬۰۰۰ تومان");
        assertThat(text).doesNotContain(plan.id().toString());
    }

    @Test
    void rendersUnlimitedPlansExplicitly() {
        AvailablePlanResult plan = new AvailablePlanResult(
                UUID.randomUUID(),
                "unlimited",
                "نامحدود",
                "",
                PlanType.UNLIMITED,
                250_000L,
                CurrencyCode.IRT,
                30,
                null,
                null,
                10
        );

        String text = formatter.format("FA", new TelegramTariffPage(List.of(plan), 1, 1, false, false));

        assertThat(text).contains("نامحدود");
    }
}
