package com.parazit.panel.application.wallet.topup;

import com.parazit.panel.application.port.in.wallet.GetOrCreateWalletUseCase;
import com.parazit.panel.application.port.in.wallet.topup.CreateWalletTopUpRequestUseCase;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.sales.SalesAvailabilityService;
import com.parazit.panel.application.wallet.topup.command.CreateWalletTopUpRequestCommand;
import com.parazit.panel.application.wallet.topup.result.WalletTopUpRequestResult;
import com.parazit.panel.config.properties.WalletTopUpProperties;
import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserStatus;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.wallet.WalletStatus;
import com.parazit.panel.domain.wallet.topup.WalletTopUpRequest;
import com.parazit.panel.domain.wallet.topup.WalletTopUpStatus;
import com.parazit.panel.domain.wallet.topup.repository.WalletTopUpRequestRepository;
import java.util.EnumSet;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateWalletTopUpRequestService implements CreateWalletTopUpRequestUseCase {

    private static final EnumSet<WalletTopUpStatus> NON_TERMINAL = EnumSet.of(
            WalletTopUpStatus.AWAITING_PAYMENT_METHOD,
            WalletTopUpStatus.PENDING_PAYMENT,
            WalletTopUpStatus.PAYMENT_APPROVED
    );

    private final UserRepository userRepository;
    private final GetOrCreateWalletUseCase getOrCreateWalletUseCase;
    private final WalletTopUpRequestRepository requestRepository;
    private final WalletTopUpAmountPolicy amountPolicy;
    private final WalletTopUpProperties properties;
    private final SalesAvailabilityService salesAvailabilityService;
    private final SystemClockPort clock;

    public CreateWalletTopUpRequestService(
            UserRepository userRepository,
            GetOrCreateWalletUseCase getOrCreateWalletUseCase,
            WalletTopUpRequestRepository requestRepository,
            WalletTopUpAmountPolicy amountPolicy,
            WalletTopUpProperties properties,
            SalesAvailabilityService salesAvailabilityService,
            SystemClockPort clock
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.getOrCreateWalletUseCase = Objects.requireNonNull(getOrCreateWalletUseCase, "getOrCreateWalletUseCase must not be null");
        this.requestRepository = Objects.requireNonNull(requestRepository, "requestRepository must not be null");
        this.amountPolicy = Objects.requireNonNull(amountPolicy, "amountPolicy must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.salesAvailabilityService = Objects.requireNonNull(salesAvailabilityService, "salesAvailabilityService must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    @Transactional
    public WalletTopUpRequestResult create(CreateWalletTopUpRequestCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        if (!properties.enabled()) {
            throw new WalletTopUpException("wallet top-up is disabled");
        }
        Money amount = amountPolicy.validate(command.amount());
        User user = userRepository.findByTelegramUserId(command.telegramUserId())
                .orElseThrow(() -> new WalletTopUpException("user not found"));
        if (user.getStatus() != UserStatus.ACTIVE || Boolean.TRUE.equals(user.getBlocked())) {
            throw new WalletTopUpException("user is not eligible for wallet top-up");
        }

        String idempotencyKey = idempotencyKey(command);
        return requestRepository.findByUserIdAndIdempotencyKey(user.getId(), idempotencyKey)
                .map(existing -> replay(existing, amount))
                .orElseGet(() -> createNew(user, amount, idempotencyKey));
    }

    private WalletTopUpRequestResult replay(WalletTopUpRequest existing, Money amount) {
        if (!existing.matchesSemanticRequest(amount)) {
            throw new WalletTopUpException("wallet top-up request idempotency conflict");
        }
        return toResult(existing);
    }

    private WalletTopUpRequestResult createNew(User user, Money amount, String idempotencyKey) {
        var wallet = getOrCreateWalletUseCase.getOrCreate(user.getId());
        if (wallet.status() != WalletStatus.ACTIVE) {
            throw new WalletTopUpException("wallet is not active");
        }
        long pendingCount = requestRepository.countByUserIdAndStatusIn(user.getId(), NON_TERMINAL);
        if (pendingCount >= properties.maxPendingRequestsPerUser()) {
            throw new WalletTopUpException("wallet top-up pending request limit exceeded");
        }
        WalletTopUpRequest request = WalletTopUpRequest.create(
                user.getId(),
                wallet.walletId(),
                amount,
                idempotencyKey,
                clock.now(),
                properties.requestTtl()
        );
        return toResult(requestRepository.save(request));
    }

    private WalletTopUpRequestResult toResult(WalletTopUpRequest request) {
        return new WalletTopUpRequestResult(
                request.getId(),
                request.requestedMoney(),
                request.getStatus(),
                request.getExpiresAt(),
                properties.manualPaymentEnabled() && salesAvailabilityService.manualPaymentAvailable(),
                properties.onlinePaymentEnabled() && salesAvailabilityService.onlinePaymentAvailable()
        );
    }

    private static String idempotencyKey(CreateWalletTopUpRequestCommand command) {
        return "wallet-top-up-request:" + command.requestId();
    }
}
