package com.parazit.panel.domain.user.repository;

import com.parazit.panel.domain.repository.UuidRepository;
import com.parazit.panel.domain.user.User;
import java.util.Optional;

public interface UserRepository extends UuidRepository<User> {

    Optional<User> findByTelegramUserId(Long telegramUserId);

    boolean existsByTelegramUserId(Long telegramUserId);
}
