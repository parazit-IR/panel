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
            case "telegram.purchase.disabled" -> fa
                    ? "🔐 فروش اشتراک جدید در حال حاضر موقتاً غیرفعال است.\n\nسرویس‌های قبلی شما همچنان فعال هستند و از بخش «سرویس‌های من» قابل مدیریت‌اند."
                    : "🔐 New subscription sales are temporarily disabled.\n\nYour existing services remain active and can be managed from My services.";
            case "telegram.purchase.resume_at" -> fa ? "زمان تقریبی فعال‌شدن مجدد" : "Estimated resume time";
            case "telegram.purchase.plan_unavailable" -> fa
                    ? "این پلن در حال حاضر برای خرید در دسترس نیست.\n\nلطفاً دوباره پلن موردنظر را انتخاب کنید."
                    : "This plan is not currently available for purchase.\n\nPlease choose a plan again.";
            case "telegram.purchase.choose_plan" -> fa ? "تعرفه موردنظر را انتخاب کنید." : "Choose a tariff.";
            case "telegram.purchase.select_plan" -> fa ? "انتخاب این پلن" : "Select this plan";
            case "telegram.purchase.preinvoice_title" -> fa ? "🧾 پیش‌فاکتور شما" : "🧾 Your pre-invoice";
            case "telegram.purchase.customer" -> fa ? "👤 نام کاربر" : "👤 Customer";
            case "telegram.purchase.service_name" -> fa ? "🔐 نام سرویس" : "🔐 Service name";
            case "telegram.purchase.plan" -> fa ? "📦 پلن" : "📦 Plan";
            case "telegram.purchase.duration" -> fa ? "📅 مدت اعتبار" : "📅 Duration";
            case "telegram.purchase.traffic" -> fa ? "📊 حجم" : "📊 Traffic";
            case "telegram.purchase.devices" -> fa ? "📱 تعداد دستگاه" : "📱 Devices";
            case "telegram.purchase.description" -> fa ? "📝 توضیحات" : "📝 Description";
            case "telegram.purchase.original_amount" -> fa ? "💵 مبلغ" : "💵 Amount";
            case "telegram.purchase.discount" -> fa ? "🎁 ثبت کد تخفیف" : "🎁 Apply discount code";
            case "telegram.purchase.final_amount" -> fa ? "مبلغ قابل پرداخت" : "Payable amount";
            case "telegram.purchase.selection_expiry" -> fa ? "⏳ اعتبار پیش‌فاکتور" : "⏳ Pre-invoice expires";
            case "telegram.purchase.pay_and_receive" -> fa ? "💰 پرداخت و دریافت سرویس" : "💰 Pay and receive service";
            case "telegram.purchase.choose_other_plan" -> fa ? "⬅️ انتخاب پلن دیگر" : "⬅️ Choose another plan";
            case "telegram.purchase.discount_unavailable" -> fa
                    ? "🎁 امکان ثبت کد تخفیف هنوز فعال نشده است."
                    : "🎁 Discount code entry is not enabled yet.";
            case "telegram.purchase.payment_methods_title" -> fa ? "💳 روش پرداخت را انتخاب کنید." : "💳 Choose a payment method.";
            case "telegram.purchase.no_payment_method" -> fa
                    ? "در حال حاضر هیچ روش پرداختی فعال نیست.\n\nلطفاً بعداً دوباره تلاش کنید یا با پشتیبانی تماس بگیرید."
                    : "No payment method is currently active.\n\nPlease try again later or contact support.";
            case "telegram.purchase.manual_payment" -> fa ? "💳 کارت‌به‌کارت" : "💳 Card-to-card";
            case "telegram.purchase.online_payment" -> fa ? "🌐 پرداخت آنلاین" : "🌐 Online payment";
            case "telegram.purchase.wallet_payment" -> fa ? "💰 پرداخت با کیف پول" : "💰 Pay with wallet";
            case "telegram.purchase.manual_payment_disabled" -> fa ? "پرداخت کارت‌به‌کارت در حال حاضر فعال نیست." : "Manual card payment is currently disabled.";
            case "telegram.purchase.online_payment_disabled" -> fa ? "پرداخت آنلاین در حال حاضر فعال نیست." : "Online payment is currently disabled.";
            case "telegram.purchase.copy_amount" -> fa ? "📋 کپی مبلغ" : "📋 Copy amount";
            case "telegram.purchase.copy_card" -> fa ? "💳 کپی شماره کارت" : "💳 Copy card";
            case "telegram.purchase.upload_receipt" -> fa ? "✅ پرداخت کردم | ارسال رسید" : "✅ I paid | Upload receipt";
            case "telegram.purchase.check_status" -> fa ? "🔄 بررسی وضعیت" : "🔄 Check status";
            case "telegram.purchase.cancel_payment" -> fa ? "❌ لغو پرداخت" : "❌ Cancel payment";
            case "telegram.purchase.preinvoice_expired" -> fa
                    ? "این پیش‌فاکتور منقضی شده است.\n\nلطفاً دوباره پلن موردنظر را انتخاب کنید."
                    : "This pre-invoice has expired.\n\nPlease choose the plan again.";
            case "telegram.purchase.price_snapshot_note" -> fa
                    ? "مبلغ این پیش‌فاکتور بر اساس اطلاعات همین انتخاب محاسبه شده است."
                    : "This pre-invoice amount is based on this selection snapshot.";
            case "telegram.purchase.show_plans" -> fa ? "مشاهده پلن‌ها" : "Show plans";
            case "telegram.purchase.back_to_preinvoice" -> fa ? "⬅️ بازگشت به پیش‌فاکتور" : "⬅️ Back to pre-invoice";
            case "telegram.purchase.manual_payment_title" -> fa ? "💳 پرداخت کارت‌به‌کارت" : "💳 Card-to-card payment";
            case "telegram.purchase.base_amount" -> fa ? "💵 مبلغ سفارش" : "💵 Order amount";
            case "telegram.purchase.exact_payable_amount" -> fa ? "✅ مبلغ دقیق قابل واریز" : "✅ Exact payable amount";
            case "telegram.purchase.card_number" -> fa ? "💳 شماره کارت" : "💳 Card number";
            case "telegram.purchase.card_holder" -> fa ? "👤 به نام" : "👤 Card holder";
            case "telegram.purchase.payment_expiry" -> fa ? "⏳ اعتبار پرداخت" : "⏳ Payment expires";
            case "telegram.purchase.manual_warning" -> fa
                    ? "⚠️ لطفاً مبلغ دقیق اعلام‌شده را واریز کنید.\nپس از پرداخت، رسید را از طریق دکمه زیر ارسال کنید."
                    : "⚠️ Please transfer the exact announced amount.\nAfter payment, send the receipt using the button below.";
            case "telegram.purchase.online_payment_title" -> fa ? "🌐 پرداخت آنلاین" : "🌐 Online payment";
            case "telegram.purchase.online_description" -> fa
                    ? "برای ورود به صفحه امن پرداخت، دکمه زیر را بزنید."
                    : "Press the button below to open the secure payment page.";
            case "telegram.purchase.open_payment_page" -> fa ? "🌐 ورود به صفحه پرداخت" : "🌐 Open payment page";
            case "telegram.purchase.payment_action_unavailable" -> fa
                    ? "این عملیات از داخل بات هنوز تکمیل نشده است. وضعیت پرداخت شما تغییری نکرد."
                    : "This in-bot action is not completed yet. Your payment status was not changed.";
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
            case "telegram.account.title" -> fa ? "👤 اطلاعات حساب کاربری شما" : "👤 Your account information";
            case "telegram.account.title_short" -> fa ? "👤 حساب کاربری" : "👤 Account";
            case "telegram.account.telegram_id" -> fa ? "🆔 شناسه تلگرام" : "🆔 Telegram ID";
            case "telegram.account.name" -> fa ? "👤 نام" : "👤 Name";
            case "telegram.account.registered_at" -> fa ? "📅 زمان عضویت" : "📅 Registered at";
            case "telegram.account.total_services" -> fa ? "🛍 تعداد سرویس‌ها" : "🛍 Total services";
            case "telegram.account.active_services" -> fa ? "🟢 سرویس‌های فعال" : "🟢 Active services";
            case "telegram.account.expired_services" -> fa ? "⚫ سرویس‌های منقضی" : "⚫ Expired services";
            case "telegram.account.paid_orders" -> fa ? "💳 پرداخت‌های موفق" : "💳 Successful payments";
            case "telegram.account.pending_payments" -> fa ? "⏳ پرداخت‌های در انتظار" : "⏳ Pending payments";
            case "telegram.account.referral_code" -> fa ? "🎁 کد معرف" : "🎁 Referral code";
            case "telegram.account.phone" -> fa ? "📱 شماره تماس" : "📱 Phone";
            case "telegram.account.wallet_balance" -> fa ? "💰 موجودی کیف پول" : "💰 Wallet balance";
            case "telegram.account.customer_group" -> fa ? "🏷 گروه کاربری" : "🏷 Customer group";
            case "telegram.account.discount_usage" -> fa ? "🎟 تعداد تخفیف‌های استفاده‌شده" : "🎟 Used discounts";
            case "telegram.account.payments" -> fa ? "💳 پرداخت‌های من" : "💳 My payments";
            case "telegram.account.settings" -> fa ? "⚙️ تنظیمات اعلان‌ها" : "⚙️ Notification settings";
            case "telegram.wallet.title" -> fa ? "💰 کیف پول" : "💰 Wallet";
            case "telegram.wallet.balance" -> fa ? "موجودی فعلی شما" : "Current balance";
            case "telegram.wallet.transaction_count" -> fa ? "تعداد تراکنش‌ها" : "Transaction count";
            case "telegram.wallet.last_transaction" -> fa ? "آخرین تغییر" : "Last change";
            case "telegram.wallet.no_transaction" -> fa ? "بدون تراکنش" : "No transactions";
            case "telegram.wallet.history" -> fa ? "📜 تاریخچه تراکنش‌ها" : "📜 Transaction history";
            case "telegram.wallet.history_empty" -> fa
                    ? "📜 هنوز تراکنشی برای کیف پول شما ثبت نشده است."
                    : "📜 Your wallet has no transactions yet.";
            case "telegram.wallet.credit" -> fa ? "➕ افزایش موجودی" : "➕ Credit";
            case "telegram.wallet.debit" -> fa ? "➖ کاهش موجودی" : "➖ Debit";
            case "telegram.wallet.balance_after" -> fa ? "موجودی پس از تراکنش" : "Balance after transaction";
            case "telegram.wallet.top_up" -> fa ? "➕ افزایش موجودی" : "➕ Top up";
            case "telegram.wallet.top_up_unavailable" -> fa
                    ? "➕ امکان افزایش موجودی کیف پول در حال حاضر فعال نشده است."
                    : "➕ Wallet top-up is not enabled yet.";
            case "telegram.wallet.payment_confirm" -> fa ? "✅ تأیید پرداخت" : "✅ Confirm payment";
            case "telegram.wallet.payment_confirmation" -> fa
                    ? "💰 پرداخت با کیف پول\n\nمبلغ سفارش:\n{orderAmount}\n\nموجودی فعلی:\n{walletBalance}\n\nموجودی پس از پرداخت:\n{projectedBalance}"
                    : "💰 Pay with wallet\n\nOrder amount:\n{orderAmount}\n\nCurrent balance:\n{walletBalance}\n\nBalance after payment:\n{projectedBalance}";
            case "telegram.wallet.payment_insufficient" -> fa
                    ? "⚠️ موجودی کیف پول کافی نیست.\n\nمبلغ موردنیاز:\n{orderAmount}\n\nموجودی فعلی:\n{walletBalance}\n\nکسری موجودی:\n{shortfall}"
                    : "⚠️ Wallet balance is not sufficient.\n\nRequired amount:\n{orderAmount}\n\nCurrent balance:\n{walletBalance}\n\nShortfall:\n{shortfall}";
            case "telegram.wallet.payment_success_new" -> fa
                    ? "✅ پرداخت با کیف پول با موفقیت انجام شد.\n\n💵 مبلغ پرداخت:\n{amount}\n\n💰 موجودی جدید:\n{balanceAfter}\n\nسرویس شما در حال ساخت است."
                    : "✅ Wallet payment succeeded.\n\nPaid amount:\n{amount}\n\nNew balance:\n{balanceAfter}\n\nYour service is being created.";
            case "telegram.wallet.payment_success_renewal" -> fa
                    ? "✅ پرداخت تمدید با کیف پول انجام شد.\n\n💵 مبلغ پرداخت:\n{amount}\n\n💰 موجودی جدید:\n{balanceAfter}\n\nتمدید سرویس در صف اجرا قرار گرفت."
                    : "✅ Renewal wallet payment succeeded.\n\nPaid amount:\n{amount}\n\nNew balance:\n{balanceAfter}\n\nThe renewal was queued.";
            case "telegram.wallet.payment_conflict" -> fa
                    ? "برای این سفارش یک پرداخت فعال یا تأییدشده وجود دارد. لطفاً وضعیت پرداخت را بررسی کنید."
                    : "This order already has an active or approved payment. Please check payment status.";
            case "telegram.wallet.payment_unavailable" -> fa
                    ? "پرداخت با کیف پول برای این سفارش در دسترس نیست."
                    : "Wallet payment is not available for this order.";
            case "telegram.wallet.payment_expired" -> fa
                    ? "تأیید پرداخت با کیف پول منقضی شده است. لطفاً دوباره از پیش‌فاکتور اقدام کنید."
                    : "Wallet payment confirmation expired. Please start again from the pre-invoice.";
            case "telegram.wallet.top_up_prompt" -> fa
                    ? "➕ افزایش موجودی کیف پول\n\nمبلغ موردنظر را به تومان وارد کنید.\n\nحداقل:\n{minimumAmount}\n\nحداکثر:\n{maximumAmount}"
                    : "➕ Wallet top-up\n\nEnter the amount.\n\nMinimum:\n{minimumAmount}\n\nMaximum:\n{maximumAmount}";
            case "telegram.wallet.top_up_invalid_amount" -> fa
                    ? "مبلغ واردشده معتبر نیست."
                    : "The amount is not valid.";
            case "telegram.wallet.top_up_invoice" -> fa
                    ? "🧾 پیش‌فاکتور شارژ کیف پول\n\n💰 مبلغ شارژ:\n{amount}\n\n⏳ اعتبار درخواست:\n{expiresAt}\n\nروش پرداخت را انتخاب کنید."
                    : "🧾 Wallet top-up invoice\n\nAmount:\n{amount}\n\nRequest expires:\n{expiresAt}\n\nChoose a payment method.";
            case "telegram.wallet.top_up_manual" -> fa
                    ? "💳 شارژ کیف پول با کارت‌به‌کارت\n\nمبلغ شارژ:\n{requestedAmount}\n\nمبلغ دقیق قابل واریز:\n{payableAmount}\n\nشماره کارت:\n{card}\n\nاعتبار پرداخت:\n{expiresAt}\n\nپس از پرداخت، رسید را ارسال کنید."
                    : "💳 Wallet top-up by card transfer\n\nTop-up amount:\n{requestedAmount}\n\nExact payable amount:\n{payableAmount}\n\nCard:\n{card}\n\nPayment expires:\n{expiresAt}\n\nSend the receipt after payment.";
            case "telegram.wallet.top_up_online" -> fa
                    ? "🌐 پرداخت آنلاین شارژ کیف پول\n\nمبلغ شارژ:\n{amount}\n\nاعتبار درخواست:\n{expiresAt}\n\nبرای ادامه، وارد صفحه پرداخت شوید."
                    : "🌐 Online wallet top-up\n\nAmount:\n{amount}\n\nRequest expires:\n{expiresAt}\n\nOpen the payment page to continue.";
            case "telegram.wallet.top_up_status" -> fa
                    ? "🔄 وضعیت شارژ کیف پول\n\nمبلغ:\n{amount}\n\nوضعیت شارژ:\n{topUpStatus}\n\nوضعیت پرداخت:\n{paymentStatus}\n\nموجودی پس از شارژ:\n{balanceAfter}"
                    : "🔄 Wallet top-up status\n\nAmount:\n{amount}\n\nTop-up status:\n{topUpStatus}\n\nPayment status:\n{paymentStatus}\n\nBalance after credit:\n{balanceAfter}";
            case "telegram.wallet.change_amount" -> fa ? "⬅️ تغییر مبلغ" : "⬅️ Change amount";
            case "telegram.wallet.locked" -> fa ? "کیف پول شما موقتاً قفل شده است." : "Your wallet is temporarily locked.";
            case "telegram.wallet.closed" -> fa ? "کیف پول شما بسته شده است." : "Your wallet is closed.";
            case "telegram.wallet.previous" -> fa ? "قبلی" : "Previous";
            case "telegram.wallet.next" -> fa ? "بعدی" : "Next";
            case "telegram.services.title" -> fa ? "🛍 اشتراک‌های خریداری‌شده توسط شما" : "🛍 Your purchased services";
            case "telegram.services.description" -> fa
                    ? "برای مشاهده اطلاعات و مدیریت سرویس، روی نام آن کلیک کنید.\n\nبرای پیدا کردن سریع‌تر سرویس می‌توانید از جست‌وجوی سریع استفاده کنید."
                    : "Tap a service name to view and manage it.\n\nUse quick search to find a service faster.";
            case "telegram.services.empty" -> fa ? "اشتراکی برای نمایش وجود ندارد." : "No services are available.";
            case "telegram.services.search" -> fa ? "🔎 جست‌وجوی سریع" : "🔎 Quick search";
            case "telegram.services.search_prompt" -> fa ? "نام یا شناسه سرویس را ارسال کنید." : "Send the service name or identifier.";
            case "telegram.services.search_too_short" -> fa ? "عبارت جست‌وجو باید حداقل ۳ کاراکتر باشد." : "Search query must be at least 3 characters.";
            case "telegram.services.search_no_result" -> fa ? "سرویسی با این مشخصات در حساب شما پیدا نشد." : "No service with these details was found in your account.";
            case "telegram.services.search_expired" -> fa
                    ? "زمان جست‌وجو به پایان رسید. دوباره از بخش «سرویس‌های من» جست‌وجو را شروع کنید."
                    : "Search expired. Start quick search again from My services.";
            case "telegram.services.search_results" -> fa ? "نتیجه جست‌وجو:" : "Search results:";
            case "telegram.services.previous" -> fa ? "قبلی" : "Previous";
            case "telegram.services.next" -> fa ? "بعدی" : "Next";
            case "telegram.services.details" -> fa ? "🛍 اطلاعات سرویس" : "🛍 Service details";
            case "telegram.service.name" -> fa ? "👤 نام سرویس" : "👤 Service name";
            case "telegram.service.plan" -> fa ? "📦 پلن" : "📦 Plan";
            case "telegram.service.status" -> fa ? "📌 وضعیت" : "📌 Status";
            case "telegram.service.total_traffic" -> fa ? "📊 حجم کل" : "📊 Total traffic";
            case "telegram.service.used_traffic" -> fa ? "📉 حجم مصرف‌شده" : "📉 Used traffic";
            case "telegram.service.remaining_traffic" -> fa ? "📈 حجم باقی‌مانده" : "📈 Remaining traffic";
            case "telegram.service.expires_at" -> fa ? "📅 تاریخ انقضا" : "📅 Expires at";
            case "telegram.service.remaining_time" -> fa ? "⏳ زمان باقی‌مانده" : "⏳ Remaining time";
            case "telegram.service.usage_unavailable" -> fa ? "📊 اطلاعات مصرف" : "📊 Usage";
            case "telegram.service.usage_unavailable_text" -> fa ? "در حال حاضر در دسترس نیست." : "Currently unavailable.";
            case "telegram.service.usage_stale" -> fa ? "⚠️ اطلاعات مصرف ممکن است به‌روز نباشد." : "⚠️ Usage data may be stale.";
            case "telegram.service.refresh" -> fa ? "🔄 به‌روزرسانی وضعیت" : "🔄 Refresh status";
            case "telegram.service.subscription_link" -> fa ? "🔗 دریافت لینک اشتراک" : "🔗 Get subscription link";
            case "telegram.service.qr" -> fa ? "📱 دریافت QR Code" : "📱 Get QR Code";
            case "telegram.service.vless" -> fa ? "📋 دریافت کانفیگ" : "📋 Get config";
            case "telegram.service.renewal" -> fa ? "♻️ تمدید سرویس" : "♻️ Renew service";
            case "telegram.service.provisioning" -> fa
                    ? "سرویس در حال ساخت است. چند لحظه دیگر وضعیت را به‌روزرسانی کنید."
                    : "The service is being prepared. Refresh the status in a moment.";
            case "telegram.service.failed" -> fa
                    ? "ساخت سرویس با خطا روبه‌رو شده است. لطفاً از بخش پشتیبانی پیگیری کنید."
                    : "Service provisioning failed. Please contact support.";
            case "telegram.service.status.PROVISIONING" -> fa ? "🟡 در حال ساخت" : "🟡 Provisioning";
            case "telegram.service.status.ACTIVE" -> fa ? "🟢 فعال" : "🟢 Active";
            case "telegram.service.status.SUSPENDED" -> fa ? "🟠 تعلیق‌شده" : "🟠 Suspended";
            case "telegram.service.status.EXPIRED" -> fa ? "⚫ منقضی‌شده" : "⚫ Expired";
            case "telegram.service.status.REVOKED" -> fa ? "🔴 لغوشده" : "🔴 Revoked";
            case "telegram.service.status.FAILED" -> fa ? "❌ خطای ساخت" : "❌ Provisioning failed";
            case "telegram.service.status.UNKNOWN" -> fa ? "❔ نامشخص" : "❔ Unknown";
            case "telegram.renewal.title" -> fa ? "♻️ تمدید سرویس" : "♻️ Renew service";
            case "telegram.renewal.select_service" -> fa
                    ? "سرویس موردنظر خود را برای تمدید انتخاب کنید."
                    : "Choose the service you want to renew.";
            case "telegram.renewal.no_service" -> fa
                    ? "♻️ در حال حاضر سرویسی برای تمدید در حساب شما وجود ندارد.\n\nمی‌توانید از بخش «خرید اشتراک» یک سرویس جدید تهیه کنید."
                    : "There is currently no renewable service in your account.\n\nYou can buy a new service from Buy subscription.";
            case "telegram.renewal.service_name" -> fa ? "نام سرویس" : "Service name";
            case "telegram.renewal.current_status" -> fa ? "وضعیت فعلی" : "Current status";
            case "telegram.renewal.current_plan" -> fa ? "پلن فعلی" : "Current plan";
            case "telegram.renewal.current_expiry" -> fa ? "تاریخ انقضای فعلی" : "Current expiry";
            case "telegram.renewal.remaining_time" -> fa ? "زمان باقی‌مانده" : "Remaining time";
            case "telegram.renewal.remaining_traffic" -> fa ? "حجم باقی‌مانده" : "Remaining traffic";
            case "telegram.renewal.select_plan" -> fa ? "پلن تمدید را انتخاب کنید." : "Choose a renewal plan.";
            case "telegram.renewal.no_plan" -> fa
                    ? "در حال حاضر پلن سازگار برای تمدید این سرویس وجود ندارد."
                    : "No compatible renewal plan is currently available for this service.";
            case "telegram.renewal.plan_duration" -> fa ? "مدت تمدید" : "Renewal duration";
            case "telegram.renewal.plan_traffic" -> fa ? "پلن تمدید" : "Renewal plan";
            case "telegram.renewal.plan_price" -> fa ? "قیمت پلن" : "Plan price";
            case "telegram.renewal.preinvoice_title" -> fa ? "🧾 پیش‌فاکتور تمدید سرویس" : "🧾 Renewal pre-invoice";
            case "telegram.renewal.proposed_expiry" -> fa ? "تاریخ انقضای پیشنهادی" : "Proposed expiry";
            case "telegram.renewal.traffic_policy" -> fa ? "حجم پس از تمدید" : "Traffic after renewal";
            case "telegram.renewal.amount" -> fa ? "مبلغ قابل پرداخت" : "Payable amount";
            case "telegram.renewal.pay_and_renew" -> fa ? "💰 پرداخت و تمدید سرویس" : "💰 Pay and renew service";
            case "telegram.renewal.choose_other_plan" -> fa ? "⬅️ انتخاب پلن دیگر" : "⬅️ Choose another plan";
            case "telegram.renewal.selection_expired" -> fa
                    ? "این پیش‌فاکتور تمدید منقضی شده است.\n\nلطفاً دوباره سرویس و پلن تمدید را انتخاب کنید."
                    : "This renewal pre-invoice has expired.\n\nPlease choose the service and renewal plan again.";
            case "telegram.renewal.not_available" -> fa ? "♻️ تمدید سرویس در حال حاضر فعال نیست." : "♻️ Renewal is currently unavailable.";
            case "telegram.renewal.existing_order" -> fa
                    ? "برای این سرویس یک سفارش تمدید فعال وجود دارد. لطفاً پرداخت همان سفارش را ادامه دهید."
                    : "This service already has an active renewal order. Please continue that payment.";
            case "telegram.renewal.awaiting_payment" -> fa ? "💳 روش پرداخت تمدید را انتخاب کنید." : "💳 Choose a renewal payment method.";
            case "telegram.renewal.cannot_renew" -> fa
                    ? "این سرویس در وضعیت فعلی قابل تمدید نیست."
                    : "This service cannot be renewed in its current state.";
            case "telegram.renewal.buy_new_service" -> fa ? "خرید اشتراک" : "Buy subscription";
            default -> key;
        };
    }
}
