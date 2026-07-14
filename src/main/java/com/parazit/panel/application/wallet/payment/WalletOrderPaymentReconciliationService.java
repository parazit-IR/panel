package com.parazit.panel.application.wallet.payment;

import com.parazit.panel.application.port.in.wallet.payment.ReconcileWalletOrderPaymentUseCase;
import com.parazit.panel.application.wallet.payment.command.ReconcileWalletOrderPaymentCommand;
import com.parazit.panel.application.wallet.payment.result.WalletOrderPaymentReconciliationResult;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.OrderStatus;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.wallet.WalletTransaction;
import com.parazit.panel.domain.wallet.WalletTransactionDirection;
import com.parazit.panel.domain.wallet.WalletTransactionType;
import com.parazit.panel.domain.wallet.repository.WalletTransactionRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletOrderPaymentReconciliationService implements ReconcileWalletOrderPaymentUseCase {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final WalletTransactionRepository transactionRepository;

    public WalletOrderPaymentReconciliationService(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            WalletTransactionRepository transactionRepository
    ) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.transactionRepository = Objects.requireNonNull(transactionRepository, "transactionRepository must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public WalletOrderPaymentReconciliationResult reconcile(ReconcileWalletOrderPaymentCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new WalletOrderPaymentException("order not found"));
        Payment payment = paymentRepository.findAllByOrderId(order.getId()).stream()
                .filter(candidate -> candidate.getMethod() == PaymentMethod.WALLET)
                .filter(candidate -> candidate.getStatus() == PaymentStatus.APPROVED)
                .findFirst()
                .orElse(null);
        WalletTransaction transaction = payment == null || payment.getWalletTransactionId() == null
                ? null
                : transactionRepository.findById(payment.getWalletTransactionId()).orElse(null);
        boolean paymentExists = payment != null;
        boolean transactionExists = transaction != null;
        boolean orderPaid = order.getStatus() == OrderStatus.PAID
                || order.getStatus() == OrderStatus.PROVISIONING
                || order.getStatus() == OrderStatus.COMPLETED
                || order.getStatus() == OrderStatus.RENEWAL_PENDING;
        boolean amountMatches = transactionExists
                && transaction.getAmount() == order.getFinalAmount()
                && transaction.getCurrency().equalsIgnoreCase(order.getCurrency())
                && payment.getBaseAmount() == order.getFinalAmount()
                && payment.getCurrency().equalsIgnoreCase(order.getCurrency());
        boolean userMatches = transactionExists
                && transaction.getUserId().equals(order.getUserId())
                && payment.getUserId().equals(order.getUserId());
        boolean ledgerMatches = transactionExists
                && transaction.getType() == WalletTransactionType.PURCHASE
                && transaction.getDirection() == WalletTransactionDirection.DEBIT
                && PayOrderWithWalletService.REFERENCE_TYPE.equals(transaction.getReferenceType())
                && order.getId().equals(transaction.getReferenceId());
        boolean consistent = paymentExists && transactionExists && orderPaid && amountMatches && userMatches && ledgerMatches;
        return new WalletOrderPaymentReconciliationResult(
                order.getId(),
                paymentExists,
                transactionExists,
                orderPaid,
                amountMatches,
                userMatches,
                consistent
        );
    }
}
