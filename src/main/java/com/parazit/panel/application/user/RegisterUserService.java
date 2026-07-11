package com.parazit.panel.application.user;

import com.parazit.panel.application.port.in.user.RegisterUserUseCase;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.referral.EnsureUserReferralCodeService;
import com.parazit.panel.application.user.command.RegisterUserCommand;
import com.parazit.panel.application.user.result.RegisterUserResult;
import com.parazit.panel.application.user.settings.UserSettingsDefaultsService;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUserService implements RegisterUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterUserService.class);
    private static final int MAX_REFERRAL_CODE_COLLISION_RETRIES = 5;

    private final UserRepository userRepository;
    private final SystemClockPort systemClockPort;
    private final UserLanguageResolver userLanguageResolver;
    private final RegisterUserCreationService creationService;
    private final UserSettingsDefaultsService userSettingsDefaultsService;
    private final EnsureUserReferralCodeService ensureUserReferralCodeService;

    public RegisterUserService(
            UserRepository userRepository,
            SystemClockPort systemClockPort,
            UserLanguageResolver userLanguageResolver,
            RegisterUserCreationService creationService,
            UserSettingsDefaultsService userSettingsDefaultsService,
            EnsureUserReferralCodeService ensureUserReferralCodeService
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.systemClockPort = Objects.requireNonNull(systemClockPort, "systemClockPort must not be null");
        this.userLanguageResolver = Objects.requireNonNull(userLanguageResolver, "userLanguageResolver must not be null");
        this.creationService = Objects.requireNonNull(creationService, "creationService must not be null");
        this.userSettingsDefaultsService = Objects.requireNonNull(userSettingsDefaultsService, "userSettingsDefaultsService must not be null");
        this.ensureUserReferralCodeService = Objects.requireNonNull(ensureUserReferralCodeService, "ensureUserReferralCodeService must not be null");
    }

    @Override
    @Transactional
    public RegisterUserResult register(RegisterUserCommand command) {
        validate(command);

        Instant now = systemClockPort.now();
        Long telegramUserId = command.telegramUserId();

        return userRepository.findByTelegramUserId(telegramUserId)
                .map(user -> refreshExistingUser(user, command, now))
                .orElseGet(() -> registerNewUser(command, now));
    }

    private RegisterUserResult registerNewUser(RegisterUserCommand command, Instant now) {
        UserLanguage initialLanguage = userLanguageResolver.resolveOrDefault(command.languageCode());
        for (int attempt = 1; attempt <= MAX_REFERRAL_CODE_COLLISION_RETRIES; attempt++) {
            User user = User.create(
                    command.telegramUserId(),
                    command.username(),
                    command.firstName(),
                    command.lastName(),
                    initialLanguage,
                    now
            );
            ensureUserReferralCodeService.assignReferralCode(user);

            try {
                User saved = creationService.create(user);
                userSettingsDefaultsService.ensureDefaults(saved);
                log.atInfo()
                        .addKeyValue("userId", saved.getId())
                        .addKeyValue("telegramUserId", saved.getTelegramUserId())
                        .addKeyValue("newlyCreated", true)
                        .log("Registered new Telegram user");
                return RegisterUserResult.from(saved, true);
            } catch (DataIntegrityViolationException exception) {
                if (userRepository.findByTelegramUserId(command.telegramUserId()).isPresent()) {
                    log.atWarn()
                            .addKeyValue("telegramUserId", command.telegramUserId())
                            .addKeyValue("newlyCreated", false)
                            .log("Recovered concurrent Telegram user registration");
                    return recoverConcurrentRegistration(command, now);
                }
                log.atDebug()
                        .addKeyValue("attempt", attempt)
                        .log("Retrying registration after referral code collision");
            }
        }

        throw new UserRegistrationException("Could not register user with a unique referral code");
    }

    private RegisterUserResult recoverConcurrentRegistration(RegisterUserCommand command, Instant now) {
        User existingUser = userRepository.findByTelegramUserId(command.telegramUserId())
                .orElseThrow(() -> new UserRegistrationException("Concurrent registration recovery failed"));

        return refreshExistingUser(existingUser, command, now);
    }

    private RegisterUserResult refreshExistingUser(User user, RegisterUserCommand command, Instant now) {
        user.updateTelegramProfile(command.username(), command.firstName(), command.lastName(), now);
        ensureUserReferralCodeService.assignReferralCode(user);
        User saved = userRepository.save(user);
        userSettingsDefaultsService.ensureDefaults(saved);

        log.atDebug()
                .addKeyValue("userId", saved.getId())
                .addKeyValue("telegramUserId", saved.getTelegramUserId())
                .addKeyValue("newlyCreated", false)
                .log("Refreshed existing Telegram user profile");

        return RegisterUserResult.from(saved, false);
    }

    private void validate(RegisterUserCommand command) {
        if (command == null) {
            throw new InvalidRegistrationCommandException("registration command must not be null");
        }
        if (command.telegramUserId() <= 0) {
            throw new InvalidRegistrationCommandException("telegramUserId must be positive");
        }
        validateRequiredText(command.firstName(), "firstName", User.FIRST_NAME_MAX_LENGTH);
        validateOptionalText(command.username(), "username", User.USERNAME_MAX_LENGTH);
        validateOptionalText(command.lastName(), "lastName", User.LAST_NAME_MAX_LENGTH);
    }

    private void validateRequiredText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new InvalidRegistrationCommandException(fieldName + " must not be blank");
        }
        if (value.trim().length() > maxLength) {
            throw new InvalidRegistrationCommandException(fieldName + " must be at most " + maxLength + " characters");
        }
    }

    private void validateOptionalText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (value.trim().length() > maxLength) {
            throw new InvalidRegistrationCommandException(fieldName + " must be at most " + maxLength + " characters");
        }
    }
}
