package com.parazit.panel.application.wallet.topup;

import com.parazit.panel.application.payment.PaymentFactory;
import com.parazit.panel.application.payment.manual.command.InitializeManualCardPaymentCommand;
import com.parazit.panel.application.payment.manual.result.InitializeManualCardPaymentResult;
import com.parazit.panel.application.payment.zarinpal.command.InitializeZarinpalPaymentCommand;
import com.parazit.panel.application.payment.zarinpal.result.InitializeZarinpalPaymentResult;
import com.parazit.panel.application.port.in.payment.manual.InitializeManualCardPaymentUseCase;
import com.parazit.panel.application.port.in.payment.zarinpal.InitializeZarinpalPaymentUseCase;
import com.parazit.panel.application.port.in.wallet.topup.CreateWalletTopUpPaymentUseCase;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.sales.SalesAvailabilityService;
import com.parazit.panel.application.wallet.topup.command.CreateWalletTopUpPaymentCommand;
import com.parazit.panel.application.wallet.topup.result.WalletTopUpPaymentResult;
import com.parazit.panel.config.properties.WalletTopUpProperties;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.payment.PaymentOperation;
import com.parazit.panel.domain.payment.PaymentOperationType;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.repository.PaymentOperationRepository;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.wallet.topup.WalletTopUpRequest;
import com.parazit.panel.domain.wallet.topup.WalletTopUpStatus;
import com.parazit.panel.domain.wallet.topup.repository.WalletTopUpRequestRepository;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateWalletTopUpPaymentService implements CreateWalletTopUpPaymentUseCase {

    private final UserRepository userRepository;
    private final WalletTopUpRequestRepository requestRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentOperationRepository operationRepository;
    private final PaymentFactory paymentFactory;
    private final WalletTopUpAmountPolicy amountPolicy;
    private final WalletTopUpProperties properties;
    private final SalesAvailabilityService salesAvailabilityService;
    private final InitializeManualCardPaymentUseCase manualPaymentUseCase;
    private final InitializeZarinpalPaymentUseCase zarinpalPaymentUseCase;
    private final SystemClockPort clock;

    public CreateWalletTopUpPaymentService(
            UserRepository userRepository,
            WalletTopUpRequestRepository requestRepository,
            PaymentRepository paymentRepository,
            PaymentOperationRepository operationRepository,
            PaymentFactory paymentFactory,
            WalletTopUpAmountPolicy amountPolicy,
            WalletTopUpProperties properties,
            SalesAvailabilityService salesAvailabilityService,
            InitializeManualCardPaymentUseCase manualPaymentUseCase,
            InitializeZarinpalPaymentUseCase zarinpalPaymentUseCase,
            SystemClockPort clock
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.requestRepository = Objects.requireNonNull(requestRepository, "requestRepository must not be null");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.operationRepository = Objects.requireNonNull(operationRepository, "operationRepository must not be null");
        this.paymentFactory = Objects.requireNonNull(paymentFactory, "paymentFactory must not be null");
        this.amountPolicy = Objects.requireNonNull(amountPolicy, "amountPolicy must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.salesAvailabilityService = Objects.requireNonNull(salesAvailabilityService, "salesAvailabilityService must not be null");
        this.manualPaymentUseCase = Objects.requireNonNull(manualPaymentUseCase, "manualPaymentUseCase must not be null");
        this.zarinpalPaymentUseCase = Objects.requireNonNull(zarinpalPaymentUseCase, "zarinpalPaymentUseCase must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    @Transactional
    public WalletTopUpPaymentResult create(CreateWalletTopUpPaymentCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        User user = userRepository.findByTelegramUserId(command.telegramUserId())
                .orElseThrow(() -> new WalletTopUpException("user not found"));
        WalletTopUpRequest request = requestRepository.findByIdForUpdate(command.topUpRequestId())
                .orElseThrow(() -> new WalletTopUpException("wallet top-up request not found"));
        validateRequest(command, user, request);
        Payment payment = existingOrCreatePayment(command, user, request);
        request.attachPayment(payment.getId(), clock.now());
        requestRepository.save(request);

        if (command.paymentMethod() == PaymentMethod.CARD_TO_CARD) {
            InitializeManualCardPaymentResult manual = manualPaymentUseCase.initialize(new InitializeManualCardPaymentCommand(
                    deterministicUuid("wallet-top-up-manual", command.requestId(), request.getId(), payment.getId()),
                    payment.getId(),
                    command.telegramUserId()
            ));
            return result(request, payment, manual, null);
        }

        InitializeZarinpalPaymentResult online = zarinpalPaymentUseCase.initialize(new InitializeZarinpalPaymentCommand(
                deterministicUuid("wallet-top-up-zarinpal", command.requestId(), request.getId(), payment.getId()),
                payment.getId(),
                command.telegramUserId(),
                "Wallet top-up",
                null,
                null
        ));
        return result(request, payment, null, online);
    }

    private void validateRequest(CreateWalletTopUpPaymentCommand command, User user, WalletTopUpRequest request) {
        if (!properties.enabled()) {
            throw new WalletTopUpException("wallet top-up is disabled");
        }
        if (!request.getUserId().equals(user.getId())) {
            throw new WalletTopUpException("wallet top-up request not found");
        }
        if (request.isTerminal() || request.getStatus() == WalletTopUpStatus.PAYMENT_APPROVED) {
            throw new WalletTopUpException("wallet top-up request is not payable");
        }
        if (request.getPaymentId() == null && !request.getExpiresAt().isAfter(clock.now())) {
            request.expire(clock.now());
            requestRepository.save(request);
            throw new WalletTopUpException("wallet top-up request is expired");
        }
        amountPolicy.validate(request.requestedMoney());
        if (command.paymentMethod() == PaymentMethod.CARD_TO_CARD
                && (!properties.manualPaymentEnabled() || !salesAvailabilityService.manualPaymentAvailable())) {
            throw new WalletTopUpException("manual wallet top-up payment is unavailable");
        }
        if (command.paymentMethod() == PaymentMethod.ZARINPAL
                && (!properties.onlinePaymentEnabled() || !salesAvailabilityService.onlinePaymentAvailable())) {
            throw new WalletTopUpException("online wallet top-up payment is unavailable");
        }
    }

    private Payment existingOrCreatePayment(CreateWalletTopUpPaymentCommand command, User user, WalletTopUpRequest request) {
        if (request.getPaymentId() != null) {
            Payment payment = paymentRepository.findById(request.getPaymentId())
                    .orElseThrow(() -> new WalletTopUpException("wallet top-up payment not found"));
            if (payment.getMethod() != command.paymentMethod()) {
                throw new WalletTopUpException("wallet top-up payment method is already selected");
            }
            return payment;
        }
        paymentRepository.findByWalletTopUpRequestId(request.getId()).ifPresent(existing -> {
            throw new WalletTopUpException("wallet top-up payment already exists");
        });
        Payment payment = paymentFactory.createWalletTopUp(
                user.getId(),
                request.getId(),
                command.paymentMethod(),
                request.getRequestedAmount(),
                request.getCurrency()
        );
        Payment saved = paymentRepository.save(payment);
        operationRepository.save(PaymentOperation.record(
                saved.getId(),
                PaymentOperationType.CREATED,
                clock.now(),
                "Wallet top-up payment created"
        ));
        return saved;
    }

    private static WalletTopUpPaymentResult result(
            WalletTopUpRequest request,
            Payment payment,
            InitializeManualCardPaymentResult manual,
            InitializeZarinpalPaymentResult online
    ) {
        PaymentStatus status = manual != null ? manual.instruction().paymentStatus() : payment.getStatus();
        if (online != null) {
            status = online.paymentStatus();
        }
        return new WalletTopUpPaymentResult(
                request.getId(),
                payment.getId(),
                payment.getMethod(),
                status,
                request.getStatus(),
                request.requestedMoney(),
                request.getExpiresAt(),
                manual,
                online
        );
    }

    private static UUID deterministicUuid(String namespace, UUID... values) {
        StringBuilder raw = new StringBuilder(namespace);
        for (UUID value : values) {
            raw.append(':').append(value);
        }
        return UUID.nameUUIDFromBytes(raw.toString().getBytes(StandardCharsets.UTF_8));
    }
}
