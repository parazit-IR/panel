package com.parazit.panel.api.internal.referral;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AssignReferralRequest(
        @NotBlank
        @Size(min = 8, max = 16)
        @Pattern(regexp = "^[A-HJ-NP-Za-hj-np-z2-9]+$")
        String referralCode
) {
}
