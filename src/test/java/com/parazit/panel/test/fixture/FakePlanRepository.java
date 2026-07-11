package com.parazit.panel.test.fixture;

import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanStatus;
import com.parazit.panel.domain.plan.PlanType;
import com.parazit.panel.domain.plan.repository.PlanRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

public class FakePlanRepository implements PlanRepository {

    public static final Instant CREATED_AT = Instant.parse("2026-07-10T12:00:00Z");
    public static final Instant UPDATED_AT = Instant.parse("2026-07-10T12:05:00Z");

    private final Map<UUID, Plan> plansById = new LinkedHashMap<>();
    private boolean failNextSaveWithDuplicateKey;

    public int findByIdCalls;
    public int findByCodeCalls;
    public int findAllOrderByDisplayOrderAscCodeAscCalls;
    public int findAllByStatusOrderByDisplayOrderAscCodeAscCalls;
    public int findAllByTypeOrderByDisplayOrderAscCodeAscCalls;
    public int findAllByStatusAndTypeOrderByDisplayOrderAscCodeAscCalls;
    public int existsByCodeCalls;
    public int saveCalls;
    public int deleteCalls;

    @Override
    public Optional<Plan> findByCode(String code) {
        findByCodeCalls++;
        String normalizedCode = Plan.normalizeCode(code);
        return plansById.values()
                .stream()
                .filter(plan -> normalizedCode.equals(plan.getCode()))
                .findFirst();
    }

    @Override
    public boolean existsByCode(String code) {
        existsByCodeCalls++;
        return findByCode(code).isPresent();
    }

    @Override
    public List<Plan> findAllByStatusOrderByDisplayOrderAscCodeAsc(PlanStatus status) {
        findAllByStatusOrderByDisplayOrderAscCodeAscCalls++;
        return sortedPlans().stream()
                .filter(plan -> status == plan.getStatus())
                .toList();
    }

    @Override
    public List<Plan> findAllByTypeOrderByDisplayOrderAscCodeAsc(PlanType type) {
        findAllByTypeOrderByDisplayOrderAscCodeAscCalls++;
        return sortedPlans().stream()
                .filter(plan -> type == plan.getType())
                .toList();
    }

    @Override
    public List<Plan> findAllByStatusAndTypeOrderByDisplayOrderAscCodeAsc(PlanStatus status, PlanType type) {
        findAllByStatusAndTypeOrderByDisplayOrderAscCodeAscCalls++;
        return sortedPlans().stream()
                .filter(plan -> status == plan.getStatus())
                .filter(plan -> type == plan.getType())
                .toList();
    }

    @Override
    public List<Plan> findAllOrderByDisplayOrderAscCodeAsc() {
        findAllOrderByDisplayOrderAscCodeAscCalls++;
        return sortedPlans();
    }

    @Override
    public Optional<Plan> findById(UUID id) {
        findByIdCalls++;
        return Optional.ofNullable(plansById.get(id));
    }

    @Override
    public List<Plan> findAll() {
        return new ArrayList<>(plansById.values());
    }

    @Override
    public Plan save(Plan plan) {
        saveCalls++;
        if (failNextSaveWithDuplicateKey) {
            failNextSaveWithDuplicateKey = false;
            throw new DataIntegrityViolationException("duplicate key");
        }
        if (plan.getId() == null) {
            ReflectionTestUtils.setField(plan, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(plan, "createdAt", CREATED_AT);
            ReflectionTestUtils.setField(plan, "updatedAt", CREATED_AT);
        } else {
            ReflectionTestUtils.setField(plan, "updatedAt", UPDATED_AT);
        }
        plansById.put(plan.getId(), plan);
        return plan;
    }

    @Override
    public List<Plan> saveAll(Collection<Plan> entities) {
        return entities.stream()
                .map(this::save)
                .toList();
    }

    @Override
    public boolean existsById(UUID id) {
        return plansById.containsKey(id);
    }

    @Override
    public long count() {
        return plansById.size();
    }

    @Override
    public void delete(Plan entity) {
        deleteCalls++;
        plansById.remove(entity.getId());
    }

    @Override
    public void deleteById(UUID id) {
        deleteCalls++;
        plansById.remove(id);
    }

    public void failNextSaveWithDuplicateKey() {
        this.failNextSaveWithDuplicateKey = true;
    }

    private List<Plan> sortedPlans() {
        return plansById.values()
                .stream()
                .sorted(Comparator.comparingInt(Plan::getDisplayOrder).thenComparing(Plan::getCode))
                .toList();
    }
}
