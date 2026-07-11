package com.parazit.panel.application.xui.client;

import com.parazit.panel.domain.plan.selection.PlanSelection;
import com.parazit.panel.domain.plan.selection.PlanSelectionStatus;
import com.parazit.panel.domain.plan.selection.repository.PlanSelectionRepository;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import com.parazit.panel.domain.xui.provisioning.XuiProvisionStatus;
import com.parazit.panel.domain.xui.provisioning.repository.XuiClientProvisionRepository;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UpdateXuiProvisionStatusTransaction {

    private final XuiClientProvisionRepository provisionRepository;
    private final PlanSelectionRepository planSelectionRepository;

    public UpdateXuiProvisionStatusTransaction(
            XuiClientProvisionRepository provisionRepository,
            PlanSelectionRepository planSelectionRepository
    ) {
        this.provisionRepository = Objects.requireNonNull(provisionRepository, "provisionRepository must not be null");
        this.planSelectionRepository = Objects.requireNonNull(planSelectionRepository, "planSelectionRepository must not be null");
    }

    @Transactional
    public ClaimedXuiProvision claimProvisioning(UUID provisionId) {
        XuiClientProvision provision = find(provisionId);
        if (provision.getStatus() == XuiProvisionStatus.ACTIVE || provision.getStatus() == XuiProvisionStatus.PROVISIONING) {
            return new ClaimedXuiProvision(provision, false);
        }
        boolean claimed = provisionRepository.transitionStatus(provisionId, provision.getStatus(), XuiProvisionStatus.PROVISIONING);
        return new ClaimedXuiProvision(find(provisionId), claimed);
    }

    @Transactional
    public XuiClientProvision markActive(UUID provisionId, Instant now) {
        XuiClientProvision provision = find(provisionId);
        provision.markActive(now);
        PlanSelection selection = planSelectionRepository.findById(provision.getPlanSelectionId())
                .orElse(null);
        if (selection != null && selection.getStatus() == PlanSelectionStatus.ACTIVE) {
            selection.consume(now);
            planSelectionRepository.save(selection);
        }
        return provisionRepository.save(provision);
    }

    @Transactional
    public XuiClientProvision markFailed(UUID provisionId, String code, String message) {
        XuiClientProvision provision = find(provisionId);
        provision.markFailed(code, message);
        return provisionRepository.save(provision);
    }

    @Transactional
    public XuiClientProvision markUnknown(UUID provisionId, String code, String message) {
        XuiClientProvision provision = find(provisionId);
        provision.markUnknown(code, message);
        return provisionRepository.save(provision);
    }

    @Transactional(readOnly = true)
    public XuiClientProvision find(UUID provisionId) {
        return provisionRepository.findById(provisionId)
                .orElseThrow(() -> new XuiClientProvisionUnknownException("Xui provision could not be reloaded"));
    }
}
