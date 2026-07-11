package com.parazit.panel.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parazit.panel.config.FixedClockTestConfiguration;
import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
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
class TraceIdFilterTest extends PostgreSqlContainerSupport {

    private final MockMvc mockMvc;

    TraceIdFilterTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void generatesTraceIdHeaderAndClearsMdcAfterSuccessfulRequest() throws Exception {
        MvcResult first = mockMvc.perform(get("/internal/di/time")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(TraceIdFilter.TRACE_ID_HEADER))
                .andReturn();

        MvcResult second = mockMvc.perform(get("/internal/di/time")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(TraceIdFilter.TRACE_ID_HEADER))
                .andReturn();

        String firstTraceId = first.getResponse().getHeader(TraceIdFilter.TRACE_ID_HEADER);
        String secondTraceId = second.getResponse().getHeader(TraceIdFilter.TRACE_ID_HEADER);
        assertThat(firstTraceId).isNotBlank();
        assertThat(secondTraceId).isNotBlank().isNotEqualTo(firstTraceId);
        assertThat(MDC.get(TraceIdFilter.TRACE_ID_KEY)).isNull();
    }

    @Test
    void errorBodyContainsGeneratedTraceIdFromResponseHeader() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/_test/exceptions/illegal-argument")
                        .with(httpBasic("test", "test"))
                        .header(TraceIdFilter.TRACE_ID_HEADER, "incoming-trace-id")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists(TraceIdFilter.TRACE_ID_HEADER))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andReturn();

        String generatedTraceId = result.getResponse().getHeader(TraceIdFilter.TRACE_ID_HEADER);
        assertThat(generatedTraceId).isNotEqualTo("incoming-trace-id");
        assertThat(result.getResponse().getContentAsString()).contains("\"traceId\":\"" + generatedTraceId + "\"");
        assertThat(MDC.get(TraceIdFilter.TRACE_ID_KEY)).isNull();
    }
}
