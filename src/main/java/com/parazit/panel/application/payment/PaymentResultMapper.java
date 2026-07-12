package com.parazit.panel.application.payment;

import com.parazit.panel.application.payment.result.PaymentResult;
import com.parazit.panel.domain.payment.Payment;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class PaymentResultMapper {

    public PaymentResult toResult(Payment payment) {
        Objects.requireNonNull(payment, "payment must not be null");
        return new PaymentResult(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getBaseAmount(),
                payment.getPayableAmount(),
                payment.getCurrency(),
                payment.getExpiresAt(),
                payment.getPaidAt(),
                payment.getApprovedAt(),
                payment.getRejectedAt(),
                payment.getGatewayTransactionId(),
                payment.getGatewayAuthority(),
                payment.getRejectionReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
