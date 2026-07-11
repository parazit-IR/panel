package com.parazit.panel.api.internal.user.settings;

import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import com.parazit.panel.test.support.DatabaseCleaner;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "spring.security.user.name=test",
        "spring.security.user.password=test"
})
@AutoConfigureMockMvc
@Import(FixedClockTestConfiguration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class UserSettingsControllerTest extends PostgreSqlContainerSupport {


    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;

    UserSettingsControllerTest(MockMvc mockMvc, JdbcTemplate jdbcTemplate) {
        this.mockMvc = mockMvc;
        this.jdbcTemplate = jdbcTemplate;
    }


    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanUserModuleTables(jdbcTemplate);
    }

    @Test
    void getSettingsReturnsDefaults() throws Exception {
        registerUser();

        mockMvc.perform(get("/internal/users/7201/settings")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.settingsId").isNotEmpty())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.telegramUserId").value(7201))
                .andExpect(jsonPath("$.notificationsEnabled").value(true))
                .andExpect(jsonPath("$.renewalRemindersEnabled").value(true))
                .andExpect(jsonPath("$.usageAlertsEnabled").value(true))
                .andExpect(jsonPath("$.usageAlertThresholdPercent").value(80))
                .andExpect(jsonPath("$.createdAt").value("2026-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.updatedAt").value("2026-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.username").doesNotExist())
                .andExpect(jsonPath("$.language").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.blocked").doesNotExist());
    }

    @Test
    void getSettingsForMissingUserReturnsNotFoundWithTraceId() throws Exception {
        mockMvc.perform(get("/internal/users/9999/settings")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User not found for telegramUserId 9999"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void putSettingsPersistsAllFields() throws Exception {
        registerUser();

        mockMvc.perform(put("/internal/users/7201/settings")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "notificationsEnabled": false,
                                  "renewalRemindersEnabled": false,
                                  "usageAlertsEnabled": false,
                                  "usageAlertThresholdPercent": 45
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.notificationsEnabled").value(false))
                .andExpect(jsonPath("$.renewalRemindersEnabled").value(false))
                .andExpect(jsonPath("$.usageAlertsEnabled").value(false))
                .andExpect(jsonPath("$.usageAlertThresholdPercent").value(45));

        mockMvc.perform(get("/internal/users/7201/settings")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationsEnabled").value(false))
                .andExpect(jsonPath("$.renewalRemindersEnabled").value(false))
                .andExpect(jsonPath("$.usageAlertsEnabled").value(false))
                .andExpect(jsonPath("$.usageAlertThresholdPercent").value(45));
    }

    @Test
    void thresholdBoundariesAreAccepted() throws Exception {
        registerUser();

        updateThreshold(1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usageAlertThresholdPercent").value(1));
        updateThreshold(100)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usageAlertThresholdPercent").value(100));
    }

    @Test
    void invalidThresholdsReturnBadRequest() throws Exception {
        registerUser();

        updateThreshold(0)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
        updateThreshold(101)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void nullFieldReturnsBadRequest() throws Exception {
        registerUser();

        mockMvc.perform(put("/internal/users/7201/settings")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "notificationsEnabled": null,
                                  "renewalRemindersEnabled": true,
                                  "usageAlertsEnabled": true,
                                  "usageAlertThresholdPercent": 80
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(put("/internal/users/7201/settings")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "notificationsEnabled": true
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Malformed JSON request"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    private org.springframework.test.web.servlet.ResultActions updateThreshold(int threshold) throws Exception {
        return mockMvc.perform(put("/internal/users/7201/settings")
                .with(httpBasic("test", "test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "notificationsEnabled": true,
                          "renewalRemindersEnabled": true,
                          "usageAlertsEnabled": true,
                          "usageAlertThresholdPercent": %d
                        }
                        """.formatted(threshold)));
    }

    private void registerUser() throws Exception {
        mockMvc.perform(post("/internal/users/register")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "telegramUserId": 7201,
                                  "username": "telegram_user",
                                  "firstName": "Ali",
                                  "lastName": "Ahmadi",
                                  "languageCode": "fa"
                                }
                                """))
                .andExpect(status().isCreated());
    }
}
