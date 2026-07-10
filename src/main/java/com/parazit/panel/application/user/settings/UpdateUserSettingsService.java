package com.parazit.panel.application.user.settings;

import com.parazit.panel.application.port.in.user.settings.UpdateUserSettingsUseCase;
import com.parazit.panel.application.user.UserNotFoundException;
import com.parazit.panel.application.user.settings.command.UpdateUserSettingsCommand;
import com.parazit.panel.application.user.settings.result.UserSettingsResult;
import com.parazit.panel.common.exception.TraceIdFilter;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.settings.UserSettings;
import com.parazit.panel.domain.user.settings.repository.UserSettingsRepository;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateUserSettingsService implements UpdateUserSettingsUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateUserSettingsService.class);

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final UserSettingsDefaultsService defaultsService;

    public UpdateUserSettingsService(
            UserRepository userRepository,
            UserSettingsRepository userSettingsRepository,
            UserSettingsDefaultsService defaultsService
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.userSettingsRepository = Objects.requireNonNull(userSettingsRepository, "userSettingsRepository must not be null");
        this.defaultsService = Objects.requireNonNull(defaultsService, "defaultsService must not be null");
    }

    @Override
    @Transactional
    public UserSettingsResult updateSettings(UpdateUserSettingsCommand command) {
        validate(command);

        User user = userRepository.findByTelegramUserId(command.telegramUserId())
                .orElseThrow(() -> new UserNotFoundException(command.telegramUserId()));
        UserSettings settings = defaultsService.ensureDefaults(user);

        settings.updatePreferences(
                command.notificationsEnabled(),
                command.renewalRemindersEnabled(),
                command.usageAlertsEnabled(),
                command.usageAlertThresholdPercent()
        );
        UserSettings saved = userSettingsRepository.save(settings);

        log.atInfo()
                .addKeyValue("userId", user.getId())
                .addKeyValue("telegramUserId", user.getTelegramUserId())
                .addKeyValue("settingsId", saved.getId())
                .addKeyValue("notificationsEnabled", saved.isNotificationsEnabled())
                .addKeyValue("renewalRemindersEnabled", saved.isRenewalRemindersEnabled())
                .addKeyValue("usageAlertsEnabled", saved.isUsageAlertsEnabled())
                .addKeyValue("usageAlertThresholdPercent", saved.getUsageAlertThresholdPercent())
                .addKeyValue("traceId", MDC.get(TraceIdFilter.TRACE_ID_KEY))
                .log("Updated user settings");

        return UserSettingsResult.from(user, saved);
    }

    private void validate(UpdateUserSettingsCommand command) {
        if (command == null) {
            throw new InvalidUserSettingsCommandException("user settings update command must not be null");
        }
        if (command.telegramUserId() == null) {
            throw new InvalidUserSettingsCommandException("telegramUserId must not be null");
        }
        if (command.telegramUserId() <= 0) {
            throw new InvalidUserSettingsCommandException("telegramUserId must be positive");
        }
        if (command.usageAlertThresholdPercent() == null) {
            throw new InvalidUserSettingsCommandException("usageAlertThresholdPercent must not be null");
        }
        if (command.usageAlertThresholdPercent() < 1 || command.usageAlertThresholdPercent() > 100) {
            throw new InvalidUserSettingsCommandException("usageAlertThresholdPercent must be between 1 and 100");
        }
    }
}
