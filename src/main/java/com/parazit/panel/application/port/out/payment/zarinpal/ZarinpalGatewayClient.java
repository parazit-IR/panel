package com.parazit.panel.application.port.out.payment.zarinpal;

import com.parazit.panel.application.payment.zarinpal.model.ZarinpalCreateRequest;
import com.parazit.panel.application.payment.zarinpal.model.ZarinpalCreateResponse;
import com.parazit.panel.application.payment.zarinpal.model.ZarinpalVerifyRequest;
import com.parazit.panel.application.payment.zarinpal.model.ZarinpalVerifyResponse;

public interface ZarinpalGatewayClient {

    ZarinpalCreateResponse createPayment(ZarinpalCreateRequest request);

    ZarinpalVerifyResponse verifyPayment(ZarinpalVerifyRequest request);
}
