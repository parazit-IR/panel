package com.parazit.panel.application.payment;

import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentApprovalService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderPaymentApprovedDispatcher dispatcher;

    public PaymentApprovalService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            OrderPaymentApprovedDispatcher dispatcher
    ) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher must not be null");
    }

    @Transactional
    public PaymentApprovalResult approve(ApprovePaymentCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Payment payment = paymentRepository.findByIdForUpdate(command.paymentId())
                .orElseThrow(() -> new PaymentNotFoundException(command.paymentId()));
        Order order = orderRepository.findByIdForUpdate(payment.getOrderId())
                .orElseThrow(() -> new PaymentOrderNotFoundException(payment.getOrderId()));
        if (!payment.getUserId().equals(order.getUserId())) {
            throw new PaymentApprovalException("Payment owner does not match order owner");
        }

        if (payment.getStatus() == PaymentStatus.APPROVED) {
            assertSameReference(payment, command.providerReference());
            ApprovedOrderDispatchResult dispatchResult = dispatcher.dispatch(command, payment, order);
            return new PaymentApprovalResult(
                    payment.getId(),
                    order.getId(),
                    payment.getStatus(),
                    order.getStatus(),
                    dispatchResult.provisioningEventId(),
                    false,
                    dispatchResult.provisioningRequired()
            );
        }
        if (paymentRepository.existsApprovedPaymentForOrder(order.getId())) {
            throw new PaymentApprovalException("Order already has an approved payment");
        }
        payment.markApproved(command.approvedAt(), command.providerReference(), command.providerAuthority());
        order.markPaid(command.approvedAt());
        paymentRepository.save(payment);
        orderRepository.save(order);

        ApprovedOrderDispatchResult dispatchResult = dispatcher.dispatch(command, payment, order);
        return new PaymentApprovalResult(
                payment.getId(),
                order.getId(),
                payment.getStatus(),
                order.getStatus(),
                dispatchResult.provisioningEventId(),
                true,
                dispatchResult.provisioningRequired()
        );
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
