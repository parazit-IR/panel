package com.parazit.panel.api.internal.plan.admin;

import com.parazit.panel.application.plan.result.PlanResult;
import com.parazit.panel.application.port.in.plan.admin.ChangePlanStatusUseCase;
import com.parazit.panel.application.port.in.plan.admin.CreatePlanUseCase;
import com.parazit.panel.application.port.in.plan.admin.GetPlanUseCase;
import com.parazit.panel.application.port.in.plan.admin.ListPlansUseCase;
import com.parazit.panel.application.port.in.plan.admin.UpdatePlanUseCase;
import com.parazit.panel.domain.plan.PlanStatus;
import com.parazit.panel.domain.plan.PlanType;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal administrative endpoint for VPN plan management.
 * Authentication and authorization are intentionally deferred.
 */
@Validated
@RestController
@RequestMapping("/internal/admin/plans")
public class AdminPlanController {

    private final CreatePlanUseCase createPlanUseCase;
    private final GetPlanUseCase getPlanUseCase;
    private final ListPlansUseCase listPlansUseCase;
    private final UpdatePlanUseCase updatePlanUseCase;
    private final ChangePlanStatusUseCase changePlanStatusUseCase;
    private final AdminPlanApiMapper mapper;

    public AdminPlanController(
            CreatePlanUseCase createPlanUseCase,
            GetPlanUseCase getPlanUseCase,
            ListPlansUseCase listPlansUseCase,
            UpdatePlanUseCase updatePlanUseCase,
            ChangePlanStatusUseCase changePlanStatusUseCase,
            AdminPlanApiMapper mapper
    ) {
        this.createPlanUseCase = Objects.requireNonNull(createPlanUseCase, "createPlanUseCase must not be null");
        this.getPlanUseCase = Objects.requireNonNull(getPlanUseCase, "getPlanUseCase must not be null");
        this.listPlansUseCase = Objects.requireNonNull(listPlansUseCase, "listPlansUseCase must not be null");
        this.updatePlanUseCase = Objects.requireNonNull(updatePlanUseCase, "updatePlanUseCase must not be null");
        this.changePlanStatusUseCase = Objects.requireNonNull(changePlanStatusUseCase, "changePlanStatusUseCase must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @PostMapping
    public ResponseEntity<PlanResponse> create(@Valid @RequestBody CreatePlanRequest request) {
        PlanResult result = createPlanUseCase.create(mapper.toCommand(request));
        URI location = URI.create("/internal/admin/plans/" + result.id());

        return ResponseEntity.created(location)
                .body(mapper.toResponse(result));
    }

    @GetMapping
    public List<PlanResponse> list(
            @RequestParam(required = false) PlanStatus status,
            @RequestParam(required = false) PlanType type
    ) {
        return mapper.toResponse(listPlansUseCase.list(mapper.toListQuery(status, type)));
    }

    @GetMapping("/{planId}")
    public PlanResponse getById(@PathVariable UUID planId) {
        return mapper.toResponse(getPlanUseCase.getById(mapper.toGetByIdQuery(planId)));
    }

    @GetMapping("/by-code/{code}")
    public PlanResponse getByCode(@PathVariable String code) {
        return mapper.toResponse(getPlanUseCase.getByCode(mapper.toGetByCodeQuery(code)));
    }

    @PutMapping("/{planId}")
    public PlanResponse update(
            @PathVariable UUID planId,
            @Valid @RequestBody UpdatePlanRequest request
    ) {
        return mapper.toResponse(updatePlanUseCase.update(mapper.toCommand(planId, request)));
    }

    @PostMapping("/{planId}/activate")
    public PlanResponse activate(@PathVariable UUID planId) {
        return mapper.toResponse(changePlanStatusUseCase.activate(mapper.toChangeStatusCommand(planId)));
    }

    @PostMapping("/{planId}/deactivate")
    public PlanResponse deactivate(@PathVariable UUID planId) {
        return mapper.toResponse(changePlanStatusUseCase.deactivate(mapper.toChangeStatusCommand(planId)));
    }

    @PostMapping("/{planId}/archive")
    public PlanResponse archive(@PathVariable UUID planId) {
        return mapper.toResponse(changePlanStatusUseCase.archive(mapper.toChangeStatusCommand(planId)));
    }
}
