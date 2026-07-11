package com.parazit.panel.api.internal.referral;

import com.parazit.panel.application.port.in.referral.AssignReferralUseCase;
import com.parazit.panel.application.port.in.referral.GetReferralOverviewUseCase;
import com.parazit.panel.application.referral.result.AssignReferralResult;
import com.parazit.panel.application.referral.result.ReferralOverviewResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Temporary internal endpoint for verifying referral skeleton behavior.
 */
@Validated
@RestController
@RequestMapping("/internal/users/{telegramUserId}/referral")
public class ReferralController {

    private final GetReferralOverviewUseCase getReferralOverviewUseCase;
    private final AssignReferralUseCase assignReferralUseCase;
    private final ReferralApiMapper mapper;

    public ReferralController(
            GetReferralOverviewUseCase getReferralOverviewUseCase,
            AssignReferralUseCase assignReferralUseCase,
            ReferralApiMapper mapper
    ) {
        this.getReferralOverviewUseCase = Objects.requireNonNull(getReferralOverviewUseCase, "getReferralOverviewUseCase must not be null");
        this.assignReferralUseCase = Objects.requireNonNull(assignReferralUseCase, "assignReferralUseCase must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @GetMapping
    public ResponseEntity<ReferralOverviewResponse> getOverview(@PathVariable @Positive Long telegramUserId) {
        ReferralOverviewResult result = getReferralOverviewUseCase.getOverview(mapper.toOverviewQuery(telegramUserId));
        return ResponseEntity.ok(mapper.toResponse(result));
    }

    @PostMapping
    public ResponseEntity<AssignReferralResponse> assign(
            @PathVariable @Positive Long telegramUserId,
            @Valid @RequestBody AssignReferralRequest request
    ) {
        AssignReferralResult result = assignReferralUseCase.assign(mapper.toAssignCommand(telegramUserId, request));
        HttpStatus status = result.newlyAssigned() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(mapper.toResponse(result));
    }
}
