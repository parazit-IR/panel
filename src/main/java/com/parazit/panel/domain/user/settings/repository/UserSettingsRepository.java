package com.parazit.panel.domain.user.settings.repository;

import com.parazit.panel.domain.repository.UuidRepository;
import com.parazit.panel.domain.user.settings.UserSettings;
import java.util.Optional;
import java.util.UUID;

public interface UserSettingsRepository extends UuidRepository<UserSettings> {

    Optional<UserSettings> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
