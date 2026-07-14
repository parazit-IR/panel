package com.parazit.panel.application.renewal;

import com.parazit.panel.application.customer.CustomerServiceDisplayNamePolicy;
import com.parazit.panel.application.customer.CustomerServiceStatusMapper;
import com.parazit.panel.application.customer.result.CustomerServiceStatus;
import com.parazit.panel.application.port.in.renewal.CreateRenewalOrderUseCase;
import com.parazit.panel.application.port.in.renewal.GetRenewalPreInvoiceUseCase;
import com.parazit.panel.application.port.in.renewal.GetRenewalTargetDetailsUseCase;
import com.parazit.panel.application.port.in.renewal.ListRenewableServicesUseCase;
import com.parazit.panel.application.port.in.renewal.ListRenewalPlansUseCase;
import com.parazit.panel.application.port.in.renewal.SelectRenewalPlanUseCase;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.purchase.result.AvailablePaymentMethodResult;
import com.parazit.panel.application.renewal.command.CreateRenewalOrderCommand;
import com.parazit.panel.application.renewal.command.GetRenewalPreInvoiceCommand;
import com.parazit.panel.application.renewal.command.GetRenewalTargetDetailsCommand;
import com.parazit.panel.application.renewal.command.ListRenewableServicesCommand;
import com.parazit.panel.application.renewal.command.ListRenewalPlansCommand;
import com.parazit.panel.application.renewal.command.SelectRenewalPlanCommand;
import com.parazit.panel.application.renewal.result.CreateRenewalOrderResult;
import com.parazit.panel.application.renewal.result.RenewableServicePageResult;
import com.parazit.panel.application.renewal.result.RenewableServiceSummaryResult;
import com.parazit.panel.application.renewal.result.RenewalPlanPageResult;
import com.parazit.panel.application.renewal.result.RenewalPlanSummaryResult;
import com.parazit.panel.application.renewal.result.RenewalPreInvoiceResult;
import com.parazit.panel.application.renewal.result.RenewalSelectionResult;
import com.parazit.panel.application.renewal.result.RenewalTargetDetailsResult;
import com.parazit.panel.application.sales.PaymentMethodCapability;
import com.parazit.panel.application.sales.SalesAvailabilityService;
import com.parazit.panel.config.properties.RenewalProperties;
import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.OrderType;
import com.parazit.panel.domain.order.RenewalSnapshot;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanStatus;
import com.parazit.panel.domain.plan.PlanType;
import com.parazit.panel.domain.plan.repository.PlanRepository;
import com.parazit.panel.domain.plan.selection.PlanSelection;
import com.parazit.panel.domain.plan.selection.PlanSelectionStatus;
import com.parazit.panel.domain.plan.selection.SelectionType;
import com.parazit.panel.domain.plan.selection.repository.PlanSelectionRepository;
import com.parazit.panel.domain.subscription.Subscription;
import com.parazit.panel.domain.subscription.repository.SubscriptionRepository;
import com.parazit.panel.domain.telegram.purchase.PurchaseFlowType;
import com.parazit.panel.domain.telegram.purchase.TelegramPurchaseSession;
import com.parazit.panel.domain.telegram.purchase.repository.TelegramPurchaseSessionRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserStatus;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import com.parazit.panel.domain.xui.provisioning.repository.XuiClientProvisionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RenewalFlowService implements
        ListRenewableServicesUseCase,
        GetRenewalTargetDetailsUseCase,
        ListRenewalPlansUseCase,
        SelectRenewalPlanUseCase,
        GetRenewalPreInvoiceUseCase,
        CreateRenewalOrderUseCase {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final XuiClientProvisionRepository provisionRepository;
    private final PlanRepository planRepository;
    private final PlanSelectionRepository selectionRepository;
    private final TelegramPurchaseSessionRepository sessionRepository;
    private final OrderRepository orderRepository;
    private final RenewableSubscriptionPolicy renewablePolicy;
    private final RenewalPlanEligibilityPolicy planEligibilityPolicy;
    private final RenewalPlanCompatibilityPolicy compatibilityPolicy;
    private final RenewalExpiryCalculator expiryCalculator;
    private final CustomerServiceStatusMapper statusMapper;
    private final CustomerServiceDisplayNamePolicy displayNamePolicy;
    private final SalesAvailabilityService salesAvailabilityService;
    private final RenewalMetrics metrics;
    private final RenewalProperties properties;
    private final SystemClockPort clock;

    public RenewalFlowService(
            UserRepository userRepository,
            SubscriptionRepository subscriptionRepository,
            XuiClientProvisionRepository provisionRepository,
            PlanRepository planRepository,
            PlanSelectionRepository selectionRepository,
            TelegramPurchaseSessionRepository sessionRepository,
            OrderRepository orderRepository,
            RenewableSubscriptionPolicy renewablePolicy,
            RenewalPlanEligibilityPolicy planEligibilityPolicy,
            RenewalPlanCompatibilityPolicy compatibilityPolicy,
            RenewalExpiryCalculator expiryCalculator,
            CustomerServiceStatusMapper statusMapper,
            CustomerServiceDisplayNamePolicy displayNamePolicy,
            SalesAvailabilityService salesAvailabilityService,
            RenewalMetrics metrics,
            RenewalProperties properties,
            SystemClockPort clock
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository, "subscriptionRepository must not be null");
        this.provisionRepository = Objects.requireNonNull(provisionRepository, "provisionRepository must not be null");
        this.planRepository = Objects.requireNonNull(planRepository, "planRepository must not be null");
        this.selectionRepository = Objects.requireNonNull(selectionRepository, "selectionRepository must not be null");
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository must not be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.renewablePolicy = Objects.requireNonNull(renewablePolicy, "renewablePolicy must not be null");
        this.planEligibilityPolicy = Objects.requireNonNull(planEligibilityPolicy, "planEligibilityPolicy must not be null");
        this.compatibilityPolicy = Objects.requireNonNull(compatibilityPolicy, "compatibilityPolicy must not be null");
        this.expiryCalculator = Objects.requireNonNull(expiryCalculator, "expiryCalculator must not be null");
        this.statusMapper = Objects.requireNonNull(statusMapper, "statusMapper must not be null");
        this.displayNamePolicy = Objects.requireNonNull(displayNamePolicy, "displayNamePolicy must not be null");
        this.salesAvailabilityService = Objects.requireNonNull(salesAvailabilityService, "salesAvailabilityService must not be null");
        this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public RenewableServicePageResult list(ListRenewableServicesCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        User user = user(command.telegramUserId());
        int size = boundedSize(command.size(), properties.servicePageSize());
        int page = Math.max(command.page(), 0);
        Instant now = clock.now();
        List<RenewableServiceSummaryResult> all = subscriptionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(subscription -> serviceSummary(user, subscription, now))
                .sorted(serviceComparator())
                .toList();
        int totalPages = all.isEmpty() ? 0 : (int) Math.ceil((double) all.size() / (double) size);
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        RenewableServicePageResult result = new RenewableServicePageResult(
                all.subList(from, to),
                page,
                size,
                all.size(),
                totalPages,
                totalPages > 0 && page + 1 < totalPages,
                page > 0 && totalPages > 0
        );
        metrics.serviceList(all.isEmpty() ? "empty" : "success");
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public RenewalTargetDetailsResult get(GetRenewalTargetDetailsCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        TargetContext target = target(user(command.telegramUserId()), command.subscriptionId(), false);
        return targetDetails(target, clock.now());
    }

    @Override
    @Transactional(readOnly = true)
    public RenewalPlanPageResult list(ListRenewalPlansCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        TargetContext target = target(user(command.telegramUserId()), command.subscriptionId(), false);
        RenewableSubscriptionDecision decision = renewability(target, false, clock.now());
        if (!decision.renewable()) {
            throw new RenewalFlowException(decision.messageKey());
        }
        int size = boundedSize(command.size(), properties.planPageSize());
        int page = Math.max(command.page(), 0);
        List<RenewalPlanSummaryResult> all = planRepository.findAllByStatusOrderByDisplayOrderAscCodeAsc(PlanStatus.ACTIVE)
                .stream()
                .filter(planEligibilityPolicy::eligible)
                .filter(plan -> compatibilityPolicy.compatible(target.subscription(), target.provision(), plan))
                .map(this::planSummary)
                .toList();
        int totalPages = all.isEmpty() ? 0 : (int) Math.ceil((double) all.size() / (double) size);
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        RenewalPlanPageResult result = new RenewalPlanPageResult(all.subList(from, to), page, size, all.size(), totalPages, totalPages > 0 && page + 1 < totalPages, page > 0 && totalPages > 0);
        metrics.planList(all.isEmpty() ? "empty" : "success");
        return result;
    }

    @Override
    @Transactional
    public RenewalSelectionResult select(SelectRenewalPlanCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        User user = userForUpdate(command.telegramUserId());
        TargetContext target = target(user, command.subscriptionId(), false);
        Instant now = clock.now();
        requireRenewable(target, false, now);
        Plan plan = renewalPlan(command.planId(), target);
        PlanSelection selection = activeRenewalSelection(user, target.subscription().getId())
                .filter(existing -> existing.getPlanId().equals(plan.getId()))
                .filter(existing -> !existing.isExpiredAt(now))
                .orElseGet(() -> createRenewalSelection(user, target, plan, now));
        TelegramPurchaseSession session = renewalSession(user, target, selection, now);
        metrics.selection("success");
        return selectionResult(target, selection, session);
    }

    @Override
    @Transactional
    public RenewalPreInvoiceResult get(GetRenewalPreInvoiceCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        User user = user(command.telegramUserId());
        TelegramPurchaseSession session = activeSession(user, command.purchaseSessionId());
        PlanSelection selection = activeSelection(session);
        TargetContext target = target(user, session.getTargetSubscriptionId(), false);
        requireRenewable(target, true, clock.now());
        renewalPlan(selection.getPlanId(), target);
        session.showPreInvoice();
        sessionRepository.save(session);
        metrics.preInvoice("success");
        return preInvoice(user, target, session, selection);
    }

    @Override
    @Transactional
    public CreateRenewalOrderResult create(CreateRenewalOrderCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        User user = userForUpdate(command.telegramUserId());
        TelegramPurchaseSession session = activeSession(user, command.purchaseSessionId());
        PlanSelection selection = activeSelection(session);
        TargetContext target = target(user, session.getTargetSubscriptionId(), true);
        Order existing = orderRepository.findByPlanSelectionId(selection.getId())
                .filter(order -> order.getUserId().equals(user.getId()))
                .orElse(null);
        if (existing == null) {
            existing = orderRepository.findActiveByTargetSubscriptionIdAndType(target.subscription().getId(), OrderType.RENEWAL)
                    .filter(order -> order.getUserId().equals(user.getId()))
                    .orElse(null);
        }
        boolean reused = existing != null;
        Order order = reused ? existing : createOrder(user, target, selection);
        if (order.getStatus() == com.parazit.panel.domain.order.OrderStatus.CREATED) {
            order.markPaymentPending();
            order = orderRepository.save(order);
        }
        session.attachOrder(order.getId());
        session.showPaymentMethods();
        sessionRepository.save(session);
        if (reused) {
            metrics.orderReuse("success");
        } else {
            metrics.orderCreation("success");
        }
        return new CreateRenewalOrderResult(session.getId(), order.getId(), order.getFinalAmount(), order.getCurrency(), availableMethods(), clock.now(), reused);
    }

    private PlanSelection createRenewalSelection(User user, TargetContext target, Plan plan, Instant now) {
        selectionRepository.findActiveByUserId(user.getId()).ifPresent(existing -> {
            if (existing.isExpiredAt(now)) {
                existing.expire(now);
            } else {
                existing.clear(now);
            }
            selectionRepository.save(existing);
        });
        try {
            return selectionRepository.save(PlanSelection.createRenewal(
                    user.getId(),
                    target.subscription().getId(),
                    plan,
                    now,
                    properties.renewalSelectionTtl()
            ));
        } catch (DataIntegrityViolationException exception) {
            return activeRenewalSelection(user, target.subscription().getId()).orElseThrow(() -> exception);
        }
    }

    private TelegramPurchaseSession renewalSession(User user, TargetContext target, PlanSelection selection, Instant now) {
        Optional<TelegramPurchaseSession> existing = sessionRepository.findByPlanSelectionId(selection.getId())
                .filter(session -> session.getFlowType() == PurchaseFlowType.RENEWAL)
                .filter(session -> session.activeAt(now));
        if (existing.isPresent()) {
            return existing.get();
        }
        sessionRepository.findAllActiveByUserIdAndFlowType(user.getId(), PurchaseFlowType.RENEWAL).forEach(active -> {
            if (active.expiredAt(now)) {
                active.expire(now);
            } else {
                active.cancel();
            }
            sessionRepository.save(active);
        });
        return sessionRepository.save(TelegramPurchaseSession.createRenewal(
                user.getId(),
                user.getTelegramUserId(),
                selection.getId(),
                target.subscription().getId(),
                selection.getExpiresAt()
        ));
    }

    private Order createOrder(User user, TargetContext target, PlanSelection selection) {
        requireRenewable(target, false, clock.now());
        renewalPlan(selection.getPlanId(), target);
        RenewalSnapshot snapshot = snapshot(target, selection);
        try {
            return orderRepository.save(Order.createRenewal(
                    user.getId(),
                    selection.getPlanId(),
                    selection.getId(),
                    target.subscription().getId(),
                    snapshot,
                    snapshot.finalAmount().amount(),
                    snapshot.finalAmount().currency().name()
            ));
        } catch (DataIntegrityViolationException exception) {
            return orderRepository.findByPlanSelectionId(selection.getId())
                    .or(() -> orderRepository.findActiveByTargetSubscriptionIdAndType(target.subscription().getId(), OrderType.RENEWAL))
                    .orElseThrow(() -> exception);
        }
    }

    private RenewalSnapshot snapshot(TargetContext target, PlanSelection selection) {
        Money amount = new Money(selection.getPriceAmountSnapshot(), selection.getCurrencySnapshot());
        return new RenewalSnapshot(
                target.subscription().getId(),
                target.provision().getId(),
                target.displayName(),
                target.serviceUsername(),
                target.currentExpiry(),
                target.provision().getTrafficLimitBytes(),
                target.provision().getLastKnownTotalBytes(),
                Duration.ofDays(selection.getDurationDaysSnapshot()),
                selection.getTrafficLimitBytesSnapshot(),
                properties.defaultTrafficPolicy(),
                amount,
                amount,
                selection.getPlanNameSnapshot(),
                currentPlan(selection).getDescription(),
                selection.getPlanId(),
                clock.now()
        );
    }

    private RenewalPreInvoiceResult preInvoice(User user, TargetContext target, TelegramPurchaseSession session, PlanSelection selection) {
        Instant now = clock.now();
        Instant proposedExpiry = expiryCalculator.proposedExpiry(
                target.currentExpiry(),
                Duration.ofDays(selection.getDurationDaysSnapshot()),
                properties.expiryPolicy(),
                now
        );
        Money amount = new Money(selection.getPriceAmountSnapshot(), selection.getCurrencySnapshot());
        Order order = orderRepository.findByPlanSelectionId(selection.getId())
                .filter(existing -> existing.getUserId().equals(user.getId()))
                .orElse(null);
        Money original = order == null ? amount : new Money(order.getBaseAmount(), selection.getCurrencySnapshot());
        Money finalAmount = order == null ? amount : new Money(order.getFinalAmount(), selection.getCurrencySnapshot());
        return new RenewalPreInvoiceResult(
                session.getId(),
                selection.getId(),
                target.subscription().getId(),
                userDisplayName(user),
                target.displayName(),
                target.serviceUsername(),
                target.status(),
                Optional.ofNullable(target.currentExpiry()),
                optionalLong(target.provision().getTrafficLimitBytes()),
                remainingTraffic(target.provision()),
                selection.getPlanNameSnapshot(),
                currentPlan(selection).getDescription(),
                Duration.ofDays(selection.getDurationDaysSnapshot()),
                optionalLong(selection.getTrafficLimitBytesSnapshot()),
                properties.defaultTrafficPolicy(),
                properties.expiryPolicy(),
                proposedExpiry,
                original,
                finalAmount,
                selection.getCurrencySnapshot(),
                selection.getExpiresAt(),
                salesAvailabilityService.manualPaymentAvailable(),
                salesAvailabilityService.onlinePaymentAvailable()
        );
    }

    private RenewalSelectionResult selectionResult(TargetContext target, PlanSelection selection, TelegramPurchaseSession session) {
        return new RenewalSelectionResult(
                selection.getId(),
                session.getId(),
                target.subscription().getId(),
                target.displayName(),
                target.serviceUsername(),
                selection.getPlanNameSnapshot(),
                Duration.ofDays(selection.getDurationDaysSnapshot()),
                optionalLong(selection.getTrafficLimitBytesSnapshot()),
                new Money(selection.getPriceAmountSnapshot(), selection.getCurrencySnapshot()),
                selection.getExpiresAt()
        );
    }

    private RenewalTargetDetailsResult targetDetails(TargetContext target, Instant now) {
        RenewableSubscriptionDecision decision = renewability(target, false, now);
        return new RenewalTargetDetailsResult(
                target.subscription().getId(),
                target.displayName(),
                target.serviceUsername(),
                target.status(),
                target.planName(),
                Optional.ofNullable(target.subscription().getActivatedAt()),
                Optional.ofNullable(target.currentExpiry()),
                remainingDuration(target.currentExpiry(), now),
                optionalLong(target.provision().getTrafficLimitBytes()),
                OptionalLong.of(target.provision().getLastKnownTotalBytes()),
                remainingTraffic(target.provision()),
                decision.renewable(),
                decision.renewable() ? Optional.empty() : Optional.of(decision.messageKey())
        );
    }

    private RenewableServiceSummaryResult serviceSummary(User user, Subscription subscription, Instant now) {
        XuiClientProvision provision = provisionRepository.findById(subscription.getXuiClientProvisionId()).orElse(null);
        PlanSelection originalSelection = selectionRepository.findById(subscription.getPlanSelectionId()).orElse(null);
        String planName = originalSelection == null ? "VPN" : originalSelection.getPlanNameSnapshot();
        CustomerServiceStatus status = statusMapper.map(subscription, provision, now);
        String displayName = displayNamePolicy.displayName(subscription, provision, planName);
        String username = displayNamePolicy.serviceUsername(subscription, provision, planName);
        boolean hasActiveOrder = orderRepository.existsActiveByTargetSubscriptionIdAndType(subscription.getId(), OrderType.RENEWAL);
        RenewableSubscriptionDecision decision = renewablePolicy.evaluate(user.getId(), subscription, provision, hasActiveOrder, now);
        Instant expiry = currentExpiry(subscription, provision);
        return new RenewableServiceSummaryResult(
                subscription.getId(),
                displayName,
                username,
                status,
                Optional.ofNullable(expiry),
                remainingDuration(expiry, now),
                provision == null ? OptionalLong.empty() : optionalLong(provision.getTrafficLimitBytes()),
                provision == null ? OptionalLong.empty() : remainingTraffic(provision),
                decision.renewable(),
                decision.optionalReason()
        );
    }

    private TargetContext target(User user, UUID subscriptionId, boolean allowExistingOrder) {
        Subscription subscription = subscriptionRepository.findByUserIdAndId(user.getId(), Objects.requireNonNull(subscriptionId, "subscriptionId must not be null"))
                .orElseThrow(() -> new RenewalFlowException("telegram.renewal.not_available"));
        XuiClientProvision provision = provisionRepository.findById(subscription.getXuiClientProvisionId())
                .orElseThrow(() -> new RenewalFlowException("telegram.renewal.cannot_renew"));
        PlanSelection originalSelection = selectionRepository.findById(subscription.getPlanSelectionId()).orElse(null);
        String planName = originalSelection == null ? "VPN" : originalSelection.getPlanNameSnapshot();
        CustomerServiceStatus status = statusMapper.map(subscription, provision, clock.now());
        return new TargetContext(
                user,
                subscription,
                provision,
                planName,
                displayNamePolicy.displayName(subscription, provision, planName),
                displayNamePolicy.serviceUsername(subscription, provision, planName),
                status,
                currentExpiry(subscription, provision),
                allowExistingOrder
        );
    }

    private RenewableSubscriptionDecision renewability(TargetContext target, boolean ignoreActiveOrder, Instant now) {
        boolean hasActiveOrder = !ignoreActiveOrder
                && orderRepository.existsActiveByTargetSubscriptionIdAndType(target.subscription().getId(), OrderType.RENEWAL);
        return renewablePolicy.evaluate(target.user().getId(), target.subscription(), target.provision(), hasActiveOrder, now);
    }

    private void requireRenewable(TargetContext target, boolean ignoreActiveOrder, Instant now) {
        RenewableSubscriptionDecision decision = renewability(target, ignoreActiveOrder, now);
        if (!decision.renewable()) {
            metrics.rejected(decision.reason(), target.status());
            throw new RenewalFlowException(decision.messageKey());
        }
    }

    private Plan renewalPlan(UUID planId, TargetContext target) {
        Plan plan = planRepository.findByIdAndStatus(Objects.requireNonNull(planId, "planId must not be null"), PlanStatus.ACTIVE)
                .orElseThrow(() -> new RenewalFlowException("telegram.renewal.no_plan"));
        if (!planEligibilityPolicy.eligible(plan) || !compatibilityPolicy.compatible(target.subscription(), target.provision(), plan)) {
            throw new RenewalFlowException("telegram.renewal.no_plan");
        }
        return plan;
    }

    private Plan currentPlan(PlanSelection selection) {
        return planRepository.findById(selection.getPlanId())
                .orElseThrow(() -> new RenewalFlowException("telegram.renewal.no_plan"));
    }

    private Optional<PlanSelection> activeRenewalSelection(User user, UUID subscriptionId) {
        return selectionRepository.findActiveByUserIdAndTypeAndTargetSubscriptionId(user.getId(), SelectionType.RENEWAL, subscriptionId);
    }

    private TelegramPurchaseSession activeSession(User user, UUID purchaseSessionId) {
        TelegramPurchaseSession session = sessionRepository.findByIdForUpdate(Objects.requireNonNull(purchaseSessionId, "purchaseSessionId must not be null"))
                .orElseThrow(() -> new RenewalFlowException("telegram.renewal.selection_expired"));
        if (!session.getUserId().equals(user.getId())
                || !session.getTelegramUserId().equals(user.getTelegramUserId())
                || session.getFlowType() != PurchaseFlowType.RENEWAL) {
            throw new RenewalFlowException("telegram.renewal.not_available");
        }
        Instant now = clock.now();
        if (session.expiredAt(now)) {
            session.expire(now);
            sessionRepository.save(session);
            throw new RenewalFlowException("telegram.renewal.selection_expired");
        }
        if (!session.activeAt(now)) {
            throw new RenewalFlowException("telegram.renewal.selection_expired");
        }
        return session;
    }

    private PlanSelection activeSelection(TelegramPurchaseSession session) {
        PlanSelection selection = selectionRepository.findById(session.getPlanSelectionId())
                .orElseThrow(() -> new RenewalFlowException("telegram.renewal.selection_expired"));
        if (selection.getSelectionType() != SelectionType.RENEWAL || selection.getStatus() != PlanSelectionStatus.ACTIVE || selection.isExpiredAt(clock.now())) {
            throw new RenewalFlowException("telegram.renewal.selection_expired");
        }
        return selection;
    }

    private User user(long telegramUserId) {
        return userRepository.findByTelegramUserId(telegramUserId)
                .filter(this::eligibleUser)
                .orElseThrow(() -> new RenewalFlowException("telegram.error.unknown_message"));
    }

    private User userForUpdate(long telegramUserId) {
        return userRepository.findByTelegramUserIdForUpdate(telegramUserId)
                .filter(this::eligibleUser)
                .orElseThrow(() -> new RenewalFlowException("telegram.error.unknown_message"));
    }

    private boolean eligibleUser(User user) {
        return user.getStatus() == UserStatus.ACTIVE && !Boolean.TRUE.equals(user.getBlocked());
    }

    private RenewalPlanSummaryResult planSummary(Plan plan) {
        return new RenewalPlanSummaryResult(
                plan.getId(),
                plan.getName(),
                plan.getDescription(),
                Duration.ofDays(plan.getDurationDays()),
                plan.getType() == PlanType.UNLIMITED ? OptionalLong.empty() : OptionalLong.of(plan.getTrafficLimitBytes()),
                plan.getMaxDevices() == null ? OptionalInt.empty() : OptionalInt.of(plan.getMaxDevices()),
                new Money(plan.getPriceAmount(), plan.getCurrency()),
                properties.defaultTrafficPolicy(),
                true
        );
    }

    private List<AvailablePaymentMethodResult> availableMethods() {
        return List.of(
                        new AvailablePaymentMethodResult(
                                PaymentMethod.CARD_TO_CARD,
                                salesAvailabilityService.manualPaymentAvailable(),
                                "telegram.purchase.manual_payment",
                                10,
                                PaymentMethodCapability.MANUAL_TRANSFER,
                                "MANUAL_PAYMENT_DISABLED"
                        ),
                        new AvailablePaymentMethodResult(
                                PaymentMethod.ZARINPAL,
                                salesAvailabilityService.onlinePaymentAvailable(),
                                "telegram.purchase.online_payment",
                                20,
                                PaymentMethodCapability.ONLINE_REDIRECT,
                                "ONLINE_PAYMENT_DISABLED"
                        ),
                        new AvailablePaymentMethodResult(
                                PaymentMethod.WALLET,
                                salesAvailabilityService.walletPaymentAvailable(),
                                "telegram.purchase.wallet_payment",
                                30,
                                PaymentMethodCapability.INTERNAL_BALANCE,
                                "WALLET_PAYMENT_DISABLED"
                        )
                )
                .stream()
                .filter(AvailablePaymentMethodResult::enabled)
                .sorted(Comparator.comparingInt(AvailablePaymentMethodResult::displayOrder))
                .toList();
    }

    private static Comparator<RenewableServiceSummaryResult> serviceComparator() {
        return Comparator.comparingInt((RenewableServiceSummaryResult service) -> service.renewable() ? 0 : 1)
                .thenComparing(service -> service.expiresAt().orElse(Instant.MAX))
                .thenComparing(RenewableServiceSummaryResult::subscriptionId);
    }

    private static OptionalLong optionalLong(Long value) {
        return value == null ? OptionalLong.empty() : OptionalLong.of(value);
    }

    private static OptionalLong optionalLong(long value) {
        return OptionalLong.of(value);
    }

    private static OptionalLong remainingTraffic(XuiClientProvision provision) {
        long total = provision.getTrafficLimitBytes();
        if (total <= 0) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(Math.max(total - provision.getLastKnownTotalBytes(), 0L));
    }

    private static Optional<Duration> remainingDuration(Instant expiry, Instant now) {
        if (expiry == null || !expiry.isAfter(now)) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(now, expiry));
    }

    private static Instant currentExpiry(Subscription subscription, XuiClientProvision provision) {
        return subscription.getExpiresAt() == null ? provision.getExpiresAt() : subscription.getExpiresAt();
    }

    private static int boundedSize(int requested, int configured) {
        int fallback = configured <= 0 ? 5 : configured;
        return Math.max(1, Math.min(requested <= 0 ? fallback : requested, 25));
    }

    private static String userDisplayName(User user) {
        String fullName = (user.getFirstName() + " " + (user.getLastName() == null ? "" : user.getLastName())).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return "@" + user.getUsername();
        }
        return String.valueOf(user.getTelegramUserId());
    }

    private record TargetContext(
            User user,
            Subscription subscription,
            XuiClientProvision provision,
            String planName,
            String displayName,
            String serviceUsername,
            CustomerServiceStatus status,
            Instant currentExpiry,
            boolean allowExistingOrder
    ) {
    }
}
