package com.parazit.panel.infrastructure.persistence.plan.selection;

import com.parazit.panel.domain.plan.selection.PlanSelection;
import com.parazit.panel.domain.plan.selection.PlanSelectionStatus;
import com.parazit.panel.domain.plan.selection.SelectionType;
import com.parazit.panel.domain.plan.selection.repository.PlanSelectionRepository;
import com.parazit.panel.infrastructure.persistence.repository.JpaRepositoryAdapter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class PlanSelectionRepositoryAdapter extends JpaRepositoryAdapter<PlanSelection, UUID> implements PlanSelectionRepository {

    private final SpringDataPlanSelectionRepository repository;

    public PlanSelectionRepositoryAdapter(SpringDataPlanSelectionRepository repository) {
        super(repository);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public PlanSelection save(PlanSelection selection) {
        return repository.saveAndFlush(Objects.requireNonNull(selection, "selection must not be null"));
    }

    @Override
    public Optional<PlanSelection> findActiveByUserId(UUID userId) {
        return repository.findByUserIdAndStatus(
                Objects.requireNonNull(userId, "userId must not be null"),
                PlanSelectionStatus.ACTIVE
        );
    }

    @Override
    public Optional<PlanSelection> findActiveByUserIdAndType(UUID userId, SelectionType selectionType) {
        return repository.findByUserIdAndSelectionTypeAndStatus(
                Objects.requireNonNull(userId, "userId must not be null"),
                Objects.requireNonNull(selectionType, "selectionType must not be null"),
                PlanSelectionStatus.ACTIVE
        );
    }

    @Override
    public Optional<PlanSelection> findActiveByUserIdAndTypeAndTargetSubscriptionId(
            UUID userId,
            SelectionType selectionType,
            UUID targetSubscriptionId
    ) {
        return repository.findByUserIdAndSelectionTypeAndTargetSubscriptionIdAndStatus(
                Objects.requireNonNull(userId, "userId must not be null"),
                Objects.requireNonNull(selectionType, "selectionType must not be null"),
                Objects.requireNonNull(targetSubscriptionId, "targetSubscriptionId must not be null"),
                PlanSelectionStatus.ACTIVE
        );
    }

    @Override
    public List<PlanSelection> findAllByUserIdOrderBySelectedAtDesc(UUID userId) {
        return repository.findAllByUserIdOrderBySelectedAtDesc(
                Objects.requireNonNull(userId, "userId must not be null")
        );
    }

    @Override
    public boolean existsActiveByUserId(UUID userId) {
        return repository.existsByUserIdAndStatus(
                Objects.requireNonNull(userId, "userId must not be null"),
                PlanSelectionStatus.ACTIVE
        );
    }
}
