package com.parazit.panel.infrastructure.persistence.user.settings;

import com.parazit.panel.domain.user.settings.UserSettings;
import com.parazit.panel.domain.user.settings.repository.UserSettingsRepository;
import com.parazit.panel.infrastructure.persistence.repository.JpaRepositoryAdapter;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class UserSettingsRepositoryAdapter extends JpaRepositoryAdapter<UserSettings, UUID> implements UserSettingsRepository {

    private final SpringDataUserSettingsRepository repository;

    public UserSettingsRepositoryAdapter(SpringDataUserSettingsRepository repository) {
        super(repository);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public UserSettings save(UserSettings settings) {
        return repository.saveAndFlush(Objects.requireNonNull(settings, "settings must not be null"));
    }

    @Override
    public Optional<UserSettings> findByUserId(UUID userId) {
        return repository.findByUserId(requireUserId(userId));
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return repository.existsByUserId(requireUserId(userId));
    }

    private UUID requireUserId(UUID userId) {
        return Objects.requireNonNull(userId, "userId must not be null");
    }
}
