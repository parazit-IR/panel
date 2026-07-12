package com.parazit.panel.api.internal.payment;

import com.parazit.panel.application.port.in.payment.CreatePaymentUseCase;
import com.parazit.panel.application.port.in.payment.GetPaymentUseCase;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping
public class PaymentController {

    private final CreatePaymentUseCase createPaymentUseCase;
    private final GetPaymentUseCase getPaymentUseCase;
    private final PaymentApiMapper mapper;

    public PaymentController(
            CreatePaymentUseCase createPaymentUseCase,
            GetPaymentUseCase getPaymentUseCase,
            PaymentApiMapper mapper
    ) {
        this.createPaymentUseCase = Objects.requireNonNull(createPaymentUseCase, "createPaymentUseCase must not be null");
        this.getPaymentUseCase = Objects.requireNonNull(getPaymentUseCase, "getPaymentUseCase must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @PostMapping(
            path = "/internal/payments",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(@Valid @RequestBody CreatePaymentRequest request) {
        return mapper.toResponse(createPaymentUseCase.create(mapper.toCommand(request)));
    }

    @GetMapping(path = "/internal/payments/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public PaymentResponse getById(@PathVariable UUID id) {
        return mapper.toResponse(getPaymentUseCase.getById(id));
    }

    @GetMapping(path = "/internal/orders/{id}/payments", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<PaymentResponse> listByOrderId(@PathVariable UUID id) {
        return getPaymentUseCase.listByOrderId(id)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}
