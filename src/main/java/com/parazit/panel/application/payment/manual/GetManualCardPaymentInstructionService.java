package com.parazit.panel.application.payment.manual;

import com.parazit.panel.application.payment.manual.query.GetManualCardPaymentInstructionQuery;
import com.parazit.panel.application.payment.manual.result.ManualCardPaymentInstructionResult;
import com.parazit.panel.application.port.in.payment.manual.GetManualCardPaymentInstructionUseCase;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class GetManualCardPaymentInstructionService implements GetManualCardPaymentInstructionUseCase {

    private final ManualPaymentLookupTransaction lookupTransaction;
    private final ManualCardPaymentResultMapper mapper;

    public GetManualCardPaymentInstructionService(
            ManualPaymentLookupTransaction lookupTransaction,
            ManualCardPaymentResultMapper mapper
    ) {
        this.lookupTransaction = Objects.requireNonNull(lookupTransaction, "lookupTransaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public ManualCardPaymentInstructionResult getCurrent(GetManualCardPaymentInstructionQuery query) {
        ManualCardPaymentReservationResult result = lookupTransaction.getCurrent(query);
        return mapper.toInstructionResult(result.payment(), result.instruction(), result.destination(), false);
    }
}
