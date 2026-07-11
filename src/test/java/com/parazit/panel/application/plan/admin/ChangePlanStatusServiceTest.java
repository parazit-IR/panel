package com.parazit.panel.application.plan.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.plan.PlanResultMapper;
import com.parazit.panel.application.plan.admin.command.ChangePlanStatusCommand;
import com.parazit.panel.application.plan.result.PlanResult;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanStatus;
import com.parazit.panel.test.fixture.FakePlanRepository;
import com.parazit.panel.test.fixture.PlanTestData;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChangePlanStatusServiceTest {

    @Test
    void supportsAllowedTransitionsAndReturnsNewStatus() {
        Fixture fixture = new Fixture();

        Plan draftToActive = fixture.save("DRAFT_ACTIVE");
        assertThat(fixture.service.activate(command(draftToActive)).status()).isEqualTo(PlanStatus.ACTIVE);
        assertThat(fixture.service.deactivate(command(draftToActive)).status()).isEqualTo(PlanStatus.INACTIVE);
        assertThat(fixture.service.activate(command(draftToActive)).status()).isEqualTo(PlanStatus.ACTIVE);

        Plan draftToArchived = fixture.save("DRAFT_ARCHIVED");
        assertThat(fixture.service.archive(command(draftToArchived)).status()).isEqualTo(PlanStatus.ARCHIVED);

        Plan activeToArchived = fixture.save("ACTIVE_ARCHIVED");
        fixture.service.activate(command(activeToArchived));
        assertThat(fixture.service.archive(command(activeToArchived)).status()).isEqualTo(PlanStatus.ARCHIVED);

        Plan inactiveToArchived = fixture.save("INACTIVE_ARCHIVED");
        fixture.service.activate(command(inactiveToArchived));
        fixture.service.deactivate(command(inactiveToArchived));
        assertThat(fixture.service.archive(command(inactiveToArchived)).status()).isEqualTo(PlanStatus.ARCHIVED);
    }

    @Test
    void rejectsInvalidArchivedMissingAndNullTransitions() {
        Fixture fixture = new Fixture();
        Plan draft = fixture.save("DRAFT_PLAN");
        assertThatThrownBy(() -> fixture.service.deactivate(command(draft)))
                .isInstanceOf(InvalidPlanStateTransitionException.class)
                .hasMessage("Cannot deactivate plan " + draft.getId() + " with status DRAFT");

        Plan archived = fixture.save("ARCHIVED_PLAN");
        fixture.service.archive(command(archived));
        assertThatThrownBy(() -> fixture.service.activate(command(archived)))
                .isInstanceOf(InvalidPlanStateTransitionException.class)
                .hasMessage("Cannot activate plan " + archived.getId() + " with status ARCHIVED");

        UUID missingId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        assertThatThrownBy(() -> fixture.service.activate(new ChangePlanStatusCommand(missingId)))
                .isInstanceOf(PlanNotFoundException.class)
                .hasMessage("Plan not found for id " + missingId);

        assertThatNullPointerException()
                .isThrownBy(() -> fixture.service.activate(null))
                .withMessage("command must not be null");
        assertThatNullPointerException()
                .isThrownBy(() -> fixture.service.activate(new ChangePlanStatusCommand(null)))
                .withMessage("planId must not be null");
    }

    @Test
    void savesChangedStatus() {
        Fixture fixture = new Fixture();
        Plan plan = fixture.save("SAVE_STATUS");
        fixture.repository.saveCalls = 0;

        PlanResult result = fixture.service.activate(command(plan));

        assertThat(result.status()).isEqualTo(PlanStatus.ACTIVE);
        assertThat(result.available()).isTrue();
        assertThat(fixture.repository.findByIdCalls).isEqualTo(1);
        assertThat(fixture.repository.saveCalls).isEqualTo(1);
    }

    private ChangePlanStatusCommand command(Plan plan) {
        return new ChangePlanStatusCommand(plan.getId());
    }

    private static final class Fixture {

        private final FakePlanRepository repository = new FakePlanRepository();
        private final ChangePlanStatusService service = new ChangePlanStatusService(repository, new PlanResultMapper());

        private Plan save(String code) {
            return repository.save(PlanTestData.trafficLimitedPlan(code, 1));
        }
    }
}
