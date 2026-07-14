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

    @Test
    void mapsProcessingCompletedRetryAndDeadStatesWithoutRawEnums() {
        assertThat(mapper.renewalLabel(
                OrderStatus.RENEWAL_PENDING,
                PaymentStatus.APPROVED,
                RenewalOutboxStatus.PROCESSING,
                "fa"
        )).isEqualTo("در حال اعمال تمدید");

        assertThat(mapper.renewalLabel(
                OrderStatus.COMPLETED,
                PaymentStatus.APPROVED,
                RenewalOutboxStatus.PROCESSED,
                "fa"
        )).isEqualTo("تمدید انجام شد");

        assertThat(mapper.renewalLabel(
                OrderStatus.RENEWAL_PENDING,
                PaymentStatus.APPROVED,
                RenewalOutboxStatus.FAILED,
                "fa"
        )).isEqualTo("تلاش مجدد در حال برنامه‌ریزی است");

        assertThat(mapper.renewalLabel(
                OrderStatus.RENEWAL_PENDING,
                PaymentStatus.APPROVED,
                RenewalOutboxStatus.DEAD,
                "fa"
        )).isEqualTo("تمدید ناموفق");
    }
}
