package com.parazit.panel.infrastructure.persistence.xui.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.repository.PlanRepository;
import com.parazit.panel.domain.plan.selection.PlanSelection;
import com.parazit.panel.domain.plan.selection.repository.PlanSelectionRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.xui.operation.XuiClientOperation;
import com.parazit.panel.domain.xui.operation.XuiClientOperationStatus;
import com.parazit.panel.domain.xui.operation.XuiClientOperationType;
import com.parazit.panel.domain.xui.operation.repository.XuiClientOperationRepository;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import com.parazit.panel.domain.xui.provisioning.repository.XuiClientProvisionRepository;
import com.parazit.panel.test.fixture.PlanTestData;
import com.parazit.panel.test.support.DatabaseCleaner;
import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
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
class XuiClientOperationRepositoryIntegrationTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-12T00:00:00Z");
    private static final UUID OPERATION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private final XuiClientOperationRepository operationRepository;
    private final XuiClientProvisionRepository provisionRepository;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final PlanSelectionRepository planSelectionRepository;
    private final JdbcTemplate jdbcTemplate;

    XuiClientOperationRepositoryIntegrationTest(
            XuiClientOperationRepository operationRepository,
            XuiClientProvisionRepository provisionRepository,
            UserRepository userRepository,
            PlanRepository planRepository,
            PlanSelectionRepository planSelectionRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.operationRepository = operationRepository;
        this.provisionRepository = provisionRepository;
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.planSelectionRepository = planSelectionRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPlanSelectionTables(jdbcTemplate);
    }

    @Test
    void savesFindsAndEnforcesUniqueOperationId() {
        XuiClientProvision provision = provisionRepository.save(provision(selection()));
        XuiClientOperation saved = operationRepository.save(XuiClientOperation.create(
                OPERATION_ID,
                provision.getId(),
                XuiClientOperationType.RENEW_EXPIRY,
                "abc123",
                NOW
        ));

        assertThat(saved.getId()).isNotNull();
        assertThat(operationRepository.findByOperationId(OPERATION_ID)).contains(saved);
        assertThat(operationRepository.findAllByProvisionIdOrderByRequestedAtDesc(provision.getId())).contains(saved);

        assertThatThrownBy(() -> operationRepository.save(XuiClientOperation.create(
                OPERATION_ID,
                provision.getId(),
                XuiClientOperationType.ENABLE,
                "def456",
                NOW
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void onlyOneInProgressOperationPerProvisionIsAllowed() {
        XuiClientProvision provision = provisionRepository.save(provision(selection()));
        XuiClientOperation first = XuiClientOperation.create(OPERATION_ID, provision.getId(), XuiClientOperationType.RENEW_EXPIRY, "abc123", NOW);
        first.markInProgress();
        operationRepository.save(first);

        XuiClientOperation second = XuiClientOperation.create(
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                provision.getId(),
                XuiClientOperationType.ADD_TRAFFIC,
                "def456",
                NOW.plusSeconds(1)
        );
        second.markInProgress();

        assertThat(operationRepository.existsByProvisionIdAndStatus(provision.getId(), XuiClientOperationStatus.IN_PROGRESS))
                .isTrue();
        assertThatThrownBy(() -> operationRepository.save(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private PlanSelection selection() {
        User user = userRepository.save(User.create(9009L, "xuiop", "Ali", null, UserLanguage.FA, NOW));
        Plan plan = planRepository.save(PlanTestData.trafficLimitedPlan("XUI_OPERATION", 1));
        plan.activate();
        return planSelectionRepository.save(PlanSelection.create(user.getId(), planRepository.save(plan), NOW, Duration.ofHours(1)));
    }

    private static XuiClientProvision provision(PlanSelection selection) {
        return XuiClientProvision.createPending(
                selection.getUserId(),
                selection.getPlanId(),
                selection.getId(),
                7,
                "11111111-1111-1111-1111-111111111111",
                "vpn_operation",
                "sub-operation",
                1024,
                NOW.plus(Duration.ofDays(30)),
                1,
                NOW
        );
    }
}
