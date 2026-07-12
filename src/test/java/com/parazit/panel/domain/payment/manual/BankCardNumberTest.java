package com.parazit.panel.domain.payment.manual;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BankCardNumberTest {

    @Test
    void normalizesFormatsMasksAndRedacts() {
        BankCardNumber card = BankCardNumber.parse("۶۰۳۷-۹۹۰۰-۰۰۰۰-۰۰۱۴");

        assertThat(card.value()).isEqualTo("6037990000000014");
        assertThat(card.formatted()).isEqualTo("6037-9900-0000-0014");
        assertThat(card.masked()).isEqualTo("6037-****-****-0014");
        assertThat(card.toString()).doesNotContain("6037990000000014");
    }

    @Test
    void rejectsInvalidCards() {
        assertThatThrownBy(() -> BankCardNumber.parse("123"))
                .isInstanceOf(InvalidBankCardNumberException.class);
        assertThatThrownBy(() -> BankCardNumber.parse("6037990000000015"))
                .isInstanceOf(InvalidBankCardNumberException.class);
        assertThatThrownBy(() -> BankCardNumber.parse("60379900000000AA"))
                .isInstanceOf(InvalidBankCardNumberException.class);
    }
}
