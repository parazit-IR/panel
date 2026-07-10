package com.parazit.panel.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.user.command.ChangeUserLanguageCommand;
import com.parazit.panel.application.user.result.UserLanguageResult;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.UserStatus;
import com.parazit.panel.domain.user.repository.UserRepository;
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

class ChangeUserLanguageServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-07-10T12:00:00Z");
    private static final Instant INITIAL_UPDATED_AT = Instant.parse("2026-07-10T12:05:00Z");
    private static final Instant LANGUAGE_UPDATED_AT = Instant.parse("2026-07-10T12:10:00Z");
    private static final Instant LAST_INTERACTION_AT = Instant.parse("2026-07-10T12:03:00Z");
    private static final Instant CHANGE_TIME = Instant.parse("2026-07-10T12:09:00Z");

    @Test
    void changesFaToEn() {
        FakeUserRepository repository = new FakeUserRepository();
        FakeClock clock = new FakeClock(CHANGE_TIME);
        User user = user(UserLanguage.FA);
        repository.save(user);
        repository.resetCounters();

        UserLanguageResult result = service(repository, clock)
                .changeLanguage(new ChangeUserLanguageCommand(5001L, "en"));

        assertThat(repository.findByTelegramUserIdCalls).isEqualTo(1);
        assertThat(repository.saveCalls).isEqualTo(1);
        assertThat(clock.nowCalls).isEqualTo(1);
        assertThat(result.userId()).isEqualTo(user.getId());
        assertThat(result.telegramUserId()).isEqualTo(5001L);
        assertThat(result.language()).isEqualTo(UserLanguage.EN);
        assertThat(result.updatedAt()).isEqualTo(LANGUAGE_UPDATED_AT);
        assertThat(user.getLastInteractionAt()).isEqualTo(CHANGE_TIME);
    }

    @Test
    void changesEnToFa() {
        FakeUserRepository repository = new FakeUserRepository();
        User user = user(UserLanguage.EN);
        repository.save(user);
        repository.resetCounters();

        UserLanguageResult result = service(repository, new FakeClock(CHANGE_TIME))
                .changeLanguage(new ChangeUserLanguageCommand(5001L, "fa-IR"));

        assertThat(repository.saveCalls).isEqualTo(1);
        assertThat(result.language()).isEqualTo(UserLanguage.FA);
    }

    @Test
    void sameLanguageChangeIsIdempotentAndDoesNotSave() {
        FakeUserRepository repository = new FakeUserRepository();
        FakeClock clock = new FakeClock(CHANGE_TIME);
        User user = user(UserLanguage.EN);
        repository.save(user);
        repository.resetCounters();

        UserLanguageResult result = service(repository, clock)
                .changeLanguage(new ChangeUserLanguageCommand(5001L, "EN-us"));

        assertThat(repository.findByTelegramUserIdCalls).isEqualTo(1);
        assertThat(repository.saveCalls).isZero();
        assertThat(clock.nowCalls).isZero();
        assertThat(result.language()).isEqualTo(UserLanguage.EN);
        assertThat(result.updatedAt()).isEqualTo(INITIAL_UPDATED_AT);
        assertThat(user.getLastInteractionAt()).isEqualTo(LAST_INTERACTION_AT);
    }

    @Test
    void rejectsUnsupportedLanguage() {
        FakeUserRepository repository = new FakeUserRepository();

        assertThatThrownBy(() -> service(repository, new FakeClock(CHANGE_TIME))
                .changeLanguage(new ChangeUserLanguageCommand(5001L, "de")))
                .isInstanceOf(InvalidUserLanguageCommandException.class)
                .hasMessage("Unsupported languageCode: de");

        assertThat(repository.findByTelegramUserIdCalls).isZero();
        assertThat(repository.saveCalls).isZero();
    }

    @Test
    void rejectsBlankLanguage() {
        FakeUserRepository repository = new FakeUserRepository();

        assertThatThrownBy(() -> service(repository, new FakeClock(CHANGE_TIME))
                .changeLanguage(new ChangeUserLanguageCommand(5001L, "   ")))
                .isInstanceOf(InvalidUserLanguageCommandException.class)
                .hasMessage("languageCode must not be blank");

        assertThat(repository.findByTelegramUserIdCalls).isZero();
        assertThat(repository.saveCalls).isZero();
    }

    @Test
    void missingUserThrowsUserNotFoundException() {
        FakeUserRepository repository = new FakeUserRepository();

        assertThatThrownBy(() -> service(repository, new FakeClock(CHANGE_TIME))
                .changeLanguage(new ChangeUserLanguageCommand(9999L, "en")))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found for telegramUserId 9999");

        assertThat(repository.findByTelegramUserIdCalls).isEqualTo(1);
        assertThat(repository.saveCalls).isZero();
    }

    @Test
    void blockedSuspendedAndProfileFieldsRemainUnchanged() {
        FakeUserRepository repository = new FakeUserRepository();
        User user = user(UserLanguage.FA);
        user.suspend();
        user.block();
        repository.save(user);
        repository.resetCounters();

        service(repository, new FakeClock(CHANGE_TIME))
                .changeLanguage(new ChangeUserLanguageCommand(5001L, "en"));

        User persisted = repository.findByTelegramUserId(5001L).orElseThrow();
        assertThat(persisted.getUsername()).isEqualTo("telegram_user");
        assertThat(persisted.getFirstName()).isEqualTo("Ali");
        assertThat(persisted.getLastName()).isEqualTo("Ahmadi");
        assertThat(persisted.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(persisted.getBlocked()).isTrue();
        assertThat(persisted.getLanguage()).isEqualTo(UserLanguage.EN);
    }

    private ChangeUserLanguageService service(FakeUserRepository repository, SystemClockPort clock) {
        return new ChangeUserLanguageService(repository, new UserLanguageResolver(), clock);
    }

    private User user(UserLanguage language) {
        User user = User.create(5001L, "@telegram_user", "Ali", "Ahmadi", language, LAST_INTERACTION_AT);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(user, "createdAt", CREATED_AT);
        ReflectionTestUtils.setField(user, "updatedAt", INITIAL_UPDATED_AT);
        return user;
    }

    private static final class FakeClock implements SystemClockPort {

        private final Instant instant;
        private int nowCalls;

        private FakeClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public Instant now() {
            nowCalls++;
            return instant;
        }
    }

    private static final class FakeUserRepository implements UserRepository {

        private final Map<Long, User> users = new LinkedHashMap<>();
        private int findByTelegramUserIdCalls;
        private int saveCalls;

        @Override
        public Optional<User> findByTelegramUserId(Long telegramUserId) {
            findByTelegramUserIdCalls++;
            return Optional.ofNullable(users.get(telegramUserId));
        }

        @Override
        public boolean existsByTelegramUserId(Long telegramUserId) {
            return users.containsKey(telegramUserId);
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
            if (user.getId() == null) {
                ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
                ReflectionTestUtils.setField(user, "createdAt", CREATED_AT);
            }
            ReflectionTestUtils.setField(user, "updatedAt", saveCalls == 1 && !users.containsKey(user.getTelegramUserId())
                    ? INITIAL_UPDATED_AT
                    : LANGUAGE_UPDATED_AT);
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
            saveCalls = 0;
        }
    }
}
