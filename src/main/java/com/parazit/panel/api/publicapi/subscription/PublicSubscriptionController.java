package com.parazit.panel.api.publicapi.subscription;

import com.parazit.panel.application.port.in.subscription.ResolveSubscriptionByTokenUseCase;
import com.parazit.panel.application.subscription.SubscriptionNotAccessibleException;
import com.parazit.panel.application.subscription.SubscriptionRenderingException;
import com.parazit.panel.application.subscription.SubscriptionTokenInvalidException;
import com.parazit.panel.application.subscription.UnsupportedInboundConfigurationException;
import com.parazit.panel.application.subscription.result.ResolvedSubscriptionContent;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sub")
public class PublicSubscriptionController {

    private static final Logger log = LoggerFactory.getLogger(PublicSubscriptionController.class);

    private final ResolveSubscriptionByTokenUseCase resolveUseCase;

    public PublicSubscriptionController(ResolveSubscriptionByTokenUseCase resolveUseCase) {
        this.resolveUseCase = Objects.requireNonNull(resolveUseCase, "resolveUseCase must not be null");
    }

    @GetMapping("/{token}")
    public ResponseEntity<byte[]> get(
            @PathVariable String token,
            @RequestParam(required = false) String format
    ) {
        long startedAt = System.nanoTime();
        try {
            ResolvedSubscriptionContent resolved = resolveUseCase.resolve(token, format);
            HttpHeaders headers = new HttpHeaders();
            resolved.rendered().headers().values().forEach(headers::set);
            headers.set(HttpHeaders.CONTENT_TYPE, resolved.rendered().contentType());
            log.atInfo()
                    .addKeyValue("subscriptionId", resolved.subscriptionId())
                    .addKeyValue("provisionId", resolved.provisionId())
                    .addKeyValue("format", resolved.format())
                    .addKeyValue("durationMs", elapsedMillis(startedAt))
                    .log("Public subscription request completed");
            return new ResponseEntity<>(resolved.rendered().body(), headers, HttpStatus.OK);
        } catch (SubscriptionTokenInvalidException | SubscriptionNotAccessibleException exception) {
            return unavailable404();
        } catch (SubscriptionRenderingException | UnsupportedInboundConfigurationException exception) {
            log.warn("Public subscription render unavailable path=/sub/{redacted} reason={}", exception.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, private")
                    .header("X-Content-Type-Options", "nosniff")
                    .build();
        } catch (IllegalArgumentException exception) {
            return unavailable404();
        }
    }

    private static ResponseEntity<byte[]> unavailable404() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, private")
                .header("X-Content-Type-Options", "nosniff")
                .build();
    }

    private static long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
