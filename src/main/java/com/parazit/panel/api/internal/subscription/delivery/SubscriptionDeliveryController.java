package com.parazit.panel.api.internal.subscription.delivery;

import com.parazit.panel.application.port.in.subscription.delivery.BuildSubscriptionUrlUseCase;
import com.parazit.panel.application.port.in.subscription.delivery.GenerateSubscriptionUrlQrCodeUseCase;
import com.parazit.panel.application.port.in.subscription.delivery.GenerateVlessConfigQrCodeUseCase;
import com.parazit.panel.application.port.in.subscription.delivery.GetSubscriptionConfigEntryUseCase;
import com.parazit.panel.application.port.in.subscription.delivery.GetSubscriptionDeliverySummaryUseCase;
import com.parazit.panel.application.port.in.subscription.delivery.GetSubscriptionRenderedContentUseCase;
import com.parazit.panel.application.qrcode.model.QrErrorCorrection;
import com.parazit.panel.application.qrcode.model.QrImageFormat;
import com.parazit.panel.application.subscription.delivery.QrCodeImageResult;
import com.parazit.panel.application.subscription.delivery.SubscriptionRenderedContentResult;
import jakarta.validation.Valid;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
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
@RequestMapping("/internal/users/{telegramUserId}/subscriptions/{subscriptionId}/delivery")
public class SubscriptionDeliveryController {

    private final GetSubscriptionDeliverySummaryUseCase summaryUseCase;
    private final BuildSubscriptionUrlUseCase buildUrlUseCase;
    private final GenerateSubscriptionUrlQrCodeUseCase subscriptionQrUseCase;
    private final GetSubscriptionConfigEntryUseCase configEntryUseCase;
    private final GenerateVlessConfigQrCodeUseCase vlessQrUseCase;
    private final GetSubscriptionRenderedContentUseCase renderedContentUseCase;
    private final SubscriptionDeliveryApiMapper mapper;

    public SubscriptionDeliveryController(
            GetSubscriptionDeliverySummaryUseCase summaryUseCase,
            BuildSubscriptionUrlUseCase buildUrlUseCase,
            GenerateSubscriptionUrlQrCodeUseCase subscriptionQrUseCase,
            GetSubscriptionConfigEntryUseCase configEntryUseCase,
            GenerateVlessConfigQrCodeUseCase vlessQrUseCase,
            GetSubscriptionRenderedContentUseCase renderedContentUseCase,
            SubscriptionDeliveryApiMapper mapper
    ) {
        this.summaryUseCase = Objects.requireNonNull(summaryUseCase, "summaryUseCase must not be null");
        this.buildUrlUseCase = Objects.requireNonNull(buildUrlUseCase, "buildUrlUseCase must not be null");
        this.subscriptionQrUseCase = Objects.requireNonNull(subscriptionQrUseCase, "subscriptionQrUseCase must not be null");
        this.configEntryUseCase = Objects.requireNonNull(configEntryUseCase, "configEntryUseCase must not be null");
        this.vlessQrUseCase = Objects.requireNonNull(vlessQrUseCase, "vlessQrUseCase must not be null");
        this.renderedContentUseCase = Objects.requireNonNull(renderedContentUseCase, "renderedContentUseCase must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @GetMapping
    public ResponseEntity<SubscriptionDeliverySummaryResponse> summary(
            @PathVariable Long telegramUserId,
            @PathVariable UUID subscriptionId
    ) {
        return noStoreJson(mapper.toResponse(summaryUseCase.get(telegramUserId, subscriptionId)));
    }

    @PostMapping("/subscription-url")
    public ResponseEntity<BuildSubscriptionUrlResponse> subscriptionUrl(
            @PathVariable Long telegramUserId,
            @PathVariable UUID subscriptionId,
            @Valid @RequestBody BuildSubscriptionUrlRequest request
    ) {
        return noStoreJson(mapper.toResponse(buildUrlUseCase.build(mapper.toBuildCommand(telegramUserId, subscriptionId, request))));
    }

    @PostMapping("/subscription-url/qr")
    public ResponseEntity<byte[]> subscriptionUrlQr(
            @PathVariable Long telegramUserId,
            @PathVariable UUID subscriptionId,
            @Valid @RequestBody GenerateSubscriptionUrlQrRequest request
    ) {
        return image(subscriptionQrUseCase.generate(mapper.toSubscriptionQrCommand(telegramUserId, subscriptionId, request)));
    }

    @GetMapping("/configs/{configIndex}")
    public ResponseEntity<SubscriptionConfigEntryResponse> config(
            @PathVariable Long telegramUserId,
            @PathVariable UUID subscriptionId,
            @PathVariable int configIndex
    ) {
        return noStoreJson(mapper.toResponse(configEntryUseCase.get(telegramUserId, subscriptionId, configIndex)));
    }

    @GetMapping("/configs/{configIndex}/qr")
    public ResponseEntity<byte[]> configQr(
            @PathVariable Long telegramUserId,
            @PathVariable UUID subscriptionId,
            @PathVariable int configIndex,
            @RequestParam(required = false) QrImageFormat format,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer marginModules,
            @RequestParam(required = false) QrErrorCorrection errorCorrection,
            @RequestParam(required = false) Boolean transparentBackground,
            @RequestParam(required = false) Boolean download
    ) {
        return image(vlessQrUseCase.generate(mapper.toVlessQrCommand(
                telegramUserId,
                subscriptionId,
                configIndex,
                format,
                size,
                marginModules,
                errorCorrection,
                transparentBackground,
                download
        )));
    }

    @GetMapping("/content")
    public ResponseEntity<byte[]> content(
            @PathVariable Long telegramUserId,
            @PathVariable UUID subscriptionId,
            @RequestParam(required = false) String format
    ) {
        SubscriptionRenderedContentResult result = renderedContentUseCase.get(telegramUserId, subscriptionId, format);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .contentLength(result.body().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, private")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .header("X-Content-Type-Options", "nosniff")
                .body(result.body());
    }

    private static <T> ResponseEntity<T> noStoreJson(T body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .header("X-Content-Type-Options", "nosniff")
                .body(body);
    }

    private static ResponseEntity<byte[]> image(QrCodeImageResult result) {
        ContentDisposition disposition = result.download()
                ? ContentDisposition.attachment().filename(result.filename()).build()
                : ContentDisposition.inline().filename(result.filename()).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .contentLength(result.bytes().length)
                .eTag(result.etag())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, private")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .header("X-Content-Type-Options", "nosniff")
                .body(result.bytes());
    }
}
