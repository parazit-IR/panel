package com.parazit.panel.infrastructure.persistence.xui.provisioning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.repository.PlanRepository;
import com.parazit.panel.domain.plan.selection.PlanSelection;
import com.parazit.panel.domain.plan.selection.repository.PlanSelectionRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import com.parazit.panel.domain.xui.provisioning.XuiProvisionStatus;
import com.parazit.panel.domain.xui.provisioning.repository.XuiClientProvisionRepository;
import com.parazit.panel.test.fixture.PlanTestData;
import com.parazit.panel.test.support.DatabaseCleaner;
import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "spring.security.user.name=test",
        "spring.security.user.password=test"
})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class XuiClientProvisionRepositoryIntegrationTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-11T00:00:00Z");

    private final XuiClientProvisionRepository provisionRepository;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final PlanSelectionRepository planSelectionRepository;
    private final JdbcTemplate jdbcTemplate;
    private final Flyway flyway;

    XuiClientProvisionRepositoryIntegrationTest(
            XuiClientProvisionRepository provisionRepository,
            UserRepository userRepository,
            PlanRepository planRepository,
            PlanSelectionRepository planSelectionRepository,
            JdbcTemplate jdbcTemplate,
            Flyway flyway
    ) {
        this.provisionRepository = provisionRepository;
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.planSelectionRepository = planSelectionRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.flyway = flyway;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPlanSelectionTables(jdbcTemplate);
    }

    @Test
    void savesFindsTransitionsAndPersistsStatus() {
        PlanSelection selection = selection(9001L, "XUI_REPO_ONE");
        XuiClientProvision saved = provisionRepository.save(provision(selection, "11111111-1111-1111-1111-111111111111", "vpn_repo_one"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(provisionRepository.findByPlanSelectionId(selection.getId())).contains(saved);
        assertThat(provisionRepository.findByRemoteClientId(saved.getRemoteClientId())).contains(saved);
        assertThat(provisionRepository.findByRemoteEmail(saved.getRemoteEmail())).contains(saved);
        assertThat(provisionRepository.findAllByUserId(saved.getUserId())).hasSize(1);
        assertThat(provisionRepository.transitionStatus(saved.getId(), XuiProvisionStatus.PENDING, XuiProvisionStatus.PROVISIONING))
                .isTrue();

        XuiClientProvision reloaded = provisionRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(XuiProvisionStatus.PROVISIONING);
        reloaded.markActive(NOW);
        provisionRepository.save(reloaded);

        assertThat(provisionRepository.findById(saved.getId()).orElseThrow().getProvisionedAt()).isEqualTo(NOW);

        XuiClientProvision active = provisionRepository.findById(saved.getId()).orElseThrow();
        active.markDisabling();
        active.markDisabled(NOW.plusSeconds(1));
        provisionRepository.save(active);
        assertThat(provisionRepository.findById(saved.getId()).orElseThrow().getDisabledAt())
                .isEqualTo(NOW.plusSeconds(1));

        XuiClientProvision disabled = provisionRepository.findById(saved.getId()).orElseThrow();
        disabled.markDeleting();
        disabled.markDeleted(NOW.plusSeconds(2));
        provisionRepository.save(disabled);
        XuiClientProvision deleted = provisionRepository.findById(saved.getId()).orElseThrow();
        assertThat(deleted.getStatus()).isEqualTo(XuiProvisionStatus.DELETED);
        assertThat(deleted.getDeletedAt()).isEqualTo(NOW.plusSeconds(2));
        assertThat(deleted.getRemoteClientId()).isEqualTo(saved.getRemoteClientId());
    }

    @Test
    void uniquePlanSelectionRemoteClientAndEmailAreEnforced() {
        PlanSelection first = selection(9002L, "XUI_REPO_TWO");
        PlanSelection second = selection(9003L, "XUI_REPO_THREE");
        provisionRepository.save(provision(first, "22222222-2222-2222-2222-222222222222", "vpn_repo_two"));

        assertThatThrownBy(() -> provisionRepository.save(provision(first, "33333333-3333-3333-3333-333333333333", "vpn_repo_three")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> provisionRepository.save(provision(second, "22222222-2222-2222-2222-222222222222", "vpn_repo_four")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> provisionRepository.save(provision(second, "44444444-4444-4444-4444-444444444444", "vpn_repo_two")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void flywayIncludesProvisioningMigration() {
        assertThat(Arrays.stream(flyway.info().applied()))
                .anySatisfy(info -> assertThat(info.getVersion().getVersion()).isEqualTo("7"));
        assertThat(Arrays.stream(flyway.info().applied()))
                .anySatisfy(info -> assertThat(info.getVersion().getVersion()).isEqualTo("8"));
        assertThat(Arrays.stream(flyway.info().applied()))
                .anySatisfy(info -> assertThat(info.getVersion().getVersion()).isEqualTo("9"));
    }

    private PlanSelection selection(Long telegramUserId, String planCode) {
        User user = userRepository.save(User.create(telegramUserId, "user" + telegramUserId, "Ali", null, UserLanguage.FA, NOW));
        Plan plan = planRepository.save(PlanTestData.trafficLimitedPlan(planCode, 1));
        plan.activate();
        Plan activePlan = planRepository.save(plan);
        return planSelectionRepository.save(PlanSelection.create(user.getId(), activePlan, NOW, Duration.ofHours(1)));
    }

    private static XuiClientProvision provision(PlanSelection selection, String clientId, String email) {
        return XuiClientProvision.createPending(
                selection.getUserId(),
                selection.getPlanId(),
                selection.getId(),
                7,
                clientId,
                email,
                "sub123",
                1024,
                NOW.plusSeconds(86_400),
                1,
                NOW
        );
    }
}
