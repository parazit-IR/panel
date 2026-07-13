package com.parazit.panel.application.subscription;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.domain.subscription.repository.SubscriptionRepository;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionAccessMetricsService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionAccessMetricsService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final SystemClockPort clock;

    public SubscriptionAccessMetricsService(
            SubscriptionRepository subscriptionRepository,
            SystemClockPort clock
    ) {
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository, "subscriptionRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public void recordSuccessfulAccess(UUID subscriptionId) {
        try {
            subscriptionRepository.incrementAccessMetadata(subscriptionId, clock.now());
        } catch (RuntimeException exception) {
            log.debug("Subscription access metrics update failed subscriptionId={}", subscriptionId);
        }
    }
}
