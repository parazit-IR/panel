package com.parazit.panel.application.promotion.result;

public record PromotionReconciliationResult(
        long discountRedemptions,
        long giftRedemptions,
        boolean readOnly
) {
}
