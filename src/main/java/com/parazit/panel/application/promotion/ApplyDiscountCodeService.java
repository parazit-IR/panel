package com.parazit.panel.application.promotion;

import com.parazit.panel.application.port.in.promotion.ApplyDiscountCodeUseCase;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.port.out.promotion.PromotionCodeHasher;
import com.parazit.panel.application.promotion.command.ApplyDiscountCodeCommand;
import com.parazit.panel.application.promotion.result.DiscountApplicationResult;
import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.promotion.DiscountCode;
import com.parazit.panel.domain.promotion.DiscountRejectionReason;
import com.parazit.panel.domain.promotion.PromotionRedemption;
import com.parazit.panel.domain.promotion.PromotionRedemptionStatus;
import com.parazit.panel.domain.promotion.repository.DiscountCodeRepository;
import com.parazit.panel.domain.promotion.repository.PromotionRedemptionRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplyDiscountCodeService implements ApplyDiscountCodeUseCase {

    private static final List<PromotionRedemptionStatus> COUNTED_DISCOUNT_STATUSES = List.of(
            PromotionRedemptionStatus.RESERVED,
            PromotionRedemptionStatus.CONSUMED
    );

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final DiscountCodeRepository discountCodeRepository;
    private final PromotionRedemptionRepository redemptionRepository;
    private final PromotionCodeNormalizer normalizer;
    private final PromotionCodeHasher hasher;
    private final DiscountEligibilityPolicy eligibilityPolicy;
    private final DiscountAmountCalculator calculator;
    private final SystemClockPort clock;

    public ApplyDiscountCodeService(
            UserRepository userRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            DiscountCodeRepository discountCodeRepository,
            PromotionRedemptionRepository redemptionRepository,
            PromotionCodeNormalizer normalizer,
            PromotionCodeHasher hasher,
            DiscountEligibilityPolicy eligibilityPolicy,
            DiscountAmountCalculator calculator,
            SystemClockPort clock
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.discountCodeRepository = Objects.requireNonNull(discountCodeRepository, "discountCodeRepository must not be null");
        this.redemptionRepository = Objects.requireNonNull(redemptionRepository, "redemptionRepository must not be null");
        this.normalizer = Objects.requireNonNull(normalizer, "normalizer must not be null");
        this.hasher = Objects.requireNonNull(hasher, "hasher must not be null");
        this.eligibilityPolicy = Objects.requireNonNull(eligibilityPolicy, "eligibilityPolicy must not be null");
        this.calculator = Objects.requireNonNull(calculator, "calculator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    @Transactional
    public DiscountApplicationResult apply(ApplyDiscountCodeCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        User user = userRepository.findByTelegramUserIdForUpdate(command.telegramUserId())
                .orElseThrow(() -> new PromotionException("telegram.promotion.invalid_code"));
        Order order = orderRepository.findByIdForUpdate(command.orderId())
                .orElseThrow(() -> new PromotionException("telegram.promotion.invalid_code"));
        if (!order.getUserId().equals(user.getId())) {
            throw new PromotionException("telegram.promotion.invalid_code");
        }
        String hash = hasher.hashNormalizedCode(normalizer.normalize(command.rawCode()));
        DiscountCode code = discountCodeRepository.findByCodeHashForUpdate(hash)
                .orElseThrow(() -> new PromotionException("telegram.promotion.invalid_code"));
        var now = clock.now();
        List<com.parazit.panel.domain.payment.Payment> payments = paymentRepository.findAllByOrderId(order.getId());
        DiscountRejectionReason decision = eligibilityPolicy.evaluate(user.getId(), order, code, payments, now);
        if (decision != DiscountRejectionReason.NONE) {
            throw new PromotionException(eligibilityPolicy.messageKey(decision));
        }
        long userUses = redemptionRepository.countByUserIdAndDiscountCodeIdAndStatusIn(
                user.getId(),
                code.getId(),
                COUNTED_DISCOUNT_STATUSES
        );
        if (userUses >= code.getPerUserUsageLimit()) {
            throw new PromotionException("telegram.promotion.user_limit");
        }
        var existing = redemptionRepository.findByOrderIdAndDiscountCodeId(order.getId(), code.getId()).orElse(null);
        if (existing != null && existing.getStatus() == PromotionRedemptionStatus.RESERVED) {
            return result(order, existing, true);
        }
        DiscountCalculationResult calculated = calculator.calculate(code, new Money(order.getBaseAmount(), code.currencyCode()));
        code.reserveUse();
        order.applyDiscount(code.getId(), calculated.discountAmount().amount());
        PromotionRedemption redemption = PromotionRedemption.reserveDiscount(
                code.getId(),
                user.getId(),
                order.getId(),
                calculated.originalAmount(),
                calculated.discountAmount(),
                calculated.finalAmount(),
                "discount:" + order.getId() + ":" + code.getId(),
                now
        );
        discountCodeRepository.save(code);
        orderRepository.save(order);
        return result(orderRepository.save(order), redemptionRepository.save(redemption), false);
    }

    private static DiscountApplicationResult result(Order order, PromotionRedemption redemption, boolean replayed) {
        return new DiscountApplicationResult(
                order.getId(),
                redemption.getId(),
                redemption.originalMoney(),
                redemption.discountMoney(),
                redemption.finalMoney(),
                replayed,
                redemption.getRedeemedAt()
        );
    }
}
