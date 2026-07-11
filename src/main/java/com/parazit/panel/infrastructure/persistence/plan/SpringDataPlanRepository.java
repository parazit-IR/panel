package com.parazit.panel.infrastructure.persistence.plan;

import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanStatus;
import com.parazit.panel.domain.plan.PlanType;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataPlanRepository extends SpringDataUuidRepository<Plan> {

    Optional<Plan> findByCode(String code);

    Optional<Plan> findByIdAndStatus(UUID id, PlanStatus status);

    Optional<Plan> findByCodeAndStatus(String code, PlanStatus status);

    boolean existsByCode(String code);

    List<Plan> findAllByStatusOrderByDisplayOrderAscCodeAsc(PlanStatus status);

    List<Plan> findAllByTypeOrderByDisplayOrderAscCodeAsc(PlanType type);

    List<Plan> findAllByStatusAndTypeOrderByDisplayOrderAscCodeAsc(PlanStatus status, PlanType type);

    List<Plan> findAllByOrderByDisplayOrderAscCodeAsc();
}
