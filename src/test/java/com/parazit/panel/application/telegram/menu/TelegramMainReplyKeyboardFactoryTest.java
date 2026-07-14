package com.parazit.panel.application.telegram.menu;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.sales.SalesAvailabilityService;
import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.config.properties.ManualPaymentProperties;
import com.parazit.panel.config.properties.PaymentProperties;
import com.parazit.panel.config.properties.SalesControlProperties;
import com.parazit.panel.config.properties.SubscriptionProperties;
import com.parazit.panel.config.properties.TelegramFeaturePlaceholderProperties;
import com.parazit.panel.config.properties.TelegramMenuProperties;
import com.parazit.panel.config.properties.WalletProperties;
import com.parazit.panel.config.properties.WalletPaymentProperties;
import com.parazit.panel.config.properties.ZarinpalProperties;
import com.parazit.panel.domain.plan.CurrencyCode;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TelegramMainReplyKeyboardFactoryTest {

    @Test
    void buildsDeterministicPersianPersistentKeyboardWithoutDuplicates() {
        TelegramMessageCatalog catalog = new TelegramMessageCatalog();
        TelegramMainReplyKeyboardFactory factory = new TelegramMainReplyKeyboardFactory(
                new TelegramMenuLabelProvider(catalog),
                availabilityService(),
                catalog,
                menuProperties()
        );

        var keyboard = factory.mainKeyboard("FA");

        assertThat(keyboard.resizeKeyboard()).isTrue();
        assertThat(keyboard.oneTimeKeyboard()).isFalse();
        assertThat(keyboard.persistent()).isTrue();
        assertThat(keyboard.rows()).hasSize(5);
        assertThat(keyboard.rows().get(0).buttons()).extracting("text")
                .containsExactly("🔐 خرید اشتراک", "♻️ تمدید سرویس");
        assertThat(keyboard.rows().get(1).buttons()).extracting("text")
                .containsExactly("🛍 سرویس‌های من", "🔑 اکانت تست");
        assertThat(keyboard.rows().stream()
                .flatMap(row -> row.buttons().stream())
                .map(button -> button.text())
                .distinct()
                .count()).isEqualTo(8);
    }

    private static TelegramMenuFeatureAvailabilityService availabilityService() {
        return new TelegramMenuFeatureAvailabilityService(
                menuProperties(),
                new TelegramFeaturePlaceholderProperties(false, false, false, false, false),
                new PaymentProperties(false, "", Duration.ofMinutes(30)),
                subscriptionProperties(),
                new WalletProperties(true, CurrencyCode.IRT, 10, 50, false, true, true, 3),
                salesAvailabilityService()
        );
    }

    private static SalesAvailabilityService salesAvailabilityService() {
        return new SalesAvailabilityService(
                new SalesControlProperties(true, true, false, false, false, false, false, false, "", "", "", null),
                new ManualPaymentProperties(false, Duration.ofMinutes(30), 1000, 9999, 10, "", "", "", "", true, Duration.ofMinutes(2)),
                new ZarinpalProperties(false, "", null, null, null, null, null, null, null, null, Duration.ofSeconds(1), Duration.ofSeconds(1), 0, Duration.ofMillis(100), true, true),
                new WalletPaymentProperties(false, true, true, CurrencyCode.IRT, 0, 0, 3, Duration.ofMinutes(15)),
                List.of(),
                () -> Instant.parse("2026-07-14T00:00:00Z")
        );
    }

    private static TelegramMenuProperties menuProperties() {
        return new TelegramMenuProperties(true, true, true, true, true, true, true, true, false, false, "fa");
    }

    private static SubscriptionProperties subscriptionProperties() {
        return new SubscriptionProperties(
                true,
                URI.create("http://localhost:8081"),
                32,
                12,
                160,
                Duration.ofSeconds(5),
                true,
                true,
                true,
                "base64",
                "Panel VPN",
                24,
                URI.create("http://localhost:8081/support")
        );
    }
}
