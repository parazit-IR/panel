package com.parazit.panel.infrastructure.payment.zarinpal.dto;

import java.util.List;

public record ZarinpalResponseEnvelopeRemoteDto<T>(
        T data,
        List<ZarinpalErrorRemoteDto> errors
) {
}
