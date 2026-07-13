package com.parazit.panel.application.purchase;

import com.parazit.panel.application.payment.PaymentConflictException;
import com.parazit.panel.application.payment.command.CreatePaymentCommand;
import com.parazit.panel.application.payment.manual.command.InitializeManualCardPaymentCommand;
import com.parazit.panel.application.payment.manual.result.InitializeManualCardPaymentResult;
import com.parazit.panel.application.payment.result.PaymentResult;
import com.parazit.panel.application.payment.zarinpal.command.InitializeZarinpalPaymentCommand;
import com.parazit.panel.application.payment.zarinpal.result.InitializeZarinpalPaymentResult;
import com.parazit.panel.application.plan.catalog.AvailablePlanNotFoundException;
import com.parazit.panel.application.plan.selection.command.SelectPlanCommand;
import com.parazit.panel.application.plan.selection.result.PlanSelectionResult;
import com.parazit.panel.application.port.in.payment.CreatePaymentUseCase;
import com.parazit.panel.application.port.in.payment.manual.InitializeManualCardPaymentUseCase;
import com.parazit.panel.application.port.in.payment.zarinpal.InitializeZarinpalPaymentUseCase;
import com.parazit.panel.application.port.in.plan.selection.SelectPlanUseCase;
import com.parazit.panel.application.port.in.purchase.ContinuePurchaseToPaymentUseCase;
import com.parazit.panel.application.port.in.purchase.GetPurchasePreInvoiceUseCase;
import com.parazit.panel.application.port.in.purchase.SelectPurchasePaymentMethodUseCase;
import com.parazit.panel.application.port.in.purchase.SelectPurchasePlanUseCase;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.purchase.result.AvailablePaymentMethodResult;
import com.parazit.panel.application.purchase.result.PurchaseManualPaymentResult;
import com.parazit.panel.application.purchase.result.PurchaseOnlinePaymentResult;
import com.parazit.panel.application.purchase.result.PurchasePaymentMethodsResult;
import com.parazit.panel.application.purchase.result.PurchasePreInvoiceResult;
import com.parazit.panel.application.sales.PaymentMethodCapability;
import com.parazit.panel.application.sales.PlanPurchasability;
import com.parazit.panel.application.sales.PlanPurchasabilityPolicy;
import com.parazit.panel.application.sales.SalesAvailabilityService;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanStatus;
import com.parazit.panel.domain.plan.repository.PlanRepository;
import com.parazit.panel.domain.plan.selection.PlanSelection;
import com.parazit.panel.domain.plan.selection.PlanSelectionStatus;
import com.parazit.panel.domain.plan.selection.repository.PlanSelectionRepository;
import com.parazit.panel.domain.telegram.purchase.TelegramPurchaseSession;
import com.parazit.panel.domain.telegram.purchase.repository.TelegramPurchaseSessionRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseFlowService implements
        SelectPurchasePlanUseCase,
        GetPurchasePreInvoiceUseCase,
        ContinuePurchaseToPaymentUseCase,
        SelectPurchasePaymentMethodUseCase {

    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final PlanSelectionRepository selectionRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final TelegramPurchaseSessionRepository sessionRepository;
    private final SelectPlanUseCase selectPlanUseCase;
    private final CreatePaymentUseCase createPaymentUseCase;
    private final InitializeManualCardPaymentUseCase manualPaymentUseCase;
    private final InitializeZarinpalPaymentUseCase zarinpalPaymentUseCase;
    private final SalesAvailabilityService salesAvailabilityService;
    private final PlanPurchasabilityPolicy purchasabilityPolicy;
    private final SystemClockPort clock;

    public PurchaseFlowService(
            UserRepository userRepository,
            PlanRepository planRepository,
            PlanSelectionRepository selectionRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            TelegramPurchaseSessionRepository sessionRepository,
            SelectPlanUseCase selectPlanUseCase,
            CreatePaymentUseCase createPaymentUseCase,
            InitializeManualCardPaymentUseCase manualPaymentUseCase,
            InitializeZarinpalPaymentUseCase zarinpalPaymentUseCase,
            SalesAvailabilityService salesAvailabilityService,
            PlanPurchasabilityPolicy purchasabilityPolicy,
            SystemClockPort clock
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.planRepository = Objects.requireNonNull(planRepository, "planRepository must not be null");
        this.selectionRepository = Objects.requireNonNull(selectionRepository, "selectionRepository must not be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository must not be null");
        this.selectPlanUseCase = Objects.requireNonNull(selectPlanUseCase, "selectPlanUseCase must not be null");
        this.createPaymentUseCase = Objects.requireNonNull(createPaymentUseCase, "createPaymentUseCase must not be null");
        this.manualPaymentUseCase = Objects.requireNonNull(manualPaymentUseCase, "manualPaymentUseCase must not be null");
        this.zarinpalPaymentUseCase = Objects.requireNonNull(zarinpalPaymentUseCase, "zarinpalPaymentUseCase must not be null");
        this.salesAvailabilityService = Objects.requireNonNull(salesAvailabilityService, "salesAvailabilityService must not be null");
        this.purchasabilityPolicy = Objects.requireNonNull(purchasabilityPolicy, "purchasabilityPolicy must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    @Transactional
    public PurchasePreInvoiceResult select(long telegramUserId, UUID planId) {
        requireSales();
        Plan plan = planRepository.findByIdAndStatus(Objects.requireNonNull(planId, "planId must not be null"), PlanStatus.ACTIVE)
                .orElseThrow(() -> new AvailablePlanNotFoundException(planId));
        requirePurchasable(plan);

        PlanSelectionResult selection = selectPlanUseCase.select(new SelectPlanCommand(telegramUserId, planId));
        User user = user(telegramUserId);
        TelegramPurchaseSession session = sessionRepository.findByPlanSelectionId(selection.selectionId())
                .filter(existing -> existing.getUserId().equals(user.getId()))
                .filter(existing -> existing.activeAt(clock.now()))
                .orElseGet(() -> createSession(user, selection));
        session.showPreInvoice();
        return toPreInvoice(user, sessionRepository.save(session), selection(selection.selectionId()));
    }

    @Override
    @Transactional
    public PurchasePreInvoiceResult get(long telegramUserId, UUID purchaseSessionId) {
        User user = user(telegramUserId);
        TelegramPurchaseSession session = activeSession(user, purchaseSessionId);
        PlanSelection selection = selection(session.getPlanSelectionId());
        requireActiveSelection(selection);
        requireSales();
        requirePurchasable(currentPlan(selection));
        session.showPreInvoice();
        return toPreInvoice(user, sessionRepository.save(session), selection);
    }

    @Override
    @Transactional
    public PurchasePaymentMethodsResult continueToPayment(long telegramUserId, UUID purchaseSessionId) {
        User user = user(telegramUserId);
        TelegramPurchaseSession session = activeSession(user, purchaseSessionId);
        PlanSelection selection = selection(session.getPlanSelectionId());
        requireActiveSelection(selection);
        requireSales();
        requirePurchasable(currentPlan(selection));

        Order order = orderRepository.findByPlanSelectionId(selection.getId())
                .filter(existing -> existing.getUserId().equals(user.getId()))
                .orElseGet(() -> createOrder(user, selection));
        session.attachOrder(order.getId());
        session.showPaymentMethods();
        sessionRepository.save(session);
        return new PurchasePaymentMethodsResult(
                session.getId(),
                order.getId(),
                order.getFinalAmount(),
                order.getCurrency(),
                availableMethods(),
                clock.now()
        );
    }

    @Override
    @Transactional
    public PurchaseManualPaymentResult selectManual(long telegramUserId, UUID purchaseSessionId) {
        if (!salesAvailabilityService.manualPaymentAvailable()) {
            throw new PurchaseFlowException("telegram.purchase.manual_payment_disabled");
        }
        User user = user(telegramUserId);
        TelegramPurchaseSession session = activeSession(user, purchaseSessionId);
        Order order = ensureOrder(user, session);
        Payment payment = payment(order, user, PaymentMethod.CARD_TO_CARD);
        session.attachPayment(payment.getId());
        sessionRepository.save(session);
        InitializeManualCardPaymentResult result = manualPaymentUseCase.initialize(new InitializeManualCardPaymentCommand(
                deterministicUuid("manual-instruction", session.getId(), payment.getId()),
                payment.getId(),
                telegramUserId
        ));
        return new PurchaseManualPaymentResult(session.getId(), order.getId(), result.instruction());
    }

    @Override
    @Transactional
    public PurchaseOnlinePaymentResult selectOnline(long telegramUserId, UUID purchaseSessionId) {
        if (!salesAvailabilityService.onlinePaymentAvailable()) {
            throw new PurchaseFlowException("telegram.purchase.online_payment_disabled");
        }
        User user = user(telegramUserId);
        TelegramPurchaseSession session = activeSession(user, purchaseSessionId);
        Order order = ensureOrder(user, session);
        Payment payment = payment(order, user, PaymentMethod.ZARINPAL);
        session.attachPayment(payment.getId());
        sessionRepository.save(session);
        InitializeZarinpalPaymentResult result = zarinpalPaymentUseCase.initialize(new InitializeZarinpalPaymentCommand(
                deterministicUuid("zarinpal-request", session.getId(), payment.getId()),
                payment.getId(),
                telegramUserId,
                "VPN subscription purchase",
                null,
                null
        ));
        return new PurchaseOnlinePaymentResult(session.getId(), order.getId(), order.getFinalAmount(), order.getCurrency(), result);
    }

    private TelegramPurchaseSession createSession(User user, PlanSelectionResult selection) {
        Instant now = clock.now();
        sessionRepository.findAllActiveByUserId(user.getId()).forEach(existing -> {
            if (existing.expiredAt(now)) {
                existing.expire(now);
            } else {
                existing.cancel();
            }
            sessionRepository.save(existing);
        });
        return sessionRepository.save(TelegramPurchaseSession.create(
                user.getId(),
                user.getTelegramUserId(),
                selection.selectionId(),
                selection.expiresAt()
        ));
    }

    private Order createOrder(User user, PlanSelection selection) {
        try {
            Order order = Order.createForPlanSelection(
                    user.getId(),
                    selection.getPlanId(),
                    selection.getId(),
                    selection.getPriceAmountSnapshot(),
                    selection.getCurrencySnapshot().name()
            );
            return orderRepository.save(order);
        } catch (DataIntegrityViolationException exception) {
            return orderRepository.findByPlanSelectionId(selection.getId())
                    .orElseThrow(() -> exception);
        }
    }

    private Payment payment(Order order, User user, PaymentMethod method) {
        Payment existing = paymentRepository.findAllByOrderId(order.getId()).stream()
                .filter(payment -> payment.getMethod() == method)
                .filter(payment -> !payment.isTerminal())
                .filter(payment -> payment.getExpiresAt().isAfter(clock.now()))
                .max(Comparator.comparing(Payment::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        if (existing != null) {
            return existing;
        }
        PaymentResult result = createPaymentUseCase.create(new CreatePaymentCommand(
                order.getId(),
                user.getId(),
                method,
                order.getFinalAmount(),
                order.getCurrency()
        ));
        return paymentRepository.findById(result.id()).orElseThrow(() -> new PaymentConflictException("Payment was not persisted"));
    }

    private Order ensureOrder(User user, TelegramPurchaseSession session) {
        UUID orderId = session.getOrderId();
        if (orderId != null) {
            Order existing = orderRepository.findById(orderId).orElse(null);
            if (existing != null && existing.getUserId().equals(user.getId())) {
                return existing;
            }
        }
        return orderRepository.findByPlanSelectionId(session.getPlanSelectionId())
                .filter(order -> order.getUserId().equals(user.getId()))
                .orElseGet(() -> {
                    PlanSelection selection = selection(session.getPlanSelectionId());
                    requireActiveSelection(selection);
                    requireSales();
                    requirePurchasable(currentPlan(selection));
                    Order created = createOrder(user, selection);
                    session.attachOrder(created.getId());
                    sessionRepository.save(session);
                    return created;
                });
    }

    private PurchasePreInvoiceResult toPreInvoice(User user, TelegramPurchaseSession session, PlanSelection selection) {
        return new PurchasePreInvoiceResult(
                session.getId(),
                selection.getId(),
                displayName(user),
                serviceName(selection),
                selection.getPlanNameSnapshot(),
                currentPlanDescription(selection),
                selection.getDurationDaysSnapshot(),
                optionalLong(selection.getTrafficLimitBytesSnapshot()),
                optionalInt(selection.getMaxDevicesSnapshot()),
                selection.getPriceAmountSnapshot(),
                0L,
                selection.getPriceAmountSnapshot(),
                selection.getCurrencySnapshot(),
                false,
                false,
                salesAvailabilityService.manualPaymentAvailable(),
                salesAvailabilityService.onlinePaymentAvailable(),
                selection.getExpiresAt(),
                clock.now()
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
                        )
                ).stream()
                .filter(AvailablePaymentMethodResult::enabled)
                .sorted(Comparator.comparingInt(AvailablePaymentMethodResult::displayOrder))
                .toList();
    }

    private User user(long telegramUserId) {
        if (telegramUserId <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
        return userRepository.findByTelegramUserId(telegramUserId)
                .orElseThrow(() -> new PurchaseFlowException("telegram.error.unknown_message"));
    }

    private TelegramPurchaseSession activeSession(User user, UUID purchaseSessionId) {
        UUID requiredId = Objects.requireNonNull(purchaseSessionId, "purchaseSessionId must not be null");
        TelegramPurchaseSession session = sessionRepository.findByIdForUpdate(requiredId)
                .orElseThrow(() -> new PurchaseFlowException("telegram.purchase.preinvoice_expired"));
        if (!session.getUserId().equals(user.getId()) || !session.getTelegramUserId().equals(user.getTelegramUserId())) {
            throw new PurchaseFlowException("telegram.purchase.plan_unavailable");
        }
        Instant now = clock.now();
        if (session.expiredAt(now)) {
            session.expire(now);
            sessionRepository.save(session);
            throw new PurchaseFlowException("telegram.purchase.preinvoice_expired");
        }
        if (!session.activeAt(now)) {
            throw new PurchaseFlowException("telegram.purchase.preinvoice_expired");
        }
        return session;
    }

    private PlanSelection selection(UUID selectionId) {
        return selectionRepository.findById(Objects.requireNonNull(selectionId, "selectionId must not be null"))
                .orElseThrow(() -> new PurchaseFlowException("telegram.purchase.preinvoice_expired"));
    }

    private void requireActiveSelection(PlanSelection selection) {
        Instant now = clock.now();
        if (selection.getStatus() != PlanSelectionStatus.ACTIVE || selection.isExpiredAt(now)) {
            throw new PurchaseFlowException("telegram.purchase.preinvoice_expired");
        }
    }

    private Plan currentPlan(PlanSelection selection) {
        return planRepository.findById(selection.getPlanId())
                .orElseThrow(() -> new PurchaseFlowException("telegram.purchase.plan_unavailable"));
    }

    private void requireSales() {
        if (!salesAvailabilityService.newPurchaseAvailable()) {
            throw new PurchaseFlowException("telegram.purchase.disabled");
        }
    }

    private void requirePurchasable(Plan plan) {
        if (purchasabilityPolicy.evaluate(plan) != PlanPurchasability.PURCHASABLE) {
            throw new PurchaseFlowException("telegram.purchase.plan_unavailable");
        }
    }

    private String currentPlanDescription(PlanSelection selection) {
        return planRepository.findById(selection.getPlanId())
                .map(Plan::getDescription)
                .orElse("");
    }

    private static String displayName(User user) {
        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return "@" + user.getUsername();
        }
        return String.valueOf(user.getTelegramUserId());
    }

    private static String serviceName(PlanSelection selection) {
        return selection.getPlanNameSnapshot();
    }

    private static OptionalLong optionalLong(Long value) {
        return value == null ? OptionalLong.empty() : OptionalLong.of(value);
    }

    private static OptionalInt optionalInt(Integer value) {
        return value == null ? OptionalInt.empty() : OptionalInt.of(value);
    }

    private static UUID deterministicUuid(String prefix, UUID sessionId, UUID paymentId) {
        String value = prefix + ":" + sessionId + ":" + paymentId;
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }

}
