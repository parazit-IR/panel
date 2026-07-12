package com.parazit.panel.api.internal.payment.manual.review;

import com.parazit.panel.application.payment.manual.review.command.ApproveManualPaymentReviewCommand;
import com.parazit.panel.application.payment.manual.review.command.ClaimManualPaymentReviewCommand;
import com.parazit.panel.application.payment.manual.review.command.RejectManualPaymentReviewCommand;
import com.parazit.panel.application.payment.manual.review.command.ReleaseManualPaymentReviewCommand;
import com.parazit.panel.application.port.in.payment.manual.review.ApproveManualPaymentReviewUseCase;
import com.parazit.panel.application.port.in.payment.manual.review.ClaimManualPaymentReviewUseCase;
import com.parazit.panel.application.port.in.payment.manual.review.GetManualPaymentReviewUseCase;
import com.parazit.panel.application.port.in.payment.manual.review.ListManualPaymentReviewsUseCase;
import com.parazit.panel.application.port.in.payment.manual.review.RejectManualPaymentReviewUseCase;
import com.parazit.panel.application.port.in.payment.manual.review.ReleaseManualPaymentReviewUseCase;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.MediaType;
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
@RequestMapping(path = "/internal/admin/manual-payments/reviews", produces = MediaType.APPLICATION_JSON_VALUE)
public class ManualPaymentReviewController {

    private final ClaimManualPaymentReviewUseCase claimUseCase;
    private final ReleaseManualPaymentReviewUseCase releaseUseCase;
    private final ApproveManualPaymentReviewUseCase approveUseCase;
    private final RejectManualPaymentReviewUseCase rejectUseCase;
    private final GetManualPaymentReviewUseCase getUseCase;
    private final ListManualPaymentReviewsUseCase listUseCase;
    private final ManualPaymentReviewApiMapper mapper;

    public ManualPaymentReviewController(
            ClaimManualPaymentReviewUseCase claimUseCase,
            ReleaseManualPaymentReviewUseCase releaseUseCase,
            ApproveManualPaymentReviewUseCase approveUseCase,
            RejectManualPaymentReviewUseCase rejectUseCase,
            GetManualPaymentReviewUseCase getUseCase,
            ListManualPaymentReviewsUseCase listUseCase,
            ManualPaymentReviewApiMapper mapper
    ) {
        this.claimUseCase = Objects.requireNonNull(claimUseCase, "claimUseCase must not be null");
        this.releaseUseCase = Objects.requireNonNull(releaseUseCase, "releaseUseCase must not be null");
        this.approveUseCase = Objects.requireNonNull(approveUseCase, "approveUseCase must not be null");
        this.rejectUseCase = Objects.requireNonNull(rejectUseCase, "rejectUseCase must not be null");
        this.getUseCase = Objects.requireNonNull(getUseCase, "getUseCase must not be null");
        this.listUseCase = Objects.requireNonNull(listUseCase, "listUseCase must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @PostMapping("/{receiptId}/claim")
    public ManualPaymentReviewResponse claim(@PathVariable UUID receiptId) {
        return mapper.toResponse(claimUseCase.claim(new ClaimManualPaymentReviewCommand(receiptId)));
    }

    @PostMapping("/{receiptId}/release")
    public ManualPaymentReviewResponse release(@PathVariable UUID receiptId) {
        return mapper.toResponse(releaseUseCase.release(new ReleaseManualPaymentReviewCommand(receiptId)));
    }

    @PostMapping(path = "/{receiptId}/approve", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ManualPaymentReviewResponse approve(
            @PathVariable UUID receiptId,
            @RequestBody(required = false) ApproveManualPaymentReviewRequest request
    ) {
        String note = request == null ? null : request.operatorNote();
        return mapper.toResponse(approveUseCase.approve(new ApproveManualPaymentReviewCommand(receiptId, note)));
    }

    @PostMapping(path = "/{receiptId}/reject", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ManualPaymentReviewResponse reject(
            @PathVariable UUID receiptId,
            @Valid @RequestBody RejectManualPaymentReviewRequest request
    ) {
        return mapper.toResponse(rejectUseCase.reject(new RejectManualPaymentReviewCommand(
                receiptId,
                request.reason(),
                request.operatorNote()
        )));
    }

    @GetMapping("/{receiptId}")
    public ManualPaymentReviewResponse get(@PathVariable UUID receiptId) {
        return mapper.toResponse(getUseCase.get(mapper.toGetQuery(receiptId)));
    }

    @GetMapping
    public List<ManualPaymentReviewResponse> list(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset
    ) {
        return listUseCase.list(mapper.toListQuery(limit, offset))
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}
