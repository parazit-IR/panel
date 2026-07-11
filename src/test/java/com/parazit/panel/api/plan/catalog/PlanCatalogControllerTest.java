package com.parazit.panel.api.plan.catalog;

import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import com.parazit.panel.test.support.DatabaseCleaner;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.repository.PlanRepository;
import com.parazit.panel.test.fixture.PlanTestData;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "spring.security.user.name=test",
        "spring.security.user.password=test"
})
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class PlanCatalogControllerTest extends PostgreSqlContainerSupport {

    private final MockMvc mockMvc;
    private final PlanRepository planRepository;
    private final JdbcTemplate jdbcTemplate;

    PlanCatalogControllerTest(MockMvc mockMvc, PlanRepository planRepository, JdbcTemplate jdbcTemplate) {
        this.mockMvc = mockMvc;
        this.planRepository = planRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPlanTables(jdbcTemplate);
    }

    @Test
    void listReturnsOnlyActivePlansWithMinimalContract() throws Exception {
        Plan draft = planRepository.save(PlanTestData.trafficLimitedPlan("DRAFT_HIDDEN", 0));
        Plan activeLimited = activate(planRepository.save(PlanTestData.trafficLimitedPlan("A_LIMITED_ACTIVE", 1)));
        activate(planRepository.save(PlanTestData.unlimitedPlan("B_UNLIMITED_ACTIVE", 2)));
        Plan inactive = activate(planRepository.save(PlanTestData.unlimitedPlan("INACTIVE_HIDDEN", 3)));
        inactive.deactivate();
        planRepository.save(inactive);
        Plan archived = planRepository.save(PlanTestData.unlimitedPlan("ARCHIVED_HIDDEN", 4));
        archived.archive();
        planRepository.save(archived);

        mockMvc.perform(get("/api/plans")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(activeLimited.getId().toString()))
                .andExpect(jsonPath("$[0].code").value("A_LIMITED_ACTIVE"))
                .andExpect(jsonPath("$[0].name").value("Monthly 30 GiB"))
                .andExpect(jsonPath("$[0].description").value("30 GiB plan"))
                .andExpect(jsonPath("$[0].type").value("TRAFFIC_LIMITED"))
                .andExpect(jsonPath("$[0].priceAmount").value(500000))
                .andExpect(jsonPath("$[0].currency").value("IRT"))
                .andExpect(jsonPath("$[0].durationDays").value(30))
                .andExpect(jsonPath("$[0].trafficLimitBytes").value(32212254720L))
                .andExpect(jsonPath("$[0].maxDevices").value(2))
                .andExpect(jsonPath("$[0].status").doesNotExist())
                .andExpect(jsonPath("$[0].available").doesNotExist())
                .andExpect(jsonPath("$[0].displayOrder").doesNotExist())
                .andExpect(jsonPath("$[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$[0].updatedAt").doesNotExist())
                .andExpect(jsonPath("$[0].hibernateLazyInitializer").doesNotExist())
                .andExpect(jsonPath("$[1].code").value("B_UNLIMITED_ACTIVE"))
                .andExpect(content().string(not(containsString(draft.getId().toString()))));
    }

    @Test
    void listSupportsTypeFilterInvalidTypeAndEmptyResult() throws Exception {
        activate(planRepository.save(PlanTestData.trafficLimitedPlan("A_LIMITED_ACTIVE", 1)));
        activate(planRepository.save(PlanTestData.unlimitedPlan("B_UNLIMITED_ACTIVE", 2)));

        mockMvc.perform(get("/api/plans?type=UNLIMITED")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("B_UNLIMITED_ACTIVE"))
                .andExpect(jsonPath("$[1]").doesNotExist());
        mockMvc.perform(get("/api/plans?type=BAD")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("type has an invalid value"))
                .andExpect(jsonPath("$.path").value("/api/plans"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());

        DatabaseCleaner.cleanPlanTables(jdbcTemplate);
        mockMvc.perform(get("/api/plans")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getByIdReturnsActivePlanAndHidesMissingOrNonActivePlans() throws Exception {
        Plan active = activate(planRepository.save(PlanTestData.unlimitedPlan("ACTIVE_LOOKUP", 1)));
        Plan draft = planRepository.save(PlanTestData.unlimitedPlan("DRAFT_LOOKUP", 2));
        Plan inactive = activate(planRepository.save(PlanTestData.unlimitedPlan("INACTIVE_LOOKUP", 3)));
        inactive.deactivate();
        planRepository.save(inactive);
        Plan archived = planRepository.save(PlanTestData.unlimitedPlan("ARCHIVED_LOOKUP", 4));
        archived.archive();
        planRepository.save(archived);

        mockMvc.perform(get("/api/plans/{planId}", active.getId())
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ACTIVE_LOOKUP"))
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist());

        assertHiddenById(draft.getId());
        assertHiddenById(inactive.getId());
        assertHiddenById(archived.getId());
        mockMvc.perform(get("/api/plans/{planId}", UUID.fromString("00000000-0000-0000-0000-000000000001"))
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
        mockMvc.perform(get("/api/plans/not-a-uuid")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("planId has an invalid value"));
    }

    @Test
    void getByCodeAcceptsLowercaseAndHidesMissingOrInactivePlans() throws Exception {
        activate(planRepository.save(PlanTestData.trafficLimitedPlan("ACTIVE_CODE", 1)));
        Plan inactive = activate(planRepository.save(PlanTestData.unlimitedPlan("HIDDEN_CODE", 2)));
        inactive.deactivate();
        planRepository.save(inactive);

        mockMvc.perform(get("/api/plans/by-code/{code}", "active_code")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ACTIVE_CODE"))
                .andExpect(jsonPath("$.status").doesNotExist());
        mockMvc.perform(get("/api/plans/by-code/{code}", "missing_code")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
        mockMvc.perform(get("/api/plans/by-code/{code}", "hidden_code")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("INACTIVE"))))
                .andExpect(content().string(not(containsString("ARCHIVED"))))
                .andExpect(content().string(not(containsString("DRAFT"))));
    }

    private Plan activate(Plan plan) {
        plan.activate();
        return planRepository.save(plan);
    }

    private void assertHiddenById(UUID planId) throws Exception {
        mockMvc.perform(get("/api/plans/{planId}", planId)
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Available plan not found for id " + planId))
                .andExpect(jsonPath("$.path").value("/api/plans/" + planId))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(content().string(not(containsString("INACTIVE"))))
                .andExpect(content().string(not(containsString("ARCHIVED"))))
                .andExpect(content().string(not(containsString("DRAFT"))));
    }
}
