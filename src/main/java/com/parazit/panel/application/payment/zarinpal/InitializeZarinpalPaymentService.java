package com.parazit.panel.application.payment.zarinpal;

import com.parazit.panel.application.payment.zarinpal.command.InitializeZarinpalPaymentCommand;
import com.parazit.panel.application.payment.zarinpal.model.ZarinpalCreateRequest;
import com.parazit.panel.application.payment.zarinpal.model.ZarinpalCreateResponse;
import com.parazit.panel.application.payment.zarinpal.result.InitializeZarinpalPaymentResult;
import com.parazit.panel.application.port.in.payment.zarinpal.InitializeZarinpalPaymentUseCase;
import com.parazit.panel.application.port.out.payment.zarinpal.ZarinpalGatewayClient;
import com.parazit.panel.config.properties.ZarinpalProperties;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.zarinpal.ZarinpalPaymentAttempt;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InitializeZarinpalPaymentService implements InitializeZarinpalPaymentUseCase {

    private final ZarinpalProperties properties;
    private final PrepareZarinpalRequestTransaction prepareTransaction;
    private final CompleteZarinpalRequestTransaction completeTransaction;
    private final ZarinpalAmountConverter amountConverter;
    private final ZarinpalGatewayClient gatewayClient;

    public InitializeZarinpalPaymentService(
            ZarinpalProperties properties,
            PrepareZarinpalRequestTransaction prepareTransaction,
            CompleteZarinpalRequestTransaction completeTransaction,
            ZarinpalAmountConverter amountConverter,
            ZarinpalGatewayClient gatewayClient
    ) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.prepareTransaction = Objects.requireNonNull(prepareTransaction, "prepareTransaction must not be null");
        this.completeTransaction = Objects.requireNonNull(completeTransaction, "completeTransaction must not be null");
        this.amountConverter = Objects.requireNonNull(amountConverter, "amountConverter must not be null");
        this.gatewayClient = Objects.requireNonNull(gatewayClient, "gatewayClient must not be null");
    }

    @Override
    public InitializeZarinpalPaymentResult initialize(InitializeZarinpalPaymentCommand command) {
        if (!properties.enabled()) {
            throw new ZarinpalDisabledException();
        }
        PreparedZarinpalRequest prepared = prepareTransaction.prepare(command);
        if (prepared.replay()) {
            return toResult(prepared.payment(), prepared.attempt(), false);
        }

        Payment payment = prepared.payment();
        ZarinpalPaymentAttempt attempt = prepared.attempt();
        try {
            ZarinpalCreateResponse response = gatewayClient.createPayment(new ZarinpalCreateRequest(
                    properties.merchantId(),
                    attempt.getGatewayAmount(),
                    amountConverter.gatewayCurrency(payment.getCurrency()),
                    properties.callbackUrl().toString(),
                    command.description().trim(),
                    normalizeOptional(command.mobile()),
                    normalizeOptional(command.email())
            ));
            if (!response.successful()) {
                completeTransaction.markFailed(attempt.getId(), String.valueOf(response.code()), response.message());
                throw new ZarinpalRequestFailedException("Zarinpal rejected the payment request");
            }
            ZarinpalPaymentAttempt completed = completeTransaction.markReady(payment.getId(), attempt.getId(), response);
            payment.markWaitingForPayment();
            return toResult(payment, completed, true);
        } catch (ZarinpalRequestFailedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            completeTransaction.markUnknown(attempt.getId(), "UNKNOWN", "Zarinpal payment request outcome is unknown");
            throw new ZarinpalRequestUnknownException("Zarinpal payment request outcome is unknown");
        }
    }

    private InitializeZarinpalPaymentResult toResult(
            Payment payment,
            ZarinpalPaymentAttempt attempt,
            boolean newlyInitialized
    ) {
        String paymentUrl = attempt.getAuthority() == null ? null : properties.startPayUrl(attempt.getAuthority()).toString();
        return new InitializeZarinpalPaymentResult(
                payment.getId(),
                attempt.getId(),
                attempt.getRequestId(),
                payment.getStatus(),
                attempt.getStatus(),
                attempt.getAuthority(),
                paymentUrl,
                payment.getExpiresAt(),
                newlyInitialized
        );
    }

    private String normalizeOptional(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }
}
