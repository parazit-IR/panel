package com.parazit.panel.api.internal.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeUserLanguageRequest(
        @NotBlank
        @Size(max = 16)
        String languageCode
) {
}
