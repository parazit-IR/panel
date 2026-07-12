package com.parazit.panel.application.payment.zarinpal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ZarinpalAmountConverterTest {

    private final ZarinpalAmountConverter converter = new ZarinpalAmountConverter();

    @Test
    void keepsLocalIrtAmountForGatewayIrtContract() {
        assertThat(converter.toGatewayAmount(500_000L, " irt ")).isEqualTo(500_000L);
        assertThat(converter.gatewayCurrency("IRT")).isEqualTo("IRT");
    }

    @Test
    void rejectsZeroNegativeAndUnsupportedCurrency() {
        assertThatThrownBy(() -> converter.toGatewayAmount(0, "IRT"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> converter.toGatewayAmount(-1, "IRT"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> converter.toGatewayAmount(100, "USD"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
