package com.parazit.panel.infrastructure.persistence.user;

import com.parazit.panel.domain.user.User;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;

public interface SpringDataUserRepository extends SpringDataUuidRepository<User> {

    Optional<User> findByTelegramUserId(Long telegramUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findWithLockByTelegramUserId(Long telegramUserId);

    boolean existsByTelegramUserId(Long telegramUserId);

    Optional<User> findByReferralCode(String referralCode);

    boolean existsByReferralCode(String referralCode);
}
