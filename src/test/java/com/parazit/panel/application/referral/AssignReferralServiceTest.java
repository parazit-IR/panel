package com.parazit.panel.application.referral;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.referral.command.AssignReferralCommand;
import com.parazit.panel.application.referral.result.AssignReferralResult;
import com.parazit.panel.application.user.UserNotFoundException;
import com.parazit.panel.domain.referral.Referral;
import com.parazit.panel.domain.referral.ReferralStatus;
import com.parazit.panel.domain.referral.repository.ReferralRepository;
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

class AssignReferralServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-10T12:00:00Z");

    @Test
    void assignsReferralWhenReferredUserHasNoReferrer() {
        Fixture fixture = new Fixture();
        User referrer = fixture.saveUser(1001L, "ABCD2345EF");
        User referred = fixture.saveUser(1002L, "EFGH6789JK");

        AssignReferralResult result = fixture.service.assign(new AssignReferralCommand(1002L, "abcd2345ef"));

        assertThat(result.referrerUserId()).isEqualTo(referrer.getId());
        assertThat(result.referredUserId()).isEqualTo(referred.getId());
        assertThat(result.referralCodeUsed()).isEqualTo("ABCD2345EF");
        assertThat(result.status()).isEqualTo(ReferralStatus.PENDING);
        assertThat(result.referredAt()).isEqualTo(NOW);
        assertThat(result.newlyAssigned()).isTrue();
        assertThat(fixture.referralRepository.countByReferrerUserId(referrer.getId())).isEqualTo(1);
    }

    @Test
    void repeatedSameAssignmentReturnsExistingReferral() {
        Fixture fixture = new Fixture();
        fixture.saveUser(1001L, "ABCD2345EF");
        fixture.saveUser(1002L, "EFGH6789JK");
        AssignReferralResult first = fixture.service.assign(new AssignReferralCommand(1002L, "ABCD2345EF"));

        AssignReferralResult second = fixture.service.assign(new AssignReferralCommand(1002L, "abcd2345ef"));

        assertThat(second.referralId()).isEqualTo(first.referralId());
        assertThat(second.newlyAssigned()).isFalse();
        assertThat(fixture.referralRepository.count()).isEqualTo(1);
    }

    @Test
    void differentExistingReferrerReturnsConflict() {
        Fixture fixture = new Fixture();
        fixture.saveUser(1001L, "ABCD2345EF");
        fixture.saveUser(1002L, "EFGH6789JK");
        fixture.saveUser(1003L, "JKLM2345NP");
        fixture.service.assign(new AssignReferralCommand(1002L, "ABCD2345EF"));

        assertThatThrownBy(() -> fixture.service.assign(new AssignReferralCommand(1002L, "JKLM2345NP")))
                .isInstanceOf(ReferralAlreadyAssignedException.class)
                .hasMessage("Referral has already been assigned to another referrer");
        assertThat(fixture.referralRepository.count()).isEqualTo(1);
    }

    @Test
    void rejectsUnknownCodeSelfReferralMissingUserAndNullCommand() {
        Fixture fixture = new Fixture();
        fixture.saveUser(1001L, "ABCD2345EF");
        fixture.saveUser(1002L, "EFGH6789JK");

        assertThatThrownBy(() -> fixture.service.assign(new AssignReferralCommand(9999L, "ABCD2345EF")))
                .isInstanceOf(UserNotFoundException.class);
        assertThatThrownBy(() -> fixture.service.assign(new AssignReferralCommand(1002L, "JKLM2345NP")))
                .isInstanceOf(ReferralCodeNotFoundException.class);
        assertThatThrownBy(() -> fixture.service.assign(new AssignReferralCommand(1001L, "ABCD2345EF")))
                .isInstanceOf(SelfReferralNotAllowedException.class);
        assertThatNullPointerException()
                .isThrownBy(() -> fixture.service.assign(null))
                .withMessage("command must not be null");
    }

    private static final class Fixture {

        private final FakeUserRepository userRepository = new FakeUserRepository();
        private final FakeReferralRepository referralRepository = new FakeReferralRepository();
        private final AssignReferralService service = new AssignReferralService(
                userRepository,
                referralRepository,
                new ReferralCreationService(referralRepository),
                () -> NOW
        );

        private User saveUser(Long telegramUserId, String referralCode) {
            User user = User.create(telegramUserId, null, "Ali", null, UserLanguage.FA, NOW);
            user.assignReferralCode(referralCode);
            return userRepository.save(user);
        }
    }

    private static final class FakeUserRepository implements UserRepository {

        private final Map<UUID, User> usersById = new LinkedHashMap<>();

        @Override
        public Optional<User> findByTelegramUserId(Long telegramUserId) {
            return usersById.values().stream()
                    .filter(user -> telegramUserId.equals(user.getTelegramUserId()))
                    .findFirst();
        }

        @Override
        public boolean existsByTelegramUserId(Long telegramUserId) {
            return findByTelegramUserId(telegramUserId).isPresent();
        }

        @Override
        public Optional<User> findByReferralCode(String referralCode) {
            return usersById.values().stream()
                    .filter(user -> referralCode.equals(user.getReferralCode()))
                    .findFirst();
        }

        @Override
        public boolean existsByReferralCode(String referralCode) {
            return findByReferralCode(referralCode).isPresent();
        }

        @Override
        public Optional<User> findById(UUID id) {
            return Optional.ofNullable(usersById.get(id));
        }

        @Override
        public List<User> findAll() {
            return new ArrayList<>(usersById.values());
        }

        @Override
        public User save(User user) {
            if (user.getId() == null) {
                ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
            }
            usersById.put(user.getId(), user);
            return user;
        }

        @Override
        public List<User> saveAll(Collection<User> entities) {
            return entities.stream().map(this::save).toList();
        }

        @Override
        public boolean existsById(UUID id) {
            return usersById.containsKey(id);
        }

        @Override
        public long count() {
            return usersById.size();
        }

        @Override
        public void delete(User entity) {
            usersById.remove(entity.getId());
        }

        @Override
        public void deleteById(UUID id) {
            usersById.remove(id);
        }
    }

    private static final class FakeReferralRepository implements ReferralRepository {

        private final Map<UUID, Referral> referralsById = new LinkedHashMap<>();

        @Override
        public Optional<Referral> findByReferredUserId(UUID referredUserId) {
            return referralsById.values().stream()
                    .filter(referral -> referredUserId.equals(referral.getReferredUserId()))
                    .findFirst();
        }

        @Override
        public List<Referral> findAllByReferrerUserId(UUID referrerUserId) {
            return referralsById.values().stream()
                    .filter(referral -> referrerUserId.equals(referral.getReferrerUserId()))
                    .toList();
        }

        @Override
        public boolean existsByReferredUserId(UUID referredUserId) {
            return findByReferredUserId(referredUserId).isPresent();
        }

        @Override
        public long countByReferrerUserId(UUID referrerUserId) {
            return findAllByReferrerUserId(referrerUserId).size();
        }

        @Override
        public Optional<Referral> findById(UUID id) {
            return Optional.ofNullable(referralsById.get(id));
        }

        @Override
        public List<Referral> findAll() {
            return new ArrayList<>(referralsById.values());
        }

        @Override
        public Referral save(Referral referral) {
            if (referral.getId() == null) {
                ReflectionTestUtils.setField(referral, "id", UUID.randomUUID());
            }
            referralsById.put(referral.getId(), referral);
            return referral;
        }

        @Override
        public List<Referral> saveAll(Collection<Referral> entities) {
            return entities.stream().map(this::save).toList();
        }

        @Override
        public boolean existsById(UUID id) {
            return referralsById.containsKey(id);
        }

        @Override
        public long count() {
            return referralsById.size();
        }

        @Override
        public void delete(Referral entity) {
            referralsById.remove(entity.getId());
        }

        @Override
        public void deleteById(UUID id) {
            referralsById.remove(id);
        }
    }
}
