package com.parazit.panel.domain.promotion.repository;

import com.parazit.panel.domain.promotion.PromotionRedemption;
import com.parazit.panel.domain.promotion.PromotionRedemptionStatus;
import com.parazit.panel.domain.repository.UuidRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromotionRedemptionRepository extends UuidRepository<PromotionRedemption> {

    Optional<PromotionRedemption> findByOrderIdAndDiscountCodeId(UUID orderId, UUID discountCodeId);

    Optional<PromotionRedemption> findActiveDiscountByOrderId(UUID orderId);

    Optional<PromotionRedemption> findByUserIdAndGiftCodeId(UUID userId, UUID giftCodeId);

    long countByUserIdAndDiscountCodeIdAndStatusIn(UUID userId, UUID discountCodeId, List<PromotionRedemptionStatus> statuses);

    long countByUserIdAndGiftCodeIdAndStatusIn(UUID userId, UUID giftCodeId, List<PromotionRedemptionStatus> statuses);
}
