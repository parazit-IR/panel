package com.parazit.panel.application.user.settings;

import com.parazit.panel.application.port.in.user.settings.GetUserSettingsUseCase;
import com.parazit.panel.application.user.UserNotFoundException;
import com.parazit.panel.application.user.settings.query.GetUserSettingsQuery;
import com.parazit.panel.application.user.settings.result.UserSettingsResult;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.settings.UserSettings;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetUserSettingsService implements GetUserSettingsUseCase {

    private final UserRepository userRepository;
    private final UserSettingsDefaultsService defaultsService;

    public GetUserSettingsService(
            UserRepository userRepository,
            UserSettingsDefaultsService defaultsService
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.defaultsService = Objects.requireNonNull(defaultsService, "defaultsService must not be null");
    }

    @Override
    @Transactional
    public UserSettingsResult getSettings(GetUserSettingsQuery query) {
        validate(query);

        User user = userRepository.findByTelegramUserId(query.telegramUserId())
                .orElseThrow(() -> new UserNotFoundException(query.telegramUserId()));
        UserSettings settings = defaultsService.ensureDefaults(user);

        return UserSettingsResult.from(user, settings);
    }

    private void validate(GetUserSettingsQuery query) {
        if (query == null) {
            throw new InvalidUserSettingsCommandException("user settings query must not be null");
        }
        if (query.telegramUserId() == null) {
            throw new InvalidUserSettingsCommandException("telegramUserId must not be null");
        }
        if (query.telegramUserId() <= 0) {
            throw new InvalidUserSettingsCommandException("telegramUserId must be positive");
        }
    }
}
