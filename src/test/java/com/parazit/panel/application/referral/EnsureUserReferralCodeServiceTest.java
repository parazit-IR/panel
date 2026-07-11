package com.parazit.panel.application.referral;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.port.out.referral.ReferralCodeGenerator;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
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

class EnsureUserReferralCodeServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-10T12:00:00Z");

    @Test
    void returnsExistingCodeWithoutGenerating() {
        FakeUserRepository repository = new FakeUserRepository();
        SequenceGenerator generator = new SequenceGenerator("EFGH6789JK");
        User user = user(1001L);
        user.assignReferralCode("ABCD2345EF");

        String code = new EnsureUserReferralCodeService(repository, generator).ensureReferralCode(user);

        assertThat(code).isEqualTo("ABCD2345EF");
        assertThat(generator.calls).isZero();
        assertThat(repository.saveCalls).isZero();
    }

    @Test
    void generatesAndSavesMissingCode() {
        FakeUserRepository repository = new FakeUserRepository();
        User user = repository.save(user(1002L));

        String code = new EnsureUserReferralCodeService(repository, new SequenceGenerator("ABCD2345EF"))
                .ensureReferralCode(user);

        assertThat(code).isEqualTo("ABCD2345EF");
        assertThat(repository.findById(user.getId()).orElseThrow().getReferralCode()).isEqualTo("ABCD2345EF");
        assertThat(repository.saveCalls).isEqualTo(2);
    }

    @Test
    void retriesGeneratedCodeCollisionBeforeAssignment() {
        FakeUserRepository repository = new FakeUserRepository();
        User existing = user(1003L);
        existing.assignReferralCode("ABCD2345EF");
        repository.save(existing);
        User missing = repository.save(user(1004L));

        String code = new EnsureUserReferralCodeService(repository, new SequenceGenerator("ABCD2345EF", "EFGH6789JK"))
                .ensureReferralCode(missing);

        assertThat(code).isEqualTo("EFGH6789JK");
    }

    @Test
    void stopsAfterBoundedRetryExhaustion() {
        FakeUserRepository repository = new FakeUserRepository();
        User existing = user(1005L);
        existing.assignReferralCode("ABCD2345EF");
        repository.save(existing);

        assertThatThrownBy(() -> new EnsureUserReferralCodeService(repository, new SequenceGenerator(
                "ABCD2345EF", "ABCD2345EF", "ABCD2345EF", "ABCD2345EF", "ABCD2345EF"
        )).ensureReferralCode(user(1006L)))
                .isInstanceOf(ReferralAssignmentException.class)
                .hasMessage("Could not generate a unique referral code");
    }

    private User user(Long telegramUserId) {
        return User.create(telegramUserId, null, "Ali", null, UserLanguage.FA, NOW);
    }

    private static final class SequenceGenerator implements ReferralCodeGenerator {

        private final List<String> codes;
        private int calls;

        private SequenceGenerator(String... codes) {
            this.codes = List.of(codes);
        }

        @Override
        public String generate() {
            return codes.get(Math.min(calls++, codes.size() - 1));
        }
    }

    private static final class FakeUserRepository implements UserRepository {

        private final Map<UUID, User> users = new LinkedHashMap<>();
        private int saveCalls;

        @Override
        public Optional<User> findByTelegramUserId(Long telegramUserId) {
            return users.values().stream()
                    .filter(user -> telegramUserId.equals(user.getTelegramUserId()))
                    .findFirst();
        }

        @Override
        public boolean existsByTelegramUserId(Long telegramUserId) {
            return findByTelegramUserId(telegramUserId).isPresent();
        }

        @Override
        public Optional<User> findByReferralCode(String referralCode) {
            return users.values().stream()
                    .filter(user -> referralCode.equals(user.getReferralCode()))
                    .findFirst();
        }

        @Override
        public boolean existsByReferralCode(String referralCode) {
            return findByReferralCode(referralCode).isPresent();
        }

        @Override
        public Optional<User> findById(UUID id) {
            return Optional.ofNullable(users.get(id));
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
            }
            users.put(user.getId(), user);
            return user;
        }

        @Override
        public List<User> saveAll(Collection<User> entities) {
            return entities.stream().map(this::save).toList();
        }

        @Override
        public boolean existsById(UUID id) {
            return users.containsKey(id);
        }

        @Override
        public long count() {
            return users.size();
        }

        @Override
        public void delete(User entity) {
            users.remove(entity.getId());
        }

        @Override
        public void deleteById(UUID id) {
            users.remove(id);
        }
    }
}
