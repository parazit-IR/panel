package com.parazit.panel.infrastructure.payment.manual;

import com.parazit.panel.application.port.out.payment.manual.ManualPaymentDestinationProvider;
import com.parazit.panel.config.properties.ManualPaymentProperties;
import com.parazit.panel.domain.payment.manual.BankCardNumber;
import com.parazit.panel.domain.payment.manual.ManualPaymentDestination;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ConfiguredManualPaymentDestinationProvider implements ManualPaymentDestinationProvider {

    private final ManualPaymentProperties properties;

    public ConfiguredManualPaymentDestinationProvider(ManualPaymentProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public Optional<ManualPaymentDestination> firstActiveDestination() {
        if (!properties.enabled()) {
            return Optional.empty();
        }
        return Optional.of(new ManualPaymentDestination(
                properties.destinationId(),
                properties.bankName(),
                properties.cardHolderName(),
                BankCardNumber.parse(properties.cardNumber()),
                true,
                0
        ));
    }
}
