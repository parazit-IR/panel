package com.parazit.panel.api.internal.payment;

import com.parazit.panel.application.payment.command.CreatePaymentCommand;
import com.parazit.panel.application.payment.result.PaymentResult;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class PaymentApiMapper {

    public CreatePaymentCommand toCommand(CreatePaymentRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return new CreatePaymentCommand(
                request.orderId(),
                request.userId(),
                request.paymentMethod(),
                request.amount(),
                request.currency()
        );
    }

    public PaymentResponse toResponse(PaymentResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new PaymentResponse(
                result.id(),
                result.orderId(),
                result.userId(),
                result.method(),
                result.status(),
                result.baseAmount(),
                result.payableAmount(),
                result.currency(),
                result.expiresAt(),
                result.paidAt(),
                result.approvedAt(),
                result.rejectedAt(),
                result.gatewayTransactionId(),
                result.gatewayAuthority(),
                result.rejectionReason(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
