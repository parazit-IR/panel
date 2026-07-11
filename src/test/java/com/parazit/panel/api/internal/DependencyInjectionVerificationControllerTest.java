package com.parazit.panel.api.internal;

import com.parazit.panel.test.support.PostgreSqlContainerSupport;

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
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "spring.security.user.name=test",
        "spring.security.user.password=test"
})
@AutoConfigureMockMvc
@Import(FixedClockTestConfiguration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class DependencyInjectionVerificationControllerTest extends PostgreSqlContainerSupport {


    private final MockMvc mockMvc;

    DependencyInjectionVerificationControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
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
