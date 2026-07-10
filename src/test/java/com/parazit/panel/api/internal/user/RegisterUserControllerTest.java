package com.parazit.panel.api.internal.user;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
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
class RegisterUserControllerTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("panel_register_user_controller_test")
            .withUsername("panel")
            .withPassword("panel");

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;

    RegisterUserControllerTest(MockMvc mockMvc, JdbcTemplate jdbcTemplate) {
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
    void validNewRegistrationReturnsCreated() throws Exception {
        mockMvc.perform(post("/internal/users/register")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "telegramUserId": 123456789,
                                  "username": "example_user",
                                  "firstName": "Ali",
                                  "lastName": "Ahmadi",
                                  "languageCode": "fa"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.telegramUserId").value(123456789))
                .andExpect(jsonPath("$.username").value("example_user"))
                .andExpect(jsonPath("$.firstName").value("Ali"))
                .andExpect(jsonPath("$.lastName").value("Ahmadi"))
                .andExpect(jsonPath("$.language").value("FA"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.blocked").value(false))
                .andExpect(jsonPath("$.newlyCreated").value(true))
                .andExpect(jsonPath("$.registeredAt").value("2026-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.lastInteractionAt").value("2026-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.updatedAt").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void repeatedRegistrationReturnsOk() throws Exception {
        postValidRegistration(987654321L, "first_user");

        mockMvc.perform(post("/internal/users/register")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "telegramUserId": 987654321,
                                  "username": "second_user",
                                  "firstName": "Sara",
                                  "lastName": "Karimi",
                                  "languageCode": "en"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.telegramUserId").value(987654321))
                .andExpect(jsonPath("$.username").value("second_user"))
                .andExpect(jsonPath("$.firstName").value("Sara"))
                .andExpect(jsonPath("$.lastName").value("Karimi"))
                .andExpect(jsonPath("$.language").value("FA"))
                .andExpect(jsonPath("$.newlyCreated").value(false));
    }

    @Test
    void invalidTelegramUserIdReturnsBadRequestWithTraceId() throws Exception {
        mockMvc.perform(post("/internal/users/register")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "telegramUserId": 0,
                                  "firstName": "Ali"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void blankFirstNameReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/internal/users/register")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "telegramUserId": 123,
                                  "firstName": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void tooLongUsernameReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/internal/users/register")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "telegramUserId": 123,
                                  "username": "%s",
                                  "firstName": "Ali"
                                }
                                """.formatted("a".repeat(65))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/internal/users/register")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Malformed JSON request"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void responseDoesNotExposeEntityInternals() throws Exception {
        mockMvc.perform(post("/internal/users/register")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "telegramUserId": 555,
                                  "firstName": "Ali"
                                }
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.updatedAt").doesNotExist())
                .andExpect(jsonPath("$.hibernateLazyInitializer").doesNotExist());
    }

    private void postValidRegistration(Long telegramUserId, String username) throws Exception {
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
}
