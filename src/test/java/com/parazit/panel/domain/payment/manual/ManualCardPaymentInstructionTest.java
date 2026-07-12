package com.parazit.panel.domain.payment.manual;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ManualCardPaymentInstructionTest {

    private static final Instant NOW = Instant.parse("2026-07-12T12:00:00Z");
    private static final ManualPaymentDestination DESTINATION = new ManualPaymentDestination(
            "PRIMARY_CARD",
            "Example Bank",
            "Example Holder",
            BankCardNumber.parse("6037990000000014"),
            true,
            0
    );

    @Test
    void createsActivatesExpiresAndCancels() {
        ManualCardPaymentInstruction instruction = instruction();

        assertThat(instruction.getStatus()).isEqualTo(ManualPaymentInstructionStatus.CREATED);
        assertThat(instruction.getBaseAmount()).isEqualTo(100_000L);
        assertThat(instruction.getUniqueSuffixAmount()).isEqualTo(1_638L);
        assertThat(instruction.getPayableAmount()).isEqualTo(101_638L);
        assertThat(instruction.getCardNumberMaskedSnapshot()).isEqualTo("6037-****-****-0014");

        instruction.activate();
        assertThat(instruction.getStatus()).isEqualTo(ManualPaymentInstructionStatus.ACTIVE);
        assertThat(instruction.isExpiredAt(NOW.plus(Duration.ofMinutes(31)))).isTrue();

        instruction.expire(NOW.plus(Duration.ofMinutes(31)));
        assertThat(instruction.getStatus()).isEqualTo(ManualPaymentInstructionStatus.EXPIRED);
        assertThat(instruction.getExpiredAt()).isNotNull();
    }

    @Test
    void rejectsInvalidAmountsAndTransitions() {
        assertThatThrownBy(() -> ManualCardPaymentInstruction.create(
                UUID.randomUUID(), UUID.randomUUID(), 0, 1, "IRT", DESTINATION, NOW, Duration.ofMinutes(30)
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ManualCardPaymentInstruction.create(
                UUID.randomUUID(), UUID.randomUUID(), Long.MAX_VALUE, 1, "IRT", DESTINATION, NOW, Duration.ofMinutes(30)
        )).isInstanceOf(ArithmeticException.class);

        ManualCardPaymentInstruction instruction = instruction();
        assertThatCode(() -> instruction.cancel(NOW)).doesNotThrowAnyException();
        assertThat(instruction.getStatus()).isEqualTo(ManualPaymentInstructionStatus.CANCELLED);
        assertThatThrownBy(() -> instruction.activate())
                .isInstanceOf(IllegalStateException.class);
    }

    private ManualCardPaymentInstruction instruction() {
        return ManualCardPaymentInstruction.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                100_000L,
                1_638L,
                "IRT",
                DESTINATION,
                NOW,
                Duration.ofMinutes(30)
        );
    }
}
