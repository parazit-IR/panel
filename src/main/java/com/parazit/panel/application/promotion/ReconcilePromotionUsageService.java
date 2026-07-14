package com.parazit.panel.application.promotion;

import com.parazit.panel.application.port.in.promotion.ReconcilePromotionUsageUseCase;
import com.parazit.panel.application.promotion.result.PromotionReconciliationResult;
import com.parazit.panel.domain.promotion.PromotionCodeType;
import com.parazit.panel.domain.promotion.repository.PromotionRedemptionRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReconcilePromotionUsageService implements ReconcilePromotionUsageUseCase {

    private final PromotionRedemptionRepository redemptionRepository;

    public ReconcilePromotionUsageService(PromotionRedemptionRepository redemptionRepository) {
        this.redemptionRepository = Objects.requireNonNull(redemptionRepository, "redemptionRepository must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionReconciliationResult reconcile() {
        long discount = redemptionRepository.findAll().stream()
                .filter(redemption -> redemption.getCodeType() == PromotionCodeType.DISCOUNT)
                .count();
        long gift = redemptionRepository.findAll().stream()
                .filter(redemption -> redemption.getCodeType() == PromotionCodeType.GIFT)
                .count();
        return new PromotionReconciliationResult(discount, gift, true);
    }
}
