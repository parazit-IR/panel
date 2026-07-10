package com.parazit.panel.api.internal.user.settings;

import com.parazit.panel.application.port.in.user.settings.GetUserSettingsUseCase;
import com.parazit.panel.application.port.in.user.settings.UpdateUserSettingsUseCase;
import com.parazit.panel.application.user.settings.result.UserSettingsResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.Objects;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal endpoint for user preference settings.
 * It is not a public user-management API.
 */
@Validated
@RestController
@RequestMapping("/internal/users")
public class UserSettingsController {

    private final GetUserSettingsUseCase getUserSettingsUseCase;
    private final UpdateUserSettingsUseCase updateUserSettingsUseCase;
    private final UserSettingsApiMapper mapper;

    public UserSettingsController(
            GetUserSettingsUseCase getUserSettingsUseCase,
            UpdateUserSettingsUseCase updateUserSettingsUseCase,
            UserSettingsApiMapper mapper
    ) {
        this.getUserSettingsUseCase = Objects.requireNonNull(getUserSettingsUseCase, "getUserSettingsUseCase must not be null");
        this.updateUserSettingsUseCase = Objects.requireNonNull(updateUserSettingsUseCase, "updateUserSettingsUseCase must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @GetMapping("/{telegramUserId}/settings")
    public UserSettingsResponse getSettings(@PathVariable @Positive Long telegramUserId) {
        UserSettingsResult result = getUserSettingsUseCase.getSettings(mapper.toQuery(telegramUserId));

        return mapper.toResponse(result);
    }

    @PutMapping("/{telegramUserId}/settings")
    public UserSettingsResponse updateSettings(
            @PathVariable @Positive Long telegramUserId,
            @Valid @RequestBody UpdateUserSettingsRequest request
    ) {
        UserSettingsResult result = updateUserSettingsUseCase.updateSettings(
                mapper.toCommand(telegramUserId, request)
        );

        return mapper.toResponse(result);
    }
}
