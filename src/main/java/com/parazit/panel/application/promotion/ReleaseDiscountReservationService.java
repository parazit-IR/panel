package com.parazit.panel.application.promotion;

import com.parazit.panel.application.port.in.promotion.ReleaseDiscountReservationUseCase;
import com.parazit.panel.domain.promotion.DiscountCode;
import com.parazit.panel.domain.promotion.PromotionRedemption;
import com.parazit.panel.domain.promotion.PromotionRedemptionStatus;
import com.parazit.panel.domain.promotion.repository.DiscountCodeRepository;
import com.parazit.panel.domain.promotion.repository.PromotionRedemptionRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReleaseDiscountReservationService implements ReleaseDiscountReservationUseCase {

    private final PromotionRedemptionRepository redemptionRepository;
    private final DiscountCodeRepository discountCodeRepository;
    private final com.parazit.panel.application.port.out.SystemClockPort clock;

    public ReleaseDiscountReservationService(
            PromotionRedemptionRepository redemptionRepository,
            DiscountCodeRepository discountCodeRepository,
            com.parazit.panel.application.port.out.SystemClockPort clock
    ) {
        this.redemptionRepository = Objects.requireNonNull(redemptionRepository, "redemptionRepository must not be null");
        this.discountCodeRepository = Objects.requireNonNull(discountCodeRepository, "discountCodeRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    @Transactional
    public void releaseForOrder(UUID orderId) {
        PromotionRedemption redemption = redemptionRepository.findActiveDiscountByOrderId(orderId).orElse(null);
        if (redemption == null || redemption.getStatus() != PromotionRedemptionStatus.RESERVED) {
            return;
        }
        DiscountCode code = discountCodeRepository.findById(redemption.getDiscountCodeId()).orElse(null);
        if (code != null) {
            code.releaseUse();
            discountCodeRepository.save(code);
        }
        redemption.release(clock.now());
        redemptionRepository.save(redemption);
    }
}
