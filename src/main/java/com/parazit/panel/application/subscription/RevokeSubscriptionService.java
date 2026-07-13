package com.parazit.panel.application.subscription;

import com.parazit.panel.application.port.in.subscription.RevokeSubscriptionUseCase;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.subscription.command.RevokeSubscriptionCommand;
import com.parazit.panel.application.subscription.result.SubscriptionResult;
import com.parazit.panel.application.user.UserNotFoundException;
import com.parazit.panel.domain.plan.selection.PlanSelection;
import com.parazit.panel.domain.plan.selection.repository.PlanSelectionRepository;
import com.parazit.panel.domain.subscription.Subscription;
import com.parazit.panel.domain.subscription.SubscriptionStatus;
import com.parazit.panel.domain.subscription.repository.SubscriptionRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RevokeSubscriptionService implements RevokeSubscriptionUseCase {

    private static final Logger log = LoggerFactory.getLogger(RevokeSubscriptionService.class);

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanSelectionRepository planSelectionRepository;
    private final SystemClockPort clock;
    private final SubscriptionResultMapper mapper;

    public RevokeSubscriptionService(
            UserRepository userRepository,
            SubscriptionRepository subscriptionRepository,
            PlanSelectionRepository planSelectionRepository,
            SystemClockPort clock,
            SubscriptionResultMapper mapper
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository, "subscriptionRepository must not be null");
        this.planSelectionRepository = Objects.requireNonNull(planSelectionRepository, "planSelectionRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    @Transactional
    public SubscriptionResult revoke(RevokeSubscriptionCommand command) {
        User user = userRepository.findByTelegramUserId(command.telegramUserId())
                .orElseThrow(() -> new UserNotFoundException(command.telegramUserId()));
        Subscription subscription = subscriptionRepository.findByIdForUpdate(command.subscriptionId())
                .orElseThrow(() -> new SubscriptionNotFoundException(command.subscriptionId()));
        if (!subscription.getUserId().equals(user.getId())) {
            throw new SubscriptionOwnershipException();
        }
        if (!subscription.isTerminal() || subscription.getStatus() == SubscriptionStatus.REVOKED) {
            subscription.revoke(clock.now(), command.reason());
            subscription = subscriptionRepository.save(subscription);
        }
        PlanSelection selection = planSelectionRepository.findById(subscription.getPlanSelectionId())
                .orElseThrow(() -> new SubscriptionNotAccessibleException("Plan selection not found"));
        log.atInfo().addKeyValue("subscriptionId", subscription.getId()).log("Subscription revoked");
        return mapper.toResult(subscription, selection, clock.now());
    }
}
