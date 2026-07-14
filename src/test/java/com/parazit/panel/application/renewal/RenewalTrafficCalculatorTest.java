package com.parazit.panel.application.renewal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.domain.order.RenewalTrafficPolicy;
import org.junit.jupiter.api.Test;

class RenewalTrafficCalculatorTest {

    private final RenewalTrafficCalculator calculator = new RenewalTrafficCalculator();

    @Test
    void resetsToPlanLimitAndUsage() {
        RenewalTrafficCalculation result = calculator.calculate(
                RenewalTrafficPolicy.RESET_TO_PLAN_LIMIT,
                gb(50),
                gb(30),
                gb(40)
        );

        assertThat(result.desiredTotalTrafficBytes()).isEqualTo(gb(40));
        assertThat(result.resetUsage()).isTrue();
    }

    @Test
    void addsRenewalTrafficToRemainingQuotaAndResetsUsage() {
        RenewalTrafficCalculation result = calculator.calculate(
                RenewalTrafficPolicy.ADD_TO_REMAINING,
                gb(50),
                gb(30),
                gb(40)
        );

        assertThat(result.desiredTotalTrafficBytes()).isEqualTo(gb(60));
        assertThat(result.resetUsage()).isTrue();
    }

    @Test
    void addsRenewalTrafficToTotalAndPreservesUsage() {
        RenewalTrafficCalculation result = calculator.calculate(
                RenewalTrafficPolicy.ADD_TO_TOTAL_LIMIT,
                gb(50),
                gb(30),
                gb(40)
        );

        assertThat(result.desiredTotalTrafficBytes()).isEqualTo(gb(90));
        assertThat(result.resetUsage()).isFalse();
    }

    @Test
    void preservesTrafficWhenUnchanged() {
        RenewalTrafficCalculation result = calculator.calculate(
                RenewalTrafficPolicy.UNCHANGED,
                gb(50),
                gb(30),
                gb(40)
        );

        assertThat(result.desiredTotalTrafficBytes()).isEqualTo(gb(50));
        assertThat(result.resetUsage()).isFalse();
    }

    @Test
    void treatsZeroCurrentOrNullRenewalTrafficAsUnlimited() {
        assertThat(calculator.calculate(RenewalTrafficPolicy.ADD_TO_TOTAL_LIMIT, 0, gb(30), gb(40))
                .desiredTotalTrafficBytes()).isZero();
        assertThat(calculator.calculate(RenewalTrafficPolicy.RESET_TO_PLAN_LIMIT, gb(50), gb(30), null)
                .desiredTotalTrafficBytes()).isZero();
    }

    @Test
    void rejectsNegativeCurrentState() {
        assertThatThrownBy(() -> calculator.calculate(RenewalTrafficPolicy.UNCHANGED, -1, 0, gb(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static long gb(long value) {
        return value * 1_073_741_824L;
    }
}
