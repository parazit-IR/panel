package com.parazit.panel.api.plan.selection;

import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import com.parazit.panel.test.support.DatabaseCleaner;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.selection.PlanSelectionStatus;
import com.parazit.panel.domain.plan.repository.PlanRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.test.fixture.PlanTestData;
import com.parazit.panel.test.support.MutableClockTestConfiguration;
import com.parazit.panel.test.support.MutableTestClock;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "spring.security.user.name=test",
        "spring.security.user.password=test",
        "app.plan-selection.ttl=PT1S"
})
@AutoConfigureMockMvc
@Import(MutableClockTestConfiguration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class PlanSelectionControllerTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-11T00:00:00Z");

    private final MockMvc mockMvc;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final JdbcTemplate jdbcTemplate;
    private final MutableTestClock clock;

    PlanSelectionControllerTest(
            MockMvc mockMvc,
            UserRepository userRepository,
            PlanRepository planRepository,
            JdbcTemplate jdbcTemplate,
            Clock clock
    ) {
        this.mockMvc = mockMvc;
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.clock = (MutableTestClock) clock;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPlanSelectionTables(jdbcTemplate);
        clock.setInstant(NOW);
    }

    @Test
    void putCreatesSamePlanIdempotentlyAndReplacesSelection() throws Exception {
        activeUser(6001L);
        Plan planA = activePlan(PlanTestData.unlimitedPlan("API_SELECT_A", 1));
        Plan planB = activePlan(PlanTestData.trafficLimitedPlan("API_SELECT_B", 2));

        mockMvc.perform(put("/api/users/{telegramUserId}/plan-selection", 6001L)
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(selectBody(planA.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectionId").isNotEmpty())
                .andExpect(jsonPath("$.planId").value(planA.getId().toString()))
                .andExpect(jsonPath("$.planCode").value("API_SELECT_A"))
                .andExpect(jsonPath("$.planName").value("Monthly Unlimited"))
                .andExpect(jsonPath("$.planType").value("UNLIMITED"))
                .andExpect(jsonPath("$.priceAmount").value(900000))
                .andExpect(jsonPath("$.currency").value("IRT"))
                .andExpect(jsonPath("$.durationDays").value(30))
                .andExpect(jsonPath("$.trafficLimitBytes").doesNotExist())
                .andExpect(jsonPath("$.maxDevices").doesNotExist())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.selectedAt").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.newlySelected").value(true))
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.telegramUserId").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist());

        mockMvc.perform(put("/api/users/{telegramUserId}/plan-selection", 6001L)
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selectBody(planA.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planCode").value("API_SELECT_A"))
                .andExpect(jsonPath("$.newlySelected").value(false));

        mockMvc.perform(put("/api/users/{telegramUserId}/plan-selection", 6001L)
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selectBody(planB.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planCode").value("API_SELECT_B"))
                .andExpect(jsonPath("$.trafficLimitBytes").value(32212254720L))
                .andExpect(jsonPath("$.maxDevices").value(2))
                .andExpect(jsonPath("$.newlySelected").value(true));
    }

    @Test
    void getReturnsCurrentSelectionAndExpiredSelectionReturnsNotFound() throws Exception {
        activeUser(6002L);
        Plan plan = activePlan(PlanTestData.unlimitedPlan("API_GET", 1));
        select(6002L, plan.getId());

        mockMvc.perform(get("/api/users/{telegramUserId}/plan-selection", 6002L)
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planCode").value("API_GET"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.userId").doesNotExist());

        clock.setInstant(NOW.plusSeconds(1));

        mockMvc.perform(get("/api/users/{telegramUserId}/plan-selection", 6002L)
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/users/6002/plan-selection"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void deleteClearsActiveSelectionAndMissingSelectionReturnsNotFound() throws Exception {
        activeUser(6003L);
        Plan plan = activePlan(PlanTestData.unlimitedPlan("API_CLEAR", 1));
        select(6003L, plan.getId());

        mockMvc.perform(delete("/api/users/{telegramUserId}/plan-selection", 6003L)
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planCode").value("API_CLEAR"))
                .andExpect(jsonPath("$.status").value(PlanSelectionStatus.CLEARED.name()));

        mockMvc.perform(delete("/api/users/{telegramUserId}/plan-selection", 6003L)
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void putRejectsMissingHiddenIneligibleAndInvalidRequests() throws Exception {
        User blocked = activeUser(6004L);
        blocked.block();
        userRepository.save(blocked);
        activeUser(6005L);
        Plan draft = planRepository.save(PlanTestData.unlimitedPlan("API_DRAFT", 1));

        mockMvc.perform(put("/api/users/{telegramUserId}/plan-selection", 404L)
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selectBody(draft.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
        mockMvc.perform(put("/api/users/{telegramUserId}/plan-selection", 6005L)
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selectBody(draft.getId())))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("DRAFT"))));
        mockMvc.perform(put("/api/users/{telegramUserId}/plan-selection", 6004L)
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selectBody(activePlan(PlanTestData.unlimitedPlan("API_ALLOWED_PLAN", 2)).getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
        mockMvc.perform(put("/api/users/{telegramUserId}/plan-selection", 6005L)
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("planId: must not be null"));
        mockMvc.perform(put("/api/users/{telegramUserId}/plan-selection", 6005L)
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));
        mockMvc.perform(put("/api/users/{telegramUserId}/plan-selection", -1L)
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selectBody(draft.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    private User activeUser(Long telegramUserId) {
        return userRepository.save(User.create(telegramUserId, "user" + telegramUserId, "Ali", null, UserLanguage.FA, NOW));
    }

    private Plan activePlan(Plan plan) {
        Plan saved = planRepository.save(plan);
        saved.activate();
        return planRepository.save(saved);
    }

    private void select(Long telegramUserId, UUID planId) throws Exception {
        mockMvc.perform(put("/api/users/{telegramUserId}/plan-selection", telegramUserId)
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selectBody(planId)))
                .andExpect(status().isOk());
    }

    private String selectBody(UUID planId) {
        return "{\"planId\":\"" + planId + "\"}";
    }
}
