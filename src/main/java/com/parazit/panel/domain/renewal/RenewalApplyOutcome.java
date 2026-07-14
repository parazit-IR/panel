package com.parazit.panel.domain.renewal;

public enum RenewalApplyOutcome {
    APPLIED,
    ALREADY_APPLIED,
    RETRY_SCHEDULED,
    FAILED_PERMANENTLY,
    MANUAL_REVIEW_REQUIRED
}
