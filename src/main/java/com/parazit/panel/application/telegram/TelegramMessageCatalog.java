package com.parazit.panel.application.telegram;

import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class TelegramMessageCatalog {

    public String text(String language, String key) {
        String normalized = language == null ? "EN" : language.toUpperCase(Locale.ROOT);
        boolean fa = normalized.startsWith("FA");
        return switch (key) {
            case "welcome" -> fa ? "به پنل VPN خوش آمدید." : "Welcome to your VPN panel.";
            case "main_menu_title" -> fa ? "منوی اصلی" : "Main menu";
            case "my_subscriptions" -> fa ? "اشتراک‌های من" : "My subscriptions";
            case "help" -> fa ? "برای مدیریت اشتراک از منو استفاده کنید." : "Use the menu to manage your subscriptions.";
            case "no_subscriptions" -> fa ? "اشتراکی برای نمایش وجود ندارد." : "No subscriptions are available.";
            case "not_available" -> fa ? "این مورد در دسترس نیست." : "This item is not available.";
            case "temporary_error" -> fa ? "خطای موقت. دوباره تلاش کنید." : "Temporary error. Please try again.";
            case "rotation_warning" -> fa
                    ? "ساخت لینک جدید، لینک قبلی را نامعتبر می‌کند."
                    : "Generating a new subscription link invalidates the previous link.";
            case "rotation_cancelled" -> fa ? "ساخت لینک جدید لغو شد." : "New link generation cancelled.";
            case "private_only" -> fa ? "برای دریافت اطلاعات اشتراک، چت خصوصی لازم است." : "Please use a private chat for subscription credentials.";
            default -> key;
        };
    }
}
