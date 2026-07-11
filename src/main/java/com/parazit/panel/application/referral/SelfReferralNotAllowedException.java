package com.parazit.panel.application.referral;

public class SelfReferralNotAllowedException extends RuntimeException {

    public SelfReferralNotAllowedException() {
        super("A user cannot refer themselves");
    }
}
