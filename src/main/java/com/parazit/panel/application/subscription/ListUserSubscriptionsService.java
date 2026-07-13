package com.parazit.panel.application.subscription;

import com.parazit.panel.application.port.in.subscription.ListUserSubscriptionsUseCase;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.subscription.result.SubscriptionResult;
import com.parazit.panel.application.user.UserNotFoundException;
import com.parazit.panel.domain.plan.selection.PlanSelection;
import com.parazit.panel.domain.plan.selection.repository.PlanSelectionRepository;
import com.parazit.panel.domain.subscription.Subscription;
import com.parazit.panel.domain.subscription.repository.SubscriptionRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListUserSubscriptionsService implements ListUserSubscriptionsUseCase {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanSelectionRepository planSelectionRepository;
    private final SystemClockPort clock;
    private final SubscriptionResultMapper mapper;

    public ListUserSubscriptionsService(
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
    @Transactional(readOnly = true)
    public List<SubscriptionResult> list(Long telegramUserId) {
        User user = userRepository.findByTelegramUserId(telegramUserId)
                .orElseThrow(() -> new UserNotFoundException(telegramUserId));
        return subscriptionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResult)
                .toList();
    }

    private SubscriptionResult toResult(Subscription subscription) {
        PlanSelection selection = planSelectionRepository.findById(subscription.getPlanSelectionId())
                .orElseThrow(() -> new SubscriptionNotAccessibleException("Plan selection not found"));
        return mapper.toResult(subscription, selection, clock.now());
    }
}
