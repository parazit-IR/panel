package com.parazit.panel.application.payment.manual.receipt.query;

import java.util.UUID;

public record GetManualPaymentReceiptByIdQuery(UUID receiptId, Long telegramUserId) {
}
