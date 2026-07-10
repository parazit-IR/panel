package com.parazit.panel.api.internal.user;

import com.parazit.panel.application.port.in.user.ChangeUserLanguageUseCase;
import com.parazit.panel.application.port.in.user.GetUserLanguageUseCase;
import com.parazit.panel.application.user.result.UserLanguageResult;
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
 * Internal endpoint for reading and changing a user's preferred language.
 * It is not a public user-management API.
 */
@Validated
@RestController
@RequestMapping("/internal/users")
public class UserLanguageController {

    private final GetUserLanguageUseCase getUserLanguageUseCase;
    private final ChangeUserLanguageUseCase changeUserLanguageUseCase;
    private final UserLanguageApiMapper mapper;

    public UserLanguageController(
            GetUserLanguageUseCase getUserLanguageUseCase,
            ChangeUserLanguageUseCase changeUserLanguageUseCase,
            UserLanguageApiMapper mapper
    ) {
        this.getUserLanguageUseCase = Objects.requireNonNull(getUserLanguageUseCase, "getUserLanguageUseCase must not be null");
        this.changeUserLanguageUseCase = Objects.requireNonNull(changeUserLanguageUseCase, "changeUserLanguageUseCase must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @GetMapping("/{telegramUserId}/language")
    public UserLanguageResponse getLanguage(@PathVariable @Positive Long telegramUserId) {
        UserLanguageResult result = getUserLanguageUseCase.getLanguage(mapper.toQuery(telegramUserId));

        return mapper.toResponse(result);
    }

    @PutMapping("/{telegramUserId}/language")
    public UserLanguageResponse changeLanguage(
            @PathVariable @Positive Long telegramUserId,
            @Valid @RequestBody ChangeUserLanguageRequest request
    ) {
        UserLanguageResult result = changeUserLanguageUseCase.changeLanguage(
                mapper.toCommand(telegramUserId, request)
        );

        return mapper.toResponse(result);
    }
}
