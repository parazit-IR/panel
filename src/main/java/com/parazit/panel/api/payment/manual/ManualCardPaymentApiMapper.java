package com.parazit.panel.api.payment.manual;

import com.parazit.panel.application.payment.manual.command.CancelManualCardPaymentInstructionCommand;
import com.parazit.panel.application.payment.manual.command.InitializeManualCardPaymentCommand;
import com.parazit.panel.application.payment.manual.query.GetManualCardPaymentInstructionQuery;
import com.parazit.panel.application.payment.manual.result.ManualCardPaymentInstructionResult;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ManualCardPaymentApiMapper {

    public InitializeManualCardPaymentCommand toCommand(
            UUID paymentId,
            InitializeManualCardPaymentRequest request
    ) {
        return new InitializeManualCardPaymentCommand(
                request.instructionRequestId(),
                paymentId,
                request.telegramUserId()
        );
    }

    public GetManualCardPaymentInstructionQuery toQuery(UUID paymentId, Long telegramUserId) {
        return new GetManualCardPaymentInstructionQuery(telegramUserId, paymentId);
    }

    public CancelManualCardPaymentInstructionCommand toCommand(
            UUID paymentId,
            CancelManualCardPaymentRequest request
    ) {
        return new CancelManualCardPaymentInstructionCommand(paymentId, request.telegramUserId());
    }

    public ManualCardPaymentResponse toResponse(ManualCardPaymentInstructionResult result) {
        return new ManualCardPaymentResponse(
                result.paymentId(),
                result.instructionId(),
                result.instructionRequestId(),
                result.paymentStatus(),
                result.instructionStatus(),
                result.baseAmount(),
                result.uniqueSuffixAmount(),
                result.payableAmount(),
                result.currency(),
                result.bankName(),
                result.cardHolderName(),
                result.cardNumber(),
                result.cardNumberFormatted(),
                result.cardNumberMasked(),
                result.issuedAt(),
                result.expiresAt(),
                result.newlyInitialized()
        );
    }
}
