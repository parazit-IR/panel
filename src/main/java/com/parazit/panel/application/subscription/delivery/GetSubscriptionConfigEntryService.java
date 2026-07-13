package com.parazit.panel.application.subscription.delivery;

import com.parazit.panel.application.port.in.subscription.delivery.GetSubscriptionConfigEntryUseCase;
import com.parazit.panel.application.subscription.SubscriptionNotFoundException;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GetSubscriptionConfigEntryService implements GetSubscriptionConfigEntryUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetSubscriptionConfigEntryService.class);

    private final SubscriptionDeliveryContentResolver resolver;

    public GetSubscriptionConfigEntryService(SubscriptionDeliveryContentResolver resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver must not be null");
    }

    @Override
    public SubscriptionConfigEntryResult get(Long telegramUserId, UUID subscriptionId, int configIndex) {
        SubscriptionDeliveryContent content = resolver.resolveContent(telegramUserId, subscriptionId);
        ResolvedSubscriptionConfigEntry entry = entry(content, configIndex);
        log.atInfo()
                .addKeyValue("subscriptionId", subscriptionId)
                .addKeyValue("configIndex", configIndex)
                .log("Subscription configuration entry delivered");
        return new SubscriptionConfigEntryResult(
                subscriptionId,
                entry.index(),
                entry.protocol(),
                entry.displayName(),
                entry.uri(),
                entry.server(),
                entry.port(),
                entry.transport(),
                entry.security(),
                content.expiresAt()
        );
    }

    private static ResolvedSubscriptionConfigEntry entry(SubscriptionDeliveryContent content, int configIndex) {
        if (configIndex < 1) {
            throw new IllegalArgumentException("configIndex must be one-based and positive");
        }
        return content.entries()
                .stream()
                .filter(candidate -> candidate.index() == configIndex)
                .findFirst()
                .orElseThrow(() -> new SubscriptionNotFoundException(content.subscriptionId()));
    }
}
