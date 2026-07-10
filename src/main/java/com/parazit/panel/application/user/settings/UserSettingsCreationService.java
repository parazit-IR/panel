package com.parazit.panel.application.user.settings;

import com.parazit.panel.domain.user.settings.UserSettings;
import com.parazit.panel.domain.user.settings.repository.UserSettingsRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserSettingsCreationService {

    private final UserSettingsRepository userSettingsRepository;

    public UserSettingsCreationService(UserSettingsRepository userSettingsRepository) {
        this.userSettingsRepository = Objects.requireNonNull(userSettingsRepository, "userSettingsRepository must not be null");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UserSettings createDefault(UUID userId) {
        return userSettingsRepository.save(UserSettings.createDefault(userId));
    }
}
