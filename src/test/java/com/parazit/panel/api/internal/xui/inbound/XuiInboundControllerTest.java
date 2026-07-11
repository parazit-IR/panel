package com.parazit.panel.api.internal.xui.inbound;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parazit.panel.application.port.out.xui.XuiInboundClient;
import com.parazit.panel.application.xui.model.XuiInboundSnapshot;
import com.parazit.panel.infrastructure.xui.exception.XuiAuthenticationException;
import com.parazit.panel.infrastructure.xui.exception.XuiConnectionException;
import com.parazit.panel.infrastructure.xui.exception.XuiInvalidResponseException;
import com.parazit.panel.infrastructure.xui.exception.XuiTimeoutException;
import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "spring.security.user.name=test",
        "spring.security.user.password=test"
})
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class XuiInboundControllerTest extends PostgreSqlContainerSupport {

    private final MockMvc mockMvc;
    private final FakeXuiInboundClient inboundClient;

    XuiInboundControllerTest(MockMvc mockMvc, FakeXuiInboundClient inboundClient) {
        this.mockMvc = mockMvc;
        this.inboundClient = inboundClient;
    }

    @BeforeEach
    void setUp() {
        inboundClient.reset();
    }

    @Test
    void listReturnsSanitizedArrayContract() throws Exception {
        inboundClient.inbounds = List.of(inbound(7, "Reality Main"));

        mockMvc.perform(get("/internal/xui/inbounds")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].remark").value("Reality Main"))
                .andExpect(jsonPath("$[0].protocol").value("VLESS"))
                .andExpect(jsonPath("$[0].port").value(443))
                .andExpect(jsonPath("$[0].enabled").value(true))
                .andExpect(jsonPath("$[0].clientCount").value(0))
                .andExpect(jsonPath("$[0].publicKey").value("PUBLIC_KEY"))
                .andExpect(jsonPath("$[0].shortIds[0]").value("abcd"))
                .andExpect(jsonPath("$[0].clients").doesNotExist())
                .andExpect(jsonPath("$[0].settings").doesNotExist())
                .andExpect(jsonPath("$[0].streamSettings").doesNotExist())
                .andExpect(jsonPath("$[0].privateKey").doesNotExist())
                .andExpect(content().string(not(containsString("PRIVATE_KEY"))));
    }

    @Test
    void getByIdAndRemarkHandleSuccessMissingAndAmbiguous() throws Exception {
        inboundClient.inbounds = List.of(inbound(7, "Reality Main"), inbound(8, "Duplicate"), inbound(9, "duplicate"));

        mockMvc.perform(get("/internal/xui/inbounds/{inboundId}", 7)
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
        mockMvc.perform(get("/internal/xui/inbounds/{inboundId}", 99)
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
        mockMvc.perform(get("/internal/xui/inbounds/not-a-number")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("inboundId has an invalid value"));

        mockMvc.perform(get("/internal/xui/inbounds/by-remark/{remark}", "reality main")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
        mockMvc.perform(get("/internal/xui/inbounds/by-remark/{remark}", "missing")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/internal/xui/inbounds/by-remark/{remark}", "duplicate")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void eligibleReturnsDeterministicMatchOrNotFound() throws Exception {
        inboundClient.inbounds = List.of(inbound(9, "secondary"), inbound(7, "primary"));

        mockMvc.perform(get("/internal/xui/inbounds/eligible?protocol=vless&security=reality&network=tcp")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));

        inboundClient.inbounds = List.of(disabledInbound(7, "disabled"));
        mockMvc.perform(get("/internal/xui/inbounds/eligible")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/internal/xui/inbounds/eligible"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void remoteFailuresUseStandardErrorContract() throws Exception {
        assertMapped(new XuiInvalidResponseException("Xui response could not be parsed"), 502, "Bad Gateway");
        assertMapped(new XuiConnectionException("Xui server is unreachable", new RuntimeException("connect")), 503, "Service Unavailable");
        assertMapped(new XuiTimeoutException("Xui request timed out", new RuntimeException("timeout")), 504, "Gateway Timeout");
        assertMapped(new XuiAuthenticationException("Xui authentication failed"), 502, "Bad Gateway");
    }

    private void assertMapped(RuntimeException exception, int status, String error) throws Exception {
        inboundClient.failure = exception;

        mockMvc.perform(get("/internal/xui/inbounds")
                        .with(httpBasic("test", "test"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.error").value(error))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(content().string(not(containsString("cookie"))))
                .andExpect(content().string(not(containsString("PRIVATE_KEY"))));

        inboundClient.failure = null;
    }

    private static XuiInboundSnapshot inbound(long id, String remark) {
        return new XuiInboundSnapshot(
                id,
                remark,
                "VLESS",
                443,
                true,
                null,
                0,
                0,
                0,
                null,
                List.of(),
                "tcp",
                "REALITY",
                "vpn.example.test",
                "PUBLIC_KEY",
                "abcd"
        );
    }

    private static XuiInboundSnapshot disabledInbound(long id, String remark) {
        return new XuiInboundSnapshot(
                id,
                remark,
                "VLESS",
                443,
                false,
                null,
                0,
                0,
                0,
                null,
                List.of(),
                "tcp",
                "REALITY",
                "vpn.example.test",
                "PUBLIC_KEY",
                "abcd"
        );
    }

    @TestConfiguration
    static class XuiInboundControllerTestConfiguration {

        @Bean
        @Primary
        FakeXuiInboundClient fakeXuiInboundClient() {
            return new FakeXuiInboundClient();
        }
    }

    static class FakeXuiInboundClient implements XuiInboundClient {

        private List<XuiInboundSnapshot> inbounds = List.of();
        private RuntimeException failure;

        void reset() {
            inbounds = List.of();
            failure = null;
        }

        @Override
        public List<XuiInboundSnapshot> getInbounds() {
            if (failure != null) {
                throw failure;
            }
            return inbounds;
        }

        @Override
        public Optional<XuiInboundSnapshot> getInboundById(long inboundId) {
            if (failure != null) {
                throw failure;
            }
            return inbounds.stream()
                    .filter(inbound -> inbound.id() == inboundId)
                    .findFirst();
        }
    }
}
