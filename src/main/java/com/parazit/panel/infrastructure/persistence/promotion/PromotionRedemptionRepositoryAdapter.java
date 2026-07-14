package com.parazit.panel.infrastructure.persistence.promotion;

import com.parazit.panel.domain.promotion.PromotionRedemption;
import com.parazit.panel.domain.promotion.PromotionRedemptionStatus;
import com.parazit.panel.domain.promotion.repository.PromotionRedemptionRepository;
import com.parazit.panel.infrastructure.persistence.repository.JpaRepositoryAdapter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class PromotionRedemptionRepositoryAdapter extends JpaRepositoryAdapter<PromotionRedemption, UUID> implements PromotionRedemptionRepository {

    private static final List<PromotionRedemptionStatus> ACTIVE_DISCOUNT_STATUSES = List.of(
            PromotionRedemptionStatus.RESERVED,
            PromotionRedemptionStatus.CONSUMED
    );

    private final SpringDataPromotionRedemptionRepository repository;

    public PromotionRedemptionRepositoryAdapter(SpringDataPromotionRedemptionRepository repository) {
        super(repository);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public PromotionRedemption save(PromotionRedemption entity) {
        return repository.saveAndFlush(Objects.requireNonNull(entity, "entity must not be null"));
    }

    @Override
    public Optional<PromotionRedemption> findByOrderIdAndDiscountCodeId(UUID orderId, UUID discountCodeId) {
        return repository.findByOrderIdAndDiscountCodeId(
                Objects.requireNonNull(orderId, "orderId must not be null"),
                Objects.requireNonNull(discountCodeId, "discountCodeId must not be null")
        );
    }

    @Override
    public Optional<PromotionRedemption> findActiveDiscountByOrderId(UUID orderId) {
        return repository.findFirstByOrderIdAndStatusInOrderByCreatedAtDesc(
                Objects.requireNonNull(orderId, "orderId must not be null"),
                ACTIVE_DISCOUNT_STATUSES
        );
    }

    @Override
    public Optional<PromotionRedemption> findByUserIdAndGiftCodeId(UUID userId, UUID giftCodeId) {
        return repository.findByUserIdAndGiftCodeId(
                Objects.requireNonNull(userId, "userId must not be null"),
                Objects.requireNonNull(giftCodeId, "giftCodeId must not be null")
        );
    }

    @Override
    public long countByUserIdAndDiscountCodeIdAndStatusIn(UUID userId, UUID discountCodeId, List<PromotionRedemptionStatus> statuses) {
        return repository.countByUserIdAndDiscountCodeIdAndStatusIn(
                Objects.requireNonNull(userId, "userId must not be null"),
                Objects.requireNonNull(discountCodeId, "discountCodeId must not be null"),
                List.copyOf(Objects.requireNonNull(statuses, "statuses must not be null"))
        );
    }

    @Override
    public long countByUserIdAndGiftCodeIdAndStatusIn(UUID userId, UUID giftCodeId, List<PromotionRedemptionStatus> statuses) {
        return repository.countByUserIdAndGiftCodeIdAndStatusIn(
                Objects.requireNonNull(userId, "userId must not be null"),
                Objects.requireNonNull(giftCodeId, "giftCodeId must not be null"),
                List.copyOf(Objects.requireNonNull(statuses, "statuses must not be null"))
        );
    }
}
