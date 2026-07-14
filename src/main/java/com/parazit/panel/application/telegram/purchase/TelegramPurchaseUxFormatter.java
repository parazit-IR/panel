package com.parazit.panel.application.telegram.purchase;

import com.parazit.panel.application.payment.manual.result.ManualCardPaymentInstructionResult;
import com.parazit.panel.application.payment.zarinpal.result.InitializeZarinpalPaymentResult;
import com.parazit.panel.application.plan.catalog.result.AvailablePlanResult;
import com.parazit.panel.application.purchase.result.PurchasePaymentMethodsResult;
import com.parazit.panel.application.purchase.result.PurchasePreInvoiceResult;
import com.parazit.panel.application.telegram.TelegramHtmlEscaper;
import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.application.telegram.TelegramPersianTextFormatter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TelegramPurchaseUxFormatter {

    private static final long GIB = 1024L * 1024L * 1024L;
    private static final DateTimeFormatter FA_DATE_TIME = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
            .withZone(ZoneId.of("Asia/Tehran"));

    private final TelegramMessageCatalog catalog;
    private final TelegramHtmlEscaper html;
    private final TelegramPersianTextFormatter numbers;

    public TelegramPurchaseUxFormatter(
            TelegramMessageCatalog catalog,
            TelegramHtmlEscaper html,
            TelegramPersianTextFormatter numbers
    ) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.html = Objects.requireNonNull(html, "html must not be null");
        this.numbers = Objects.requireNonNull(numbers, "numbers must not be null");
    }

    public String planCatalog(String language, List<AvailablePlanResult> plans) {
        if (plans.isEmpty()) {
            return catalog.text(language, "telegram.plans.empty");
        }
        return catalog.text(language, "telegram.plans.buy_title")
                + "\n\n"
                + catalog.text(language, "telegram.purchase.choose_plan");
    }

    public String planDetails(String language, AvailablePlanResult plan) {
        StringBuilder builder = new StringBuilder();
        builder.append("🔐 ").append(html.escape(plan.name())).append("\n\n");
        builder.append(catalog.text(language, "telegram.purchase.duration")).append(":\n")
                .append(duration(language, plan.durationDays())).append("\n\n");
        builder.append(catalog.text(language, "telegram.purchase.traffic")).append(":\n")
                .append(traffic(language, plan.trafficLimitBytes())).append("\n\n");
        if (plan.maxDevices() != null) {
            builder.append(catalog.text(language, "telegram.purchase.devices")).append(":\n")
                    .append(numbers.formatNumber(plan.maxDevices(), language)).append("\n\n");
        }
        builder.append(catalog.text(language, "telegram.purchase.original_amount")).append(":\n")
                .append(numbers.formatAmount(plan.priceAmount(), plan.currency().name(), language));
        if (plan.description() != null && !plan.description().isBlank()) {
            builder.append("\n\n").append(catalog.text(language, "telegram.purchase.description")).append(":\n")
                    .append(html.escape(plan.description()));
        }
        return builder.toString();
    }

    public String preInvoice(String language, PurchasePreInvoiceResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append(catalog.text(language, "telegram.purchase.preinvoice_title")).append("\n\n");
        append(builder, catalog.text(language, "telegram.purchase.customer"), html.escape(result.customerDisplayName()));
        append(builder, catalog.text(language, "telegram.purchase.service_name"), html.escape(result.serviceName()));
        append(builder, catalog.text(language, "telegram.purchase.plan"), html.escape(result.planName()));
        append(builder, catalog.text(language, "telegram.purchase.duration"), duration(language, result.durationDays()));
        append(builder, catalog.text(language, "telegram.purchase.traffic"), traffic(language, result.trafficBytes().isPresent() ? result.trafficBytes().getAsLong() : null));
        if (result.maxDevices().isPresent()) {
            append(builder, catalog.text(language, "telegram.purchase.devices"), numbers.formatNumber(result.maxDevices().getAsInt(), language));
        }
        append(builder, catalog.text(language, "telegram.purchase.original_amount"), numbers.formatAmount(result.originalAmount(), result.currency().name(), language));
        if (result.discountAmount() > 0) {
            append(builder, catalog.text(language, "telegram.purchase.discount"), numbers.formatAmount(result.discountAmount(), result.currency().name(), language));
        }
        append(builder, catalog.text(language, "telegram.purchase.final_amount"), numbers.formatAmount(result.finalAmount(), result.currency().name(), language));
        if (result.planDescription() != null && !result.planDescription().isBlank()) {
            append(builder, catalog.text(language, "telegram.purchase.description"), html.escape(result.planDescription()));
        }
        append(builder, catalog.text(language, "telegram.purchase.selection_expiry"), dateTime(language, result.selectionExpiresAt()));
        builder.append("\n").append(catalog.text(language, "telegram.purchase.price_snapshot_note"));
        return builder.toString();
    }

    public String paymentMethods(String language, PurchasePaymentMethodsResult result) {
        if (result.methods().isEmpty()) {
            return catalog.text(language, "telegram.purchase.no_payment_method");
        }
        return catalog.text(language, "telegram.purchase.payment_methods_title")
                + "\n\n"
                + catalog.text(language, "telegram.purchase.final_amount")
                + ":\n"
                + numbers.formatAmount(result.finalAmount(), result.currency(), language);
    }

    public String manualPayment(String language, ManualCardPaymentInstructionResult instruction) {
        return catalog.text(language, "telegram.purchase.manual_payment_title")
                + "\n\n"
                + catalog.text(language, "telegram.purchase.base_amount")
                + ":\n<code>"
                + html.escape(numbers.formatAmount(instruction.baseAmount(), instruction.currency(), language))
                + "</code>\n\n"
                + catalog.text(language, "telegram.purchase.exact_payable_amount")
                + ":\n<code>"
                + html.escape(numbers.formatAmount(instruction.payableAmount(), instruction.currency(), language))
                + "</code>\n\n"
                + catalog.text(language, "telegram.purchase.card_number")
                + ":\n<code>"
                + html.escape(instruction.cardNumberFormatted())
                + "</code>\n\n"
                + catalog.text(language, "telegram.purchase.card_holder")
                + ":\n"
                + html.escape(instruction.cardHolderName())
                + "\n\n"
                + catalog.text(language, "telegram.purchase.payment_expiry")
                + ":\n"
                + dateTime(language, instruction.expiresAt())
                + "\n\n"
                + catalog.text(language, "telegram.purchase.manual_warning");
    }

    public String onlinePayment(String language, InitializeZarinpalPaymentResult result, long amount, String currency) {
        return catalog.text(language, "telegram.purchase.online_payment_title")
                + "\n\n"
                + catalog.text(language, "telegram.purchase.final_amount")
                + ":\n"
                + numbers.formatAmount(amount, currency, language)
                + "\n\n"
                + catalog.text(language, "telegram.purchase.online_description");
    }

    public String expired(String language) {
        return catalog.text(language, "telegram.purchase.preinvoice_expired");
    }

    public String disabled(String language) {
        return catalog.text(language, "telegram.purchase.disabled");
    }

    public String dateTime(String language, Instant instant) {
        String formatted = FA_DATE_TIME.format(instant);
        return fa(language) ? toPersianDigits(formatted) : formatted;
    }

    public String traffic(String language, Long bytes) {
        if (bytes == null) {
            return catalog.text(language, "telegram.tariffs.unlimited");
        }
        long rounded = Math.max(1L, Math.round(bytes / (double) GIB));
        return numbers.formatNumber(rounded, language) + " " + catalog.text(language, "telegram.tariffs.gigabyte");
    }

    public String duration(String language, int days) {
        return numbers.formatNumber(days, language) + " " + catalog.text(language, "telegram.plans.days");
    }

    private static void append(StringBuilder builder, String label, String value) {
        builder.append(label).append(":\n").append(value).append("\n\n");
    }

    private static boolean fa(String language) {
        return language != null && language.toUpperCase(Locale.ROOT).startsWith("FA");
    }

    private static String toPersianDigits(String value) {
        char[] persian = {'۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹'};
        StringBuilder builder = new StringBuilder(value.length());
        for (char ch : value.toCharArray()) {
            if (ch >= '0' && ch <= '9') {
                builder.append(persian[ch - '0']);
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }
}
