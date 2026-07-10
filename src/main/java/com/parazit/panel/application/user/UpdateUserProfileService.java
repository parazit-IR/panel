package com.parazit.panel.application.user;

import com.parazit.panel.application.port.in.user.UpdateUserProfileUseCase;
import com.parazit.panel.application.user.command.UpdateUserProfileCommand;
import com.parazit.panel.application.user.result.UpdateUserProfileResult;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateUserProfileService implements UpdateUserProfileUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateUserProfileService.class);

    private final UserRepository userRepository;

    public UpdateUserProfileService(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
    }

    @Override
    @Transactional
    public UpdateUserProfileResult updateProfile(UpdateUserProfileCommand command) {
        validate(command);

        User user = userRepository.findByTelegramUserId(command.telegramUserId())
                .orElseThrow(() -> new UserProfileNotFoundException(command.telegramUserId()));

        user.updateProfile(command.firstName(), command.lastName());
        User saved = userRepository.save(user);

        log.atInfo()
                .addKeyValue("telegramUserId", saved.getTelegramUserId())
                .log("Updated user profile");

        return UpdateUserProfileResult.from(saved);
    }

    private void validate(UpdateUserProfileCommand command) {
        if (command == null) {
            throw new InvalidUserProfileCommandException("user profile update command must not be null");
        }
        if (command.telegramUserId() == null) {
            throw new InvalidUserProfileCommandException("telegramUserId must not be null");
        }
        if (command.telegramUserId() <= 0) {
            throw new InvalidUserProfileCommandException("telegramUserId must be positive");
        }
        validateRequiredText(command.firstName(), "firstName", User.FIRST_NAME_MAX_LENGTH);
        validateOptionalText(command.lastName(), "lastName", User.LAST_NAME_MAX_LENGTH);
    }

    private void validateRequiredText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new InvalidUserProfileCommandException(fieldName + " must not be blank");
        }
        if (value.trim().length() > maxLength) {
            throw new InvalidUserProfileCommandException(fieldName + " must be at most " + maxLength + " characters");
        }
    }

    private void validateOptionalText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (value.trim().length() > maxLength) {
            throw new InvalidUserProfileCommandException(fieldName + " must be at most " + maxLength + " characters");
        }
    }
}
