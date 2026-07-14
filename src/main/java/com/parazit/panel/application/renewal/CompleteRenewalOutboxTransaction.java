package com.parazit.panel.application.renewal;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.renewal.result.ApplyRenewalResult;
import com.parazit.panel.application.renewal.result.RenewalApplicationTarget;
import com.parazit.panel.application.xui.model.XuiClientSnapshot;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.RenewalSnapshot;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.renewal.RenewalApplyOutcome;
import com.parazit.panel.domain.renewal.RenewalExecutionRequest;
import com.parazit.panel.domain.renewal.RenewalOutbox;
import com.parazit.panel.domain.renewal.SubscriptionRenewalHistory;
import com.parazit.panel.domain.renewal.repository.RenewalOutboxRepository;
import com.parazit.panel.domain.renewal.repository.SubscriptionRenewalHistoryRepository;
import com.parazit.panel.domain.subscription.Subscription;
import com.parazit.panel.domain.subscription.repository.SubscriptionRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import com.parazit.panel.domain.xui.provisioning.repository.XuiClientProvisionRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CompleteRenewalOutboxTransaction {

    private final RenewalOutboxRepository outboxRepository;
    private final OrderRepository orderRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final XuiClientProvisionRepository provisionRepository;
    private final SubscriptionRenewalHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final RenewalNotificationPublisher notificationPublisher;
    private final SystemClockPort clock;

    public CompleteRenewalOutboxTransaction(
            RenewalOutboxRepository outboxRepository,
            OrderRepository orderRepository,
            SubscriptionRepository subscriptionRepository,
            XuiClientProvisionRepository provisionRepository,
            SubscriptionRenewalHistoryRepository historyRepository,
            UserRepository userRepository,
            RenewalNotificationPublisher notificationPublisher,
            SystemClockPort clock
    ) {
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "outboxRepository must not be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository, "subscriptionRepository must not be null");
        this.provisionRepository = Objects.requireNonNull(provisionRepository, "provisionRepository must not be null");
        this.historyRepository = Objects.requireNonNull(historyRepository, "historyRepository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.notificationPublisher = Objects.requireNonNull(notificationPublisher, "notificationPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public ApplyRenewalResult complete(
            UUID outboxId,
            RenewalExecutionRequest executionRequest,
            RenewalApplicationTarget target,
            XuiClientSnapshot verifiedRemote,
            boolean remoteAlreadyApplied
    ) {
        RenewalOutbox outbox = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new IllegalStateException("Renewal outbox not found"));
        if (outbox.getStatus() == com.parazit.panel.domain.renewal.RenewalOutboxStatus.PROCESSED) {
            return new ApplyRenewalResult(
                    executionRequest.renewalOrderId(),
                    executionRequest.targetSubscriptionId(),
                    RenewalApplyOutcome.ALREADY_APPLIED,
                    executionRequest.previousExpiryAt(),
                    target.desiredExpiryAt(),
                    executionRequest.previousTrafficLimitBytes(),
                    target.desiredTotalTrafficBytes(),
                    verifiedRemote.totalConsumedBytes(),
                    true,
                    false,
                    outbox.getProcessedAt()
            );
        }
        Order order = orderRepository.findById(outbox.getRenewalOrderId())
                .orElseThrow(() -> new IllegalStateException("Renewal order not found"));
        Subscription subscription = subscriptionRepository.findById(outbox.getTargetSubscriptionId())
                .orElseThrow(() -> new IllegalStateException("Renewal subscription not found"));
        XuiClientProvision provision = provisionRepository.findById(outbox.getTargetProvisionId())
                .orElseThrow(() -> new IllegalStateException("Renewal provision not found"));
        User user = userRepository.findById(executionRequest.userId())
                .orElseThrow(() -> new IllegalStateException("Renewal user not found"));
        if (!subscription.getUserId().equals(order.getUserId()) || !provision.getUserId().equals(order.getUserId())) {
            throw new IllegalStateException("Renewal ownership mismatch");
        }
        java.time.Instant now = clock.now();
        java.time.Instant previousExpiry = subscription.getExpiresAt();
        long previousTraffic = provision.getTrafficLimitBytes();
        long previousUsed = provision.getLastKnownTotalBytes();
        subscription.applyRenewal(target.desiredExpiryAt(), now);
        provision.synchronizeRemoteState(
                verifiedRemote.enabled(),
                verifiedRemote.totalTrafficLimitBytes(),
                verifiedRemote.expiryTime(),
                verifiedRemote.ipLimit(),
                verifiedRemote.uploadBytes(),
                verifiedRemote.downloadBytes(),
                now
        );
        order.markRenewalCompleted(now);
        outbox.markStep(com.parazit.panel.domain.renewal.RenewalExecutionStep.LOCAL_STATE_UPDATED);
        outbox.markProcessed(now);
        if (!historyRepository.existsByRenewalOrderId(order.getId())) {
            RenewalSnapshot snapshot = order.getRenewalSnapshot();
            historyRepository.save(SubscriptionRenewalHistory.applied(
                    subscription.getId(),
                    order.getId(),
                    executionRequest.paymentId(),
                    snapshot.sourcePlanId(),
                    previousExpiry,
                    target.desiredExpiryAt(),
                    previousTraffic,
                    target.desiredTotalTrafficBytes(),
                    previousUsed,
                    verifiedRemote.totalConsumedBytes(),
                    snapshot.trafficPolicy(),
                    executionRequest.expiryPolicy(),
                    null,
                    null,
                    now
            ));
        }
        subscriptionRepository.save(subscription);
        provisionRepository.save(provision);
        orderRepository.save(order);
        outboxRepository.save(outbox);
        RenewalSnapshot snapshot = order.getRenewalSnapshot();
        notificationPublisher.publishAfterCommit(new RenewalCompletedNotificationEvent(
                user.getId(),
                user.getTelegramUserId(),
                order.getId(),
                subscription.getId(),
                snapshot.serviceDisplayName(),
                snapshot.serviceUsername(),
                target.desiredExpiryAt(),
                target.desiredTotalTrafficBytes(),
                now
        ));
        return new ApplyRenewalResult(
                order.getId(),
                subscription.getId(),
                RenewalApplyOutcome.APPLIED,
                previousExpiry,
                target.desiredExpiryAt(),
                previousTraffic,
                target.desiredTotalTrafficBytes(),
                verifiedRemote.totalConsumedBytes(),
                remoteAlreadyApplied,
                true,
                now
        );
    }
}
