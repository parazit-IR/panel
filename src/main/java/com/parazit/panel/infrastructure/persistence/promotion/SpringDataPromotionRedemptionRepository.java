package com.parazit.panel.infrastructure.persistence.promotion;

import com.parazit.panel.domain.promotion.PromotionRedemption;
import com.parazit.panel.domain.promotion.PromotionRedemptionStatus;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataPromotionRedemptionRepository extends SpringDataUuidRepository<PromotionRedemption> {

    Optional<PromotionRedemption> findByOrderIdAndDiscountCodeId(UUID orderId, UUID discountCodeId);

    Optional<PromotionRedemption> findFirstByOrderIdAndStatusInOrderByCreatedAtDesc(UUID orderId, Collection<PromotionRedemptionStatus> statuses);

    Optional<PromotionRedemption> findByUserIdAndGiftCodeId(UUID userId, UUID giftCodeId);

    long countByUserIdAndDiscountCodeIdAndStatusIn(UUID userId, UUID discountCodeId, List<PromotionRedemptionStatus> statuses);

    long countByUserIdAndGiftCodeIdAndStatusIn(UUID userId, UUID giftCodeId, List<PromotionRedemptionStatus> statuses);
}
