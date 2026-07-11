package com.parazit.panel.infrastructure.persistence.referral;

import com.parazit.panel.test.support.PostgreSqlContainerSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.config.persistence.JpaAuditingConfiguration;
import com.parazit.panel.domain.referral.Referral;
import com.parazit.panel.domain.referral.ReferralStatus;
import com.parazit.panel.domain.referral.repository.ReferralRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.infrastructure.persistence.user.SpringDataUserRepository;
import com.parazit.panel.infrastructure.persistence.user.UserRepositoryAdapter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.time.Instant;
import java.util.Arrays;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestConstructor;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan(basePackageClasses = {User.class, Referral.class})
@EnableJpaRepositories(basePackageClasses = {SpringDataUserRepository.class, SpringDataReferralRepository.class})
@Import({JpaAuditingConfiguration.class, UserRepositoryAdapter.class, ReferralRepositoryAdapter.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ReferralRepositoryIntegrationTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-10T12:00:00Z");


    private final UserRepository userRepository;
    private final ReferralRepository referralRepository;
    private final EntityManager entityManager;
    private final Flyway flyway;

    ReferralRepositoryIntegrationTest(
            UserRepository userRepository,
            ReferralRepository referralRepository,
            EntityManager entityManager,
            Flyway flyway
    ) {
        this.userRepository = userRepository;
        this.referralRepository = referralRepository;
        this.entityManager = entityManager;
        this.flyway = flyway;
    }


    @Test
    void savesReferralAndFindsByRepositoryMethods() {
        User referrer = saveUser(9001L, "ABCD2345EF");
        User referred = saveUser(9002L, "EFGH6789JK");

        Referral saved = referralRepository.save(Referral.create(referrer.getId(), referred.getId(), "abcd2345ef", NOW));
        entityManager.flush();
        entityManager.clear();

        Referral found = referralRepository.findByReferredUserId(referred.getId()).orElseThrow();
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getReferrerUserId()).isEqualTo(referrer.getId());
        assertThat(found.getReferredUserId()).isEqualTo(referred.getId());
        assertThat(found.getReferralCodeUsed()).isEqualTo("ABCD2345EF");
        assertThat(found.getStatus()).isEqualTo(ReferralStatus.PENDING);
        assertThat(found.getReferredAt()).isEqualTo(NOW);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(referralRepository.findAllByReferrerUserId(referrer.getId())).hasSize(1);
        assertThat(referralRepository.existsByReferredUserId(referred.getId())).isTrue();
        assertThat(referralRepository.countByReferrerUserId(referrer.getId())).isEqualTo(1);
    }

    @Test
    void enforcesUniqueReferredUserAndForeignKeys() {
        User referrer = saveUser(9003L, "JKLM2345NP");
        User referred = saveUser(9004L, "QRST6789UV");
        referralRepository.save(Referral.create(referrer.getId(), referred.getId(), "JKLM2345NP", NOW));
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> referralRepository.save(Referral.create(referrer.getId(), referred.getId(), "JKLM2345NP", NOW)))
                .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    void userReferralCodePersistsAndIsUniqueButNullable() {
        User first = saveUser(9005L, "WXYZ2345AA");
        User withoutCode = User.create(9006L, null, "Ali", null, UserLanguage.FA, NOW);
        userRepository.save(withoutCode);
        entityManager.flush();

        assertThat(userRepository.findByReferralCode("wxyz2345aa")).contains(first);
        assertThat(userRepository.existsByReferralCode("WXYZ2345AA")).isTrue();
        assertThat(withoutCode.getReferralCode()).isNull();

        User duplicate = User.create(9007L, null, "Sara", null, UserLanguage.FA, NOW);
        duplicate.assignReferralCode("WXYZ2345AA");
        assertThatThrownBy(() -> userRepository.save(duplicate))
                .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    void flywayMigrationRunsAndHibernateValidationSucceeds() {
        assertThat(flyway.info().current()).isNotNull();
        assertThat(Arrays.stream(flyway.info().applied()))
                .anySatisfy(info -> assertThat(info.getVersion().getVersion()).isEqualTo("4"));
    }

    private User saveUser(Long telegramUserId, String referralCode) {
        User user = User.create(telegramUserId, null, "Ali", null, UserLanguage.FA, NOW);
        user.assignReferralCode(referralCode);
        return userRepository.save(user);
    }
}
