package com.parazit.panel.infrastructure.payment.zarinpal;

import com.parazit.panel.application.payment.zarinpal.ZarinpalResponseInvalidException;
import com.parazit.panel.application.payment.zarinpal.model.ZarinpalCreateRequest;
import com.parazit.panel.application.payment.zarinpal.model.ZarinpalCreateResponse;
import com.parazit.panel.application.payment.zarinpal.model.ZarinpalVerifyRequest;
import com.parazit.panel.application.payment.zarinpal.model.ZarinpalVerifyResponse;
import com.parazit.panel.application.port.out.payment.zarinpal.ZarinpalGatewayClient;
import com.parazit.panel.config.properties.ZarinpalProperties;
import com.parazit.panel.infrastructure.payment.zarinpal.dto.ZarinpalErrorRemoteDto;
import com.parazit.panel.infrastructure.payment.zarinpal.dto.ZarinpalRequestDataRemoteDto;
import com.parazit.panel.infrastructure.payment.zarinpal.dto.ZarinpalRequestRemoteDto;
import com.parazit.panel.infrastructure.payment.zarinpal.dto.ZarinpalResponseEnvelopeRemoteDto;
import com.parazit.panel.infrastructure.payment.zarinpal.dto.ZarinpalVerifyDataRemoteDto;
import com.parazit.panel.infrastructure.payment.zarinpal.dto.ZarinpalVerifyRemoteDto;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "app.payment.zarinpal", name = "enabled", havingValue = "true")
public class RestClientZarinpalGatewayClient implements ZarinpalGatewayClient {

    private final RestClient restClient;
    private final ZarinpalProperties properties;

    public RestClientZarinpalGatewayClient(
            @Qualifier("zarinpalRestClient") RestClient restClient,
            ZarinpalProperties properties
    ) {
        this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public ZarinpalCreateResponse createPayment(ZarinpalCreateRequest request) {
        Map<String, String> metadata = new HashMap<>();
        if (request.mobile() != null) {
            metadata.put("mobile", request.mobile());
        }
        if (request.email() != null) {
            metadata.put("email", request.email());
        }
        ZarinpalRequestRemoteDto body = new ZarinpalRequestRemoteDto(
                request.merchantId(),
                request.amount(),
                request.currency(),
                request.callbackUrl(),
                request.description(),
                metadata.isEmpty() ? null : metadata
        );
        ZarinpalResponseEnvelopeRemoteDto<ZarinpalRequestDataRemoteDto> response = restClient.post()
                .uri(properties.requestPath())
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        ZarinpalRequestDataRemoteDto data = requireData(response);
        int code = data.code() == null ? firstErrorCode(response) : data.code();
        boolean successful = code == 100 && data.authority() != null && !data.authority().isBlank();
        String paymentUrl = successful ? properties.startPayUrl(data.authority().trim()).toString() : null;
        return new ZarinpalCreateResponse(successful, data.authority(), code, sanitize(data.message(), response), paymentUrl);
    }

    @Override
    public ZarinpalVerifyResponse verifyPayment(ZarinpalVerifyRequest request) {
        ZarinpalVerifyRemoteDto body = new ZarinpalVerifyRemoteDto(
                request.merchantId(),
                request.amount(),
                request.authority()
        );
        ZarinpalResponseEnvelopeRemoteDto<ZarinpalVerifyDataRemoteDto> response = restClient.post()
                .uri(properties.verifyPath())
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        ZarinpalVerifyDataRemoteDto data = requireData(response);
        int code = data.code() == null ? firstErrorCode(response) : data.code();
        boolean successful = code == 100;
        boolean alreadyVerified = code == 101;
        String refId = data.refId() == null ? null : String.valueOf(data.refId());
        return new ZarinpalVerifyResponse(
                successful,
                alreadyVerified,
                code,
                sanitize(data.message(), response),
                refId,
                data.cardHash(),
                maskPan(data.cardPan()),
                data.fee() == null ? 0 : data.fee(),
                data.feeType()
        );
    }

    private <T> T requireData(ZarinpalResponseEnvelopeRemoteDto<T> response) {
        if (response == null || response.data() == null) {
            throw new ZarinpalResponseInvalidException("Zarinpal response is missing data");
        }
        return response.data();
    }

    private int firstErrorCode(ZarinpalResponseEnvelopeRemoteDto<?> response) {
        if (response == null || response.errors() == null || response.errors().isEmpty()) {
            return 0;
        }
        ZarinpalErrorRemoteDto error = response.errors().getFirst();
        return error.code() == null ? 0 : error.code();
    }

    private String sanitize(String message, ZarinpalResponseEnvelopeRemoteDto<?> response) {
        if (message != null && !message.isBlank()) {
            return limit(message.trim());
        }
        List<ZarinpalErrorRemoteDto> errors = response == null ? null : response.errors();
        if (errors != null && !errors.isEmpty() && errors.getFirst().message() != null) {
            return limit(errors.getFirst().message().trim());
        }
        return null;
    }

    private String limit(String value) {
        return value.length() <= 500 ? value : value.substring(0, 500);
    }

    private String maskPan(String cardPan) {
        if (cardPan == null) {
            return null;
        }
        String normalized = cardPan.trim();
        if (normalized.contains("*")) {
            return normalized.length() <= 32 ? normalized : normalized.substring(0, 32);
        }
        if (normalized.length() <= 10) {
            return null;
        }
        return normalized.substring(0, 6) + "******" + normalized.substring(normalized.length() - 4);
    }
}
