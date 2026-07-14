package com.parazit.panel.application.payment;

import com.parazit.panel.application.provisioning.outbox.CreateVpnProvisioningPayloadV1;
import com.parazit.panel.application.provisioning.outbox.ProvisioningOutboxPayloadSerializer;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.payment.Payment;
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

@Service
public class NewSubscriptionPaidOrderHandler {

    private final UserRepository userRepository;
    private final ProvisioningOutboxRepository outboxRepository;
    private final ProvisioningOutboxPayloadSerializer payloadSerializer;

    public NewSubscriptionPaidOrderHandler(
            UserRepository userRepository,
            ProvisioningOutboxRepository outboxRepository,
            ProvisioningOutboxPayloadSerializer payloadSerializer
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "outboxRepository must not be null");
        this.payloadSerializer = Objects.requireNonNull(payloadSerializer, "payloadSerializer must not be null");
    }

    public ApprovedOrderDispatchResult handle(ApprovePaymentCommand command, Payment payment, Order order) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(payment, "payment must not be null");
        Objects.requireNonNull(order, "order must not be null");
        if (!order.requiresProvisioning()) {
            return ApprovedOrderDispatchResult.none();
        }
        ProvisioningOutbox existing = existingOutbox(order);
        if (existing != null) {
            return ApprovedOrderDispatchResult.provisioning(existing.getEventId());
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
            return ApprovedOrderDispatchResult.provisioning(outboxRepository.save(outbox).getEventId());
        } catch (DataIntegrityViolationException exception) {
            ProvisioningOutbox reloaded = outboxRepository
                    .findByOrderIdAndType(order.getId(), ProvisioningOutboxType.CREATE_VPN_CLIENT)
                    .orElseThrow(() -> exception);
            return ApprovedOrderDispatchResult.provisioning(reloaded.getEventId());
        }
    }

    private ProvisioningOutbox existingOutbox(Order order) {
        return outboxRepository.findByOrderIdAndType(order.getId(), ProvisioningOutboxType.CREATE_VPN_CLIENT)
                .orElse(null);
    }
}
