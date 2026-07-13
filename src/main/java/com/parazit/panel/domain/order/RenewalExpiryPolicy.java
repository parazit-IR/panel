package com.parazit.panel.domain.order;

public enum RenewalExpiryPolicy {
    EXTEND_FROM_CURRENT_EXPIRY,
    EXTEND_FROM_NOW,
    EXTEND_FROM_LATER_OF_NOW_OR_EXPIRY
}
