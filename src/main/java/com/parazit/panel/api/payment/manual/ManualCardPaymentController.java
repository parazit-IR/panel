package com.parazit.panel.api.payment.manual;

import com.parazit.panel.application.payment.manual.result.InitializeManualCardPaymentResult;
import com.parazit.panel.application.payment.manual.result.ManualCardPaymentInstructionResult;
import com.parazit.panel.application.port.in.payment.manual.CancelManualCardPaymentInstructionUseCase;
import com.parazit.panel.application.port.in.payment.manual.GetManualCardPaymentInstructionUseCase;
import com.parazit.panel.application.port.in.payment.manual.InitializeManualCardPaymentUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/internal/payments/{paymentId}/manual-card")
public class ManualCardPaymentController {

    private final InitializeManualCardPaymentUseCase initializeUseCase;
    private final GetManualCardPaymentInstructionUseCase getUseCase;
    private final CancelManualCardPaymentInstructionUseCase cancelUseCase;
    private final ManualCardPaymentApiMapper mapper;

    public ManualCardPaymentController(
            InitializeManualCardPaymentUseCase initializeUseCase,
            GetManualCardPaymentInstructionUseCase getUseCase,
            CancelManualCardPaymentInstructionUseCase cancelUseCase,
            ManualCardPaymentApiMapper mapper
    ) {
        this.initializeUseCase = Objects.requireNonNull(initializeUseCase, "initializeUseCase must not be null");
        this.getUseCase = Objects.requireNonNull(getUseCase, "getUseCase must not be null");
        this.cancelUseCase = Objects.requireNonNull(cancelUseCase, "cancelUseCase must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @PostMapping(
            path = "/initialize",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ManualCardPaymentResponse> initialize(
            @PathVariable UUID paymentId,
            @Valid @RequestBody InitializeManualCardPaymentRequest request
    ) {
        InitializeManualCardPaymentResult result = initializeUseCase.initialize(mapper.toCommand(paymentId, request));
        HttpStatus status = result.newlyInitialized() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(mapper.toResponse(result.instruction()));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ManualCardPaymentResponse getCurrent(
            @PathVariable UUID paymentId,
            @RequestParam @Positive Long telegramUserId
    ) {
        return mapper.toResponse(getUseCase.getCurrent(mapper.toQuery(paymentId, telegramUserId)));
    }

    @PostMapping(
            path = "/cancel",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ManualCardPaymentResponse cancel(
            @PathVariable UUID paymentId,
            @Valid @RequestBody CancelManualCardPaymentRequest request
    ) {
        ManualCardPaymentInstructionResult result = cancelUseCase.cancel(mapper.toCommand(paymentId, request));
        return mapper.toResponse(result);
    }
}
