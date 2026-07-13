package com.parazit.panel.application.subscription.delivery;

import com.parazit.panel.application.port.in.subscription.delivery.GetSubscriptionDeliverySummaryUseCase;
import com.parazit.panel.config.properties.QrCodeProperties;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GetSubscriptionDeliverySummaryService implements GetSubscriptionDeliverySummaryUseCase {

    private final SubscriptionDeliveryContentResolver resolver;
    private final QrCodeProperties qrProperties;

    public GetSubscriptionDeliverySummaryService(
            SubscriptionDeliveryContentResolver resolver,
            QrCodeProperties qrProperties
    ) {
        this.resolver = Objects.requireNonNull(resolver, "resolver must not be null");
        this.qrProperties = Objects.requireNonNull(qrProperties, "qrProperties must not be null");
    }

    @Override
    public SubscriptionDeliverySummary get(Long telegramUserId, UUID subscriptionId) {
        SubscriptionDeliveryContent content = resolver.resolveContent(telegramUserId, subscriptionId);
        List<SubscriptionDeliveryEntry> entries = content.entries()
                .stream()
                .map(GetSubscriptionDeliverySummaryService::entry)
                .toList();
        return new SubscriptionDeliverySummary(
                content.subscriptionId(),
                content.planName(),
                content.status(),
                content.expiresAt(),
                content.tokenVersion(),
                content.accessTokenPrefix(),
                entries.size(),
                entries,
                true,
                qrProperties.enabled(),
                qrProperties.enabled()
        );
    }

    private static SubscriptionDeliveryEntry entry(ResolvedSubscriptionConfigEntry entry) {
        return new SubscriptionDeliveryEntry(
                entry.index(),
                entry.protocol(),
                entry.displayName(),
                maskServer(entry.server()),
                entry.port(),
                entry.transport(),
                entry.security(),
                true
        );
    }

    private static String maskServer(String server) {
        if (server == null || server.isBlank()) {
            return "";
        }
        String normalized = server.trim();
        if (normalized.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            int lastDot = normalized.lastIndexOf('.');
            return normalized.substring(0, lastDot + 1) + "x";
        }
        return normalized;
    }
}

