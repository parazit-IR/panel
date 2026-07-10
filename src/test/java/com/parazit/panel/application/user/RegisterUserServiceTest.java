package com.parazit.panel.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.user.command.RegisterUserCommand;
import com.parazit.panel.application.user.result.RegisterUserResult;
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

class RegisterUserServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-01T00:00:10Z");
    private static final Instant NOW = Instant.parse("2026-07-10T12:00:00Z");
    private static final Instant LATER = Instant.parse("2026-07-10T12:05:00Z");

    @Test
    void registersNewUser() {
        FakeUserRepository repository = new FakeUserRepository();
        RegisterUserService service = service(repository, () -> NOW);

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
    }

    @Test
    void refreshesExistingUserWithoutChangingBusinessStateOrLanguage() {
        FakeUserRepository repository = new FakeUserRepository();
        User existing = User.create(1002L, "old_username", "Ali", "Ahmadi", UserLanguage.FA, NOW);
        existing.changeLanguage(UserLanguage.EN);
        existing.suspend();
        existing.block();
        repository.save(existing);
        repository.resetCounters();

        RegisterUserService service = service(repository, () -> LATER);

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
    }

    @Test
    void rejectsNullCommand() {
        RegisterUserService service = service(new FakeUserRepository(), () -> NOW);

        assertThatThrownBy(() -> service.register(null))
                .isInstanceOf(InvalidRegistrationCommandException.class)
                .hasMessage("registration command must not be null");
    }

    @Test
    void rejectsNonPositiveTelegramUserId() {
        RegisterUserService service = service(new FakeUserRepository(), () -> NOW);

        assertThatThrownBy(() -> service.register(new RegisterUserCommand(0L, null, "Ali", null, null)))
                .isInstanceOf(InvalidRegistrationCommandException.class)
                .hasMessage("telegramUserId must be positive");
    }

    @Test
    void rejectsBlankFirstName() {
        RegisterUserService service = service(new FakeUserRepository(), () -> NOW);

        assertThatThrownBy(() -> service.register(new RegisterUserCommand(1003L, null, "   ", null, null)))
                .isInstanceOf(InvalidRegistrationCommandException.class)
                .hasMessage("firstName must not be blank");
    }

    @Test
    void usesSystemClockPortForInteractionTime() {
        FakeUserRepository repository = new FakeUserRepository();
        RegisterUserService service = service(repository, () -> LATER);

        RegisterUserResult result = service.register(new RegisterUserCommand(1004L, null, "Ali", null, null));

        assertThat(result.lastInteractionAt()).isEqualTo(LATER);
    }

    private RegisterUserService service(FakeUserRepository repository, SystemClockPort clockPort) {
        return new RegisterUserService(
                repository,
                clockPort,
                new UserLanguageResolver(),
                new RegisterUserCreationService(repository)
        );
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
}
