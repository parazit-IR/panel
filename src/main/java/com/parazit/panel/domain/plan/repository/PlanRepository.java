package com.parazit.panel.domain.plan.repository;

import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanStatus;
import com.parazit.panel.domain.repository.UuidRepository;
import java.util.List;
import java.util.Optional;

public interface PlanRepository extends UuidRepository<Plan> {

    Optional<Plan> findByCode(String code);

    boolean existsByCode(String code);

    List<Plan> findAllByStatusOrderByDisplayOrderAsc(PlanStatus status);

    List<Plan> findAllOrderByDisplayOrderAsc();
}
