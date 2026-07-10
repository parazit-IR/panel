package com.parazit.panel.application.user;

import com.parazit.panel.application.port.in.user.GetUserLanguageUseCase;
import com.parazit.panel.application.user.query.GetUserLanguageQuery;
import com.parazit.panel.application.user.result.UserLanguageResult;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetUserLanguageService implements GetUserLanguageUseCase {

    private final UserRepository userRepository;

    public GetUserLanguageService(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public UserLanguageResult getLanguage(GetUserLanguageQuery query) {
        if (query == null) {
            throw new InvalidUserLanguageCommandException("user language query must not be null");
        }

        User user = userRepository.findByTelegramUserId(query.telegramUserId())
                .orElseThrow(() -> new UserNotFoundException(query.telegramUserId()));

        return UserLanguageResult.from(user);
    }
}
