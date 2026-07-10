package com.parazit.panel.api.internal.user;

import com.parazit.panel.application.port.in.user.GetUserProfileUseCase;
import com.parazit.panel.application.port.in.user.UpdateUserProfileUseCase;
import com.parazit.panel.application.user.result.UpdateUserProfileResult;
import com.parazit.panel.application.user.result.UserProfileResult;
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
 * Temporary internal endpoint for verifying user profile retrieval and update.
 * It is not a public user-management API.
 */
@Validated
@RestController
@RequestMapping("/internal/users")
public class UserProfileController {

    private final GetUserProfileUseCase getUserProfileUseCase;
    private final UpdateUserProfileUseCase updateUserProfileUseCase;
    private final ProfileApiMapper mapper;

    public UserProfileController(
            GetUserProfileUseCase getUserProfileUseCase,
            UpdateUserProfileUseCase updateUserProfileUseCase,
            ProfileApiMapper mapper
    ) {
        this.getUserProfileUseCase = Objects.requireNonNull(getUserProfileUseCase, "getUserProfileUseCase must not be null");
        this.updateUserProfileUseCase = Objects.requireNonNull(updateUserProfileUseCase, "updateUserProfileUseCase must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @GetMapping("/{telegramUserId}")
    public UserProfileResponse getProfile(@PathVariable @Positive Long telegramUserId) {
        UserProfileResult result = getUserProfileUseCase.getProfile(mapper.toQuery(telegramUserId));

        return mapper.toResponse(result);
    }

    @PutMapping("/{telegramUserId}")
    public UserProfileResponse updateProfile(
            @PathVariable @Positive Long telegramUserId,
            @Valid @RequestBody UserProfileRequest request
    ) {
        UpdateUserProfileResult result = updateUserProfileUseCase.updateProfile(
                mapper.toCommand(telegramUserId, request)
        );

        return mapper.toResponse(result);
    }
}
