package com.parazit.panel.application.renewal;

import com.parazit.panel.application.port.in.renewal.ApplyRenewalUseCase;
import com.parazit.panel.application.port.out.renewal.RenewalExecutionPayloadSerializerPort;
import com.parazit.panel.application.port.out.renewal.RenewalTargetPayloadSerializerPort;
import com.parazit.panel.application.port.out.xui.XuiClientManagementClient;
import com.parazit.panel.application.port.out.xui.XuiClientStateClient;
import com.parazit.panel.application.renewal.command.ApplyRenewalCommand;
import com.parazit.panel.application.renewal.result.ApplyRenewalResult;
import com.parazit.panel.application.renewal.result.RenewalApplicationTarget;
import com.parazit.panel.application.xui.client.XuiRemoteClientMissingException;
import com.parazit.panel.application.xui.client.model.ResetXuiClientTrafficRequest;
import com.parazit.panel.application.xui.client.model.UpdateXuiClientRequest;
import com.parazit.panel.application.xui.model.XuiClientSnapshot;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.OrderStatus;
import com.parazit.panel.domain.order.OrderType;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.renewal.RenewalExecutionRequest;
import com.parazit.panel.domain.renewal.RenewalExecutionStep;
import com.parazit.panel.domain.renewal.RenewalOutbox;
import com.parazit.panel.domain.renewal.RenewalOutboxStatus;
import com.parazit.panel.domain.renewal.repository.RenewalOutboxRepository;
import com.parazit.panel.domain.subscription.Subscription;
import com.parazit.panel.domain.subscription.SubscriptionStatus;
import com.parazit.panel.domain.subscription.repository.SubscriptionRepository;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import com.parazit.panel.domain.xui.provisioning.XuiProvisionStatus;
import com.parazit.panel.domain.xui.provisioning.repository.XuiClientProvisionRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ApplyRenewalService implements ApplyRenewalUseCase {

    private final RenewalOutboxRepository outboxRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final XuiClientProvisionRepository provisionRepository;
    private final RenewalExecutionPayloadSerializerPort executionPayloadSerializer;
    private final RenewalTargetPayloadSerializerPort targetPayloadSerializer;
    private final RenewalApplicationTargetFactory targetFactory;
    private final RenewalRemoteVerificationPolicy verificationPolicy;
    private final XuiClientStateClient stateClient;
    private final XuiClientManagementClient managementClient;
    private final CompleteRenewalOutboxTransaction completeTransaction;

    public ApplyRenewalService(
            RenewalOutboxRepository outboxRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            SubscriptionRepository subscriptionRepository,
            XuiClientProvisionRepository provisionRepository,
            RenewalExecutionPayloadSerializerPort executionPayloadSerializer,
            RenewalTargetPayloadSerializerPort targetPayloadSerializer,
            RenewalApplicationTargetFactory targetFactory,
            RenewalRemoteVerificationPolicy verificationPolicy,
            XuiClientStateClient stateClient,
            XuiClientManagementClient managementClient,
            CompleteRenewalOutboxTransaction completeTransaction
    ) {
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "outboxRepository must not be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository, "subscriptionRepository must not be null");
        this.provisionRepository = Objects.requireNonNull(provisionRepository, "provisionRepository must not be null");
        this.executionPayloadSerializer = Objects.requireNonNull(executionPayloadSerializer, "executionPayloadSerializer must not be null");
        this.targetPayloadSerializer = Objects.requireNonNull(targetPayloadSerializer, "targetPayloadSerializer must not be null");
        this.targetFactory = Objects.requireNonNull(targetFactory, "targetFactory must not be null");
        this.verificationPolicy = Objects.requireNonNull(verificationPolicy, "verificationPolicy must not be null");
        this.stateClient = Objects.requireNonNull(stateClient, "stateClient must not be null");
        this.managementClient = Objects.requireNonNull(managementClient, "managementClient must not be null");
        this.completeTransaction = Objects.requireNonNull(completeTransaction, "completeTransaction must not be null");
    }

    @Override
    public ApplyRenewalResult apply(ApplyRenewalCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        RenewalOutbox outbox = outboxRepository.findById(command.renewalOutboxId())
                .orElseThrow(() -> new IllegalStateException("Renewal outbox not found"));
        if (outbox.getStatus() == RenewalOutboxStatus.PROCESSED) {
            RenewalApplicationTarget target = targetPayloadSerializer.deserialize(outbox.getTargetPayload());
            RenewalExecutionRequest request = executionPayloadSerializer.deserialize(outbox.getPayload());
            XuiClientProvision provision = provisionRepository.findById(outbox.getTargetProvisionId())
                    .orElseThrow(() -> new IllegalStateException("Renewal provision not found"));
            XuiClientSnapshot remote = remote(provision);
            return completeTransaction.complete(outbox.getId(), request, target, remote, true);
        }
        if (outbox.getStatus() != RenewalOutboxStatus.PROCESSING) {
            throw new IllegalStateException("Renewal outbox is not claimed");
        }
        validateCommand(command, outbox);
        RenewalExecutionRequest executionRequest = executionPayloadSerializer.deserialize(outbox.getPayload());
        Order order = orderRepository.findById(outbox.getRenewalOrderId())
                .orElseThrow(() -> new IllegalStateException("Renewal order not found"));
        Payment payment = paymentRepository.findById(outbox.getPaymentId())
                .orElseThrow(() -> new IllegalStateException("Renewal payment not found"));
        Subscription subscription = subscriptionRepository.findById(outbox.getTargetSubscriptionId())
                .orElseThrow(() -> new IllegalStateException("Renewal subscription not found"));
        XuiClientProvision provision = provisionRepository.findById(outbox.getTargetProvisionId())
                .orElseThrow(() -> new IllegalStateException("Renewal provision not found"));
        validateState(order, payment, subscription, provision);
        RenewalApplicationTarget target = target(outbox, order, subscription, provision, executionRequest);
        XuiClientSnapshot beforeUpdate = remote(provision);
        verificationPolicy.verifyIdentity(provision, beforeUpdate);
        boolean remoteAlreadyApplied = targetMatches(target, beforeUpdate);
        if (!remoteAlreadyApplied) {
            managementClient.updateClient(new UpdateXuiClientRequest(
                    provision.getInboundId(),
                    provision.getRemoteClientId(),
                    provision.getRemoteEmail(),
                    true,
                    target.desiredExpiryAt(),
                    target.desiredTotalTrafficBytes(),
                    null,
                    null
            ));
            markStep(outbox, RenewalExecutionStep.CLIENT_UPDATED);
        }
        XuiClientSnapshot afterUpdate = remote(provision);
        verificationPolicy.verifyIdentity(provision, afterUpdate);
        if (target.resetUsage() && (afterUpdate.uploadBytes() != 0 || afterUpdate.downloadBytes() != 0)) {
            managementClient.resetTraffic(new ResetXuiClientTrafficRequest(
                    provision.getInboundId(),
                    provision.getRemoteClientId(),
                    provision.getRemoteEmail()
            ));
            markStep(outbox, RenewalExecutionStep.TRAFFIC_RESET);
        }
        XuiClientSnapshot verified = remote(provision);
        verificationPolicy.verifyIdentity(provision, verified);
        verificationPolicy.verifyTarget(target, verified);
        markStep(outbox, RenewalExecutionStep.REMOTE_VERIFIED);
        return completeTransaction.complete(outbox.getId(), executionRequest, target, verified, remoteAlreadyApplied);
    }

    private RenewalApplicationTarget target(
            RenewalOutbox outbox,
            Order order,
            Subscription subscription,
            XuiClientProvision provision,
            RenewalExecutionRequest executionRequest
    ) {
        if (outbox.getTargetPayload() != null) {
            return targetPayloadSerializer.deserialize(outbox.getTargetPayload());
        }
        XuiClientSnapshot remote = remote(provision);
        verificationPolicy.verifyIdentity(provision, remote);
        RenewalApplicationTarget target = targetFactory.create(
                order.getId(),
                subscription.getId(),
                provision,
                order.getRenewalSnapshot(),
                executionRequest,
                remote
        );
        outbox.storeTarget(RenewalApplicationTarget.VERSION_V1, targetPayloadSerializer.serialize(target));
        outboxRepository.save(outbox);
        return target;
    }

    private void validateCommand(ApplyRenewalCommand command, RenewalOutbox outbox) {
        if (!outbox.getRenewalOrderId().equals(command.renewalOrderId())
                || !outbox.getTargetSubscriptionId().equals(command.targetSubscriptionId())
                || !outbox.getTargetProvisionId().equals(command.targetProvisionId())) {
            throw new IllegalArgumentException("Renewal command does not match outbox");
        }
    }

    private void validateState(Order order, Payment payment, Subscription subscription, XuiClientProvision provision) {
        if (order.getType() != OrderType.RENEWAL || order.getStatus() != OrderStatus.RENEWAL_PENDING) {
            throw new IllegalStateException("Renewal order is not ready for application");
        }
        if (!payment.getOrderId().equals(order.getId()) || payment.getStatus() != PaymentStatus.APPROVED) {
            throw new IllegalStateException("Renewal payment is not approved");
        }
        if (!subscription.getUserId().equals(order.getUserId())
                || !provision.getUserId().equals(order.getUserId())
                || !subscription.getXuiClientProvisionId().equals(provision.getId())) {
            throw new IllegalStateException("Renewal target ownership mismatch");
        }
        if (subscription.getStatus() == SubscriptionStatus.REVOKED || subscription.getStatus() == SubscriptionStatus.INVALID) {
            throw new IllegalStateException("Renewal subscription is not renewable");
        }
        if (provision.getStatus() == XuiProvisionStatus.DELETED || provision.getStatus() == XuiProvisionStatus.DELETING) {
            throw new IllegalStateException("Renewal provision is not renewable");
        }
    }

    private XuiClientSnapshot remote(XuiClientProvision provision) {
        return stateClient.findClient(provision.getInboundId(), provision.getRemoteClientId())
                .orElseThrow(XuiRemoteClientMissingException::new);
    }

    private void markStep(RenewalOutbox outbox, RenewalExecutionStep step) {
        outbox.markStep(step);
        outboxRepository.save(outbox);
    }

    private static boolean targetMatches(RenewalApplicationTarget target, XuiClientSnapshot remote) {
        return remote.expiryTime() != null
                && remote.expiryTime().equals(target.desiredExpiryAt())
                && remote.totalTrafficLimitBytes() == target.desiredTotalTrafficBytes()
                && (!target.resetUsage() || (remote.uploadBytes() == 0 && remote.downloadBytes() == 0));
    }
}
