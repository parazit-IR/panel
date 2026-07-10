package com.parazit.panel.api.internal.user;

import com.parazit.panel.domain.user.UserLanguage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserProfileRequest(
        @NotBlank
        @Size(max = 128)
        String firstName,

        @Size(max = 128)
        String lastName,

        @NotNull
        UserLanguage language
) {
}
