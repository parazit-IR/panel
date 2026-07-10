package com.parazit.panel.api.internal.user;

import com.parazit.panel.application.user.command.ChangeUserLanguageCommand;
import com.parazit.panel.application.user.query.GetUserLanguageQuery;
import com.parazit.panel.application.user.result.UserLanguageResult;
import org.springframework.stereotype.Component;

@Component
public class UserLanguageApiMapper {

    public GetUserLanguageQuery toQuery(Long telegramUserId) {
        return new GetUserLanguageQuery(telegramUserId);
    }

    public ChangeUserLanguageCommand toCommand(Long telegramUserId, ChangeUserLanguageRequest request) {
        return new ChangeUserLanguageCommand(
                telegramUserId,
                request.languageCode()
        );
    }

    public UserLanguageResponse toResponse(UserLanguageResult result) {
        return new UserLanguageResponse(
                result.userId(),
                result.telegramUserId(),
                result.language(),
                result.updatedAt()
        );
    }
}
