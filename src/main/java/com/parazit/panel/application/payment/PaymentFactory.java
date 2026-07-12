package com.parazit.panel.application.payment;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.config.properties.PaymentProperties;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentMethod;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PaymentFactory {

    private final PaymentProperties properties;
    private final SystemClockPort clock;

    public PaymentFactory(PaymentProperties properties, SystemClockPort clock) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public Payment create(UUID orderId, UUID userId, PaymentMethod method, long amount, String currency) {
        Instant expiresAt = clock.now().plus(properties.defaultExpiration());
        long payableAmount = calculatePayableAmount(amount, method);
        return Payment.create(orderId, userId, method, amount, payableAmount, currency, expiresAt);
    }

    private long calculatePayableAmount(long baseAmount, PaymentMethod method) {
        Objects.requireNonNull(method, "method must not be null");
        return baseAmount;
    }
}
