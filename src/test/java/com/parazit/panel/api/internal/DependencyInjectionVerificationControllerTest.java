package com.parazit.panel.api.internal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parazit.panel.config.FixedClockTestConfiguration;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
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
class DependencyInjectionVerificationControllerTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("panel_di_web_test")
            .withUsername("panel")
            .withPassword("panel");

    private final MockMvc mockMvc;

    DependencyInjectionVerificationControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Test
    void returnsCurrentTimeFromFixedClock() throws Exception {
        MvcResult result = mockMvc.perform(get("/internal/di/time")
                        .header("Authorization", basicAuthHeader())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.currentTime").value("2026-01-01T00:00:00Z"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertThatCode(() -> java.time.Instant.parse(
                response.replaceFirst(".*\\\"currentTime\\\":\\\"([^\\\"]+)\\\".*", "$1")
        )).doesNotThrowAnyException();
    }

    private String basicAuthHeader() {
        String token = Base64.getEncoder()
                .encodeToString("test:test".getBytes(StandardCharsets.UTF_8));
        return "Basic " + token;
    }
}
