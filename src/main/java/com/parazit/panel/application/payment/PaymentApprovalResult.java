package com.parazit.panel.application.payment;

import com.parazit.panel.domain.order.OrderStatus;
import com.parazit.panel.domain.payment.PaymentStatus;
import java.util.UUID;

public record PaymentApprovalResult(
        UUID paymentId,
        UUID orderId,
        PaymentStatus paymentStatus,
        OrderStatus orderStatus,
        UUID provisioningEventId,
        boolean newlyApproved,
        boolean provisioningRequired
) {
}
