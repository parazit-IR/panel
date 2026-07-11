package com.parazit.panel.infrastructure.persistence.plan;

import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanStatus;
import com.parazit.panel.domain.plan.repository.PlanRepository;
import com.parazit.panel.infrastructure.persistence.repository.JpaRepositoryAdapter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class PlanRepositoryAdapter extends JpaRepositoryAdapter<Plan, UUID> implements PlanRepository {

    private final SpringDataPlanRepository repository;

    public PlanRepositoryAdapter(SpringDataPlanRepository repository) {
        super(repository);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Plan save(Plan plan) {
        return repository.saveAndFlush(Objects.requireNonNull(plan, "plan must not be null"));
    }

    @Override
    public Optional<Plan> findByCode(String code) {
        return repository.findByCode(Plan.normalizeCode(code));
    }

    @Override
    public boolean existsByCode(String code) {
        return repository.existsByCode(Plan.normalizeCode(code));
    }

    @Override
    public List<Plan> findAllByStatusOrderByDisplayOrderAsc(PlanStatus status) {
        return repository.findAllByStatusOrderByDisplayOrderAscCodeAsc(
                Objects.requireNonNull(status, "status must not be null")
        );
    }

    @Override
    public List<Plan> findAllOrderByDisplayOrderAsc() {
        return repository.findAllByOrderByDisplayOrderAscCodeAsc();
    }
}
