package com.parazit.panel.application.payment.manual;

import com.parazit.panel.application.payment.manual.command.InitializeManualCardPaymentCommand;
import com.parazit.panel.application.payment.manual.result.InitializeManualCardPaymentResult;
import com.parazit.panel.application.port.in.payment.manual.InitializeManualCardPaymentUseCase;
import com.parazit.panel.application.port.out.payment.manual.ManualPaymentSuffixGenerator;
import com.parazit.panel.config.properties.ManualPaymentProperties;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class InitializeManualCardPaymentService implements InitializeManualCardPaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(InitializeManualCardPaymentService.class);

    private final ManualPaymentProperties properties;
    private final ManualPaymentSuffixGenerator suffixGenerator;
    private final ManualCardPaymentReservationTransaction reservationTransaction;
    private final ManualCardPaymentResultMapper mapper;

    public InitializeManualCardPaymentService(
            ManualPaymentProperties properties,
            ManualPaymentSuffixGenerator suffixGenerator,
            ManualCardPaymentReservationTransaction reservationTransaction,
            ManualCardPaymentResultMapper mapper
    ) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.suffixGenerator = Objects.requireNonNull(suffixGenerator, "suffixGenerator must not be null");
        this.reservationTransaction = Objects.requireNonNull(reservationTransaction, "reservationTransaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public InitializeManualCardPaymentResult initialize(InitializeManualCardPaymentCommand command) {
        if (!properties.enabled()) {
            throw new ManualCardPaymentDisabledException();
        }
        RuntimeException lastConflict = null;
        for (int attempt = 1; attempt <= properties.maxGenerationAttempts(); attempt++) {
            long suffix = suffixGenerator.generate(properties.minimumSuffix(), properties.maximumSuffix());
            try {
                ManualCardPaymentReservationResult result = reservationTransaction.reserve(command, suffix);
                if (result.newlyInitialized()) {
                    log.info(
                            "Manual card payment instruction created paymentId={} instructionId={} attempt={}",
                            result.payment().getId(),
                            result.instruction().getId(),
                            attempt
                    );
                } else {
                    log.info(
                            "Manual card payment instruction returned idempotently paymentId={} instructionId={}",
                            result.payment().getId(),
                            result.instruction().getId()
                    );
                }
                return mapper.toInitializeResult(
                        result.payment(),
                        result.instruction(),
                        result.destination(),
                        result.newlyInitialized()
                );
            } catch (DataIntegrityViolationException exception) {
                lastConflict = exception;
                log.debug("Manual payment amount reservation conflict attempt={}", attempt);
            }
        }
        if (lastConflict != null) {
            throw new ManualPaymentAmountReservationException();
        }
        throw new ManualPaymentAmountReservationException();
    }
}
