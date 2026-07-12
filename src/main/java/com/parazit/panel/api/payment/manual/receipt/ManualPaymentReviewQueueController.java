package com.parazit.panel.api.payment.manual.receipt;

import com.parazit.panel.application.port.in.payment.manual.receipt.ListManualPaymentReviewQueueUseCase;
import java.util.List;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/admin/manual-payments")
public class ManualPaymentReviewQueueController {

    private final ListManualPaymentReviewQueueUseCase listUseCase;
    private final ManualPaymentReceiptApiMapper mapper;

    public ManualPaymentReviewQueueController(
            ListManualPaymentReviewQueueUseCase listUseCase,
            ManualPaymentReceiptApiMapper mapper
    ) {
        this.listUseCase = Objects.requireNonNull(listUseCase, "listUseCase must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @GetMapping(path = "/review-queue", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ManualPaymentReviewQueueItemResponse> list(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Boolean duplicateOnly
    ) {
        return listUseCase.list(mapper.toQueueQuery(limit, offset, duplicateOnly))
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}
