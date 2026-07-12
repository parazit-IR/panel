package com.parazit.panel.api.payment.zarinpal;

import com.parazit.panel.application.payment.zarinpal.command.HandleZarinpalCallbackCommand;
import com.parazit.panel.application.payment.zarinpal.command.InitializeZarinpalPaymentCommand;
import com.parazit.panel.application.payment.zarinpal.result.InitializeZarinpalPaymentResult;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ZarinpalPaymentApiMapper {

    public InitializeZarinpalPaymentCommand toCommand(UUID paymentId, InitializeZarinpalPaymentRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return new InitializeZarinpalPaymentCommand(
                request.requestId(),
                paymentId,
                request.telegramUserId(),
                request.description(),
                request.mobile(),
                request.email()
        );
    }

    public HandleZarinpalCallbackCommand toCallbackCommand(String authority, String status) {
        return new HandleZarinpalCallbackCommand(authority, status);
    }

    public InitializeZarinpalPaymentResponse toResponse(InitializeZarinpalPaymentResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new InitializeZarinpalPaymentResponse(
                result.paymentId(),
                result.attemptId(),
                result.requestId(),
                result.paymentStatus(),
                result.attemptStatus(),
                result.paymentUrl(),
                result.expiresAt(),
                result.newlyInitialized()
        );
    }
}
