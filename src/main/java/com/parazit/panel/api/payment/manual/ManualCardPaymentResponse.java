package com.parazit.panel.api.payment.manual;

import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.manual.ManualPaymentInstructionStatus;
import java.time.Instant;
import java.util.UUID;

public final class ManualCardPaymentResponse {

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

    public ManualCardPaymentResponse(
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
            boolean newlyInitialized
    ) {
        this.paymentId = paymentId;
        this.instructionId = instructionId;
        this.instructionRequestId = instructionRequestId;
        this.paymentStatus = paymentStatus;
        this.instructionStatus = instructionStatus;
        this.baseAmount = baseAmount;
        this.uniqueSuffixAmount = uniqueSuffixAmount;
        this.payableAmount = payableAmount;
        this.currency = currency;
        this.bankName = bankName;
        this.cardHolderName = cardHolderName;
        this.cardNumber = cardNumber;
        this.cardNumberFormatted = cardNumberFormatted;
        this.cardNumberMasked = cardNumberMasked;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.newlyInitialized = newlyInitialized;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public UUID getInstructionId() {
        return instructionId;
    }

    public UUID getInstructionRequestId() {
        return instructionRequestId;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public ManualPaymentInstructionStatus getInstructionStatus() {
        return instructionStatus;
    }

    public long getBaseAmount() {
        return baseAmount;
    }

    public long getUniqueSuffixAmount() {
        return uniqueSuffixAmount;
    }

    public long getPayableAmount() {
        return payableAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getBankName() {
        return bankName;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getCardNumberFormatted() {
        return cardNumberFormatted;
    }

    public String getCardNumberMasked() {
        return cardNumberMasked;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isNewlyInitialized() {
        return newlyInitialized;
    }

    @Override
    public String toString() {
        return "ManualCardPaymentResponse["
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
