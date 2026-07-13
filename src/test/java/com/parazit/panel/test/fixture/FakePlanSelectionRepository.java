package com.parazit.panel.test.fixture;

import com.parazit.panel.domain.plan.selection.PlanSelection;
import com.parazit.panel.domain.plan.selection.PlanSelectionStatus;
import com.parazit.panel.domain.plan.selection.SelectionType;
import com.parazit.panel.domain.plan.selection.repository.PlanSelectionRepository;
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

public class FakePlanSelectionRepository implements PlanSelectionRepository {

    public static final Instant CREATED_AT = Instant.parse("2026-07-10T12:00:00Z");
    public static final Instant UPDATED_AT = Instant.parse("2026-07-10T12:05:00Z");

    private final Map<UUID, PlanSelection> selectionsById = new LinkedHashMap<>();
    private boolean failNextSaveWithDataIntegrityViolation;

    public int findActiveByUserIdCalls;
    public int findAllByUserIdOrderBySelectedAtDescCalls;
    public int existsActiveByUserIdCalls;
    public int saveCalls;
    public int deleteCalls;

    @Override
    public Optional<PlanSelection> findActiveByUserId(UUID userId) {
        findActiveByUserIdCalls++;
        return selectionsById.values()
                .stream()
                .filter(selection -> selection.getUserId().equals(userId))
                .filter(selection -> selection.getStatus() == PlanSelectionStatus.ACTIVE)
                .findFirst();
    }

    @Override
    public Optional<PlanSelection> findActiveByUserIdAndType(UUID userId, SelectionType selectionType) {
        return selectionsById.values()
                .stream()
                .filter(selection -> selection.getUserId().equals(userId))
                .filter(selection -> selection.getSelectionType() == selectionType)
                .filter(selection -> selection.getStatus() == PlanSelectionStatus.ACTIVE)
                .findFirst();
    }

    @Override
    public Optional<PlanSelection> findActiveByUserIdAndTypeAndTargetSubscriptionId(
            UUID userId,
            SelectionType selectionType,
            UUID targetSubscriptionId
    ) {
        return selectionsById.values()
                .stream()
                .filter(selection -> selection.getUserId().equals(userId))
                .filter(selection -> selection.getSelectionType() == selectionType)
                .filter(selection -> targetSubscriptionId.equals(selection.getTargetSubscriptionId()))
                .filter(selection -> selection.getStatus() == PlanSelectionStatus.ACTIVE)
                .findFirst();
    }

    @Override
    public List<PlanSelection> findAllByUserIdOrderBySelectedAtDesc(UUID userId) {
        findAllByUserIdOrderBySelectedAtDescCalls++;
        return selectionsById.values()
                .stream()
                .filter(selection -> selection.getUserId().equals(userId))
                .sorted(Comparator.comparing(PlanSelection::getSelectedAt).reversed())
                .toList();
    }

    @Override
    public boolean existsActiveByUserId(UUID userId) {
        existsActiveByUserIdCalls++;
        return findActiveByUserId(userId).isPresent();
    }

    @Override
    public Optional<PlanSelection> findById(UUID id) {
        return Optional.ofNullable(selectionsById.get(id));
    }

    @Override
    public List<PlanSelection> findAll() {
        return new ArrayList<>(selectionsById.values());
    }

    @Override
    public PlanSelection save(PlanSelection selection) {
        saveCalls++;
        if (failNextSaveWithDataIntegrityViolation) {
            failNextSaveWithDataIntegrityViolation = false;
            throw new DataIntegrityViolationException("active selection conflict");
        }
        if (selection.getId() == null) {
            ReflectionTestUtils.setField(selection, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(selection, "createdAt", CREATED_AT);
            ReflectionTestUtils.setField(selection, "updatedAt", CREATED_AT);
        } else {
            ReflectionTestUtils.setField(selection, "updatedAt", UPDATED_AT);
        }
        selectionsById.put(selection.getId(), selection);
        return selection;
    }

    @Override
    public List<PlanSelection> saveAll(Collection<PlanSelection> entities) {
        return entities.stream().map(this::save).toList();
    }

    @Override
    public boolean existsById(UUID id) {
        return selectionsById.containsKey(id);
    }

    @Override
    public long count() {
        return selectionsById.size();
    }

    @Override
    public void delete(PlanSelection entity) {
        deleteCalls++;
        selectionsById.remove(entity.getId());
    }

    @Override
    public void deleteById(UUID id) {
        deleteCalls++;
        selectionsById.remove(id);
    }

    public void failNextSaveWithDataIntegrityViolation() {
        this.failNextSaveWithDataIntegrityViolation = true;
    }
}
