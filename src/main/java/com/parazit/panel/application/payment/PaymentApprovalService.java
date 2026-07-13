package com.parazit.panel.application.payment;

import com.parazit.panel.application.provisioning.outbox.CreateVpnProvisioningPayloadV1;
import com.parazit.panel.application.provisioning.outbox.ProvisioningOutboxPayloadSerializer;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.provisioning.outbox.ProvisioningOutbox;
import com.parazit.panel.domain.provisioning.outbox.ProvisioningOutboxType;
import com.parazit.panel.domain.provisioning.outbox.repository.ProvisioningOutboxRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentApprovalService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProvisioningOutboxRepository outboxRepository;
    private final ProvisioningOutboxPayloadSerializer payloadSerializer;

    public PaymentApprovalService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            ProvisioningOutboxRepository outboxRepository,
            ProvisioningOutboxPayloadSerializer payloadSerializer
    ) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "outboxRepository must not be null");
        this.payloadSerializer = Objects.requireNonNull(payloadSerializer, "payloadSerializer must not be null");
    }

    @Transactional
    public PaymentApprovalResult approve(ApprovePaymentCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Payment payment = paymentRepository.findById(command.paymentId())
                .orElseThrow(() -> new PaymentNotFoundException(command.paymentId()));
        Order order = orderRepository.findByIdForUpdate(payment.getOrderId())
                .orElseThrow(() -> new PaymentOrderNotFoundException(payment.getOrderId()));
        if (!payment.getUserId().equals(order.getUserId())) {
            throw new PaymentApprovalException("Payment owner does not match order owner");
        }

        if (payment.getStatus() == PaymentStatus.APPROVED) {
            assertSameReference(payment, command.providerReference());
            ProvisioningOutbox existing = existingOutbox(order);
            return new PaymentApprovalResult(
                    payment.getId(),
                    order.getId(),
                    payment.getStatus(),
                    order.getStatus(),
                    existing == null ? null : existing.getEventId(),
                    false,
                    existing != null
            );
        }
        if (paymentRepository.existsApprovedPaymentForOrder(order.getId())) {
            throw new PaymentApprovalException("Order already has an approved payment");
        }
        payment.markApproved(command.approvedAt(), command.providerReference(), command.providerAuthority());
        order.markPaid(command.approvedAt());
        paymentRepository.save(payment);
        orderRepository.save(order);

        ProvisioningOutbox outbox = null;
        if (order.requiresProvisioning()) {
            outbox = createOutbox(command, payment, order);
        }
        return new PaymentApprovalResult(
                payment.getId(),
                order.getId(),
                payment.getStatus(),
                order.getStatus(),
                outbox == null ? null : outbox.getEventId(),
                true,
                outbox != null
        );
    }

    private ProvisioningOutbox createOutbox(ApprovePaymentCommand command, Payment payment, Order order) {
        ProvisioningOutbox existing = existingOutbox(order);
        if (existing != null) {
            return existing;
        }
        User user = userRepository.findById(order.getUserId())
                .orElseThrow(() -> new PaymentApprovalException("Payment user could not be found"));
        UUID eventId = UUID.nameUUIDFromBytes((order.getId() + ":" + ProvisioningOutboxType.CREATE_VPN_CLIENT)
                .getBytes(StandardCharsets.UTF_8));
        CreateVpnProvisioningPayloadV1 payload = new CreateVpnProvisioningPayloadV1(
                order.getId(),
                payment.getId(),
                payment.getUserId(),
                user.getTelegramUserId(),
                order.getPlanId(),
                order.getPlanSelectionId(),
                null
        );
        ProvisioningOutbox outbox = ProvisioningOutbox.create(
                eventId,
                order.getId(),
                payment.getId(),
                payment.getUserId(),
                order.getPlanId(),
                order.getPlanSelectionId(),
                ProvisioningOutboxType.CREATE_VPN_CLIENT,
                ProvisioningOutboxPayloadSerializer.CREATE_VPN_CLIENT_V1,
                payloadSerializer.serialize(payload),
                command.approvedAt()
        );
        try {
            return outboxRepository.save(outbox);
        } catch (DataIntegrityViolationException exception) {
            return outboxRepository.findByOrderIdAndType(order.getId(), ProvisioningOutboxType.CREATE_VPN_CLIENT)
                    .orElseThrow(() -> exception);
        }
    }

    private ProvisioningOutbox existingOutbox(Order order) {
        if (!order.requiresProvisioning()) {
            return null;
        }
        return outboxRepository.findByOrderIdAndType(order.getId(), ProvisioningOutboxType.CREATE_VPN_CLIENT)
                .orElse(null);
    }

    private static void assertSameReference(Payment payment, String providerReference) {
        if (providerReference == null) {
            return;
        }
        if (payment.getGatewayTransactionId() != null && !payment.getGatewayTransactionId().equals(providerReference)) {
            throw new PaymentApprovalException("Payment was already approved with a different reference");
        }
    }
}
