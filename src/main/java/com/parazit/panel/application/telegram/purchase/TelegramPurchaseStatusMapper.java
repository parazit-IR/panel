package com.parazit.panel.application.telegram.purchase;

import com.parazit.panel.domain.payment.PaymentStatus;
import org.springframework.stereotype.Component;

@Component
public class TelegramPurchaseStatusMapper {

    public String label(String language, PaymentStatus status) {
        boolean fa = language != null && language.toUpperCase(java.util.Locale.ROOT).startsWith("FA");
        return switch (status) {
            case CREATED -> fa ? "در انتظار پرداخت" : "Waiting for payment";
            case WAITING_FOR_PAYMENT -> fa ? "در انتظار ارسال رسید" : "Waiting for receipt";
            case RECEIPT_SUBMITTED, WAITING_FOR_REVIEW -> fa ? "در انتظار بررسی رسید" : "Waiting for receipt review";
            case PROCESSING -> fa ? "در حال بررسی پرداخت" : "Processing payment";
            case APPROVED -> fa ? "پرداخت تأیید شد" : "Payment approved";
            case CANCELLED -> fa ? "پرداخت لغو شد" : "Payment cancelled";
            case EXPIRED -> fa ? "پرداخت منقضی شد" : "Payment expired";
            case REJECTED, FAILED -> fa ? "پرداخت ناموفق بود" : "Payment failed";
            case UNKNOWN -> fa ? "وضعیت پرداخت مشخص نیست" : "Payment status is unknown";
        };
    }
}
