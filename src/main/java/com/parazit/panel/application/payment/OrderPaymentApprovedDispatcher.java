package com.parazit.panel.application.payment;

import com.parazit.panel.application.renewal.HandleApprovedRenewalOrderService;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.OrderType;
import com.parazit.panel.domain.payment.Payment;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class OrderPaymentApprovedDispatcher {

    private final NewSubscriptionPaidOrderHandler newSubscriptionHandler;
    private final HandleApprovedRenewalOrderService renewalHandler;

    public OrderPaymentApprovedDispatcher(
            NewSubscriptionPaidOrderHandler newSubscriptionHandler,
            HandleApprovedRenewalOrderService renewalHandler
    ) {
        this.newSubscriptionHandler = Objects.requireNonNull(newSubscriptionHandler, "newSubscriptionHandler must not be null");
        this.renewalHandler = Objects.requireNonNull(renewalHandler, "renewalHandler must not be null");
    }

    public ApprovedOrderDispatchResult dispatch(ApprovePaymentCommand command, Payment payment, Order order) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(payment, "payment must not be null");
        Objects.requireNonNull(order, "order must not be null");
        if (!payment.getOrderId().equals(order.getId())) {
            throw new PaymentApprovalException("Payment order does not match approved order");
        }
        if (order.getType() == OrderType.NEW_SUBSCRIPTION) {
            return newSubscriptionHandler.handle(command, payment, order);
        }
        if (order.getType() == OrderType.RENEWAL) {
            renewalHandler.handleApproved(command, payment, order);
            return ApprovedOrderDispatchResult.none();
        }
        throw new PaymentApprovalException("Unsupported paid order type");
    }
}
