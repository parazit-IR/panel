package com.parazit.panel.api.internal.plan.admin;

import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import com.parazit.panel.test.support.DatabaseCleaner;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "spring.security.user.name=test",
        "spring.security.user.password=test"
})
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class AdminPlanControllerTest extends PostgreSqlContainerSupport {

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    AdminPlanControllerTest(MockMvc mockMvc, JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPlanTables(jdbcTemplate);
    }

    @Test
    void postCreatesLimitedPlanWithLocationHeader() throws Exception {
        MvcResult result = postPlan(limitedJson("monthly_30gb", 1))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/internal/admin/plans/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.code").value("MONTHLY_30GB"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.type").value("TRAFFIC_LIMITED"))
                .andExpect(jsonPath("$.priceAmount").value(500000))
                .andExpect(jsonPath("$.currency").value("IRT"))
                .andExpect(jsonPath("$.durationDays").value(30))
                .andExpect(jsonPath("$.trafficLimitBytes").value(32212254720L))
                .andExpect(jsonPath("$.maxDevices").value(2))
                .andExpect(jsonPath("$.displayOrder").value(1))
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.hibernateLazyInitializer").doesNotExist())
                .andReturn();

        UUID id = idFrom(result);
        postPlan(limitedJson("duplicate", 2))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/internal/admin/plans/{planId}", id)
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MONTHLY_30GB"));
    }

    @Test
    void postCreatesUnlimitedPlanAndDuplicateReturnsConflictWithoutSqlDetails() throws Exception {
        postPlan(unlimitedJson("monthly_unlimited", 1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("MONTHLY_UNLIMITED"))
                .andExpect(jsonPath("$.type").value("UNLIMITED"))
                .andExpect(jsonPath("$.trafficLimitBytes").doesNotExist())
                .andExpect(jsonPath("$.maxDevices").doesNotExist());

        postPlan(unlimitedJson("monthly_unlimited", 2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Plan code already exists: MONTHLY_UNLIMITED"))
                .andExpect(jsonPath("$.path").value("/internal/admin/plans"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(content().string(not(containsString("uq_plans_code"))))
                .andExpect(content().string(not(containsString("DataIntegrityViolationException"))));
    }

    @Test
    void postValidationFailuresReturnBadRequest() throws Exception {
        postPlan(limitedJson("bad code", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
        postPlan(limitedJson("VALID_CODE", 1).replace("\"Limited\"", "\"   \""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
        postPlan(limitedJson("VALID_CODE", 1).replace("500000", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
        postPlan(limitedJson("VALID_CODE", 1).replace("\"durationDays\": 30", "\"durationDays\": 0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
        postPlan("""
                {
                  "code": "UNLIMITED_WITH_TRAFFIC",
                  "name": "Unlimited",
                  "type": "UNLIMITED",
                  "priceAmount": 900000,
                  "currency": "IRT",
                  "durationDays": 30,
                  "trafficLimitBytes": 32212254720,
                  "displayOrder": 1
                }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("trafficLimitBytes must be null for unlimited plans"));
        postPlan("{")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));
    }

    @Test
    void getListSupportsFiltersAndInvalidFiltersReturnBadRequest() throws Exception {
        UUID limitedId = idFrom(postPlan(limitedJson("A_LIMITED", 1)).andReturn());
        UUID unlimitedId = idFrom(postPlan(unlimitedJson("B_UNLIMITED", 2)).andReturn());
        mockMvc.perform(post("/internal/admin/plans/{planId}/activate", unlimitedId)
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get("/internal/admin/plans")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(limitedId.toString()))
                .andExpect(jsonPath("$[0].code").value("A_LIMITED"))
                .andExpect(jsonPath("$[1].id").value(unlimitedId.toString()))
                .andExpect(jsonPath("$[1].available").value(true));

        mockMvc.perform(get("/internal/admin/plans?status=ACTIVE")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("B_UNLIMITED"));
        mockMvc.perform(get("/internal/admin/plans?type=TRAFFIC_LIMITED")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("A_LIMITED"));
        mockMvc.perform(get("/internal/admin/plans?status=BAD")
                        .with(httpBasic("test", "test")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
        mockMvc.perform(get("/internal/admin/plans?type=BAD")
                        .with(httpBasic("test", "test")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void getByIdAndCodeHandleExistingMissingAndInvalidValues() throws Exception {
        UUID id = idFrom(postPlan(unlimitedJson("MONTHLY_UNLIMITED", 1)).andReturn());

        mockMvc.perform(get("/internal/admin/plans/{planId}", id)
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MONTHLY_UNLIMITED"));
        mockMvc.perform(get("/internal/admin/plans/by-code/{code}", "monthly_unlimited")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
        mockMvc.perform(get("/internal/admin/plans/{planId}", UUID.fromString("00000000-0000-0000-0000-000000000001"))
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
        mockMvc.perform(get("/internal/admin/plans/by-code/{code}", "MISSING_PLAN")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
        mockMvc.perform(get("/internal/admin/plans/not-a-uuid")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("planId has an invalid value"));
    }

    @Test
    void putUpdatesPlanRejectsMissingArchivedInvalidAndIgnoresCodeField() throws Exception {
        UUID id = idFrom(postPlan(limitedJson("UPDATABLE_PLAN", 1)).andReturn());

        mockMvc.perform(put("/internal/admin/plans/{planId}", id)
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "SHOULD_BE_IGNORED",
                                  "name": "Updated Unlimited",
                                  "description": "Updated",
                                  "type": "UNLIMITED",
                                  "priceAmount": 700000,
                                  "currency": "IRT",
                                  "durationDays": 60,
                                  "displayOrder": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("UPDATABLE_PLAN"))
                .andExpect(jsonPath("$.name").value("Updated Unlimited"))
                .andExpect(jsonPath("$.type").value("UNLIMITED"));

        mockMvc.perform(put("/internal/admin/plans/{planId}", UUID.fromString("00000000-0000-0000-0000-000000000001"))
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(updateUnlimitedJson()))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/internal/admin/plans/{planId}", id)
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(updateUnlimitedJson().replace("\"durationDays\": 60", "\"durationDays\": 0")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/internal/admin/plans/{planId}/archive", id)
                        .with(httpBasic("test", "test"))
                        .with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(put("/internal/admin/plans/{planId}", id)
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(updateUnlimitedJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Plan cannot be modified: " + id));
    }

    @Test
    void statusEndpointsPersistTransitionsAndRejectInvalidOrMissing() throws Exception {
        UUID id = idFrom(postPlan(unlimitedJson("STATUS_PLAN", 1)).andReturn());

        mockMvc.perform(post("/internal/admin/plans/{planId}/activate", id)
                        .with(httpBasic("test", "test"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.available").value(true));
        mockMvc.perform(post("/internal/admin/plans/{planId}/deactivate", id)
                        .with(httpBasic("test", "test"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
        mockMvc.perform(post("/internal/admin/plans/{planId}/activate", id)
                        .with(httpBasic("test", "test"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        mockMvc.perform(post("/internal/admin/plans/{planId}/archive", id)
                        .with(httpBasic("test", "test"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
        mockMvc.perform(post("/internal/admin/plans/{planId}/activate", id)
                        .with(httpBasic("test", "test"))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
        mockMvc.perform(post("/internal/admin/plans/{planId}/archive", id)
                        .with(httpBasic("test", "test"))
                        .with(csrf()))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/internal/admin/plans/{planId}/activate", UUID.fromString("00000000-0000-0000-0000-000000000001"))
                        .with(httpBasic("test", "test"))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    private org.springframework.test.web.servlet.ResultActions postPlan(String json) throws Exception {
        return mockMvc.perform(post("/internal/admin/plans")
                .with(httpBasic("test", "test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private UUID idFrom(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(node.get("id").asText());
    }

    private String limitedJson(String code, int displayOrder) {
        return """
                {
                  "code": "%s",
                  "name": "Limited",
                  "description": "Limited plan",
                  "type": "TRAFFIC_LIMITED",
                  "priceAmount": 500000,
                  "currency": "IRT",
                  "durationDays": 30,
                  "trafficLimitBytes": 32212254720,
                  "maxDevices": 2,
                  "displayOrder": %d
                }
                """.formatted(code, displayOrder);
    }

    private String unlimitedJson(String code, int displayOrder) {
        return """
                {
                  "code": "%s",
                  "name": "Unlimited",
                  "type": "UNLIMITED",
                  "priceAmount": 900000,
                  "currency": "IRT",
                  "durationDays": 30,
                  "displayOrder": %d
                }
                """.formatted(code, displayOrder);
    }

    private String updateUnlimitedJson() {
        return """
                {
                  "name": "Updated Unlimited",
                  "type": "UNLIMITED",
                  "priceAmount": 700000,
                  "currency": "IRT",
                  "durationDays": 60,
                  "displayOrder": 5
                }
                """;
    }
}
