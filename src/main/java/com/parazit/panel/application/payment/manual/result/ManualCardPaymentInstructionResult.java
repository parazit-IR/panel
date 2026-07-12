package com.parazit.panel.application.payment.manual.result;

import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.manual.ManualPaymentInstructionStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class ManualCardPaymentInstructionResult {

    private final UUID paymentId;
    private final UUID instructionId;
    private final UUID instructionRequestId;
    private final PaymentStatus paymentStatus;
    private final ManualPaymentInstructionStatus instructionStatus;
    private final long baseAmount;
    private final long uniqueSuffixAmount;
    private final long payableAmount;
    private final String currency;
    private final String bankName;
    private final String cardHolderName;
    private final String cardNumber;
    private final String cardNumberFormatted;
    private final String cardNumberMasked;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private final boolean newlyInitialized;
    private final ManualPaymentDisplayInstructions displayInstructions;

    public ManualCardPaymentInstructionResult(
            UUID paymentId,
            UUID instructionId,
            UUID instructionRequestId,
            PaymentStatus paymentStatus,
            ManualPaymentInstructionStatus instructionStatus,
            long baseAmount,
            long uniqueSuffixAmount,
            long payableAmount,
            String currency,
            String bankName,
            String cardHolderName,
            String cardNumber,
            String cardNumberFormatted,
            String cardNumberMasked,
            Instant issuedAt,
            Instant expiresAt,
            boolean newlyInitialized,
            ManualPaymentDisplayInstructions displayInstructions
    ) {
        this.paymentId = Objects.requireNonNull(paymentId, "paymentId must not be null");
        this.instructionId = Objects.requireNonNull(instructionId, "instructionId must not be null");
        this.instructionRequestId = Objects.requireNonNull(instructionRequestId, "instructionRequestId must not be null");
        this.paymentStatus = Objects.requireNonNull(paymentStatus, "paymentStatus must not be null");
        this.instructionStatus = Objects.requireNonNull(instructionStatus, "instructionStatus must not be null");
        this.baseAmount = baseAmount;
        this.uniqueSuffixAmount = uniqueSuffixAmount;
        this.payableAmount = payableAmount;
        this.currency = Objects.requireNonNull(currency, "currency must not be null");
        this.bankName = Objects.requireNonNull(bankName, "bankName must not be null");
        this.cardHolderName = Objects.requireNonNull(cardHolderName, "cardHolderName must not be null");
        this.cardNumber = Objects.requireNonNull(cardNumber, "cardNumber must not be null");
        this.cardNumberFormatted = Objects.requireNonNull(cardNumberFormatted, "cardNumberFormatted must not be null");
        this.cardNumberMasked = Objects.requireNonNull(cardNumberMasked, "cardNumberMasked must not be null");
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.newlyInitialized = newlyInitialized;
        this.displayInstructions = Objects.requireNonNull(displayInstructions, "displayInstructions must not be null");
    }

    public UUID paymentId() {
        return paymentId;
    }

    public UUID instructionId() {
        return instructionId;
    }

    public UUID instructionRequestId() {
        return instructionRequestId;
    }

    public PaymentStatus paymentStatus() {
        return paymentStatus;
    }

    public ManualPaymentInstructionStatus instructionStatus() {
        return instructionStatus;
    }

    public long baseAmount() {
        return baseAmount;
    }

    public long uniqueSuffixAmount() {
        return uniqueSuffixAmount;
    }

    public long payableAmount() {
        return payableAmount;
    }

    public String currency() {
        return currency;
    }

    public String bankName() {
        return bankName;
    }

    public String cardHolderName() {
        return cardHolderName;
    }

    public String cardNumber() {
        return cardNumber;
    }

    public String cardNumberFormatted() {
        return cardNumberFormatted;
    }

    public String cardNumberMasked() {
        return cardNumberMasked;
    }

    public Instant issuedAt() {
        return issuedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public boolean newlyInitialized() {
        return newlyInitialized;
    }

    public ManualPaymentDisplayInstructions displayInstructions() {
        return displayInstructions;
    }

    @Override
    public String toString() {
        return "ManualCardPaymentInstructionResult["
                + "paymentId="
                + paymentId
                + ", instructionId="
                + instructionId
                + ", instructionRequestId="
                + instructionRequestId
                + ", paymentStatus="
                + paymentStatus
                + ", instructionStatus="
                + instructionStatus
                + ", baseAmount="
                + baseAmount
                + ", uniqueSuffixAmount="
                + uniqueSuffixAmount
                + ", payableAmount="
                + payableAmount
                + ", currency="
                + currency
                + ", bankName="
                + bankName
                + ", cardHolderName=<redacted>"
                + ", cardNumber=<redacted>"
                + ", cardNumberMasked="
                + cardNumberMasked
                + ", issuedAt="
                + issuedAt
                + ", expiresAt="
                + expiresAt
                + ", newlyInitialized="
                + newlyInitialized
                + ']';
    }
}
