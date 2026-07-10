package com.parazit.panel.application.user;

import com.parazit.panel.application.port.in.user.ChangeUserLanguageUseCase;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.user.command.ChangeUserLanguageCommand;
import com.parazit.panel.application.user.result.UserLanguageResult;
import com.parazit.panel.common.exception.TraceIdFilter;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangeUserLanguageService implements ChangeUserLanguageUseCase {

    private static final Logger log = LoggerFactory.getLogger(ChangeUserLanguageService.class);

    private final UserRepository userRepository;
    private final UserLanguageResolver userLanguageResolver;
    private final SystemClockPort systemClockPort;

    public ChangeUserLanguageService(
            UserRepository userRepository,
            UserLanguageResolver userLanguageResolver,
            SystemClockPort systemClockPort
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.userLanguageResolver = Objects.requireNonNull(userLanguageResolver, "userLanguageResolver must not be null");
        this.systemClockPort = Objects.requireNonNull(systemClockPort, "systemClockPort must not be null");
    }

    @Override
    @Transactional
    public UserLanguageResult changeLanguage(ChangeUserLanguageCommand command) {
        if (command == null) {
            throw new InvalidUserLanguageCommandException("user language command must not be null");
        }

        UserLanguage newLanguage = userLanguageResolver.resolveRequired(command.languageCode());
        User user = userRepository.findByTelegramUserId(command.telegramUserId())
                .orElseThrow(() -> new UserNotFoundException(command.telegramUserId()));
        UserLanguage oldLanguage = user.getLanguage();

        if (oldLanguage == newLanguage) {
            log.atDebug()
                    .addKeyValue("userId", user.getId())
                    .addKeyValue("telegramUserId", user.getTelegramUserId())
                    .addKeyValue("oldLanguage", oldLanguage)
                    .addKeyValue("newLanguage", newLanguage)
                    .addKeyValue("traceId", MDC.get(TraceIdFilter.TRACE_ID_KEY))
                    .log("User language unchanged");
            return UserLanguageResult.from(user);
        }

        Instant interactionTime = systemClockPort.now();
        user.changeLanguage(newLanguage);
        user.recordInteraction(interactionTime);
        User saved = userRepository.save(user);

        log.atInfo()
                .addKeyValue("userId", saved.getId())
                .addKeyValue("telegramUserId", saved.getTelegramUserId())
                .addKeyValue("oldLanguage", oldLanguage)
                .addKeyValue("newLanguage", saved.getLanguage())
                .addKeyValue("traceId", MDC.get(TraceIdFilter.TRACE_ID_KEY))
                .log("Changed user language");

        return UserLanguageResult.from(saved);
    }
}
