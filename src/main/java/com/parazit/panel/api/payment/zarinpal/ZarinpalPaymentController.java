package com.parazit.panel.api.payment.zarinpal;

import com.parazit.panel.application.payment.zarinpal.result.InitializeZarinpalPaymentResult;
import com.parazit.panel.application.port.in.payment.zarinpal.InitializeZarinpalPaymentUseCase;
import jakarta.validation.Valid;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/internal/payments")
public class ZarinpalPaymentController {

    private final InitializeZarinpalPaymentUseCase initializeUseCase;
    private final ZarinpalPaymentApiMapper mapper;

    public ZarinpalPaymentController(
            InitializeZarinpalPaymentUseCase initializeUseCase,
            ZarinpalPaymentApiMapper mapper
    ) {
        this.initializeUseCase = Objects.requireNonNull(initializeUseCase, "initializeUseCase must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @PostMapping(
            path = "/{paymentId}/zarinpal/initialize",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<InitializeZarinpalPaymentResponse> initialize(
            @PathVariable UUID paymentId,
            @Valid @RequestBody InitializeZarinpalPaymentRequest request
    ) {
        InitializeZarinpalPaymentResult result = initializeUseCase.initialize(mapper.toCommand(paymentId, request));
        HttpStatus status = result.newlyInitialized() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(mapper.toResponse(result));
    }
}
