package com.parazit.panel.application.renewal;

import com.parazit.panel.domain.renewal.RenewalFailureClass;

public record RenewalFailure(RenewalFailureClass failureClass, String code, boolean retryable) {
}
