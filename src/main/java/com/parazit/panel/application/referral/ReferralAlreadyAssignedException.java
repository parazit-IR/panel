package com.parazit.panel.application.referral;

public class ReferralAlreadyAssignedException extends RuntimeException {

    public ReferralAlreadyAssignedException() {
        super("Referral has already been assigned to another referrer");
    }
}
