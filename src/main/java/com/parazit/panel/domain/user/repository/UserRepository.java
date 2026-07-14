package com.parazit.panel.domain.user.repository;

import com.parazit.panel.domain.repository.UuidRepository;
import com.parazit.panel.domain.user.User;
import java.util.Optional;

public interface UserRepository extends UuidRepository<User> {

    Optional<User> findByTelegramUserId(Long telegramUserId);

    default Optional<User> findByIdForUpdate(java.util.UUID userId) {
        return findById(userId);
    }

    default Optional<User> findByTelegramUserIdForUpdate(Long telegramUserId) {
        return findByTelegramUserId(telegramUserId);
    }

    boolean existsByTelegramUserId(Long telegramUserId);

    default Optional<User> findByReferralCode(String referralCode) {
        throw new UnsupportedOperationException("findByReferralCode is not implemented");
    }

    default boolean existsByReferralCode(String referralCode) {
        throw new UnsupportedOperationException("existsByReferralCode is not implemented");
    }
}
