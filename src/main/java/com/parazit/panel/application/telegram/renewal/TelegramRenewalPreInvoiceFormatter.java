package com.parazit.panel.application.telegram.renewal;

import com.parazit.panel.application.renewal.result.CreateRenewalOrderResult;
import com.parazit.panel.application.renewal.result.RenewableServicePageResult;
import com.parazit.panel.application.renewal.result.RenewableServiceSummaryResult;
import com.parazit.panel.application.renewal.result.RenewalPlanPageResult;
import com.parazit.panel.application.renewal.result.RenewalPlanSummaryResult;
import com.parazit.panel.application.renewal.result.RenewalPreInvoiceResult;
import com.parazit.panel.application.renewal.result.RenewalTargetDetailsResult;
import com.parazit.panel.application.telegram.TelegramHtmlEscaper;
import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.application.telegram.TelegramPersianTextFormatter;
import com.parazit.panel.application.telegram.service.TelegramCustomerTextFormatter;
import com.parazit.panel.domain.order.RenewalExpiryPolicy;
import com.parazit.panel.domain.order.RenewalTrafficPolicy;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalLong;
import org.springframework.stereotype.Component;

@Component
public class TelegramRenewalPreInvoiceFormatter {

    private static final DateTimeFormatter FA_DATE_TIME = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
            .withZone(ZoneId.of("Asia/Tehran"));

    private final TelegramMessageCatalog catalog;
    private final TelegramHtmlEscaper html;
    private final TelegramPersianTextFormatter money;
    private final TelegramCustomerTextFormatter text;
    private final TelegramRenewalStatusMapper statusMapper;

    public TelegramRenewalPreInvoiceFormatter(
            TelegramMessageCatalog catalog,
            TelegramHtmlEscaper html,
            TelegramPersianTextFormatter money,
            TelegramCustomerTextFormatter text,
            TelegramRenewalStatusMapper statusMapper
    ) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.html = Objects.requireNonNull(html, "html must not be null");
        this.money = Objects.requireNonNull(money, "money must not be null");
        this.text = Objects.requireNonNull(text, "text must not be null");
        this.statusMapper = Objects.requireNonNull(statusMapper, "statusMapper must not be null");
    }

    public String serviceList(String language, RenewableServicePageResult page) {
        if (page.items().isEmpty()) {
            return catalog.text(language, "telegram.renewal.no_service");
        }
        return catalog.text(language, "telegram.renewal.title")
                + "\n\n"
                + catalog.text(language, "telegram.renewal.select_service");
    }

    public String serviceButton(String language, RenewableServiceSummaryResult service) {
        String prefix = service.renewable() ? "🟢 " : "⚫ ";
        String suffix = service.remainingDuration()
                .map(duration -> text.duration(duration, language) + (fa(language) ? " باقی‌مانده" : " remaining"))
                .orElse(fa(language) ? "منقضی‌شده" : "expired");
        return prefix + service.serviceUsername() + " — " + suffix;
    }

    public String targetDetails(String language, RenewalTargetDetailsResult target) {
        StringBuilder builder = new StringBuilder();
        builder.append(catalog.text(language, "telegram.renewal.service_name")).append(":\n")
                .append(html.escape(target.serviceUsername())).append("\n\n");
        append(builder, catalog.text(language, "telegram.renewal.current_status"), statusMapper.label(target.status(), language));
        append(builder, catalog.text(language, "telegram.renewal.current_plan"), html.escape(target.currentPlanName()));
        append(builder, catalog.text(language, "telegram.renewal.current_expiry"), target.currentExpiryAt().map(expiry -> dateTime(language, expiry)).orElse("-"));
        target.remainingDuration().ifPresent(duration -> append(builder, catalog.text(language, "telegram.renewal.remaining_time"), text.duration(duration, language)));
        target.remainingTrafficBytes().ifPresent(bytes -> append(builder, catalog.text(language, "telegram.renewal.remaining_traffic"), text.traffic(bytes, language)));
        if (!target.renewable()) {
            builder.append("\n").append(catalog.text(language, "telegram.renewal.cannot_renew"));
        }
        return builder.toString();
    }

    public String planList(String language, RenewalPlanPageResult page) {
        if (page.items().isEmpty()) {
            return catalog.text(language, "telegram.renewal.no_plan");
        }
        return catalog.text(language, "telegram.renewal.select_plan");
    }

    public String planButton(String language, RenewalPlanSummaryResult plan) {
        String duration = text.duration(plan.duration(), language);
        String traffic = traffic(language, plan.trafficBytes());
        String amount = money.formatAmount(plan.price().amount(), plan.price().currency().name(), language);
        return duration + " — " + traffic + " — " + amount;
    }

    public String preInvoice(String language, RenewalPreInvoiceResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append(catalog.text(language, "telegram.renewal.preinvoice_title")).append("\n\n");
        append(builder, "👤 " + catalog.text(language, "telegram.renewal.service_name"), html.escape(result.serviceUsername()));
        append(builder, "📌 " + catalog.text(language, "telegram.renewal.current_status"), statusMapper.label(result.currentStatus(), language));
        append(builder, "📅 " + catalog.text(language, "telegram.renewal.current_expiry"), result.currentExpiryAt().map(expiry -> dateTime(language, expiry)).orElse("-"));
        append(builder, "📦 " + catalog.text(language, "telegram.renewal.plan_traffic"), html.escape(result.renewalPlanName()));
        append(builder, "⏳ " + catalog.text(language, "telegram.renewal.plan_duration"), text.duration(result.renewalDuration(), language));
        append(builder, "📊 " + catalog.text(language, "telegram.renewal.traffic_policy"), trafficPolicy(result.trafficPolicy(), language, result.renewalTrafficBytes()));
        append(builder, "📅 " + catalog.text(language, "telegram.renewal.proposed_expiry"), dateTime(language, result.proposedExpiryAt()));
        long discount = result.originalAmount().amount() - result.finalAmount().amount();
        if (discount > 0) {
            append(builder, "🎁 " + catalog.text(language, "telegram.purchase.discount"), money.formatAmount(discount, result.finalAmount().currency().name(), language));
        }
        append(builder, "💵 " + catalog.text(language, "telegram.renewal.amount"), money.formatAmount(result.finalAmount().amount(), result.finalAmount().currency().name(), language));
        builder.append(fa(language)
                ? "⚠️ تمدید پس از تأیید پرداخت روی همین سرویس اعمال خواهد شد."
                : "Payment approval will apply renewal to this same service in the next task.");
        return builder.toString();
    }

    public String paymentMethods(String language, CreateRenewalOrderResult result) {
        if (result.methods().isEmpty()) {
            return catalog.text(language, "telegram.purchase.no_payment_method");
        }
        return catalog.text(language, "telegram.renewal.awaiting_payment")
                + "\n\n"
                + catalog.text(language, "telegram.purchase.final_amount")
                + ":\n"
                + money.formatAmount(result.renewalAmount(), result.currency(), language);
    }

    public String dateTime(String language, Instant instant) {
        String formatted = FA_DATE_TIME.format(instant);
        return fa(language) ? digits(formatted) : formatted;
    }

    private String traffic(String language, OptionalLong bytes) {
        return bytes.isPresent() ? text.traffic(bytes.getAsLong(), language) : catalog.text(language, "telegram.tariffs.unlimited");
    }

    private String trafficPolicy(RenewalTrafficPolicy policy, String language, OptionalLong renewalTrafficBytes) {
        return switch (policy) {
            case RESET_TO_PLAN_LIMIT -> fa(language)
                    ? "تنظیم حجم دوره جدید روی " + traffic(language, renewalTrafficBytes)
                    : "Reset to " + traffic(language, renewalTrafficBytes);
            case ADD_TO_REMAINING -> fa(language) ? "افزودن به حجم باقی‌مانده" : "Add to remaining traffic";
            case ADD_TO_TOTAL_LIMIT -> fa(language) ? "افزودن به سقف کل" : "Add to total limit";
            case UNCHANGED -> fa(language) ? "بدون تغییر حجم" : "Traffic unchanged";
        };
    }

    public String expiryPolicy(RenewalExpiryPolicy policy, String language) {
        return switch (policy) {
            case EXTEND_FROM_CURRENT_EXPIRY -> fa(language) ? "تمدید از تاریخ انقضای فعلی" : "Extend from current expiry";
            case EXTEND_FROM_NOW -> fa(language) ? "تمدید از اکنون" : "Extend from now";
            case EXTEND_FROM_LATER_OF_NOW_OR_EXPIRY -> fa(language) ? "تمدید از تاریخ دیرتر بین اکنون و انقضا" : "Extend from later of now or expiry";
        };
    }

    private static void append(StringBuilder builder, String label, String value) {
        builder.append(label).append(":\n").append(value).append("\n\n");
    }

    private static boolean fa(String language) {
        return language != null && language.toUpperCase(Locale.ROOT).startsWith("FA");
    }

    private static String digits(String value) {
        return value
                .replace('0', '۰')
                .replace('1', '۱')
                .replace('2', '۲')
                .replace('3', '۳')
                .replace('4', '۴')
                .replace('5', '۵')
                .replace('6', '۶')
                .replace('7', '۷')
                .replace('8', '۸')
                .replace('9', '۹');
    }
}
