package com.parazit.panel.application.telegram.renewal;

import com.parazit.panel.application.customer.result.CustomerServiceStatus;
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

    private static boolean fa(String language) {
        return language != null && language.toUpperCase(java.util.Locale.ROOT).startsWith("FA");
    }
}
