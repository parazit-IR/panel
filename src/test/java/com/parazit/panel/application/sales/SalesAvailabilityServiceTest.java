package com.parazit.panel.application.sales;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.payment.command.PaymentInitializationCommand;
import com.parazit.panel.application.payment.command.PaymentVerificationCommand;
import com.parazit.panel.application.payment.result.PaymentInitializationResult;
import com.parazit.panel.application.payment.result.PaymentVerificationResult;
import com.parazit.panel.application.port.out.payment.PaymentProcessor;
import com.parazit.panel.config.properties.ManualPaymentProperties;
import com.parazit.panel.config.properties.SalesControlProperties;
import com.parazit.panel.config.properties.WalletPaymentProperties;
import com.parazit.panel.config.properties.ZarinpalProperties;
import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.payment.PaymentMethod;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class SalesAvailabilityServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-13T12:00:00Z");

    @Test
    void combinesSalesFlagsProviderConfigurationAndRegisteredProcessors() {
        SalesAvailabilityService service = service(
                new SalesControlProperties(true, false, false, true, true, false, false, false, "", "", "", null),
                manual(true),
                zarinpal(true),
                List.of(processor(PaymentMethod.CARD_TO_CARD), processor(PaymentMethod.ZARINPAL))
        );

        assertThat(service.availability(SalesCapability.NEW_PURCHASE).enabled()).isTrue();
        assertThat(service.availability(SalesCapability.MANUAL_PAYMENT).enabled()).isTrue();
        assertThat(service.availability(SalesCapability.ONLINE_PAYMENT).enabled()).isTrue();
        assertThat(service.availability(SalesCapability.DISCOUNT_CODE).enabled()).isFalse();
        assertThat(service.availability(SalesCapability.DISCOUNT_CODE).unavailableMessageKey())
                .isEqualTo("telegram.purchase.discount_unavailable");
    }

    @Test
    void doesNotAdvertisePaymentMethodWhenProviderOrProcessorIsMissing() {
        SalesAvailabilityService service = service(
                new SalesControlProperties(true, false, false, true, true, false, false, false, "", "", "", null),
                manual(false),
                zarinpal(true),
                List.of(processor(PaymentMethod.CARD_TO_CARD))
        );

        assertThat(service.availability(SalesCapability.MANUAL_PAYMENT).enabled()).isFalse();
        assertThat(service.availability(SalesCapability.ONLINE_PAYMENT).enabled()).isFalse();
    }

    private static SalesAvailabilityService service(
            SalesControlProperties sales,
            ManualPaymentProperties manual,
            ZarinpalProperties zarinpal,
            List<PaymentProcessor> processors
    ) {
        return new SalesAvailabilityService(sales, manual, zarinpal,
                new WalletPaymentProperties(true, true, true, CurrencyCode.IRT, 0, 0, 3, Duration.ofMinutes(15)),
                processors, () -> NOW);
    }

    private static ManualPaymentProperties manual(boolean enabled) {
        return new ManualPaymentProperties(
                enabled,
                Duration.ofMinutes(30),
                101,
                4999,
                20,
                "PRIMARY",
                "Bank",
                "Holder",
                enabled ? "6037990000000014" : "",
                true,
                Duration.ofMinutes(2)
        );
    }

    private static ZarinpalProperties zarinpal(boolean enabled) {
        return new ZarinpalProperties(
                enabled,
                enabled ? "merchant" : "",
                URI.create("https://api.zarinpal.com"),
                URI.create("https://www.zarinpal.com/pg/StartPay"),
                "/pg/v4/payment/request.json",
                "/pg/v4/payment/verify.json",
                URI.create("https://example.com/callback"),
                URI.create("https://example.com/success"),
                URI.create("https://example.com/failure"),
                URI.create("https://example.com/cancel"),
                Duration.ofSeconds(5),
                Duration.ofSeconds(20),
                0,
                Duration.ofSeconds(1),
                false,
                true
        );
    }

    private static PaymentProcessor processor(PaymentMethod method) {
        return new PaymentProcessor() {
            @Override
            public PaymentMethod supportedMethod() {
                return method;
            }

            @Override
            public PaymentInitializationResult initiate(PaymentInitializationCommand command) {
                return null;
            }

            @Override
            public PaymentVerificationResult verify(PaymentVerificationCommand command) {
                return null;
            }
        };
    }
}
