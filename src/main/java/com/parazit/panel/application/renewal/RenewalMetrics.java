package com.parazit.panel.application.renewal;

import com.parazit.panel.application.customer.result.CustomerServiceStatus;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class RenewalMetrics {

    private final MeterRegistry meterRegistry;

    public RenewalMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
    }

    public void serviceList(String result) {
        meterRegistry.counter("renewal_service_list_total", "result", safe(result)).increment();
    }

    public void planList(String result) {
        meterRegistry.counter("renewal_plan_list_total", "result", safe(result)).increment();
    }

    public void selection(String result) {
        meterRegistry.counter("renewal_selection_total", "result", safe(result)).increment();
    }

    public void preInvoice(String result) {
        meterRegistry.counter("renewal_preinvoice_total", "result", safe(result)).increment();
    }

    public void orderCreation(String result) {
        meterRegistry.counter("renewal_order_creation_total", "result", safe(result)).increment();
    }

    public void orderReuse(String result) {
        meterRegistry.counter("renewal_order_reuse_total", "result", safe(result)).increment();
    }

    public void rejected(RenewalIneligibilityReason reason, CustomerServiceStatus subscriptionStatus) {
        meterRegistry.counter(
                "renewal_rejected_total",
                "reason", safe(reason == null ? "unknown" : reason.name()),
                "subscription_status", safe(subscriptionStatus == null ? "unknown" : subscriptionStatus.name())
        ).increment();
    }

    public void renewalPaymentApproval(String result, com.parazit.panel.application.payment.PaymentApprovalSource source) {
        meterRegistry.counter(
                "renewal_payment_approval_total",
                "result", safe(result),
                "source", safe(source == null ? "unknown" : source.name())
        ).increment();
    }

    public void renewalOutboxCreation() {
        meterRegistry.counter("renewal_outbox_creation_total", "result", "created").increment();
    }

    public void renewalOutboxReuse() {
        meterRegistry.counter("renewal_outbox_reuse_total", "result", "reused").increment();
    }

    public void renewalApprovalBlocked(String reason, com.parazit.panel.application.payment.PaymentApprovalSource source) {
        meterRegistry.counter(
                "renewal_approval_blocked_total",
                "reason", safe(reason),
                "source", safe(source == null ? "unknown" : source.name())
        ).increment();
    }

    public void renewalQueuedNotification(String result) {
        meterRegistry.counter("renewal_queued_notification_total", "result", safe(result)).increment();
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
    }
}
