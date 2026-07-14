package com.parazit.panel.application.promotion;

import com.parazit.panel.application.sales.SalesAvailabilityService;
import com.parazit.panel.config.properties.PromotionProperties;
import com.parazit.panel.domain.promotion.GiftCode;
import com.parazit.panel.domain.promotion.GiftCodeRejectionReason;
import com.parazit.panel.domain.promotion.GiftCodeStatus;
import com.parazit.panel.domain.wallet.Wallet;
import com.parazit.panel.domain.wallet.WalletStatus;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class GiftCodeEligibilityPolicy {

    private final SalesAvailabilityService salesAvailabilityService;
    private final PromotionProperties properties;

    public GiftCodeEligibilityPolicy(SalesAvailabilityService salesAvailabilityService, PromotionProperties properties) {
        this.salesAvailabilityService = Objects.requireNonNull(salesAvailabilityService, "salesAvailabilityService must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public GiftCodeRejectionReason evaluate(GiftCode code, Wallet wallet, Instant now) {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(wallet, "wallet must not be null");
        Objects.requireNonNull(now, "now must not be null");
        if (!salesAvailabilityService.availability(com.parazit.panel.application.sales.SalesCapability.GIFT_CODE).enabled()
                || !properties.giftEnabled()) {
            return GiftCodeRejectionReason.FEATURE_DISABLED;
        }
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            return GiftCodeRejectionReason.WALLET_UNAVAILABLE;
        }
        if (!code.isActive() || code.getStatus() != GiftCodeStatus.ACTIVE) {
            return GiftCodeRejectionReason.NOT_ACTIVE;
        }
        if (now.isBefore(code.getValidFrom())) {
            return GiftCodeRejectionReason.NOT_STARTED;
        }
        if (!now.isBefore(code.getValidUntil())) {
            return GiftCodeRejectionReason.EXPIRED;
        }
        if (code.getTotalUsageLimit() > 0 && code.getUsedCount() >= code.getTotalUsageLimit()) {
            return GiftCodeRejectionReason.EXHAUSTED;
        }
        if (code.currencyCode() != wallet.currencyCode()) {
            return GiftCodeRejectionReason.CURRENCY_MISMATCH;
        }
        return GiftCodeRejectionReason.NONE;
    }
}
