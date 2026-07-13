package com.parazit.panel.infrastructure.persistence.customer;

import com.parazit.panel.application.customer.CustomerServiceDisplayNamePolicy;
import com.parazit.panel.application.customer.CustomerServiceStatusMapper;
import com.parazit.panel.application.customer.result.CustomerServiceDetailsResult;
import com.parazit.panel.application.customer.result.CustomerServiceStatus;
import com.parazit.panel.application.customer.result.CustomerServiceSummaryResult;
import com.parazit.panel.application.customer.result.UsageFreshness;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.port.out.customer.CustomerServiceQueryPort;
import com.parazit.panel.config.properties.CustomerServicesTelegramProperties;
import com.parazit.panel.domain.plan.selection.PlanSelection;
import com.parazit.panel.domain.subscription.Subscription;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import com.parazit.panel.infrastructure.persistence.plan.selection.SpringDataPlanSelectionRepository;
import com.parazit.panel.infrastructure.persistence.subscription.SpringDataSubscriptionRepository;
import com.parazit.panel.infrastructure.persistence.user.SpringDataUserRepository;
import com.parazit.panel.infrastructure.persistence.xui.provisioning.SpringDataXuiClientProvisionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class CustomerServiceQueryAdapter implements CustomerServiceQueryPort {

    private final SpringDataUserRepository userRepository;
    private final SpringDataSubscriptionRepository subscriptionRepository;
    private final SpringDataPlanSelectionRepository planSelectionRepository;
    private final SpringDataXuiClientProvisionRepository provisionRepository;
    private final CustomerServiceStatusMapper statusMapper;
    private final CustomerServiceDisplayNamePolicy displayNamePolicy;
    private final SystemClockPort clock;
    private final CustomerServicesTelegramProperties properties;

    public CustomerServiceQueryAdapter(
            SpringDataUserRepository userRepository,
            SpringDataSubscriptionRepository subscriptionRepository,
            SpringDataPlanSelectionRepository planSelectionRepository,
            SpringDataXuiClientProvisionRepository provisionRepository,
            CustomerServiceStatusMapper statusMapper,
            CustomerServiceDisplayNamePolicy displayNamePolicy,
            SystemClockPort clock,
            CustomerServicesTelegramProperties properties
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository, "subscriptionRepository must not be null");
        this.planSelectionRepository = Objects.requireNonNull(planSelectionRepository, "planSelectionRepository must not be null");
        this.provisionRepository = Objects.requireNonNull(provisionRepository, "provisionRepository must not be null");
        this.statusMapper = Objects.requireNonNull(statusMapper, "statusMapper must not be null");
        this.displayNamePolicy = Objects.requireNonNull(displayNamePolicy, "displayNamePolicy must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public List<CustomerServiceSummaryResult> findAllByTelegramUserId(long telegramUserId) {
        return userRepository.findByTelegramUserId(telegramUserId)
                .map(user -> {
                    List<Subscription> subscriptions = subscriptionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
                    Map<UUID, PlanSelection> selections = planSelections(subscriptions);
                    Map<UUID, XuiClientProvision> provisions = provisions(subscriptions);
                    Instant now = clock.now();
                    return subscriptions.stream()
                            .map(subscription -> toSummary(subscription, selections.get(subscription.getPlanSelectionId()), provisions.get(subscription.getXuiClientProvisionId()), now))
                            .toList();
                })
                .orElseGet(List::of);
    }

    @Override
    public Optional<CustomerServiceDetailsResult> findDetails(long telegramUserId, UUID subscriptionId) {
        UUID requiredSubscriptionId = Objects.requireNonNull(subscriptionId, "subscriptionId must not be null");
        return userRepository.findByTelegramUserId(telegramUserId)
                .flatMap(user -> subscriptionRepository.findByUserIdAndId(user.getId(), requiredSubscriptionId))
                .map(subscription -> {
                    PlanSelection selection = planSelectionRepository.findById(subscription.getPlanSelectionId()).orElse(null);
                    XuiClientProvision provision = provisionRepository.findById(subscription.getXuiClientProvisionId()).orElse(null);
                    return toDetails(subscription, selection, provision, clock.now());
                });
    }

    private Map<UUID, PlanSelection> planSelections(List<Subscription> subscriptions) {
        List<UUID> ids = subscriptions.stream().map(Subscription::getPlanSelectionId).distinct().toList();
        return planSelectionRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(PlanSelection::getId, Function.identity()));
    }

    private Map<UUID, XuiClientProvision> provisions(List<Subscription> subscriptions) {
        List<UUID> ids = subscriptions.stream().map(Subscription::getXuiClientProvisionId).distinct().toList();
        return provisionRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(XuiClientProvision::getId, Function.identity()));
    }

    private CustomerServiceSummaryResult toSummary(
            Subscription subscription,
            PlanSelection selection,
            XuiClientProvision provision,
            Instant now
    ) {
        CustomerServiceDetailsResult details = toDetails(subscription, selection, provision, now);
        return new CustomerServiceSummaryResult(
                details.subscriptionId(),
                details.displayName(),
                details.serviceUsername(),
                details.status(),
                details.planName(),
                details.totalTrafficBytes(),
                details.usedTrafficBytes(),
                details.remainingTrafficBytes(),
                details.usageUpdatedAt(),
                details.expiresAt(),
                details.remainingDuration(),
                details.contentAvailable(),
                details.qrAvailable(),
                details.vlessAvailable(),
                details.renewalAvailable()
        );
    }

    private CustomerServiceDetailsResult toDetails(
            Subscription subscription,
            PlanSelection selection,
            XuiClientProvision provision,
            Instant now
    ) {
        String planName = selection == null ? "VPN" : selection.getPlanNameSnapshot();
        CustomerServiceStatus status = statusMapper.map(subscription, provision, now);
        String displayName = displayNamePolicy.displayName(subscription, provision, planName);
        Usage usage = usage(selection, provision, now);
        Optional<Instant> expiresAt = Optional.ofNullable(subscription.getExpiresAt() == null && provision != null ? provision.getExpiresAt() : subscription.getExpiresAt());
        Optional<Duration> remaining = expiresAt.filter(expiry -> expiry.isAfter(now)).map(expiry -> Duration.between(now, expiry));
        boolean deliverable = status != CustomerServiceStatus.REVOKED
                && status != CustomerServiceStatus.FAILED
                && status != CustomerServiceStatus.PROVISIONING
                && status != CustomerServiceStatus.UNKNOWN;
        return new CustomerServiceDetailsResult(
                subscription.getId(),
                displayName,
                displayNamePolicy.serviceUsername(subscription, provision, planName),
                status,
                planName,
                Duration.ofDays(selection == null ? 0 : selection.getDurationDaysSnapshot()),
                usage.total(),
                usage.used(),
                usage.remaining(),
                usage.updatedAt(),
                usage.freshness(),
                Optional.ofNullable(subscription.getActivatedAt()),
                expiresAt,
                remaining,
                deliverable && properties.allowSubscriptionDelivery(),
                deliverable && properties.allowQrDelivery(),
                deliverable && properties.allowVlessDelivery(),
                false,
                provision != null
        );
    }

    private Usage usage(PlanSelection selection, XuiClientProvision provision, Instant now) {
        if (!properties.showUsage() || provision == null || provision.getLastSynchronizedAt() == null) {
            return Usage.unavailable();
        }
        OptionalLong total = selection == null || selection.getTrafficLimitBytesSnapshot() == null
                ? OptionalLong.empty()
                : OptionalLong.of(selection.getTrafficLimitBytesSnapshot());
        long used = Math.max(provision.getLastKnownTotalBytes(), 0L);
        OptionalLong remaining = total.isPresent() ? OptionalLong.of(Math.max(total.getAsLong() - used, 0L)) : OptionalLong.empty();
        UsageFreshness freshness = provision.getLastSynchronizedAt().plus(properties.usageFreshnessTtl()).isAfter(now)
                ? UsageFreshness.FRESH
                : UsageFreshness.STALE;
        return new Usage(total, OptionalLong.of(used), remaining, Optional.of(provision.getLastSynchronizedAt()), freshness);
    }

    private record Usage(
            OptionalLong total,
            OptionalLong used,
            OptionalLong remaining,
            Optional<Instant> updatedAt,
            UsageFreshness freshness
    ) {
        static Usage unavailable() {
            return new Usage(OptionalLong.empty(), OptionalLong.empty(), OptionalLong.empty(), Optional.empty(), UsageFreshness.UNAVAILABLE);
        }
    }
}
