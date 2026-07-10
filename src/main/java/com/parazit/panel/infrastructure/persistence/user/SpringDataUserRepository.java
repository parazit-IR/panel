package com.parazit.panel.infrastructure.persistence.user;

import com.parazit.panel.domain.user.User;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import java.util.Optional;

public interface SpringDataUserRepository extends SpringDataUuidRepository<User> {

    Optional<User> findByTelegramUserId(Long telegramUserId);

    boolean existsByTelegramUserId(Long telegramUserId);
}
