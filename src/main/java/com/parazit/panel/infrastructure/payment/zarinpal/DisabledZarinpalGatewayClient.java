package com.parazit.panel.infrastructure.payment.zarinpal;

import com.parazit.panel.application.payment.zarinpal.ZarinpalDisabledException;
import com.parazit.panel.application.payment.zarinpal.model.ZarinpalCreateRequest;
import com.parazit.panel.application.payment.zarinpal.model.ZarinpalCreateResponse;
import com.parazit.panel.application.payment.zarinpal.model.ZarinpalVerifyRequest;
import com.parazit.panel.application.payment.zarinpal.model.ZarinpalVerifyResponse;
import com.parazit.panel.application.port.out.payment.zarinpal.ZarinpalGatewayClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.payment.zarinpal", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DisabledZarinpalGatewayClient implements ZarinpalGatewayClient {

    @Override
    public ZarinpalCreateResponse createPayment(ZarinpalCreateRequest request) {
        throw new ZarinpalDisabledException();
    }

    @Override
    public ZarinpalVerifyResponse verifyPayment(ZarinpalVerifyRequest request) {
        throw new ZarinpalDisabledException();
    }
}
