package com.parazit.panel.application.telegram.menu;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.config.properties.PaymentProperties;
import com.parazit.panel.config.properties.SubscriptionProperties;
import com.parazit.panel.config.properties.TelegramFeaturePlaceholderProperties;
import com.parazit.panel.config.properties.TelegramMenuProperties;
import java.net.URI;
import java.time.Duration;
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
                subscriptionProperties()
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
