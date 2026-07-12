package com.parazit.panel.application.payment.zarinpal;

import com.parazit.panel.application.payment.zarinpal.command.HandleZarinpalCallbackCommand;
import com.parazit.panel.application.payment.zarinpal.model.ZarinpalVerifyRequest;
import com.parazit.panel.application.payment.zarinpal.model.ZarinpalVerifyResponse;
import com.parazit.panel.application.payment.zarinpal.result.HandleZarinpalCallbackResult;
import com.parazit.panel.application.port.in.payment.zarinpal.HandleZarinpalCallbackUseCase;
import com.parazit.panel.application.port.out.payment.zarinpal.ZarinpalGatewayClient;
import com.parazit.panel.config.properties.ZarinpalProperties;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.zarinpal.ZarinpalCallbackOutcome;
import com.parazit.panel.domain.payment.zarinpal.ZarinpalPaymentAttempt;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class VerifyZarinpalPaymentService implements HandleZarinpalCallbackUseCase {

    private final ZarinpalProperties properties;
    private final PrepareZarinpalVerificationTransaction prepareTransaction;
    private final CompleteZarinpalVerificationTransaction completeTransaction;
    private final ZarinpalGatewayClient gatewayClient;

    public VerifyZarinpalPaymentService(
            ZarinpalProperties properties,
            PrepareZarinpalVerificationTransaction prepareTransaction,
            CompleteZarinpalVerificationTransaction completeTransaction,
            ZarinpalGatewayClient gatewayClient
    ) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.prepareTransaction = Objects.requireNonNull(prepareTransaction, "prepareTransaction must not be null");
        this.completeTransaction = Objects.requireNonNull(completeTransaction, "completeTransaction must not be null");
        this.gatewayClient = Objects.requireNonNull(gatewayClient, "gatewayClient must not be null");
    }

    @Override
    public HandleZarinpalCallbackResult handle(HandleZarinpalCallbackCommand command) {
        if (!properties.enabled()) {
            throw new ZarinpalDisabledException();
        }
        PreparedZarinpalVerification prepared = prepareTransaction.prepare(command);
        if (prepared.alreadyApproved()) {
            return toResult(prepared.payment(), prepared.attempt(), ZarinpalCallbackOutcome.ALREADY_APPROVED, false);
        }
        if (prepared.cancelled()) {
            return toResult(prepared.payment(), prepared.attempt(), ZarinpalCallbackOutcome.CANCELLED, false);
        }

        try {
            ZarinpalVerifyResponse response = gatewayClient.verifyPayment(new ZarinpalVerifyRequest(
                    properties.merchantId(),
                    prepared.attempt().getGatewayAmount(),
                    prepared.attempt().getAuthority()
            ));
            if (response.successful() || response.alreadyVerified()) {
                CompletedZarinpalVerification completed = completeTransaction.approve(
                        prepared.payment().getId(),
                        prepared.attempt().getId(),
                        response
                );
                return toResult(completed.payment(), completed.attempt(), ZarinpalCallbackOutcome.APPROVED, true);
            }
            CompletedZarinpalVerification failed = completeTransaction.fail(
                    prepared.payment().getId(),
                    prepared.attempt().getId(),
                    String.valueOf(response.code()),
                    response.message()
            );
            return toResult(failed.payment(), failed.attempt(), ZarinpalCallbackOutcome.FAILED, false);
        } catch (ZarinpalVerificationFailedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            CompletedZarinpalVerification unknown = completeTransaction.unknown(
                    prepared.payment().getId(),
                    prepared.attempt().getId(),
                    "UNKNOWN",
                    "Zarinpal verification outcome is unknown"
            );
            return toResult(unknown.payment(), unknown.attempt(), ZarinpalCallbackOutcome.UNKNOWN, false);
        }
    }

    private HandleZarinpalCallbackResult toResult(
            Payment payment,
            ZarinpalPaymentAttempt attempt,
            ZarinpalCallbackOutcome outcome,
            boolean newlyVerified
    ) {
        return new HandleZarinpalCallbackResult(
                payment.getId(),
                attempt.getId(),
                payment.getStatus(),
                attempt.getStatus(),
                outcome,
                attempt.getReferenceId(),
                newlyVerified
        );
    }
}
