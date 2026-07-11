package com.parazit.panel.infrastructure.persistence.user;

import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.referral.ReferralCodePolicy;
import com.parazit.panel.infrastructure.persistence.repository.JpaRepositoryAdapter;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryAdapter extends JpaRepositoryAdapter<User, UUID> implements UserRepository {

    private final SpringDataUserRepository repository;

    public UserRepositoryAdapter(SpringDataUserRepository repository) {
        super(repository);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public User save(User user) {
        return repository.saveAndFlush(Objects.requireNonNull(user, "user must not be null"));
    }

    @Override
    public Optional<User> findByTelegramUserId(Long telegramUserId) {
        return repository.findByTelegramUserId(requireTelegramUserId(telegramUserId));
    }

    @Override
    public boolean existsByTelegramUserId(Long telegramUserId) {
        return repository.existsByTelegramUserId(requireTelegramUserId(telegramUserId));
    }

    @Override
    public Optional<User> findByReferralCode(String referralCode) {
        return repository.findByReferralCode(ReferralCodePolicy.normalizeAndValidate(referralCode));
    }

    @Override
    public boolean existsByReferralCode(String referralCode) {
        return repository.existsByReferralCode(ReferralCodePolicy.normalizeAndValidate(referralCode));
    }

    private Long requireTelegramUserId(Long telegramUserId) {
        return Objects.requireNonNull(telegramUserId, "telegramUserId must not be null");
    }
}
