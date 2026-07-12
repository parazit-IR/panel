package com.parazit.panel.infrastructure.payment.manual;

import com.parazit.panel.application.port.out.payment.manual.ManualPaymentSuffixGenerator;
import java.security.SecureRandom;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class SecureManualPaymentSuffixGenerator implements ManualPaymentSuffixGenerator {

    private final SecureRandom secureRandom;

    public SecureManualPaymentSuffixGenerator() {
        this(new SecureRandom());
    }

    SecureManualPaymentSuffixGenerator(SecureRandom secureRandom) {
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom must not be null");
    }

    @Override
    public long generate(long minimumInclusive, long maximumInclusive) {
        if (minimumInclusive <= 0) {
            throw new IllegalArgumentException("minimumInclusive must be positive");
        }
        if (maximumInclusive < minimumInclusive) {
            throw new IllegalArgumentException("maximumInclusive must be greater than or equal to minimumInclusive");
        }
        long bound = Math.subtractExact(maximumInclusive, minimumInclusive) + 1;
        return minimumInclusive + secureRandom.nextLong(bound);
    }
}
