package com.parazit.panel.application.referral;

public class ReferralCodeNotFoundException extends RuntimeException {

    public ReferralCodeNotFoundException() {
        super("Referral code was not found");
    }
}
