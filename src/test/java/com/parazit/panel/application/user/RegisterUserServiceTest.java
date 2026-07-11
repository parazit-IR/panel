package com.parazit.panel.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.port.out.referral.ReferralCodeGenerator;
import com.parazit.panel.application.referral.EnsureUserReferralCodeService;
import com.parazit.panel.application.user.command.RegisterUserCommand;
import com.parazit.panel.application.user.result.RegisterUserResult;
import com.parazit.panel.application.user.settings.UserSettingsCreationService;
import com.parazit.panel.application.user.settings.UserSettingsDefaultsService;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.UserStatus;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.user.settings.UserSettings;
import com.parazit.panel.domain.user.settings.repository.UserSettingsRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RegisterUserServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-01T00:00:10Z");
    private static final Instant NOW = Instant.parse("2026-07-10T12:00:00Z");
    private static final Instant LATER = Instant.parse("2026-07-10T12:05:00Z");

    @Test
    void registersNewUser() {
        FakeUserRepository repository = new FakeUserRepository();
        FakeUserSettingsRepository settingsRepository = new FakeUserSettingsRepository();
        RegisterUserService service = service(repository, settingsRepository, () -> NOW);

        RegisterUserResult result = service.register(new RegisterUserCommand(
                1001L,
                "@telegram_user",
                " Ali ",
                " Ahmadi ",
                "en-US"
        ));

        assertThat(repository.findByTelegramUserIdCalls).isEqualTo(1);
        assertThat(repository.existsByTelegramUserIdCalls).isZero();
        assertThat(repository.saveCalls).isEqualTo(1);
        assertThat(repository.count()).isEqualTo(1);
        assertThat(result.userId()).isNotNull();
        assertThat(result.telegramUserId()).isEqualTo(1001L);
        assertThat(result.username()).isEqualTo("telegram_user");
        assertThat(result.firstName()).isEqualTo("Ali");
        assertThat(result.lastName()).isEqualTo("Ahmadi");
        assertThat(result.language()).isEqualTo(UserLanguage.EN);
        assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(result.blocked()).isFalse();
        assertThat(result.newlyCreated()).isTrue();
        assertThat(result.registeredAt()).isEqualTo(CREATED_AT);
        assertThat(result.lastInteractionAt()).isEqualTo(NOW);
        assertThat(settingsRepository.saveCalls).isEqualTo(1);
        UserSettings settings = settingsRepository.findByUserId(result.userId()).orElseThrow();
        assertThat(settings.isNotificationsEnabled()).isTrue();
        assertThat(settings.isRenewalRemindersEnabled()).isTrue();
        assertThat(settings.isUsageAlertsEnabled()).isTrue();
        assertThat(settings.getUsageAlertThresholdPercent()).isEqualTo(80);
    }

    @Test
    void refreshesExistingUserWithoutChangingBusinessStateOrLanguage() {
        FakeUserRepository repository = new FakeUserRepository();
        FakeUserSettingsRepository settingsRepository = new FakeUserSettingsRepository();
        User existing = User.create(1002L, "old_username", "Ali", "Ahmadi", UserLanguage.FA, NOW);
        existing.changeLanguage(UserLanguage.EN);
        existing.suspend();
        existing.block();
        repository.save(existing);
        UserSettings existingSettings = UserSettings.createDefault(existing.getId());
        existingSettings.updatePreferences(false, false, true, 35);
        settingsRepository.save(existingSettings);
        repository.resetCounters();
        settingsRepository.resetCounters();

        RegisterUserService service = service(repository, settingsRepository, () -> LATER);

        RegisterUserResult result = service.register(new RegisterUserCommand(
                1002L,
                "@new_username",
                " Sara ",
                " Karimi ",
                "fa"
        ));

        assertThat(repository.findByTelegramUserIdCalls).isEqualTo(1);
        assertThat(repository.existsByTelegramUserIdCalls).isZero();
        assertThat(repository.saveCalls).isEqualTo(1);
        assertThat(repository.count()).isEqualTo(1);
        assertThat(result.userId()).isEqualTo(existing.getId());
        assertThat(result.username()).isEqualTo("new_username");
        assertThat(result.firstName()).isEqualTo("Sara");
        assertThat(result.lastName()).isEqualTo("Karimi");
        assertThat(result.lastInteractionAt()).isEqualTo(LATER);
        assertThat(result.language()).isEqualTo(UserLanguage.EN);
        assertThat(result.status()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(result.blocked()).isTrue();
        assertThat(result.newlyCreated()).isFalse();
        assertThat(settingsRepository.saveCalls).isZero();
        UserSettings preservedSettings = settingsRepository.findByUserId(existing.getId()).orElseThrow();
        assertThat(preservedSettings.isNotificationsEnabled()).isFalse();
        assertThat(preservedSettings.isRenewalRemindersEnabled()).isFalse();
        assertThat(preservedSettings.isUsageAlertsEnabled()).isTrue();
        assertThat(preservedSettings.getUsageAlertThresholdPercent()).isEqualTo(35);
    }

    @Test
    void rejectsNullCommand() {
        RegisterUserService service = service(new FakeUserRepository(), new FakeUserSettingsRepository(), () -> NOW);

        assertThatThrownBy(() -> service.register(null))
                .isInstanceOf(InvalidRegistrationCommandException.class)
                .hasMessage("registration command must not be null");
    }

    @Test
    void rejectsNonPositiveTelegramUserId() {
        RegisterUserService service = service(new FakeUserRepository(), new FakeUserSettingsRepository(), () -> NOW);

        assertThatThrownBy(() -> service.register(new RegisterUserCommand(0L, null, "Ali", null, null)))
                .isInstanceOf(InvalidRegistrationCommandException.class)
                .hasMessage("telegramUserId must be positive");
    }

    @Test
    void rejectsBlankFirstName() {
        RegisterUserService service = service(new FakeUserRepository(), new FakeUserSettingsRepository(), () -> NOW);

        assertThatThrownBy(() -> service.register(new RegisterUserCommand(1003L, null, "   ", null, null)))
                .isInstanceOf(InvalidRegistrationCommandException.class)
                .hasMessage("firstName must not be blank");
    }

    @Test
    void usesSystemClockPortForInteractionTime() {
        FakeUserRepository repository = new FakeUserRepository();
        RegisterUserService service = service(repository, new FakeUserSettingsRepository(), () -> LATER);

        RegisterUserResult result = service.register(new RegisterUserCommand(1004L, null, "Ali", null, null));

        assertThat(result.lastInteractionAt()).isEqualTo(LATER);
    }

    @Test
    void assignsReferralCodeToNewUserAndPreservesItOnRepeatedRegistration() {
        FakeUserRepository repository = new FakeUserRepository();
        FakeUserSettingsRepository settingsRepository = new FakeUserSettingsRepository();
        RegisterUserService service = service(repository, settingsRepository, () -> NOW);

        RegisterUserResult first = service.register(new RegisterUserCommand(1005L, null, "Ali", null, null));
        String referralCode = repository.findById(first.userId()).orElseThrow().getReferralCode();
        RegisterUserResult second = service.register(new RegisterUserCommand(1005L, "updated", "Sara", null, null));

        assertThat(referralCode).isEqualTo("ABCD2345EF");
        assertThat(second.userId()).isEqualTo(first.userId());
        assertThat(repository.findById(first.userId()).orElseThrow().getReferralCode()).isEqualTo(referralCode);
    }

    private RegisterUserService service(
            FakeUserRepository repository,
            FakeUserSettingsRepository settingsRepository,
            SystemClockPort clockPort
    ) {
        UserSettingsCreationService creationService = new UserSettingsCreationService(settingsRepository);
        return new RegisterUserService(
                repository,
                clockPort,
                new UserLanguageResolver(),
                new RegisterUserCreationService(repository),
                new UserSettingsDefaultsService(settingsRepository, creationService),
                new EnsureUserReferralCodeService(repository, new FixedReferralCodeGenerator())
        );
    }

    private static final class FixedReferralCodeGenerator implements ReferralCodeGenerator {

        @Override
        public String generate() {
            return "ABCD2345EF";
        }
    }

    private static final class FakeUserRepository implements UserRepository {

        private final Map<Long, User> users = new LinkedHashMap<>();
        private int findByTelegramUserIdCalls;
        private int existsByTelegramUserIdCalls;
        private int saveCalls;

        @Override
        public Optional<User> findByTelegramUserId(Long telegramUserId) {
            findByTelegramUserIdCalls++;
            return Optional.ofNullable(users.get(telegramUserId));
        }

        @Override
        public boolean existsByTelegramUserId(Long telegramUserId) {
            existsByTelegramUserIdCalls++;
            return users.containsKey(telegramUserId);
        }

        @Override
        public Optional<User> findByReferralCode(String referralCode) {
            return users.values()
                    .stream()
                    .filter(user -> referralCode.equals(user.getReferralCode()))
                    .findFirst();
        }

        @Override
        public boolean existsByReferralCode(String referralCode) {
            return findByReferralCode(referralCode).isPresent();
        }

        @Override
        public Optional<User> findById(UUID id) {
            return users.values()
                    .stream()
                    .filter(user -> id.equals(user.getId()))
                    .findFirst();
        }

        @Override
        public List<User> findAll() {
            return new ArrayList<>(users.values());
        }

        @Override
        public User save(User user) {
            saveCalls++;
            assignPersistenceFields(user);
            users.put(user.getTelegramUserId(), user);
            return user;
        }

        @Override
        public List<User> saveAll(Collection<User> entities) {
            return entities.stream()
                    .map(this::save)
                    .toList();
        }

        @Override
        public boolean existsById(UUID id) {
            return findById(id).isPresent();
        }

        @Override
        public long count() {
            return users.size();
        }

        @Override
        public void delete(User entity) {
            users.remove(entity.getTelegramUserId());
        }

        @Override
        public void deleteById(UUID id) {
            findById(id).ifPresent(this::delete);
        }

        private void resetCounters() {
            findByTelegramUserIdCalls = 0;
            existsByTelegramUserIdCalls = 0;
            saveCalls = 0;
        }

        private void assignPersistenceFields(User user) {
            if (user.getId() == null) {
                ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
            }
            if (user.getCreatedAt() == null) {
                ReflectionTestUtils.setField(user, "createdAt", CREATED_AT);
            }
            ReflectionTestUtils.setField(user, "updatedAt", UPDATED_AT);
        }
    }

    private static final class FakeUserSettingsRepository implements UserSettingsRepository {

        private final Map<UUID, UserSettings> settingsByUserId = new LinkedHashMap<>();
        private int saveCalls;

        @Override
        public Optional<UserSettings> findByUserId(UUID userId) {
            return Optional.ofNullable(settingsByUserId.get(userId));
        }

        @Override
        public boolean existsByUserId(UUID userId) {
            return settingsByUserId.containsKey(userId);
        }

        @Override
        public Optional<UserSettings> findById(UUID id) {
            return settingsByUserId.values()
                    .stream()
                    .filter(settings -> id.equals(settings.getId()))
                    .findFirst();
        }

        @Override
        public List<UserSettings> findAll() {
            return new ArrayList<>(settingsByUserId.values());
        }

        @Override
        public UserSettings save(UserSettings settings) {
            saveCalls++;
            if (settings.getId() == null) {
                ReflectionTestUtils.setField(settings, "id", UUID.randomUUID());
            }
            if (settings.getCreatedAt() == null) {
                ReflectionTestUtils.setField(settings, "createdAt", CREATED_AT);
            }
            ReflectionTestUtils.setField(settings, "updatedAt", UPDATED_AT);
            settingsByUserId.put(settings.getUserId(), settings);
            return settings;
        }

        @Override
        public List<UserSettings> saveAll(Collection<UserSettings> entities) {
            return entities.stream().map(this::save).toList();
        }

        @Override
        public boolean existsById(UUID id) {
            return findById(id).isPresent();
        }

        @Override
        public long count() {
            return settingsByUserId.size();
        }

        @Override
        public void delete(UserSettings entity) {
            settingsByUserId.remove(entity.getUserId());
        }

        @Override
        public void deleteById(UUID id) {
            findById(id).ifPresent(this::delete);
        }

        private void resetCounters() {
            saveCalls = 0;
        }
    }
}
