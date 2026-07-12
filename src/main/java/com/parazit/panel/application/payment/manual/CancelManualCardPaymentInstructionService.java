package com.parazit.panel.application.payment.manual;

import com.parazit.panel.application.payment.manual.command.CancelManualCardPaymentInstructionCommand;
import com.parazit.panel.application.payment.manual.result.ManualCardPaymentInstructionResult;
import com.parazit.panel.application.port.in.payment.manual.CancelManualCardPaymentInstructionUseCase;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class CancelManualCardPaymentInstructionService implements CancelManualCardPaymentInstructionUseCase {

    private final ManualPaymentLookupTransaction lookupTransaction;
    private final ManualCardPaymentResultMapper mapper;

    public CancelManualCardPaymentInstructionService(
            ManualPaymentLookupTransaction lookupTransaction,
            ManualCardPaymentResultMapper mapper
    ) {
        this.lookupTransaction = Objects.requireNonNull(lookupTransaction, "lookupTransaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public ManualCardPaymentInstructionResult cancel(CancelManualCardPaymentInstructionCommand command) {
        ManualCardPaymentReservationResult result = lookupTransaction.cancel(command);
        return mapper.toInstructionResult(result.payment(), result.instruction(), result.destination(), false);
    }
}
