package com.parazit.panel.api.payment.zarinpal;

import com.parazit.panel.application.payment.zarinpal.result.HandleZarinpalCallbackResult;
import com.parazit.panel.application.port.in.payment.zarinpal.HandleZarinpalCallbackUseCase;
import com.parazit.panel.config.properties.ZarinpalProperties;
import com.parazit.panel.domain.payment.zarinpal.ZarinpalCallbackOutcome;
import java.net.URI;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/payments/zarinpal")
public class ZarinpalCallbackController {

    private final HandleZarinpalCallbackUseCase callbackUseCase;
    private final ZarinpalPaymentApiMapper mapper;
    private final ZarinpalProperties properties;

    public ZarinpalCallbackController(
            HandleZarinpalCallbackUseCase callbackUseCase,
            ZarinpalPaymentApiMapper mapper,
            ZarinpalProperties properties
    ) {
        this.callbackUseCase = Objects.requireNonNull(callbackUseCase, "callbackUseCase must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(name = "Authority", required = false) String authority,
            @RequestParam(name = "Status", required = false) String status
    ) {
        HandleZarinpalCallbackResult result = callbackUseCase.handle(mapper.toCallbackCommand(authority, status));
        return ResponseEntity.status(302).location(redirectFor(result.outcome())).build();
    }

    private URI redirectFor(ZarinpalCallbackOutcome outcome) {
        return switch (outcome) {
            case APPROVED, ALREADY_APPROVED -> appendResult(properties.successRedirectUrl(), "success");
            case CANCELLED -> appendResult(properties.cancelRedirectUrl(), "cancelled");
            case FAILED -> appendResult(properties.failureRedirectUrl(), "failed");
            case UNKNOWN -> appendResult(properties.failureRedirectUrl(), "unknown");
        };
    }

    private URI appendResult(URI base, String result) {
        String separator = base.toString().contains("?") ? "&" : "?";
        return URI.create(base + separator + "result=" + result);
    }
}
