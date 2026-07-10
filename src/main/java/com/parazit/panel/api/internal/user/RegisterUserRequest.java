package com.parazit.panel.api.internal.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotNull
        @Positive
        Long telegramUserId,

        @Size(max = 64)
        String username,

        @NotBlank
        @Size(max = 128)
        String firstName,

        @Size(max = 128)
        String lastName,

        @Size(max = 16)
        String languageCode
) {
}
