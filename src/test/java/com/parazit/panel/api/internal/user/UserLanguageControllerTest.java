package com.parazit.panel.api.internal.user;

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
class UserLanguageControllerTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("panel_user_language_controller_test")
            .withUsername("panel")
            .withPassword("panel");

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;

    UserLanguageControllerTest(MockMvc mockMvc, JdbcTemplate jdbcTemplate) {
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
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void getLanguageReturnsOk() throws Exception {
        registerUser("fa");

        mockMvc.perform(get("/internal/users/7101/language")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.telegramUserId").value(7101))
                .andExpect(jsonPath("$.language").value("FA"))
                .andExpect(jsonPath("$.updatedAt").value("2026-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.username").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andExpect(jsonPath("$.lastInteractionAt").doesNotExist());
    }

    @Test
    void getLanguageForMissingUserReturnsNotFoundWithTraceId() throws Exception {
        mockMvc.perform(get("/internal/users/9999/language")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User not found for telegramUserId 9999"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void putLanguageAcceptsSupportedLowercaseCode() throws Exception {
        registerUser("fa");

        mockMvc.perform(put("/internal/users/7101/language")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "languageCode": "en"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.telegramUserId").value(7101))
                .andExpect(jsonPath("$.language").value("EN"));
    }

    @Test
    void putLanguageAcceptsRegionalCode() throws Exception {
        registerUser("en");

        mockMvc.perform(put("/internal/users/7101/language")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "languageCode": "fa-IR"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("FA"));
    }

    @Test
    void blankLanguageReturnsBadRequest() throws Exception {
        registerUser("fa");

        mockMvc.perform(put("/internal/users/7101/language")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "languageCode": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void unsupportedLanguageReturnsBadRequest() throws Exception {
        registerUser("fa");

        mockMvc.perform(put("/internal/users/7101/language")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "languageCode": "de"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Unsupported languageCode: de"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void putLanguageForMissingUserReturnsNotFound() throws Exception {
        mockMvc.perform(put("/internal/users/9999/language")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "languageCode": "en"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(put("/internal/users/7101/language")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "languageCode": "en"
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Malformed JSON request"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    private void registerUser(String languageCode) throws Exception {
        mockMvc.perform(post("/internal/users/register")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "telegramUserId": 7101,
                                  "username": "telegram_user",
                                  "firstName": "Ali",
                                  "lastName": "Ahmadi",
                                  "languageCode": "%s"
                                }
                                """.formatted(languageCode)))
                .andExpect(status().isCreated());
    }
}
