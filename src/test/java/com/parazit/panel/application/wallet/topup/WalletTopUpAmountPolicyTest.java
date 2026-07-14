package com.parazit.panel.application.wallet.topup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.config.properties.WalletTopUpProperties;
import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.plan.CurrencyCode;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class WalletTopUpAmountPolicyTest {

    private final WalletTopUpAmountPolicy policy = new WalletTopUpAmountPolicy(new WalletTopUpProperties(
            true,
            CurrencyCode.IRT,
            1_000,
            10_000_000,
            Duration.ofMinutes(30),
            3,
            true,
            false
    ));

    @Test
    void parsesLatinPersianAndArabicDigitsWithSeparators() {
        assertThat(policy.parseCustomerInput("1000")).isEqualTo(new Money(1_000, CurrencyCode.IRT));
        assertThat(policy.parseCustomerInput("۱٬۲۳۴")).isEqualTo(new Money(1_234, CurrencyCode.IRT));
        assertThat(policy.parseCustomerInput("١,٢٣٤")).isEqualTo(new Money(1_234, CurrencyCode.IRT));
    }

    @Test
    void enforcesMinimumMaximumAndRejectsUnsafeFormats() {
        assertThatThrownBy(() -> policy.parseCustomerInput("999"))
                .isInstanceOf(WalletTopUpException.class);
        assertThatThrownBy(() -> policy.parseCustomerInput("10000001"))
                .isInstanceOf(WalletTopUpException.class);
        assertThatThrownBy(() -> policy.parseCustomerInput("1.5"))
                .isInstanceOf(WalletTopUpException.class);
        assertThatThrownBy(() -> policy.parseCustomerInput("1e6"))
                .isInstanceOf(WalletTopUpException.class);
        assertThatThrownBy(() -> policy.parseCustomerInput("-1000"))
                .isInstanceOf(WalletTopUpException.class);
        assertThatThrownBy(() -> policy.parseCustomerInput("شارژ"))
                .isInstanceOf(WalletTopUpException.class);
    }
}
