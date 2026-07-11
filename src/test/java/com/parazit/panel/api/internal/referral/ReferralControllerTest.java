package com.parazit.panel.api.internal.referral;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parazit.panel.config.FixedClockTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "spring.security.user.name=test",
        "spring.security.user.password=test"
})
@AutoConfigureMockMvc
@Testcontainers
@Import(FixedClockTestConfiguration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ReferralControllerTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("panel_referral_controller_test")
            .withUsername("panel")
            .withPassword("panel");

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;

    ReferralControllerTest(MockMvc mockMvc, JdbcTemplate jdbcTemplate) {
        this.mockMvc = mockMvc;
        this.jdbcTemplate = jdbcTemplate;
    }

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM referrals");
        jdbcTemplate.update("DELETE FROM user_settings");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void overviewReturnsReferralCodeAndCount() throws Exception {
        registerUser(7001L, "first");

        mockMvc.perform(get("/internal/users/7001/referral")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.telegramUserId").value(7001))
                .andExpect(jsonPath("$.referralCode").isNotEmpty())
                .andExpect(jsonPath("$.referralCount").value(0))
                .andExpect(jsonPath("$.updatedAt").doesNotExist());
    }

    @Test
    void firstAndRepeatedAssignmentReturnExpectedStatuses() throws Exception {
        registerUser(7002L, "referrer");
        registerUser(7003L, "referred");
        String code = referralCodeFor(7002L);

        mockMvc.perform(assignRequest(7003L, code))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.referralId").isNotEmpty())
                .andExpect(jsonPath("$.referralCodeUsed").value(code))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.referredAt").value("2026-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.newlyAssigned").value(true));

        mockMvc.perform(assignRequest(7003L, code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newlyAssigned").value(false));

        mockMvc.perform(get("/internal/users/7002/referral")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referralCount").value(1));
    }

    @Test
    void assignmentErrorsUseStandardResponses() throws Exception {
        registerUser(7004L, "first");
        registerUser(7005L, "second");
        registerUser(7006L, "third");
        String firstCode = referralCodeFor(7004L);
        String secondCode = referralCodeFor(7005L);

        mockMvc.perform(assignRequest(7006L, firstCode)).andExpect(status().isCreated());

        mockMvc.perform(assignRequest(7006L, secondCode))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
        mockMvc.perform(assignRequest(7004L, firstCode))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
        mockMvc.perform(assignRequest(7005L, "ABCDEFGH"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
        mockMvc.perform(assignRequest(7005L, " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
        mockMvc.perform(post("/internal/users/7005/referral")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));
    }

    @Test
    void missingUserReturnsNotFound() throws Exception {
        mockMvc.perform(get("/internal/users/7999/referral")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder assignRequest(Long telegramUserId, String code) {
        return post("/internal/users/%d/referral".formatted(telegramUserId))
                .with(httpBasic("test", "test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "referralCode": "%s"
                        }
                        """.formatted(code));
    }

    private void registerUser(Long telegramUserId, String username) throws Exception {
        mockMvc.perform(post("/internal/users/register")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "telegramUserId": %d,
                                  "username": "%s",
                                  "firstName": "Ali",
                                  "languageCode": "fa"
                                }
                                """.formatted(telegramUserId, username)))
                .andExpect(status().isCreated());
    }

    private String referralCodeFor(Long telegramUserId) {
        return jdbcTemplate.queryForObject(
                "SELECT referral_code FROM users WHERE telegram_user_id = ?",
                String.class,
                telegramUserId
        );
    }
}
