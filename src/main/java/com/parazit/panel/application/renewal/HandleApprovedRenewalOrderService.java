package com.parazit.panel.application.renewal;

import com.parazit.panel.application.payment.ApprovePaymentCommand;
import com.parazit.panel.application.payment.PaymentApprovalException;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.port.out.renewal.RenewalExecutionPayloadSerializerPort;
import com.parazit.panel.application.renewal.result.RenewalPaymentApprovalResult;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.OrderStatus;
import com.parazit.panel.domain.order.OrderType;
import com.parazit.panel.domain.order.RenewalSnapshot;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.renewal.RenewalApprovalOutcome;
import com.parazit.panel.domain.renewal.RenewalExecutionRequest;
import com.parazit.panel.domain.renewal.RenewalOutbox;
import com.parazit.panel.domain.renewal.RenewalOutboxStatus;
import com.parazit.panel.domain.renewal.repository.RenewalOutboxRepository;
import com.parazit.panel.domain.subscription.Subscription;
import com.parazit.panel.domain.subscription.SubscriptionStatus;
import com.parazit.panel.domain.subscription.repository.SubscriptionRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import com.parazit.panel.domain.xui.provisioning.XuiProvisionStatus;
import com.parazit.panel.domain.xui.provisioning.repository.XuiClientProvisionRepository;
import java.time.Instant;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HandleApprovedRenewalOrderService {

    private static final String REVIEW_MISSING_SNAPSHOT = "RENEWAL_SNAPSHOT_MISSING";
    private static final String REVIEW_INVALID_TARGET = "RENEWAL_TARGET_INVALID";
    private static final String REVIEW_MISSING_PROVISION = "RENEWAL_PROVISION_MISSING";
    private static final String REVIEW_INVALID_PROVISION = "RENEWAL_PROVISION_INVALID";

    private final RenewalOutboxRepository renewalOutboxRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final XuiClientProvisionRepository provisionRepository;
    private final OrderRepository orderRepository;
    private final RenewalExecutionRequestFactory requestFactory;
    private final RenewalExecutionPayloadSerializerPort payloadSerializer;
    private final SystemClockPort clock;
    private final RenewalMetrics metrics;
    private final UserRepository userRepository;
    private final RenewalQueuedNotificationPublisher notificationPublisher;

    public HandleApprovedRenewalOrderService(
            RenewalOutboxRepository renewalOutboxRepository,
            SubscriptionRepository subscriptionRepository,
            XuiClientProvisionRepository provisionRepository,
            OrderRepository orderRepository,
            RenewalExecutionRequestFactory requestFactory,
            RenewalExecutionPayloadSerializerPort payloadSerializer,
            SystemClockPort clock,
            RenewalMetrics metrics,
            UserRepository userRepository,
            RenewalQueuedNotificationPublisher notificationPublisher
    ) {
        this.renewalOutboxRepository = Objects.requireNonNull(renewalOutboxRepository, "renewalOutboxRepository must not be null");
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository, "subscriptionRepository must not be null");
        this.provisionRepository = Objects.requireNonNull(provisionRepository, "provisionRepository must not be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.requestFactory = Objects.requireNonNull(requestFactory, "requestFactory must not be null");
        this.payloadSerializer = Objects.requireNonNull(payloadSerializer, "payloadSerializer must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.notificationPublisher = Objects.requireNonNull(notificationPublisher, "notificationPublisher must not be null");
    }

    @Transactional
    public RenewalPaymentApprovalResult handleApproved(ApprovePaymentCommand command, Payment payment, Order order) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(payment, "payment must not be null");
        Objects.requireNonNull(order, "order must not be null");
        if (payment.getStatus() != PaymentStatus.APPROVED) {
            throw new PaymentApprovalException("Payment is not approved");
        }
        if (order.getType() != OrderType.RENEWAL) {
            throw new PaymentApprovalException("Order is not a renewal order");
        }
        if (!payment.getOrderId().equals(order.getId()) || !payment.getUserId().equals(order.getUserId())) {
            throw new PaymentApprovalException("Payment does not belong to renewal order");
        }
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.EXPIRED) {
            throw new PaymentApprovalException("Renewal order is not approvable");
        }

        RenewalOutbox existing = findExisting(order);
        if (existing != null) {
            order.markRenewalPending(command.approvedAt());
            orderRepository.save(order);
            metrics.renewalOutboxReuse();
            metrics.renewalPaymentApproval("already_queued", command.source());
            return new RenewalPaymentApprovalResult(
                    order.getId(),
                    payment.getId(),
                    existing.getId(),
                    existing.getStatus() == RenewalOutboxStatus.PROCESSED
                            ? RenewalApprovalOutcome.ALREADY_COMPLETED
                            : RenewalApprovalOutcome.ALREADY_QUEUED,
                    false,
                    true,
                    payment.getApprovedAt(),
                    existing.getAvailableAt()
            );
        }

        String reviewReason = validateTarget(order);
        if (reviewReason != null) {
            order.markRenewalReviewRequired(reviewReason, "Renewal approval requires manual review", command.approvedAt());
            orderRepository.save(order);
            metrics.renewalApprovalBlocked(reviewReason, command.source());
            metrics.renewalPaymentApproval("review_required", command.source());
            return new RenewalPaymentApprovalResult(
                    order.getId(),
                    payment.getId(),
                    null,
                    RenewalApprovalOutcome.REVIEW_REQUIRED,
                    false,
                    false,
                    payment.getApprovedAt(),
                    null
            );
        }

        Instant requestedAt = clock.now();
        RenewalExecutionRequest request = requestFactory.create(order, payment, requestedAt);
        RenewalOutbox outbox = RenewalOutbox.create(
                order.getId(),
                payment.getId(),
                request.targetSubscriptionId(),
                request.targetProvisionId(),
                payloadSerializer.serialize(request),
                requestedAt
        );
        SavedRenewalOutbox savedOutbox = saveOrReload(outbox);
        RenewalOutbox saved = savedOutbox.outbox();
        order.markRenewalPending(command.approvedAt());
        orderRepository.save(order);
        boolean created = savedOutbox.created();
        if (created) {
            metrics.renewalOutboxCreation();
            publishQueuedNotification(order, request, requestedAt);
        } else {
            metrics.renewalOutboxReuse();
        }
        metrics.renewalPaymentApproval(created ? "queued" : "already_queued", command.source());
        return new RenewalPaymentApprovalResult(
                order.getId(),
                payment.getId(),
                saved.getId(),
                created ? RenewalApprovalOutcome.RENEWAL_QUEUED : RenewalApprovalOutcome.ALREADY_QUEUED,
                created,
                !created,
                payment.getApprovedAt(),
                saved.getAvailableAt()
        );
    }

    private SavedRenewalOutbox saveOrReload(RenewalOutbox outbox) {
        try {
            return new SavedRenewalOutbox(renewalOutboxRepository.save(outbox), true);
        } catch (DataIntegrityViolationException exception) {
            RenewalOutbox existing = renewalOutboxRepository
                    .findByRenewalOrderIdAndEventType(outbox.getRenewalOrderId(), outbox.getEventType())
                    .orElseThrow(() -> exception);
            return new SavedRenewalOutbox(existing, false);
        }
    }

    private RenewalOutbox findExisting(Order order) {
        return renewalOutboxRepository
                .findByRenewalOrderIdAndEventType(order.getId(), RenewalOutbox.APPLY_REQUESTED_EVENT_TYPE)
                .orElse(null);
    }

    private String validateTarget(Order order) {
        RenewalSnapshot snapshot = order.getRenewalSnapshot();
        if (snapshot == null || order.getTargetSubscriptionId() == null) {
            return REVIEW_MISSING_SNAPSHOT;
        }
        Subscription subscription = subscriptionRepository.findByIdForUpdate(order.getTargetSubscriptionId())
                .orElse(null);
        if (subscription == null
                || !subscription.getUserId().equals(order.getUserId())
                || subscription.getStatus() == SubscriptionStatus.REVOKED
                || subscription.getStatus() == SubscriptionStatus.INVALID
                || !snapshot.targetSubscriptionId().equals(subscription.getId())) {
            return REVIEW_INVALID_TARGET;
        }
        XuiClientProvision provision = provisionRepository.findByIdForUpdate(snapshot.targetProvisionId()).orElse(null);
        if (provision == null) {
            return REVIEW_MISSING_PROVISION;
        }
        if (!provision.getUserId().equals(order.getUserId())
                || !subscription.getXuiClientProvisionId().equals(provision.getId())
                || provision.getStatus() != XuiProvisionStatus.ACTIVE
                || provision.getRemoteClientId() == null
                || provision.getRemoteClientId().isBlank()) {
            return REVIEW_INVALID_PROVISION;
        }
        return null;
    }

    private void publishQueuedNotification(Order order, RenewalExecutionRequest request, Instant queuedAt) {
        User user = userRepository.findById(order.getUserId())
                .orElseThrow(() -> new PaymentApprovalException("Payment user could not be found"));
        RenewalSnapshot snapshot = order.getRenewalSnapshot();
        notificationPublisher.publishAfterCommit(new RenewalQueuedNotificationEvent(
                user.getId(),
                user.getTelegramUserId(),
                order.getId(),
                request.targetSubscriptionId(),
                snapshot == null ? null : snapshot.serviceDisplayName(),
                request.serviceUsername(),
                queuedAt
        ));
    }

    private record SavedRenewalOutbox(RenewalOutbox outbox, boolean created) {
    }
}
