package com.parazit.panel.application.subscription;

import com.parazit.panel.application.port.in.subscription.ResumeSubscriptionUseCase;
import com.parazit.panel.application.port.in.subscription.SuspendSubscriptionUseCase;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.subscription.result.SubscriptionResult;
import com.parazit.panel.domain.plan.selection.PlanSelection;
import com.parazit.panel.domain.plan.selection.repository.PlanSelectionRepository;
import com.parazit.panel.domain.subscription.Subscription;
import com.parazit.panel.domain.subscription.repository.SubscriptionRepository;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import com.parazit.panel.domain.xui.provisioning.XuiProvisionStatus;
import com.parazit.panel.domain.xui.provisioning.repository.XuiClientProvisionRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SuspendSubscriptionService implements SuspendSubscriptionUseCase, ResumeSubscriptionUseCase {

    private final SubscriptionRepository subscriptionRepository;
    private final XuiClientProvisionRepository provisionRepository;
    private final PlanSelectionRepository planSelectionRepository;
    private final SystemClockPort clock;
    private final SubscriptionResultMapper mapper;

    public SuspendSubscriptionService(
            SubscriptionRepository subscriptionRepository,
            XuiClientProvisionRepository provisionRepository,
            PlanSelectionRepository planSelectionRepository,
            SystemClockPort clock,
            SubscriptionResultMapper mapper
    ) {
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository, "subscriptionRepository must not be null");
        this.provisionRepository = Objects.requireNonNull(provisionRepository, "provisionRepository must not be null");
        this.planSelectionRepository = Objects.requireNonNull(planSelectionRepository, "planSelectionRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    @Transactional
    public SubscriptionResult suspend(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findByIdForUpdate(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));
        subscription.suspend();
        subscription = subscriptionRepository.save(subscription);
        return toResult(subscription);
    }

    @Override
    @Transactional
    public SubscriptionResult resume(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findByIdForUpdate(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));
        XuiClientProvision provision = provisionRepository.findById(subscription.getXuiClientProvisionId())
                .orElseThrow(() -> new SubscriptionNotAccessibleException("Provision not found"));
        if (provision.getStatus() != XuiProvisionStatus.ACTIVE || provision.getDeletedAt() != null) {
            throw new SubscriptionNotAccessibleException("Underlying provision is not active");
        }
        subscription.resume(clock.now());
        subscription = subscriptionRepository.save(subscription);
        return toResult(subscription);
    }

    private SubscriptionResult toResult(Subscription subscription) {
        PlanSelection selection = planSelectionRepository.findById(subscription.getPlanSelectionId())
                .orElseThrow(() -> new SubscriptionNotAccessibleException("Plan selection not found"));
        return mapper.toResult(subscription, selection, clock.now());
    }
}
