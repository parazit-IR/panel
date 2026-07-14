package com.parazit.panel.application.promotion;

import com.parazit.panel.application.port.in.promotion.FinalizeDiscountRedemptionUseCase;
import com.parazit.panel.application.promotion.command.FinalizeDiscountRedemptionCommand;
import com.parazit.panel.domain.promotion.PromotionRedemption;
import com.parazit.panel.domain.promotion.PromotionRedemptionStatus;
import com.parazit.panel.domain.promotion.repository.PromotionRedemptionRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinalizeDiscountRedemptionService implements FinalizeDiscountRedemptionUseCase {

    private final PromotionRedemptionRepository redemptionRepository;
    private final com.parazit.panel.application.port.out.SystemClockPort clock;

    public FinalizeDiscountRedemptionService(
            PromotionRedemptionRepository redemptionRepository,
            com.parazit.panel.application.port.out.SystemClockPort clock
    ) {
        this.redemptionRepository = Objects.requireNonNull(redemptionRepository, "redemptionRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    @Transactional
    public void finalizeDiscount(FinalizeDiscountRedemptionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        PromotionRedemption redemption = redemptionRepository.findActiveDiscountByOrderId(command.orderId()).orElse(null);
        if (redemption == null || redemption.getStatus() == PromotionRedemptionStatus.CONSUMED) {
            return;
        }
        if (redemption.getStatus() == PromotionRedemptionStatus.RESERVED) {
            redemption.consume(clock.now());
            redemptionRepository.save(redemption);
        }
    }
}
