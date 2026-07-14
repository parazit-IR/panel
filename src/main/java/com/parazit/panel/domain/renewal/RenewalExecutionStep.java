package com.parazit.panel.domain.renewal;

public enum RenewalExecutionStep {
    NOT_STARTED,
    TARGET_CALCULATED,
    CLIENT_UPDATED,
    TRAFFIC_RESET,
    REMOTE_VERIFIED,
    LOCAL_STATE_UPDATED,
    COMPLETED
}
