package com.parazit.panel.application.user.settings;

import com.parazit.panel.common.exception.TraceIdFilter;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.settings.UserSettings;
import com.parazit.panel.domain.user.settings.repository.UserSettingsRepository;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class UserSettingsDefaultsService {

    private static final Logger log = LoggerFactory.getLogger(UserSettingsDefaultsService.class);

    private final UserSettingsRepository userSettingsRepository;
    private final UserSettingsCreationService creationService;

    public UserSettingsDefaultsService(
            UserSettingsRepository userSettingsRepository,
            UserSettingsCreationService creationService
    ) {
        this.userSettingsRepository = Objects.requireNonNull(userSettingsRepository, "userSettingsRepository must not be null");
        this.creationService = Objects.requireNonNull(creationService, "creationService must not be null");
    }

    public UserSettings ensureDefaults(User user) {
        Objects.requireNonNull(user, "user must not be null");
        return userSettingsRepository.findByUserId(user.getId())
                .orElseGet(() -> createOrRecoverDefaults(user));
    }

    private UserSettings createOrRecoverDefaults(User user) {
        try {
            UserSettings settings = creationService.createDefault(user.getId());
            log.atInfo()
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("telegramUserId", user.getTelegramUserId())
                    .addKeyValue("settingsId", settings.getId())
                    .addKeyValue("traceId", MDC.get(TraceIdFilter.TRACE_ID_KEY))
                    .log("Created default user settings");
            return settings;
        } catch (DataIntegrityViolationException exception) {
            log.atDebug()
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("telegramUserId", user.getTelegramUserId())
                    .addKeyValue("traceId", MDC.get(TraceIdFilter.TRACE_ID_KEY))
                    .log("Recovered concurrent default user settings creation");
            return userSettingsRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new IllegalStateException("Default user settings recovery failed"));
        }
    }
}
