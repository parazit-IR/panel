package com.parazit.panel.application.promotion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.config.properties.PromotionProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class PromotionCodeNormalizerTest {

    private final PromotionCodeNormalizer normalizer = new PromotionCodeNormalizer(new PromotionProperties(
            true,
            true,
            4,
            32,
            Duration.ofMinutes(30),
            true,
            true,
            false,
            false,
            "secret"
    ));

    @Test
    void normalizesLatinAndPersianDigits() {
        assertThat(normalizer.normalize(" ab-۱۲_٣ ")).isEqualTo("AB-12_3");
    }

    @Test
    void rejectsWhitespaceInsideCode() {
        assertThatThrownBy(() -> normalizer.normalize("AB 12"))
                .isInstanceOf(PromotionException.class);
    }

    @Test
    void rejectsInvalidCharacters() {
        assertThatThrownBy(() -> normalizer.normalize("AB$12"))
                .isInstanceOf(PromotionException.class);
    }

    @Test
    void masksWithoutExposingWholeCode() {
        assertThat(normalizer.mask("SPRING1405")).isEqualTo("SP***05");
    }
}
