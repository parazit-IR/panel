package com.parazit.panel.application.user;

import com.parazit.panel.application.port.in.user.GetUserProfileUseCase;
import com.parazit.panel.application.user.query.GetUserProfileQuery;
import com.parazit.panel.application.user.result.UserProfileResult;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GetUserProfileService implements GetUserProfileUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetUserProfileService.class);

    private final UserRepository userRepository;

    public GetUserProfileService(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
    }

    @Override
    public UserProfileResult getProfile(GetUserProfileQuery query) {
        validate(query);

        User user = userRepository.findByTelegramUserId(query.telegramUserId())
                .orElseThrow(() -> new UserProfileNotFoundException(query.telegramUserId()));

        log.atDebug()
                .addKeyValue("telegramUserId", user.getTelegramUserId())
                .log("Viewed user profile");

        return UserProfileResult.from(user);
    }

    private void validate(GetUserProfileQuery query) {
        if (query == null) {
            throw new InvalidUserProfileCommandException("user profile query must not be null");
        }
        if (query.telegramUserId() == null) {
            throw new InvalidUserProfileCommandException("telegramUserId must not be null");
        }
        if (query.telegramUserId() <= 0) {
            throw new InvalidUserProfileCommandException("telegramUserId must be positive");
        }
    }
}
