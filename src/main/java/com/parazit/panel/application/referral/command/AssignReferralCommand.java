package com.parazit.panel.application.referral.command;

import java.util.Objects;

public record AssignReferralCommand(Long referredTelegramUserId, String referralCode) {

    public AssignReferralCommand {
        Objects.requireNonNull(referredTelegramUserId, "referredTelegramUserId must not be null");
        Objects.requireNonNull(referralCode, "referralCode must not be null");
        if (referredTelegramUserId <= 0) {
            throw new IllegalArgumentException("referredTelegramUserId must be positive");
        }
    }
}
