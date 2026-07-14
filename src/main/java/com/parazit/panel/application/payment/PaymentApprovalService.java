package com.parazit.panel.application.payment;

import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.application.port.in.wallet.topup.HandleApprovedWalletTopUpUseCase;
import com.parazit.panel.application.port.in.promotion.FinalizeDiscountRedemptionUseCase;
import com.parazit.panel.application.promotion.command.FinalizeDiscountRedemptionCommand;
import com.parazit.panel.application.wallet.topup.command.HandleApprovedWalletTopUpCommand;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentApprovalService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderPaymentApprovedDispatcher dispatcher;
    private final HandleApprovedWalletTopUpUseCase walletTopUpHandler;
    private final FinalizeDiscountRedemptionUseCase finalizeDiscountRedemptionUseCase;

    public PaymentApprovalService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            OrderPaymentApprovedDispatcher dispatcher,
            HandleApprovedWalletTopUpUseCase walletTopUpHandler,
            FinalizeDiscountRedemptionUseCase finalizeDiscountRedemptionUseCase
    ) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher must not be null");
        this.walletTopUpHandler = Objects.requireNonNull(walletTopUpHandler, "walletTopUpHandler must not be null");
        this.finalizeDiscountRedemptionUseCase = Objects.requireNonNull(finalizeDiscountRedemptionUseCase, "finalizeDiscountRedemptionUseCase must not be null");
    }

    @Transactional
    public PaymentApprovalResult approve(ApprovePaymentCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Payment payment = paymentRepository.findByIdForUpdate(command.paymentId())
                .orElseThrow(() -> new PaymentNotFoundException(command.paymentId()));
        if (payment.targetsWalletTopUp()) {
            return approveWalletTopUp(command, payment);
        }
        Order order = orderRepository.findByIdForUpdate(payment.getOrderId())
                .orElseThrow(() -> new PaymentOrderNotFoundException(payment.getOrderId()));
        if (!payment.getUserId().equals(order.getUserId())) {
            throw new PaymentApprovalException("Payment owner does not match order owner");
        }

        if (payment.getStatus() == PaymentStatus.APPROVED) {
            assertSameReference(payment, command.providerReference());
            finalizeDiscount(payment, order);
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
        finalizeDiscount(payment, order);

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

    private PaymentApprovalResult approveWalletTopUp(ApprovePaymentCommand command, Payment payment) {
        if (payment.getWalletTopUpRequestId() == null) {
            throw new PaymentApprovalException("Wallet top-up payment target is missing");
        }
        if (payment.getStatus() == PaymentStatus.APPROVED) {
            assertSameReference(payment, command.providerReference());
            walletTopUpHandler.handle(new HandleApprovedWalletTopUpCommand(
                    payment.getId(),
                    payment.getWalletTopUpRequestId(),
                    approvalRequestId(command, payment)
            ));
            return new PaymentApprovalResult(
                    payment.getId(),
                    null,
                    payment.getStatus(),
                    null,
                    null,
                    false,
                    false
            );
        }
        if (paymentRepository.existsApprovedPaymentForWalletTopUpRequest(payment.getWalletTopUpRequestId())) {
            throw new PaymentApprovalException("Wallet top-up already has an approved payment");
        }
        payment.markApproved(command.approvedAt(), command.providerReference(), command.providerAuthority());
        paymentRepository.save(payment);
        walletTopUpHandler.handle(new HandleApprovedWalletTopUpCommand(
                payment.getId(),
                payment.getWalletTopUpRequestId(),
                approvalRequestId(command, payment)
        ));
        return new PaymentApprovalResult(
                payment.getId(),
                null,
                payment.getStatus(),
                null,
                null,
                true,
                false
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

    private static UUID approvalRequestId(ApprovePaymentCommand command, Payment payment) {
        String raw = "wallet-top-up-approval:"
                + payment.getId()
                + ':'
                + command.source()
                + ':'
                + (command.providerReference() == null ? "" : command.providerReference());
        return UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8));
    }

    private void finalizeDiscount(Payment payment, Order order) {
        if (order.getAppliedDiscountCodeId() == null) {
            return;
        }
        finalizeDiscountRedemptionUseCase.finalizeDiscount(new FinalizeDiscountRedemptionCommand(
                order.getId(),
                payment.getId(),
                UUID.nameUUIDFromBytes(("discount-finalize:" + order.getId() + ":" + payment.getId()).getBytes(StandardCharsets.UTF_8))
        ));
    }
}
