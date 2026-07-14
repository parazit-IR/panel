package com.parazit.panel.application.customer.result;

import com.parazit.panel.domain.order.Money;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public record CustomerAccountSummaryResult(
        UUID userId,
        long telegramUserId,
        String displayName,
        String username,
        Instant registeredAt,
        String locale,
        boolean telegramNotificationsEnabled,
        long totalServiceCount,
        long activeServiceCount,
        long expiredServiceCount,
        long paidOrderCount,
        long pendingPaymentCount,
        Optional<String> referralCode,
        Optional<String> phoneNumberMasked,
        Optional<Money> walletBalance,
        Optional<String> customerGroup,
        long discountUsageCount
) {

    public CustomerAccountSummaryResult {
        referralCode = referralCode == null ? Optional.empty() : referralCode;
        phoneNumberMasked = phoneNumberMasked == null ? Optional.empty() : phoneNumberMasked;
        walletBalance = walletBalance == null ? Optional.empty() : walletBalance;
        customerGroup = customerGroup == null ? Optional.empty() : customerGroup;
    }
}
