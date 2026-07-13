package com.parazit.panel.application.telegram.menu;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TelegramButtonTextNormalizerTest {

    private final TelegramButtonTextNormalizer normalizer = new TelegramButtonTextNormalizer();

    @Test
    void normalizesPersianArabicVariantsWhitespaceAndEmojiVariation() {
        assertThat(normalizer.normalize("  💰\uFE0F  كيف\u200C پول  +  شارژ  "))
                .isEqualTo("💰 کیف پول + شارژ");
    }

    @Test
    void keepsExactSemanticsAndRejectsPartialEquivalence() {
        assertThat(normalizer.exactNormalizedMatch("🔐 خرید اشتراک", "🔐 خرید اشتراک")).isTrue();
        assertThat(normalizer.exactNormalizedMatch("🔐 خرید", "🔐 خرید اشتراک")).isFalse();
    }
}
