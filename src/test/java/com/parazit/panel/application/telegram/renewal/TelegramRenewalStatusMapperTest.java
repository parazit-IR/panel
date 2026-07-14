package com.parazit.panel.application.telegram.renewal;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.domain.order.OrderStatus;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.renewal.RenewalOutboxStatus;
import org.junit.jupiter.api.Test;

class TelegramRenewalStatusMapperTest {

    private final TelegramRenewalStatusMapper mapper = new TelegramRenewalStatusMapper();

    @Test
    void mapsQueuedAndReviewStatesWithoutRawEnums() {
        assertThat(mapper.renewalLabel(
                OrderStatus.RENEWAL_PENDING,
                PaymentStatus.APPROVED,
                RenewalOutboxStatus.PENDING,
                "fa"
        )).isEqualTo("تمدید در صف اجرا");

        assertThat(mapper.renewalLabel(
                OrderStatus.RENEWAL_REVIEW_REQUIRED,
                PaymentStatus.APPROVED,
                null,
                "fa"
        )).isEqualTo("نیازمند بررسی");
    }
}
