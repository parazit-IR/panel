package com.parazit.panel.api.internal.subscription;

import com.parazit.panel.application.port.in.subscription.CreateSubscriptionUseCase;
import com.parazit.panel.application.port.in.subscription.GetUserSubscriptionUseCase;
import com.parazit.panel.application.port.in.subscription.ListUserSubscriptionsUseCase;
import com.parazit.panel.application.port.in.subscription.ResumeSubscriptionUseCase;
import com.parazit.panel.application.port.in.subscription.RevokeSubscriptionUseCase;
import com.parazit.panel.application.port.in.subscription.RotateSubscriptionTokenUseCase;
import com.parazit.panel.application.port.in.subscription.SuspendSubscriptionUseCase;
import com.parazit.panel.application.subscription.result.CreateSubscriptionResult;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/internal/users/{telegramUserId}/subscriptions")
public class UserSubscriptionController {

    private final CreateSubscriptionUseCase createUseCase;
    private final GetUserSubscriptionUseCase getUseCase;
    private final ListUserSubscriptionsUseCase listUseCase;
    private final RotateSubscriptionTokenUseCase rotateUseCase;
    private final RevokeSubscriptionUseCase revokeUseCase;
    private final SuspendSubscriptionUseCase suspendUseCase;
    private final ResumeSubscriptionUseCase resumeUseCase;
    private final SubscriptionApiMapper mapper;

    public UserSubscriptionController(
            CreateSubscriptionUseCase createUseCase,
            GetUserSubscriptionUseCase getUseCase,
            ListUserSubscriptionsUseCase listUseCase,
            RotateSubscriptionTokenUseCase rotateUseCase,
            RevokeSubscriptionUseCase revokeUseCase,
            SuspendSubscriptionUseCase suspendUseCase,
            ResumeSubscriptionUseCase resumeUseCase,
            SubscriptionApiMapper mapper
    ) {
        this.createUseCase = Objects.requireNonNull(createUseCase, "createUseCase must not be null");
        this.getUseCase = Objects.requireNonNull(getUseCase, "getUseCase must not be null");
        this.listUseCase = Objects.requireNonNull(listUseCase, "listUseCase must not be null");
        this.rotateUseCase = Objects.requireNonNull(rotateUseCase, "rotateUseCase must not be null");
        this.revokeUseCase = Objects.requireNonNull(revokeUseCase, "revokeUseCase must not be null");
        this.suspendUseCase = Objects.requireNonNull(suspendUseCase, "suspendUseCase must not be null");
        this.resumeUseCase = Objects.requireNonNull(resumeUseCase, "resumeUseCase must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @PostMapping
    public ResponseEntity<CreateSubscriptionResponse> create(
            @PathVariable Long telegramUserId,
            @Valid @RequestBody CreateSubscriptionRequest request
    ) {
        CreateSubscriptionResult result = createUseCase.create(mapper.toCreateCommand(telegramUserId, request));
        CreateSubscriptionResponse response = mapper.toCreateResponse(result);
        if (result.newlyCreated()) {
            return ResponseEntity.created(URI.create("/internal/users/" + telegramUserId + "/subscriptions/" + result.subscriptionId()))
                    .body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionResponse>> list(@PathVariable Long telegramUserId) {
        return ResponseEntity.ok(listUseCase.list(telegramUserId).stream().map(mapper::toResponse).toList());
    }

    @GetMapping("/{subscriptionId}")
    public ResponseEntity<SubscriptionResponse> get(
            @PathVariable Long telegramUserId,
            @PathVariable UUID subscriptionId
    ) {
        return ResponseEntity.ok(mapper.toResponse(getUseCase.get(telegramUserId, subscriptionId)));
    }

    @PostMapping("/{subscriptionId}/rotate-token")
    public ResponseEntity<RotateSubscriptionTokenResponse> rotate(
            @PathVariable Long telegramUserId,
            @PathVariable UUID subscriptionId,
            @RequestBody(required = false) RotateSubscriptionTokenRequest request
    ) {
        CreateSubscriptionResult result = rotateUseCase.rotate(mapper.toRotateCommand(telegramUserId, subscriptionId, request));
        return ResponseEntity.ok(mapper.toRotateResponse(result));
    }

    @PostMapping("/{subscriptionId}/revoke")
    public ResponseEntity<SubscriptionResponse> revoke(
            @PathVariable Long telegramUserId,
            @PathVariable UUID subscriptionId,
            @Valid @RequestBody(required = false) RevokeSubscriptionRequest request
    ) {
        return ResponseEntity.ok(mapper.toResponse(revokeUseCase.revoke(mapper.toRevokeCommand(telegramUserId, subscriptionId, request))));
    }

    @PostMapping("/{subscriptionId}/suspend")
    public ResponseEntity<SubscriptionResponse> suspend(@PathVariable UUID subscriptionId) {
        return ResponseEntity.ok(mapper.toResponse(suspendUseCase.suspend(subscriptionId)));
    }

    @PostMapping("/{subscriptionId}/resume")
    public ResponseEntity<SubscriptionResponse> resume(@PathVariable UUID subscriptionId) {
        return ResponseEntity.ok(mapper.toResponse(resumeUseCase.resume(subscriptionId)));
    }
}
