package com.parazit.panel.application.promotion;

import com.parazit.panel.application.port.in.promotion.RemoveDiscountCodeUseCase;
import com.parazit.panel.application.promotion.command.RemoveDiscountCodeCommand;
import com.parazit.panel.application.promotion.result.DiscountApplicationResult;
import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.promotion.DiscountCode;
import com.parazit.panel.domain.promotion.PromotionRedemption;
import com.parazit.panel.domain.promotion.repository.DiscountCodeRepository;
import com.parazit.panel.domain.promotion.repository.PromotionRedemptionRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RemoveDiscountCodeService implements RemoveDiscountCodeUseCase {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final DiscountCodeRepository discountCodeRepository;
    private final PromotionRedemptionRepository redemptionRepository;
    private final com.parazit.panel.application.port.out.SystemClockPort clock;

    public RemoveDiscountCodeService(
            UserRepository userRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            DiscountCodeRepository discountCodeRepository,
            PromotionRedemptionRepository redemptionRepository,
            com.parazit.panel.application.port.out.SystemClockPort clock
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.discountCodeRepository = Objects.requireNonNull(discountCodeRepository, "discountCodeRepository must not be null");
        this.redemptionRepository = Objects.requireNonNull(redemptionRepository, "redemptionRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    @Transactional
    public DiscountApplicationResult remove(RemoveDiscountCodeCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        User user = userRepository.findByTelegramUserIdForUpdate(command.telegramUserId())
                .orElseThrow(() -> new PromotionException("telegram.promotion.invalid_code"));
        Order order = orderRepository.findByIdForUpdate(command.orderId())
                .orElseThrow(() -> new PromotionException("telegram.promotion.invalid_code"));
        if (!order.getUserId().equals(user.getId())) {
            throw new PromotionException("telegram.promotion.invalid_code");
        }
        if (!paymentRepository.findAllByOrderId(order.getId()).isEmpty()) {
            throw new PromotionException("telegram.promotion.payment_exists");
        }
        PromotionRedemption redemption = redemptionRepository.findActiveDiscountByOrderId(order.getId())
                .orElseThrow(() -> new PromotionException("telegram.promotion.invalid_code"));
        DiscountCode code = discountCodeRepository.findByCodeHashForUpdate(
                        discountCodeRepository.findById(redemption.getDiscountCodeId())
                                .orElseThrow(() -> new PromotionException("telegram.promotion.invalid_code"))
                                .getCodeHash())
                .orElseThrow(() -> new PromotionException("telegram.promotion.invalid_code"));
        redemption.release(clock.now());
        code.releaseUse();
        order.removeDiscount();
        discountCodeRepository.save(code);
        redemptionRepository.save(redemption);
        orderRepository.save(order);
        return new DiscountApplicationResult(
                order.getId(),
                redemption.getId(),
                new Money(order.getBaseAmount(), orderCurrency(order)),
                new Money(0, orderCurrency(order)),
                new Money(order.getFinalAmount(), orderCurrency(order)),
                false,
                redemption.getRedeemedAt()
        );
    }

    private static com.parazit.panel.domain.plan.CurrencyCode orderCurrency(Order order) {
        return com.parazit.panel.domain.plan.CurrencyCode.valueOf(order.getCurrency());
    }
}
