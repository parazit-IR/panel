package com.parazit.panel.application.renewal;

import com.parazit.panel.application.port.in.renewal.GetRenewalStatusUseCase;
import com.parazit.panel.application.renewal.command.GetRenewalStatusCommand;
import com.parazit.panel.application.renewal.result.RenewalStatusResult;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.OrderType;
import com.parazit.panel.domain.order.RenewalSnapshot;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.renewal.RenewalOutbox;
import com.parazit.panel.domain.renewal.repository.RenewalOutboxRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.util.Comparator;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetRenewalStatusService implements GetRenewalStatusUseCase {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RenewalOutboxRepository renewalOutboxRepository;

    public GetRenewalStatusService(
            UserRepository userRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            RenewalOutboxRepository renewalOutboxRepository
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.renewalOutboxRepository = Objects.requireNonNull(renewalOutboxRepository, "renewalOutboxRepository must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public RenewalStatusResult get(GetRenewalStatusCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        User user = userRepository.findByTelegramUserId(command.telegramUserId())
                .orElseThrow(() -> new RenewalFlowException("telegram.renewal.not_available"));
        Order order = orderRepository.findById(command.renewalOrderId())
                .orElseThrow(() -> new RenewalFlowException("telegram.renewal.not_available"));
        if (!order.getUserId().equals(user.getId()) || order.getType() != OrderType.RENEWAL) {
            throw new RenewalFlowException("telegram.renewal.not_available");
        }
        RenewalSnapshot snapshot = order.getRenewalSnapshot();
        PaymentStatus paymentStatus = paymentRepository.findAllByOrderId(order.getId()).stream()
                .max(Comparator.comparing(Payment::getCreatedAt))
                .map(Payment::getStatus)
                .orElse(null);
        RenewalOutbox outbox = renewalOutboxRepository
                .findByRenewalOrderIdAndEventType(order.getId(), RenewalOutbox.APPLY_REQUESTED_EVENT_TYPE)
                .orElse(null);
        return new RenewalStatusResult(
                order.getId(),
                order.getType(),
                paymentStatus,
                order.getStatus(),
                outbox == null ? null : outbox.getStatus(),
                snapshot == null ? null : snapshot.serviceDisplayName(),
                snapshot == null ? null : snapshot.serviceUsername(),
                outbox == null ? null : outbox.getAvailableAt(),
                order.getUpdatedAt()
        );
    }
}
