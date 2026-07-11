package com.parazit.panel.domain.plan.selection.repository;

import com.parazit.panel.domain.plan.selection.PlanSelection;
import com.parazit.panel.domain.repository.UuidRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanSelectionRepository extends UuidRepository<PlanSelection> {

    Optional<PlanSelection> findActiveByUserId(UUID userId);

    List<PlanSelection> findAllByUserIdOrderBySelectedAtDesc(UUID userId);

    boolean existsActiveByUserId(UUID userId);
}
