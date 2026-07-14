package com.parazit.panel.application.telegram.renewal;

import com.parazit.panel.application.customer.result.CustomerServiceStatus;
import com.parazit.panel.domain.order.OrderStatus;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.renewal.RenewalOutboxStatus;
import org.springframework.stereotype.Component;

@Component
public class TelegramRenewalStatusMapper {

    public String label(CustomerServiceStatus status, String language) {
        return switch (status) {
            case ACTIVE -> fa(language) ? "🟢 فعال" : "🟢 Active";
            case EXPIRED -> fa(language) ? "⚫ منقضی‌شده" : "⚫ Expired";
            case SUSPENDED -> fa(language) ? "🟠 تعلیق‌شده" : "🟠 Suspended";
            case REVOKED -> fa(language) ? "🔴 لغوشده" : "🔴 Revoked";
            case PROVISIONING -> fa(language) ? "🟡 در حال ساخت" : "🟡 Provisioning";
            case FAILED -> fa(language) ? "❌ خطای ساخت" : "❌ Failed";
            case UNKNOWN -> fa(language) ? "❔ نامشخص" : "❔ Unknown";
        };
    }

    public String renewalLabel(
            OrderStatus orderStatus,
            PaymentStatus paymentStatus,
            RenewalOutboxStatus outboxStatus,
            String language
    ) {
        if (orderStatus == OrderStatus.RENEWAL_REVIEW_REQUIRED) {
            return fa(language) ? "نیازمند بررسی" : "Review required";
        }
        if (orderStatus == OrderStatus.COMPLETED) {
            return fa(language) ? "تمدید انجام شد" : "Renewal completed";
        }
        if (outboxStatus == RenewalOutboxStatus.PROCESSING) {
            return fa(language) ? "در حال اعمال تمدید" : "Applying renewal";
        }
        if (outboxStatus == RenewalOutboxStatus.PENDING) {
            return fa(language) ? "تمدید در صف اجرا" : "Renewal queued";
        }
        if (outboxStatus == RenewalOutboxStatus.FAILED) {
            return fa(language) ? "تلاش مجدد در حال برنامه‌ریزی است" : "Retry scheduled";
        }
        if (outboxStatus == RenewalOutboxStatus.DEAD) {
            return fa(language) ? "تمدید ناموفق" : "Renewal failed";
        }
        if (paymentStatus == PaymentStatus.APPROVED
                && (orderStatus == OrderStatus.PAID || orderStatus == OrderStatus.RENEWAL_PENDING)) {
            return fa(language) ? "پرداخت تأیید شد" : "Payment approved";
        }
        if (orderStatus == OrderStatus.CANCELLED || paymentStatus == PaymentStatus.CANCELLED) {
            return fa(language) ? "پرداخت لغوشده" : "Payment cancelled";
        }
        if (orderStatus == OrderStatus.EXPIRED || paymentStatus == PaymentStatus.EXPIRED) {
            return fa(language) ? "پرداخت منقضی‌شده" : "Payment expired";
        }
        return fa(language) ? "در انتظار پرداخت" : "Awaiting payment";
    }

    private static boolean fa(String language) {
        return language != null && language.toUpperCase(java.util.Locale.ROOT).startsWith("FA");
    }
}
