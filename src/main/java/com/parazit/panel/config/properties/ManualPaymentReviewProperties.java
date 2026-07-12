package com.parazit.panel.config.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payment.manual-review")
public record ManualPaymentReviewProperties(
        Duration claimTtl,
        int queuePageSize,
        int maxQueuePageSize,
        boolean requireTrackingNumber,
        boolean requireSenderCardLastFour,
        boolean requireOperatorNoteOnApproval
) {

    public ManualPaymentReviewProperties {
        claimTtl = claimTtl == null ? Duration.ofMinutes(15) : claimTtl;
        if (claimTtl.isZero() || claimTtl.isNegative()) {
            throw new IllegalArgumentException("claimTtl must be positive");
        }
        queuePageSize = queuePageSize <= 0 ? 50 : queuePageSize;
        maxQueuePageSize = maxQueuePageSize <= 0 ? 200 : maxQueuePageSize;
        if (queuePageSize > maxQueuePageSize) {
            throw new IllegalArgumentException("queuePageSize must be less than or equal to maxQueuePageSize");
        }
    }
}
