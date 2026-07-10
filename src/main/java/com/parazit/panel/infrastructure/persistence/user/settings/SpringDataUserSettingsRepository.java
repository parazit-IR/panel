package com.parazit.panel.infrastructure.persistence.user.settings;

import com.parazit.panel.domain.user.settings.UserSettings;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataUserSettingsRepository extends SpringDataUuidRepository<UserSettings> {

    Optional<UserSettings> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
