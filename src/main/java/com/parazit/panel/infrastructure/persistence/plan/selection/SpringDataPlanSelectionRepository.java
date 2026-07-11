package com.parazit.panel.infrastructure.persistence.plan.selection;

import com.parazit.panel.domain.plan.selection.PlanSelection;
import com.parazit.panel.domain.plan.selection.PlanSelectionStatus;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataPlanSelectionRepository extends SpringDataUuidRepository<PlanSelection> {

    Optional<PlanSelection> findByUserIdAndStatus(UUID userId, PlanSelectionStatus status);

    List<PlanSelection> findAllByUserIdOrderBySelectedAtDesc(UUID userId);

    boolean existsByUserIdAndStatus(UUID userId, PlanSelectionStatus status);
}
