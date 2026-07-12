package com.parazit.panel.config.properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.domain.payment.manual.InvalidBankCardNumberException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ManualPaymentPropertiesTest {

    @Test
    void disabledDefaultsDoNotRequireCard() {
        ManualPaymentProperties properties = new ManualPaymentProperties(
                false,
                null,
                101,
                4_999,
                20,
                null,
                null,
                null,
                null,
                true,
                null
        );

        assertThat(properties.instructionTtl()).isEqualTo(Duration.ofMinutes(30));
        assertThat(properties.toString()).doesNotContain("6037990000000014");
    }

    @Test
    void enabledRequiresValidDestinationAndCard() {
        assertThatThrownBy(() -> new ManualPaymentProperties(
                true, null, 101, 4_999, 20, "PRIMARY", "Bank", "Holder", "", true, null
        )).isInstanceOf(InvalidBankCardNumberException.class);

        ManualPaymentProperties properties = new ManualPaymentProperties(
                true,
                Duration.ofMinutes(15),
                101,
                4_999,
                20,
                "PRIMARY",
                "Bank",
                "Holder",
                "6037990000000014",
                true,
                Duration.ofMinutes(1)
        );

        assertThat(properties.enabled()).isTrue();
        assertThat(properties.toString()).doesNotContain("6037990000000014");
    }
}
