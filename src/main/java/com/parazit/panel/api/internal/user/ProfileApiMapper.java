package com.parazit.panel.api.internal.user;

import com.parazit.panel.application.user.command.UpdateUserProfileCommand;
import com.parazit.panel.application.user.query.GetUserProfileQuery;
import com.parazit.panel.application.user.result.UpdateUserProfileResult;
import com.parazit.panel.application.user.result.UserProfileResult;
import org.springframework.stereotype.Component;

@Component
public class ProfileApiMapper {

    public GetUserProfileQuery toQuery(Long telegramUserId) {
        return new GetUserProfileQuery(telegramUserId);
    }

    public UpdateUserProfileCommand toCommand(Long telegramUserId, UserProfileRequest request) {
        return new UpdateUserProfileCommand(
                telegramUserId,
                request.firstName(),
                request.lastName()
        );
    }

    public UserProfileResponse toResponse(UserProfileResult result) {
        return new UserProfileResponse(
                result.telegramUserId(),
                result.username(),
                result.firstName(),
                result.lastName(),
                result.language(),
                result.status(),
                result.blocked(),
                result.createdAt(),
                result.updatedAt(),
                result.lastInteractionAt()
        );
    }

    public UserProfileResponse toResponse(UpdateUserProfileResult result) {
        return new UserProfileResponse(
                result.telegramUserId(),
                result.username(),
                result.firstName(),
                result.lastName(),
                result.language(),
                result.status(),
                result.blocked(),
                result.createdAt(),
                result.updatedAt(),
                result.lastInteractionAt()
        );
    }
}
