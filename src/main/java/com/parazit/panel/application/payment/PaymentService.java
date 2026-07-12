package com.parazit.panel.application.payment;

import com.parazit.panel.application.payment.command.CreatePaymentCommand;
import com.parazit.panel.application.payment.result.PaymentResult;
import com.parazit.panel.application.port.in.payment.CreatePaymentUseCase;
import com.parazit.panel.application.port.in.payment.GetPaymentUseCase;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.port.out.payment.PaymentProcessor;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.payment.PaymentOperation;
import com.parazit.panel.domain.payment.PaymentOperationType;
import com.parazit.panel.domain.payment.repository.PaymentOperationRepository;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService implements CreatePaymentUseCase, GetPaymentUseCase {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentOperationRepository operationRepository;
    private final PaymentFactory paymentFactory;
    private final PaymentResultMapper mapper;
    private final SystemClockPort clock;
    private final Map<PaymentMethod, PaymentProcessor> processors;

    public PaymentService(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            PaymentOperationRepository operationRepository,
            PaymentFactory paymentFactory,
            PaymentResultMapper mapper,
            SystemClockPort clock,
            List<PaymentProcessor> processors
    ) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.operationRepository = Objects.requireNonNull(operationRepository, "operationRepository must not be null");
        this.paymentFactory = Objects.requireNonNull(paymentFactory, "paymentFactory must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.processors = buildProcessorMap(processors);
    }

    @Override
    @Transactional
    public PaymentResult create(CreatePaymentCommand command) {
        CreatePaymentCommand required = requireCommand(command);
        Order order = orderRepository.findById(required.orderId())
                .orElseThrow(() -> new PaymentOrderNotFoundException(required.orderId()));
        validateOrder(required, order);
        if (paymentRepository.existsApprovedPaymentForOrder(required.orderId())) {
            throw new PaymentConflictException("Order already has an approved payment");
        }

        Payment payment = paymentFactory.create(
                required.orderId(),
                required.userId(),
                required.paymentMethod(),
                required.amount(),
                required.currency()
        );
        Payment saved = paymentRepository.save(payment);
        operationRepository.save(PaymentOperation.record(
                saved.getId(),
                PaymentOperationType.CREATED,
                clock.now(),
                "Payment record created"
        ));

        return mapper.toResult(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResult getById(UUID paymentId) {
        return mapper.toResult(paymentRepository.findById(Objects.requireNonNull(paymentId, "paymentId must not be null"))
                .orElseThrow(() -> new PaymentNotFoundException(paymentId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResult> listByOrderId(UUID orderId) {
        UUID requiredOrderId = Objects.requireNonNull(orderId, "orderId must not be null");
        if (!orderRepository.existsById(requiredOrderId)) {
            throw new PaymentOrderNotFoundException(requiredOrderId);
        }
        return paymentRepository.findAllByOrderId(requiredOrderId)
                .stream()
                .map(mapper::toResult)
                .toList();
    }

    public PaymentProcessor processorFor(PaymentMethod method) {
        PaymentMethod requiredMethod = Objects.requireNonNull(method, "method must not be null");
        PaymentProcessor processor = processors.get(requiredMethod);
        if (processor == null) {
            throw new PaymentProcessorNotFoundException(requiredMethod);
        }
        return processor;
    }

    private void validateOrder(CreatePaymentCommand command, Order order) {
        if (!order.getUserId().equals(command.userId())) {
            throw new PaymentConflictException("Payment user does not match order owner");
        }
        if (order.getAmount() != command.amount()) {
            throw new PaymentConflictException("Payment amount does not match order amount");
        }
        if (!order.getCurrency().equalsIgnoreCase(command.currency())) {
            throw new PaymentConflictException("Payment currency does not match order currency");
        }
    }

    private CreatePaymentCommand requireCommand(CreatePaymentCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(command.orderId(), "orderId must not be null");
        Objects.requireNonNull(command.userId(), "userId must not be null");
        Objects.requireNonNull(command.paymentMethod(), "paymentMethod must not be null");
        Objects.requireNonNull(command.currency(), "currency must not be null");
        if (command.amount() < 0) {
            throw new IllegalArgumentException("amount must be zero or positive");
        }
        return command;
    }

    private Map<PaymentMethod, PaymentProcessor> buildProcessorMap(List<PaymentProcessor> processors) {
        List<PaymentProcessor> safeProcessors = Objects.requireNonNull(processors, "processors must not be null");
        Map<PaymentMethod, PaymentProcessor> mapped = safeProcessors.stream()
                .collect(Collectors.toMap(
                        PaymentProcessor::supportedMethod,
                        Function.identity(),
                        (first, second) -> {
                            throw new IllegalStateException("Duplicate payment processor for " + first.supportedMethod());
                        },
                        () -> new EnumMap<>(PaymentMethod.class)
                ));
        return Map.copyOf(mapped);
    }
}
