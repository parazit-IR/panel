package com.parazit.panel.application.subscription;

import com.parazit.panel.domain.subscription.Subscription;
import com.parazit.panel.domain.subscription.repository.SubscriptionRepository;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SubscriptionLifecycleTransaction {

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionLifecycleTransaction(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository, "subscriptionRepository must not be null");
    }

    @Transactional
    public void expire(UUID subscriptionId, Instant now) {
        Subscription subscription = subscriptionRepository.findByIdForUpdate(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));
        if (subscription.isExpiredAt(now)) {
            subscription.expire(now);
            subscriptionRepository.save(subscription);
        }
    }

    @Transactional
    public void markInvalid(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findByIdForUpdate(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));
        if (!subscription.isTerminal()) {
            subscription.markInvalid();
            subscriptionRepository.save(subscription);
        }
    }
}
