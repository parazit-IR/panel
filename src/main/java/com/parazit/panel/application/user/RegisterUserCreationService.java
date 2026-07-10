package com.parazit.panel.application.user;

import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class RegisterUserCreationService {

    private final UserRepository userRepository;

    RegisterUserCreationService(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User create(User user) {
        return userRepository.save(Objects.requireNonNull(user, "user must not be null"));
    }
}
