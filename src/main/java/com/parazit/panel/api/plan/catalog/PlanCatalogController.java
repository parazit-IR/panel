package com.parazit.panel.api.plan.catalog;

import com.parazit.panel.application.port.in.plan.catalog.GetAvailablePlanUseCase;
import com.parazit.panel.application.port.in.plan.catalog.ListAvailablePlansUseCase;
import com.parazit.panel.domain.plan.PlanType;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/plans")
public class PlanCatalogController {

    private final ListAvailablePlansUseCase listAvailablePlansUseCase;
    private final GetAvailablePlanUseCase getAvailablePlanUseCase;
    private final PlanCatalogApiMapper mapper;

    public PlanCatalogController(
            ListAvailablePlansUseCase listAvailablePlansUseCase,
            GetAvailablePlanUseCase getAvailablePlanUseCase,
            PlanCatalogApiMapper mapper
    ) {
        this.listAvailablePlansUseCase = Objects.requireNonNull(listAvailablePlansUseCase, "listAvailablePlansUseCase must not be null");
        this.getAvailablePlanUseCase = Objects.requireNonNull(getAvailablePlanUseCase, "getAvailablePlanUseCase must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @GetMapping
    public List<AvailablePlanResponse> list(@RequestParam(required = false) PlanType type) {
        return mapper.toResponse(listAvailablePlansUseCase.list(mapper.toListQuery(type)));
    }

    @GetMapping("/{planId}")
    public AvailablePlanResponse getById(@PathVariable UUID planId) {
        return mapper.toResponse(getAvailablePlanUseCase.getById(mapper.toGetByIdQuery(planId)));
    }

    @GetMapping("/by-code/{code}")
    public AvailablePlanResponse getByCode(@PathVariable String code) {
        return mapper.toResponse(getAvailablePlanUseCase.getByCode(mapper.toGetByCodeQuery(code)));
    }
}
