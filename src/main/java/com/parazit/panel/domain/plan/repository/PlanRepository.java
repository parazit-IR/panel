package com.parazit.panel.domain.plan.repository;

import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanStatus;
import com.parazit.panel.domain.plan.PlanType;
import com.parazit.panel.domain.repository.UuidRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanRepository extends UuidRepository<Plan> {

    Optional<Plan> findByCode(String code);

    Optional<Plan> findByIdAndStatus(UUID id, PlanStatus status);

    Optional<Plan> findByCodeAndStatus(String code, PlanStatus status);

    boolean existsByCode(String code);

    List<Plan> findAllByStatusOrderByDisplayOrderAscCodeAsc(PlanStatus status);

    List<Plan> findAllByTypeOrderByDisplayOrderAscCodeAsc(PlanType type);

    List<Plan> findAllByStatusAndTypeOrderByDisplayOrderAscCodeAsc(PlanStatus status, PlanType type);

    List<Plan> findAllOrderByDisplayOrderAscCodeAsc();
}
