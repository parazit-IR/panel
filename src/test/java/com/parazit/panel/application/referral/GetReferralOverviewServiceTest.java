package com.parazit.panel.application.referral;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.port.out.referral.ReferralCodeGenerator;
import com.parazit.panel.application.referral.query.GetReferralOverviewQuery;
import com.parazit.panel.application.referral.result.ReferralOverviewResult;
import com.parazit.panel.application.user.UserNotFoundException;
import com.parazit.panel.domain.referral.Referral;
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

class GetReferralOverviewServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-10T12:00:00Z");

    @Test
    void returnsOverviewForUserWithExistingReferralCode() {
        Fixture fixture = new Fixture("NEVERUSED");
        User user = fixture.saveUser(2001L, "ABCD2345EF");

        ReferralOverviewResult result = fixture.service.getOverview(new GetReferralOverviewQuery(2001L));

        assertThat(result.userId()).isEqualTo(user.getId());
        assertThat(result.telegramUserId()).isEqualTo(2001L);
        assertThat(result.referralCode()).isEqualTo("ABCD2345EF");
        assertThat(result.referralCount()).isZero();
        assertThat(result.referrerUserId()).isNull();
        assertThat(result.referrerTelegramUserId()).isNull();
        assertThat(fixture.generatorCalls).isZero();
    }

    @Test
    void backfillsMissingReferralCodeWhenOverviewIsRequested() {
        Fixture fixture = new Fixture("EFGH6789JK");
        User user = fixture.saveUserWithoutCode(2002L);

        ReferralOverviewResult result = fixture.service.getOverview(new GetReferralOverviewQuery(2002L));

        assertThat(result.referralCode()).isEqualTo("EFGH6789JK");
        assertThat(fixture.userRepository.findById(user.getId()).orElseThrow().getReferralCode()).isEqualTo("EFGH6789JK");
        assertThat(fixture.generatorCalls).isEqualTo(1);
    }

    @Test
    void returnsReferralCountAndReferrerSummary() {
        Fixture fixture = new Fixture("UNUSED2345");
        User referrer = fixture.saveUser(2003L, "JKLM2345NP");
        User referred = fixture.saveUser(2004L, "QRST6789UV");
        User anotherReferred = fixture.saveUser(2005L, "WXYZ2345AA");
        fixture.referralRepository.save(Referral.create(referrer.getId(), referred.getId(), "JKLM2345NP", NOW));
        fixture.referralRepository.save(Referral.create(referrer.getId(), anotherReferred.getId(), "JKLM2345NP", NOW));

        ReferralOverviewResult referrerOverview = fixture.service.getOverview(new GetReferralOverviewQuery(2003L));
        ReferralOverviewResult referredOverview = fixture.service.getOverview(new GetReferralOverviewQuery(2004L));

        assertThat(referrerOverview.referralCount()).isEqualTo(2);
        assertThat(referredOverview.referrerUserId()).isEqualTo(referrer.getId());
        assertThat(referredOverview.referrerTelegramUserId()).isEqualTo(2003L);
    }

    @Test
    void rejectsMissingUserAndNullQuery() {
        Fixture fixture = new Fixture("ABCD2345EF");

        assertThatThrownBy(() -> fixture.service.getOverview(new GetReferralOverviewQuery(9999L)))
                .isInstanceOf(UserNotFoundException.class);
        assertThatNullPointerException()
                .isThrownBy(() -> fixture.service.getOverview(null))
                .withMessage("query must not be null");
    }

    private static final class Fixture implements ReferralCodeGenerator {

        private final FakeUserRepository userRepository = new FakeUserRepository();
        private final FakeReferralRepository referralRepository = new FakeReferralRepository();
        private final GetReferralOverviewService service = new GetReferralOverviewService(
                userRepository,
                referralRepository,
                new EnsureUserReferralCodeService(userRepository, this)
        );
        private final String generatedCode;
        private int generatorCalls;

        private Fixture(String generatedCode) {
            this.generatedCode = generatedCode;
        }

        @Override
        public String generate() {
            generatorCalls++;
            return generatedCode;
        }

        private User saveUser(Long telegramUserId, String referralCode) {
            User user = saveUserWithoutCode(telegramUserId);
            user.assignReferralCode(referralCode);
            return userRepository.save(user);
        }

        private User saveUserWithoutCode(Long telegramUserId) {
            return userRepository.save(User.create(telegramUserId, null, "Ali", null, UserLanguage.FA, NOW));
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
