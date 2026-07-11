package com.parazit.panel.infrastructure.persistence.plan;

import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanStatus;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import java.util.List;
import java.util.Optional;

public interface SpringDataPlanRepository extends SpringDataUuidRepository<Plan> {

    Optional<Plan> findByCode(String code);

    boolean existsByCode(String code);

    List<Plan> findAllByStatusOrderByDisplayOrderAscCodeAsc(PlanStatus status);

    List<Plan> findAllByOrderByDisplayOrderAscCodeAsc();
}
