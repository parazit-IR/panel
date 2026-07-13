package com.parazit.panel.application.port.in.renewal;

import com.parazit.panel.application.renewal.command.GetRenewalPreInvoiceCommand;
import com.parazit.panel.application.renewal.result.RenewalPreInvoiceResult;

public interface GetRenewalPreInvoiceUseCase {

    RenewalPreInvoiceResult get(GetRenewalPreInvoiceCommand command);
}
