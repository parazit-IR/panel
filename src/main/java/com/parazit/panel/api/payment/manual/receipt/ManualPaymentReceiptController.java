package com.parazit.panel.api.payment.manual.receipt;

import com.parazit.panel.application.payment.manual.receipt.command.SubmitManualPaymentReceiptCommand;
import com.parazit.panel.application.payment.manual.receipt.result.ManualPaymentReceiptContentResult;
import com.parazit.panel.application.payment.manual.receipt.result.SubmitManualPaymentReceiptResult;
import com.parazit.panel.application.port.in.payment.manual.receipt.GetManualPaymentReceiptContentUseCase;
import com.parazit.panel.application.port.in.payment.manual.receipt.GetManualPaymentReceiptUseCase;
import com.parazit.panel.application.port.in.payment.manual.receipt.SubmitManualPaymentReceiptUseCase;
import com.parazit.panel.application.port.out.payment.receipt.ReceiptUploadSource;
import jakarta.validation.constraints.Positive;
import java.io.InputStream;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Validated
@RestController
@RequestMapping
public class ManualPaymentReceiptController {

    private final SubmitManualPaymentReceiptUseCase submitUseCase;
    private final GetManualPaymentReceiptUseCase getUseCase;
    private final GetManualPaymentReceiptContentUseCase contentUseCase;
    private final ManualPaymentReceiptApiMapper mapper;

    public ManualPaymentReceiptController(
            SubmitManualPaymentReceiptUseCase submitUseCase,
            GetManualPaymentReceiptUseCase getUseCase,
            GetManualPaymentReceiptContentUseCase contentUseCase,
            ManualPaymentReceiptApiMapper mapper
    ) {
        this.submitUseCase = Objects.requireNonNull(submitUseCase, "submitUseCase must not be null");
        this.getUseCase = Objects.requireNonNull(getUseCase, "getUseCase must not be null");
        this.contentUseCase = Objects.requireNonNull(contentUseCase, "contentUseCase must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @PostMapping(
            path = "/internal/payments/{paymentId}/manual-card/receipt",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ManualPaymentReceiptResponse> submit(
            @PathVariable UUID paymentId,
            @RequestPart String receiptRequestId,
            @RequestPart String telegramUserId,
            @RequestPart String claimedAmount,
            @RequestPart(required = false) String claimedTrackingNumber,
            @RequestPart(required = false) String claimedSenderCardLastFour,
            @RequestPart(required = false) String claimedPaidAt,
            @RequestPart(required = false) String userNote,
            @RequestPart MultipartFile file
    ) {
        SubmitManualPaymentReceiptResult result = submitUseCase.submit(new SubmitManualPaymentReceiptCommand(
                parseUuid(receiptRequestId, "receiptRequestId"),
                parsePositiveLong(telegramUserId, "telegramUserId"),
                paymentId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                parsePositiveLong(claimedAmount, "claimedAmount"),
                claimedTrackingNumber,
                claimedSenderCardLastFour,
                parseInstant(claimedPaidAt, "claimedPaidAt"),
                userNote,
                multipartSource(file)
        ));
        return ResponseEntity.status(result.newlySubmitted() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(mapper.toResponse(result));
    }

    @GetMapping(
            path = "/internal/payments/{paymentId}/manual-card/receipt",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ManualPaymentReceiptResponse getByPayment(
            @PathVariable UUID paymentId,
            @org.springframework.web.bind.annotation.RequestParam @Positive Long telegramUserId
    ) {
        return mapper.toResponse(getUseCase.getCurrentByPayment(mapper.toPaymentQuery(paymentId, telegramUserId)));
    }

    @GetMapping(
            path = "/internal/manual-payment-receipts/{receiptId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ManualPaymentReceiptResponse getById(
            @PathVariable UUID receiptId,
            @org.springframework.web.bind.annotation.RequestParam @Positive Long telegramUserId
    ) {
        return mapper.toResponse(getUseCase.getById(mapper.toGetQuery(receiptId, telegramUserId)));
    }

    @GetMapping("/internal/admin/manual-payments/receipts/{receiptId}/content")
    public ResponseEntity<StreamingResponseBody> content(@PathVariable UUID receiptId) {
        ManualPaymentReceiptContentResult result = contentUseCase.getContent(mapper.toContentQuery(receiptId));
        StreamingResponseBody body = output -> {
            try (InputStream input = result.contentSource().openStream()) {
                input.transferTo(output);
            }
        };
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .contentLength(result.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(result.filename())
                        .build()
                        .toString())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().cachePrivate().getHeaderValue())
                .body(body);
    }

    private static ReceiptUploadSource multipartSource(MultipartFile file) {
        return () -> file.getInputStream();
    }

    private static UUID parseUuid(String value, String fieldName) {
        String normalized = requireText(value, fieldName);
        try {
            return UUID.fromString(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(fieldName + " has an invalid value");
        }
    }

    private static long parsePositiveLong(String value, String fieldName) {
        String normalized = requireText(value, fieldName);
        try {
            long parsed = Long.parseLong(normalized);
            if (parsed <= 0) {
                throw new IllegalArgumentException(fieldName + " must be positive");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + " has an invalid value");
        }
    }

    private static Instant parseInstant(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value.trim());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(fieldName + " has an invalid value");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
