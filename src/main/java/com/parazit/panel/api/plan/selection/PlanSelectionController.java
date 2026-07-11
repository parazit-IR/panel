package com.parazit.panel.api.plan.selection;

import com.parazit.panel.application.port.in.plan.selection.ClearPlanSelectionUseCase;
import com.parazit.panel.application.port.in.plan.selection.GetCurrentPlanSelectionUseCase;
import com.parazit.panel.application.port.in.plan.selection.SelectPlanUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/users/{telegramUserId}/plan-selection")
public class PlanSelectionController {

    private final SelectPlanUseCase selectPlanUseCase;
    private final GetCurrentPlanSelectionUseCase getCurrentPlanSelectionUseCase;
    private final ClearPlanSelectionUseCase clearPlanSelectionUseCase;
    private final PlanSelectionApiMapper mapper;

    public PlanSelectionController(
            SelectPlanUseCase selectPlanUseCase,
            GetCurrentPlanSelectionUseCase getCurrentPlanSelectionUseCase,
            ClearPlanSelectionUseCase clearPlanSelectionUseCase,
            PlanSelectionApiMapper mapper
    ) {
        this.selectPlanUseCase = Objects.requireNonNull(selectPlanUseCase, "selectPlanUseCase must not be null");
        this.getCurrentPlanSelectionUseCase = Objects.requireNonNull(getCurrentPlanSelectionUseCase, "getCurrentPlanSelectionUseCase must not be null");
        this.clearPlanSelectionUseCase = Objects.requireNonNull(clearPlanSelectionUseCase, "clearPlanSelectionUseCase must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public PlanSelectionResponse select(
            @PathVariable @Positive Long telegramUserId,
            @Valid @RequestBody SelectPlanRequest request
    ) {
        return mapper.toResponse(selectPlanUseCase.select(mapper.toCommand(telegramUserId, request)));
    }

    @GetMapping
    public PlanSelectionResponse getCurrent(@PathVariable @Positive Long telegramUserId) {
        return mapper.toResponse(getCurrentPlanSelectionUseCase.getCurrent(mapper.toGetCurrentQuery(telegramUserId)));
    }

    @DeleteMapping
    public PlanSelectionResponse clear(@PathVariable @Positive Long telegramUserId) {
        return mapper.toResponse(clearPlanSelectionUseCase.clear(mapper.toClearCommand(telegramUserId)));
    }
}
