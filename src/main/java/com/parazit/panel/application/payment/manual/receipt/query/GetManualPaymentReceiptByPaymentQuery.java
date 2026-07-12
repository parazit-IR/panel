package com.parazit.panel.application.payment.manual.receipt.query;

import java.util.UUID;

public record GetManualPaymentReceiptByPaymentQuery(UUID paymentId, Long telegramUserId) {
}
