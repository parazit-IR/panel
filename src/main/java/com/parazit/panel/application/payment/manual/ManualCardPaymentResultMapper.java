package com.parazit.panel.application.payment.manual;

import com.parazit.panel.application.payment.manual.result.InitializeManualCardPaymentResult;
import com.parazit.panel.application.payment.manual.result.ManualCardPaymentInstructionResult;
import com.parazit.panel.application.payment.manual.result.ManualPaymentDisplayInstructions;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.manual.ManualCardPaymentInstruction;
import com.parazit.panel.domain.payment.manual.ManualPaymentDestination;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ManualCardPaymentResultMapper {

    public InitializeManualCardPaymentResult toInitializeResult(
            Payment payment,
            ManualCardPaymentInstruction instruction,
            ManualPaymentDestination destination,
            boolean newlyInitialized
    ) {
        return new InitializeManualCardPaymentResult(
                toInstructionResult(payment, instruction, destination, newlyInitialized)
        );
    }

    public ManualCardPaymentInstructionResult toInstructionResult(
            Payment payment,
            ManualCardPaymentInstruction instruction,
            ManualPaymentDestination destination,
            boolean newlyInitialized
    ) {
        Objects.requireNonNull(payment, "payment must not be null");
        Objects.requireNonNull(instruction, "instruction must not be null");
        Objects.requireNonNull(destination, "destination must not be null");
        ManualPaymentDisplayInstructions displayInstructions = new ManualPaymentDisplayInstructions(
                "Manual card-to-card payment",
                instruction.getBaseAmount(),
                instruction.getUniqueSuffixAmount(),
                instruction.getPayableAmount(),
                instruction.getCurrency(),
                instruction.getBankNameSnapshot(),
                instruction.getCardHolderNameSnapshot(),
                destination.formattedCardNumber(),
                instruction.getExpiresAt(),
                List.of(
                        "Transfer the exact payable amount.",
                        "Uploading a receipt does not automatically confirm payment.",
                        "The payment must be reviewed by an operator.",
                        "Do not reuse an expired instruction."
                )
        );
        return new ManualCardPaymentInstructionResult(
                payment.getId(),
                instruction.getId(),
                instruction.getInstructionRequestId(),
                payment.getStatus(),
                instruction.getStatus(),
                instruction.getBaseAmount(),
                instruction.getUniqueSuffixAmount(),
                instruction.getPayableAmount(),
                instruction.getCurrency(),
                instruction.getBankNameSnapshot(),
                instruction.getCardHolderNameSnapshot(),
                destination.cardNumber().value(),
                destination.formattedCardNumber(),
                instruction.getCardNumberMaskedSnapshot(),
                instruction.getIssuedAt(),
                instruction.getExpiresAt(),
                newlyInitialized,
                displayInstructions
        );
    }
}
