package com.parazit.panel.application.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parazit.panel.application.payment.command.CreatePaymentCommand;
import com.parazit.panel.application.payment.command.PaymentInitializationCommand;
import com.parazit.panel.application.payment.command.PaymentVerificationCommand;
import com.parazit.panel.application.payment.result.PaymentInitializationResult;
import com.parazit.panel.application.payment.result.PaymentResult;
import com.parazit.panel.application.payment.result.PaymentVerificationResult;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.port.out.payment.PaymentProcessor;
import com.parazit.panel.config.properties.PaymentProperties;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.payment.PaymentOperation;
import com.parazit.panel.domain.payment.repository.PaymentOperationRepository;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PaymentServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-12T12:00:00Z");

    private OrderRepository orderRepository;
    private PaymentRepository paymentRepository;
    private PaymentOperationRepository operationRepository;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        orderRepository = org.mockito.Mockito.mock(OrderRepository.class);
        paymentRepository = org.mockito.Mockito.mock(PaymentRepository.class);
        operationRepository = org.mockito.Mockito.mock(PaymentOperationRepository.class);
        SystemClockPort clock = () -> NOW;
        PaymentFactory factory = new PaymentFactory(new PaymentProperties(false, "", Duration.ofMinutes(30)), clock);
        PaymentResultMapper mapper = new PaymentResultMapper();
        service = new PaymentService(
                orderRepository,
                paymentRepository,
                operationRepository,
                factory,
                mapper,
                clock,
                List.of(new TestPaymentProcessor())
        );
    }

    @Test
    void createsPaymentForExistingMatchingOrder() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Order order = Order.create(userId, 250_000L, "IRT");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            ReflectionTestUtils.setField(payment, "id", UUID.randomUUID());
            return payment;
        });
        when(operationRepository.save(any(PaymentOperation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResult result = service.create(new CreatePaymentCommand(
                orderId,
                userId,
                PaymentMethod.ZARINPAL,
                250_000L,
                "IRT"
        ));

        assertThat(result.orderId()).isEqualTo(orderId);
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.method()).isEqualTo(PaymentMethod.ZARINPAL);
        assertThat(result.baseAmount()).isEqualTo(250_000L);
        assertThat(result.payableAmount()).isEqualTo(250_000L);
        assertThat(result.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(30)));
        verify(operationRepository).save(any(PaymentOperation.class));
    }

    @Test
    void rejectsMissingOrderApprovedDuplicateAndMismatchedOrder() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> service.create(command(orderId, userId)))
                .isInstanceOf(PaymentOrderNotFoundException.class);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(Order.create(UUID.randomUUID(), 100L, "IRT")));
        assertThatThrownBy(() -> service.create(command(orderId, userId)))
                .isInstanceOf(PaymentConflictException.class)
                .hasMessageContaining("user");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(Order.create(userId, 101L, "IRT")));
        assertThatThrownBy(() -> service.create(command(orderId, userId)))
                .isInstanceOf(PaymentConflictException.class)
                .hasMessageContaining("amount");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(Order.create(userId, 100L, "IRT")));
        when(paymentRepository.existsApprovedPaymentForOrder(orderId)).thenReturn(true);
        assertThatThrownBy(() -> service.create(command(orderId, userId)))
                .isInstanceOf(PaymentConflictException.class)
                .hasMessageContaining("approved");
    }

    @Test
    void selectsProcessorWithoutBusinessSwitch() {
        assertThat(service.processorFor(PaymentMethod.ZARINPAL).supportedMethod()).isEqualTo(PaymentMethod.ZARINPAL);
        assertThatThrownBy(() -> new PaymentService(
                orderRepository,
                paymentRepository,
                operationRepository,
                new PaymentFactory(new PaymentProperties(false, "", Duration.ofMinutes(30)), () -> NOW),
                new PaymentResultMapper(),
                () -> NOW,
                List.of()
        ).processorFor(PaymentMethod.CARD_TO_CARD))
                .isInstanceOf(PaymentProcessorNotFoundException.class);
    }

    private CreatePaymentCommand command(UUID orderId, UUID userId) {
        return new CreatePaymentCommand(orderId, userId, PaymentMethod.ZARINPAL, 100L, "IRT");
    }

    private static final class TestPaymentProcessor implements PaymentProcessor {

        @Override
        public PaymentMethod supportedMethod() {
            return PaymentMethod.ZARINPAL;
        }

        @Override
        public PaymentInitializationResult initiate(PaymentInitializationCommand command) {
            return new PaymentInitializationResult(command.paymentId(), supportedMethod(), true, null, null, null);
        }

        @Override
        public PaymentVerificationResult verify(PaymentVerificationCommand command) {
            return new PaymentVerificationResult(command.paymentId(), supportedMethod(), true, null, null);
        }
    }
}
