package com.parazit.panel.application.port.out.payment.manual;

import com.parazit.panel.domain.payment.manual.ManualPaymentDestination;
import java.util.Optional;

public interface ManualPaymentDestinationProvider {

    Optional<ManualPaymentDestination> firstActiveDestination();
}
