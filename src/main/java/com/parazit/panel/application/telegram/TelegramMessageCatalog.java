package com.parazit.panel.application.telegram;

import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class TelegramMessageCatalog {

    public String text(String language, String key) {
        String normalized = language == null ? "EN" : language.toUpperCase(Locale.ROOT);
        boolean fa = normalized.startsWith("FA");
        return switch (key) {
            case "welcome" -> fa ? "به فروشگاه VPN خوش آمدید." : "Welcome to your VPN store.";
            case "main_menu_title", "telegram.menu.title" -> fa ? "منوی اصلی" : "Main menu";
            case "my_subscriptions" -> fa ? "اشتراک‌های من" : "My subscriptions";
            case "help", "telegram.help.body" -> fa
                    ? "برای خرید اشتراک، مشاهده سرویس‌ها، دریافت کانفیگ و پیگیری پرداخت از منوی پایین استفاده کنید.\n\nبخش آموزش، سوالات متداول و پشتیبانی از همین بات در دسترس است.\n\nبرای خروج از عملیات جاری دستور /cancel را بفرستید."
                    : "Use the menu to buy a subscription, view services, receive configs, and follow payments.\n\nTutorials, FAQ, and support are available in the bot.\n\nSend /cancel to leave the current operation.";
            case "no_subscriptions" -> fa ? "اشتراکی برای نمایش وجود ندارد." : "No subscriptions are available.";
            case "not_available", "telegram.feature.unavailable" -> fa ? "این بخش در حال حاضر در دسترس نیست." : "This feature is currently unavailable.";
            case "temporary_error" -> fa ? "خطای موقت. دوباره تلاش کنید." : "Temporary error. Please try again.";
            case "rotation_warning" -> fa
                    ? "ساخت لینک جدید، لینک قبلی را نامعتبر می‌کند."
                    : "Generating a new subscription link invalidates the previous link.";
            case "rotation_cancelled" -> fa ? "ساخت لینک جدید لغو شد." : "New link generation cancelled.";
            case "private_only", "telegram.error.private_chat_only" -> fa
                    ? "برای استفاده از خدمات، لطفاً بات را در گفت‌وگوی خصوصی باز کنید."
                    : "Please open the bot in a private chat to use these services.";
            case "telegram.main.buy_subscription" -> fa ? "🔐 خرید اشتراک" : "🔐 Buy subscription";
            case "telegram.main.renew_service" -> fa ? "♻️ تمدید سرویس" : "♻️ Renew service";
            case "telegram.main.my_services" -> fa ? "🛍 سرویس‌های من" : "🛍 My services";
            case "telegram.main.trial_account" -> fa ? "🔑 اکانت تست" : "🔑 Trial account";
            case "telegram.main.tariffs" -> fa ? "💵 تعرفه اشتراک‌ها" : "💵 Tariffs";
            case "telegram.main.wallet" -> fa ? "💰 کیف پول + شارژ" : "💰 Wallet + top-up";
            case "telegram.main.tutorials" -> fa ? "📚 آموزش" : "📚 Tutorials";
            case "telegram.main.support" -> fa ? "☎️ پشتیبانی" : "☎️ Support";
            case "telegram.main.back_home", "telegram.navigation.home" -> fa ? "🏠 بازگشت به منوی اصلی" : "🏠 Main menu";
            case "telegram.navigation.back" -> fa ? "⬅️ بازگشت" : "⬅️ Back";
            case "telegram.navigation.close" -> fa ? "❌ بستن" : "❌ Close";
            case "telegram.main.coming_soon" -> fa ? "این بخش به‌زودی فعال می‌شود." : "This feature is coming soon.";
            case "telegram.welcome.generic_name" -> fa ? "کاربر" : "there";
            case "telegram.welcome.title" -> fa ? "سلام {firstName} عزیز 👋" : "Hello {firstName} 👋";
            case "telegram.welcome.body" -> fa
                    ? "به فروشگاه VPN خوش آمدید.\n\nاز طریق این بات می‌توانید اشتراک جدید خریداری کنید، سرویس‌های خود را مدیریت کنید، وضعیت پرداخت را ببینید و اطلاعات اتصال را دریافت کنید.\n\nیکی از گزینه‌های منوی پایین را انتخاب کنید."
                    : "Welcome to the VPN store.\n\nYou can buy a new subscription, manage your services, check payment status, and receive connection information here.\n\nChoose an option from the menu below.";
            case "telegram.menu.returned" -> fa
                    ? "🏠 به منوی اصلی بازگشتید.\n\nیکی از گزینه‌های زیر را انتخاب کنید."
                    : "🏠 You are back at the main menu.\n\nChoose one of the options below.";
            case "telegram.menu.unknown_action", "telegram.error.unknown_message" -> fa
                    ? "پیام شما قابل تشخیص نبود.\n\nلطفاً یکی از گزینه‌های منوی پایین را انتخاب کنید یا دستور /menu را بفرستید."
                    : "I could not recognize your message.\n\nPlease choose an option from the menu below or send /menu.";
            case "telegram.input.placeholder" -> fa ? "یکی از گزینه‌های منو را انتخاب کنید" : "Choose a menu option";
            case "telegram.cancel.success" -> fa
                    ? "عملیات جاری بسته شد و به منوی اصلی بازگشتید."
                    : "The current operation was closed and you are back at the main menu.";
            case "telegram.feature.purchase_unavailable" -> fa
                    ? "🔐 خرید اشتراک در حال حاضر فعال نیست."
                    : "🔐 Subscription purchase is currently unavailable.";
            case "telegram.feature.subscription_unavailable" -> fa
                    ? "🛍 سرویس‌های من در حال حاضر در دسترس نیست."
                    : "🛍 My services is currently unavailable.";
            case "telegram.feature.renewal_unavailable" -> fa
                    ? "♻️ تمدید سرویس به‌زودی از داخل بات فعال می‌شود."
                    : "♻️ Service renewal will be available in the bot soon.";
            case "telegram.feature.trial_unavailable" -> fa
                    ? "🔑 امکان دریافت اکانت تست در حال حاضر فعال نیست."
                    : "🔑 Trial accounts are currently unavailable.";
            case "telegram.feature.wallet_unavailable" -> fa
                    ? "💰 کیف پول و شارژ حساب هنوز فعال نشده است."
                    : "💰 Wallet and account top-up are not enabled yet.";
            case "telegram.feature.tutorial_unavailable" -> fa
                    ? "📚 بخش آموزش در حال آماده‌سازی است."
                    : "📚 Tutorials are being prepared.";
            case "telegram.feature.support_unavailable" -> fa
                    ? "☎️ اطلاعات پشتیبانی هنوز تنظیم نشده است."
                    : "☎️ Support information has not been configured yet.";
            case "telegram.feature.payments_unavailable" -> fa
                    ? "💳 مشاهده پرداخت‌ها از داخل بات هنوز فعال نشده است."
                    : "💳 In-bot payment history is not enabled yet.";
            case "telegram.feature.settings_unavailable" -> fa
                    ? "⚙️ تنظیمات اعلان‌ها از داخل بات هنوز فعال نشده است."
                    : "⚙️ In-bot notification settings are not enabled yet.";
            case "telegram.plans.empty" -> fa
                    ? "در حال حاضر پلنی برای نمایش وجود ندارد.\n\nلطفاً بعداً دوباره بررسی کنید."
                    : "There are no plans to show right now.\n\nPlease check again later.";
            case "telegram.plans.buy_title" -> fa ? "🔐 خرید اشتراک" : "🔐 Buy subscription";
            case "telegram.plans.tariffs_title" -> fa ? "💵 تعرفه اشتراک‌ها" : "💵 Tariffs";
            case "telegram.plans.price" -> fa ? "قیمت" : "Price";
            case "telegram.plans.duration" -> fa ? "مدت" : "Duration";
            case "telegram.plans.days" -> fa ? "روز" : "days";
            case "telegram.plans.purchase_placeholder" -> fa
                    ? "برای تکمیل خرید، بخش پرداخت داخل بات در مرحله بعدی فعال می‌شود."
                    : "In-bot payment completion will be enabled in a later step.";
            case "telegram.pagination.previous" -> fa ? "قبلی" : "Previous";
            case "telegram.pagination.next" -> fa ? "بعدی" : "Next";
            case "telegram.tariffs.title" -> fa ? "💵 تعرفه اشتراک‌ها" : "💵 Subscription tariffs";
            case "telegram.tariffs.empty" -> fa
                    ? "در حال حاضر تعرفه‌ای برای نمایش وجود ندارد."
                    : "There are no tariffs to show right now.";
            case "telegram.tariffs.buy" -> fa ? "🔐 خرید اشتراک" : "🔐 Buy subscription";
            case "telegram.tariffs.unlimited" -> fa ? "نامحدود" : "Unlimited";
            case "telegram.tariffs.duration" -> fa ? "مدت" : "Duration";
            case "telegram.tariffs.traffic" -> fa ? "حجم" : "Traffic";
            case "telegram.tariffs.price" -> fa ? "قیمت" : "Price";
            case "telegram.tariffs.gigabyte" -> fa ? "گیگابایت" : "GB";
            case "telegram.tariffs.devices" -> fa ? "تعداد دستگاه" : "Devices";
            case "telegram.tutorials.title" -> fa ? "📚 آموزش" : "📚 Tutorials";
            case "telegram.tutorials.choose" -> fa
                    ? "آموزش دستگاه مورد نظر را انتخاب کنید."
                    : "Choose your device tutorial.";
            case "telegram.tutorials.empty" -> fa
                    ? "📚 بخش آموزش در حال حاضر محتوایی برای نمایش ندارد."
                    : "📚 No tutorial content is available right now.";
            case "telegram.tutorials.android" -> fa ? "آموزش اتصال در اندروید" : "Android connection tutorial";
            case "telegram.tutorials.ios" -> fa ? "آموزش اتصال در آیفون" : "iPhone connection tutorial";
            case "telegram.tutorials.windows" -> fa ? "آموزش اتصال در ویندوز" : "Windows connection tutorial";
            case "telegram.tutorials.linux" -> fa ? "آموزش اتصال در لینوکس" : "Linux connection tutorial";
            case "telegram.tutorials.macos" -> fa ? "آموزش اتصال در مک" : "macOS connection tutorial";
            case "telegram.tutorials.downloads" -> fa ? "لینک دانلود برنامه‌ها" : "Application download links";
            case "telegram.tutorials.app_name" -> fa ? "برنامه پیشنهادی" : "Suggested app";
            case "telegram.tutorials.steps" -> fa ? "مراحل اتصال" : "Connection steps";
            case "telegram.tutorials.troubleshooting" -> fa ? "رفع مشکل" : "Troubleshooting";
            case "telegram.downloads.title" -> fa ? "🔗 لینک دانلود برنامه‌ها" : "🔗 Application download links";
            case "telegram.downloads.official_source" -> fa
                    ? "فقط لینک‌های تاییدشده و رسمی نمایش داده می‌شوند."
                    : "Only approved official links are shown.";
            case "telegram.downloads.empty" -> fa
                    ? "هنوز لینک دانلود تاییدشده‌ای تنظیم نشده است."
                    : "No approved download links are configured yet.";
            case "telegram.faq.title" -> fa ? "❓ سوالات متداول" : "❓ Frequently asked questions";
            case "telegram.faq.choose" -> fa
                    ? "سوال مورد نظر را انتخاب کنید."
                    : "Choose a question.";
            case "telegram.faq.empty" -> fa
                    ? "در حال حاضر سوال متداولی برای نمایش وجود ندارد."
                    : "No FAQ content is available right now.";
            case "telegram.faq.question" -> fa ? "سوال" : "Question";
            case "telegram.faq.back" -> fa ? "⬅️ بازگشت به سوالات" : "⬅️ Back to FAQ";
            case "telegram.support.title" -> fa ? "☎️ پشتیبانی" : "☎️ Support";
            case "telegram.support.description" -> fa
                    ? "برای پاسخ سریع‌تر، ابتدا سوالات متداول را بررسی کنید.\n\nدر صورتی که پاسخ خود را پیدا نکردید، می‌توانید با پشتیبانی در ارتباط باشید."
                    : "For a faster answer, check the FAQ first.\n\nIf you do not find your answer, contact support.";
            case "telegram.support.faq" -> fa ? "❓ سوالات متداول" : "❓ FAQ";
            case "telegram.support.direct_message" -> fa ? "☎️ ارسال پیام به پشتیبانی" : "☎️ Message support";
            case "telegram.support.working_hours" -> fa ? "ساعات پاسخ‌گویی" : "Working hours";
            case "telegram.support.unavailable" -> fa
                    ? "☎️ اطلاعات پشتیبانی هنوز تنظیم نشده است."
                    : "☎️ Support information has not been configured yet.";
            case "telegram.subscription.details_title" -> fa ? "🛍 جزئیات سرویس" : "🛍 Service details";
            case "telegram.subscription.plan" -> fa ? "پلن" : "Plan";
            case "telegram.subscription.status" -> fa ? "وضعیت" : "Status";
            case "telegram.subscription.expires" -> fa ? "پایان اعتبار" : "Expires";
            case "telegram.subscription.configs" -> fa ? "تعداد کانفیگ" : "Configs";
            case "telegram.subscription.config" -> fa ? "کانفیگ" : "Config";
            case "telegram.subscription.config_qr" -> fa ? "QR کانفیگ" : "Config QR";
            case "telegram.subscription.new_link" -> fa ? "دریافت لینک جدید" : "Get a new link";
            case "telegram.subscription.status.ACTIVE" -> fa ? "فعال" : "Active";
            case "telegram.subscription.status.SUSPENDED" -> fa ? "غیرفعال" : "Suspended";
            case "telegram.subscription.status.REVOKED" -> fa ? "لغوشده" : "Revoked";
            case "telegram.subscription.status.EXPIRED" -> fa ? "منقضی‌شده" : "Expired";
            case "telegram.action.confirm" -> fa ? "تأیید" : "Confirm";
            default -> key;
        };
    }
}
