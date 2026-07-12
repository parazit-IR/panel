package com.parazit.panel.infrastructure.payment.manual;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SecureManualPaymentSuffixGeneratorTest {

    @Test
    void generatesWithinRange() {
        SecureManualPaymentSuffixGenerator generator = new SecureManualPaymentSuffixGenerator();

        for (int index = 0; index < 100; index++) {
            assertThat(generator.generate(101, 4999)).isBetween(101L, 4999L);
        }
        assertThat(generator.generate(7, 7)).isEqualTo(7);
    }

    @Test
    void rejectsInvalidRange() {
        SecureManualPaymentSuffixGenerator generator = new SecureManualPaymentSuffixGenerator();

        assertThatThrownBy(() -> generator.generate(0, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> generator.generate(10, 9)).isInstanceOf(IllegalArgumentException.class);
    }
}
