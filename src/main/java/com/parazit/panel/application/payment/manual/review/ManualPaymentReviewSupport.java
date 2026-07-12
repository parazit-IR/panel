package com.parazit.panel.application.payment.manual.review;

import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.manual.ManualCardPaymentInstruction;
import com.parazit.panel.domain.payment.manual.ManualPaymentInstructionStatus;
import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceipt;
import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceiptStatus;
import com.parazit.panel.domain.payment.manual.repository.ManualCardPaymentInstructionRepository;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ManualPaymentReviewSupport {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ManualCardPaymentInstructionRepository instructionRepository;

    ManualPaymentReviewSupport(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            ManualCardPaymentInstructionRepository instructionRepository
    ) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.instructionRepository = Objects.requireNonNull(instructionRepository, "instructionRepository must not be null");
    }

    ManualReviewContext loadQueuedContext(ManualPaymentReceipt receipt) {
        Payment payment = paymentRepository.findById(receipt.getPaymentId())
                .orElseThrow(() -> new ManualPaymentReviewNotAllowedException("Payment for receipt could not be found"));
        Order order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new ManualPaymentReviewNotAllowedException("Order for payment could not be found"));
        ManualCardPaymentInstruction instruction = instructionRepository.findById(receipt.getInstructionId())
                .orElseThrow(() -> new ManualPaymentReviewNotAllowedException("Manual payment instruction could not be found"));
        validateLinkage(receipt, payment, order, instruction);
        return new ManualReviewContext(receipt, payment, order, instruction);
    }

    void validateReviewable(ManualReviewContext context) {
        if (context.receipt().getStatus() != ManualPaymentReceiptStatus.QUEUED_FOR_REVIEW) {
            throw new ManualPaymentReviewNotAllowedException("Receipt is not queued for review");
        }
        if (context.payment().getStatus() != PaymentStatus.WAITING_FOR_REVIEW) {
            throw new ManualPaymentReviewNotAllowedException("Payment is not waiting for review");
        }
        if (context.payment().getMethod() != PaymentMethod.CARD_TO_CARD) {
            throw new ManualPaymentReviewNotAllowedException("Payment method is not manual card payment");
        }
        if (context.instruction().getStatus() != ManualPaymentInstructionStatus.RECEIPT_PENDING) {
            throw new ManualPaymentReviewNotAllowedException("Manual instruction is not pending receipt review");
        }
        if (context.receipt().getClaimedAmount() != context.instruction().getPayableAmount()) {
            throw new ManualPaymentReviewNotAllowedException("Receipt amount does not match manual instruction amount");
        }
    }

    void saveReviewState(ManualReviewContext context) {
        paymentRepository.save(context.payment());
        orderRepository.save(context.order());
        instructionRepository.save(context.instruction());
    }

    private static void validateLinkage(
            ManualPaymentReceipt receipt,
            Payment payment,
            Order order,
            ManualCardPaymentInstruction instruction
    ) {
        UUID paymentId = payment.getId();
        if (!receipt.getPaymentId().equals(paymentId) || !instruction.getPaymentId().equals(paymentId)) {
            throw new ManualPaymentReviewNotAllowedException("Receipt, instruction, and payment do not match");
        }
        if (!payment.getOrderId().equals(order.getId())) {
            throw new ManualPaymentReviewNotAllowedException("Payment and order do not match");
        }
        if (!payment.getUserId().equals(order.getUserId()) || !receipt.getUserId().equals(payment.getUserId())) {
            throw new ManualPaymentReviewNotAllowedException("Receipt ownership does not match payment owner");
        }
    }

    record ManualReviewContext(
            ManualPaymentReceipt receipt,
            Payment payment,
            Order order,
            ManualCardPaymentInstruction instruction
    ) {
    }
}
