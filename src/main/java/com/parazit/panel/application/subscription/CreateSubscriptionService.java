package com.parazit.panel.application.subscription;

import com.parazit.panel.application.port.in.subscription.CreateSubscriptionUseCase;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.port.out.security.GeneratedSubscriptionToken;
import com.parazit.panel.application.port.out.security.SubscriptionTokenGenerator;
import com.parazit.panel.application.subscription.command.CreateSubscriptionCommand;
import com.parazit.panel.application.subscription.result.CreateSubscriptionResult;
import com.parazit.panel.application.user.UserNotFoundException;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.plan.selection.PlanSelection;
import com.parazit.panel.domain.plan.selection.repository.PlanSelectionRepository;
import com.parazit.panel.domain.subscription.Subscription;
import com.parazit.panel.domain.subscription.repository.SubscriptionRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import com.parazit.panel.domain.xui.provisioning.XuiProvisionStatus;
import com.parazit.panel.domain.xui.provisioning.repository.XuiClientProvisionRepository;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateSubscriptionService implements CreateSubscriptionUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateSubscriptionService.class);
    private static final int MAX_TOKEN_ATTEMPTS = 3;

    private final UserRepository userRepository;
    private final XuiClientProvisionRepository provisionRepository;
    private final PlanSelectionRepository planSelectionRepository;
    private final OrderRepository orderRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionTokenGenerator tokenGenerator;
    private final SystemClockPort clock;
    private final SubscriptionResultMapper mapper;

    public CreateSubscriptionService(
            UserRepository userRepository,
            XuiClientProvisionRepository provisionRepository,
            PlanSelectionRepository planSelectionRepository,
            OrderRepository orderRepository,
            SubscriptionRepository subscriptionRepository,
            SubscriptionTokenGenerator tokenGenerator,
            SystemClockPort clock,
            SubscriptionResultMapper mapper
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.provisionRepository = Objects.requireNonNull(provisionRepository, "provisionRepository must not be null");
        this.planSelectionRepository = Objects.requireNonNull(planSelectionRepository, "planSelectionRepository must not be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository, "subscriptionRepository must not be null");
        this.tokenGenerator = Objects.requireNonNull(tokenGenerator, "tokenGenerator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    @Transactional
    public CreateSubscriptionResult create(CreateSubscriptionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        User user = userRepository.findByTelegramUserId(command.telegramUserId())
                .orElseThrow(() -> new UserNotFoundException(command.telegramUserId()));
        XuiClientProvision provision = provisionRepository.findByIdForUpdate(command.xuiClientProvisionId())
                .orElseThrow(() -> new SubscriptionNotAccessibleException("Provision not found"));
        validateProvision(provision);
        validateOwnership(user, provision);

        Subscription existing = subscriptionRepository.findByXuiClientProvisionId(provision.getId()).orElse(null);
        if (existing != null) {
            log.atDebug().addKeyValue("subscriptionId", existing.getId()).log("Subscription creation replay");
            return mapper.toCreateResult(existing, null, false);
        }

        PlanSelection selection = planSelectionRepository.findById(provision.getPlanSelectionId())
                .orElseThrow(() -> new SubscriptionNotAccessibleException("Plan selection not found"));
        Order order = orderRepository.findByPlanSelectionId(provision.getPlanSelectionId())
                .orElseThrow(() -> new SubscriptionNotAccessibleException("Order not found"));
        validateRelationships(user, provision, selection, order);

        for (int attempt = 1; attempt <= MAX_TOKEN_ATTEMPTS; attempt++) {
            GeneratedSubscriptionToken token = tokenGenerator.generate();
            Subscription subscription = Subscription.activate(
                    user.getId(),
                    order.getId(),
                    selection.getId(),
                    provision.getId(),
                    provision.getInboundId(),
                    UUID.fromString(provision.getRemoteClientId()),
                    token.tokenHash(),
                    token.safePrefix(),
                    clock.now(),
                    provision.getExpiresAt(),
                    selection.getPlanNameSnapshot(),
                    "vless-reality-v1"
            );
            try {
                Subscription saved = subscriptionRepository.save(subscription);
                log.atInfo()
                        .addKeyValue("subscriptionId", saved.getId())
                        .addKeyValue("provisionId", provision.getId())
                        .addKeyValue("userId", user.getId())
                        .log("Subscription created");
                return mapper.toCreateResult(saved, token.rawToken(), true);
            } catch (DataIntegrityViolationException exception) {
                Subscription concurrent = subscriptionRepository.findByXuiClientProvisionId(provision.getId()).orElse(null);
                if (concurrent != null) {
                    return mapper.toCreateResult(concurrent, null, false);
                }
                if (attempt == MAX_TOKEN_ATTEMPTS) {
                    throw exception;
                }
                log.warn("Subscription token hash collision detected attempt={}", attempt);
            }
        }
        throw new IllegalStateException("Subscription creation failed");
    }

    private static void validateProvision(XuiClientProvision provision) {
        if (provision.getStatus() != XuiProvisionStatus.ACTIVE || provision.getDeletedAt() != null) {
            throw new SubscriptionNotAccessibleException("Provision is not active");
        }
    }

    private static void validateOwnership(User user, XuiClientProvision provision) {
        if (!provision.getUserId().equals(user.getId())) {
            throw new SubscriptionOwnershipException();
        }
    }

    private static void validateRelationships(
            User user,
            XuiClientProvision provision,
            PlanSelection selection,
            Order order
    ) {
        if (!selection.getUserId().equals(user.getId())
                || !selection.getId().equals(provision.getPlanSelectionId())
                || !selection.getPlanId().equals(provision.getPlanId())
                || !order.getUserId().equals(user.getId())
                || !Objects.equals(order.getPlanSelectionId(), selection.getId())
                || !Objects.equals(order.getPlanId(), selection.getPlanId())) {
            throw new SubscriptionNotAccessibleException("Subscription relationship validation failed");
        }
    }
}
