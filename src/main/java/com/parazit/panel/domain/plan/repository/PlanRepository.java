package com.parazit.panel.domain.plan.repository;

import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanStatus;
import com.parazit.panel.domain.plan.PlanType;
import com.parazit.panel.domain.repository.UuidRepository;
import java.util.List;
import java.util.Optional;

public interface PlanRepository extends UuidRepository<Plan> {

    Optional<Plan> findByCode(String code);

    boolean existsByCode(String code);

    List<Plan> findAllByStatusOrderByDisplayOrderAscCodeAsc(PlanStatus status);

    List<Plan> findAllByTypeOrderByDisplayOrderAscCodeAsc(PlanType type);

    List<Plan> findAllByStatusAndTypeOrderByDisplayOrderAscCodeAsc(PlanStatus status, PlanType type);

    List<Plan> findAllOrderByDisplayOrderAscCodeAsc();
}
