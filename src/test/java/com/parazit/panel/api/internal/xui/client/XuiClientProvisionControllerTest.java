package com.parazit.panel.api.internal.xui.client;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parazit.panel.application.port.in.xui.client.CreateVpnClientUseCase;
import com.parazit.panel.application.port.in.xui.client.DeleteVpnClientUseCase;
import com.parazit.panel.application.port.in.xui.client.DisableVpnClientUseCase;
import com.parazit.panel.application.xui.client.XuiClientDeleteNotAllowedException;
import com.parazit.panel.application.xui.client.XuiClientProvisionNotAllowedException;
import com.parazit.panel.application.xui.client.XuiClientProvisionUnknownException;
import com.parazit.panel.application.xui.client.command.CreateVpnClientCommand;
import com.parazit.panel.application.xui.client.command.DeleteVpnClientCommand;
import com.parazit.panel.application.xui.client.command.DisableVpnClientCommand;
import com.parazit.panel.application.xui.client.result.CreateVpnClientResult;
import com.parazit.panel.application.xui.client.result.XuiClientLifecycleResult;
import com.parazit.panel.domain.xui.provisioning.XuiProvisionStatus;
import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import java.time.Instant;
import java.util.UUID;
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
class XuiClientProvisionControllerTest extends PostgreSqlContainerSupport {

    private static final UUID PROVISION_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID PLAN_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID SELECTION_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private final MockMvc mockMvc;
    private final FakeCreateVpnClientUseCase useCase;
    private final FakeDisableVpnClientUseCase disableUseCase;
    private final FakeDeleteVpnClientUseCase deleteUseCase;

    XuiClientProvisionControllerTest(
            MockMvc mockMvc,
            FakeCreateVpnClientUseCase useCase,
            FakeDisableVpnClientUseCase disableUseCase,
            FakeDeleteVpnClientUseCase deleteUseCase
    ) {
        this.mockMvc = mockMvc;
        this.useCase = useCase;
        this.disableUseCase = disableUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @BeforeEach
    void setUp() {
        useCase.result = result(true);
        useCase.failure = null;
        disableUseCase.result = lifecycleResult(XuiProvisionStatus.DISABLED, true, true);
        disableUseCase.failure = null;
        deleteUseCase.result = lifecycleResult(XuiProvisionStatus.DELETED, true, false);
        deleteUseCase.failure = null;
    }

    @Test
    void successfulNewProvisionReturnsCreatedAndSafeFields() throws Exception {
        mockMvc.perform(post("/internal/xui/clients")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"telegramUserId":123,"planSelectionId":"cccccccc-cccc-cccc-cccc-cccccccccccc","inboundId":7}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.provisionId").value(PROVISION_ID.toString()))
                .andExpect(jsonPath("$.planId").value(PLAN_ID.toString()))
                .andExpect(jsonPath("$.planSelectionId").value(SELECTION_ID.toString()))
                .andExpect(jsonPath("$.inboundId").value(7))
                .andExpect(jsonPath("$.remoteClientId").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.remoteEmail").value("vpn_abc_def"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.trafficLimitBytes").value(1024))
                .andExpect(jsonPath("$.ipLimit").value(1))
                .andExpect(jsonPath("$.newlyCreated").value(true))
                .andExpect(jsonPath("$.subscriptionId").doesNotExist())
                .andExpect(content().string(not(containsString("cookie"))))
                .andExpect(content().string(not(containsString("PRIVATE_KEY"))));
    }

    @Test
    void idempotentReplayReturnsOk() throws Exception {
        useCase.result = result(false);

        mockMvc.perform(post("/internal/xui/clients")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"telegramUserId":123,"planSelectionId":"cccccccc-cccc-cccc-cccc-cccccccccccc"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newlyCreated").value(false));
    }

    @Test
    void invalidRequestAndProvisioningFailuresUseStandardErrorContract() throws Exception {
        mockMvc.perform(post("/internal/xui/clients")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"telegramUserId":0,"planSelectionId":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.traceId").isNotEmpty());

        useCase.failure = new XuiClientProvisionNotAllowedException("Plan selection is not active");
        mockMvc.perform(post("/internal/xui/clients")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"telegramUserId":123,"planSelectionId":"cccccccc-cccc-cccc-cccc-cccccccccccc"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.traceId").isNotEmpty());

        useCase.failure = new XuiClientProvisionUnknownException("Xui client provisioning result is unknown");
        mockMvc.perform(post("/internal/xui/clients")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"telegramUserId":123,"planSelectionId":"cccccccc-cccc-cccc-cccc-cccccccccccc"}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void disableAndDeleteReturnSafeLifecycleResponses() throws Exception {
        mockMvc.perform(post("/internal/xui/clients/{provisionId}/disable", PROVISION_ID)
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"telegramUserId":123}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provisionId").value(PROVISION_ID.toString()))
                .andExpect(jsonPath("$.status").value("DISABLED"))
                .andExpect(jsonPath("$.changed").value(true))
                .andExpect(jsonPath("$.remoteClientPresent").value(true))
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.subscriptionId").doesNotExist());

        deleteUseCase.result = lifecycleResult(XuiProvisionStatus.DELETED, false, false);
        mockMvc.perform(delete("/internal/xui/clients/{provisionId}", PROVISION_ID)
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"telegramUserId":123,"force":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELETED"))
                .andExpect(jsonPath("$.changed").value(false))
                .andExpect(jsonPath("$.remoteClientPresent").value(false))
                .andExpect(jsonPath("$.subscriptionId").doesNotExist());
    }

    @Test
    void lifecycleInvalidRequestAndConflictUseStandardErrorContract() throws Exception {
        mockMvc.perform(post("/internal/xui/clients/{provisionId}/disable", PROVISION_ID)
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"telegramUserId":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.traceId").isNotEmpty());

        deleteUseCase.failure = new XuiClientDeleteNotAllowedException("Active Xui client requires force delete");
        mockMvc.perform(delete("/internal/xui/clients/{provisionId}", PROVISION_ID)
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"telegramUserId":123,"force":false}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    private static CreateVpnClientResult result(boolean newlyCreated) {
        return new CreateVpnClientResult(
                PROVISION_ID,
                USER_ID,
                PLAN_ID,
                SELECTION_ID,
                7,
                "11111111-1111-1111-1111-111111111111",
                "vpn_abc_def",
                XuiProvisionStatus.ACTIVE,
                1024,
                Instant.parse("2026-08-10T00:00:00Z"),
                1,
                Instant.parse("2026-07-11T00:00:00Z"),
                newlyCreated
        );
    }

    private static XuiClientLifecycleResult lifecycleResult(
            XuiProvisionStatus status,
            boolean changed,
            boolean remoteClientPresent
    ) {
        return new XuiClientLifecycleResult(
                PROVISION_ID,
                USER_ID,
                7,
                "11111111-1111-1111-1111-111111111111",
                "vpn_abc_def",
                status,
                Instant.parse("2026-07-11T00:00:00Z"),
                status == XuiProvisionStatus.DISABLED ? Instant.parse("2026-07-12T00:00:00Z") : null,
                status == XuiProvisionStatus.DELETED ? Instant.parse("2026-07-13T00:00:00Z") : null,
                changed,
                remoteClientPresent
        );
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        FakeCreateVpnClientUseCase fakeCreateVpnClientUseCase() {
            return new FakeCreateVpnClientUseCase();
        }

        @Bean
        @Primary
        FakeDisableVpnClientUseCase fakeDisableVpnClientUseCase() {
            return new FakeDisableVpnClientUseCase();
        }

        @Bean
        @Primary
        FakeDeleteVpnClientUseCase fakeDeleteVpnClientUseCase() {
            return new FakeDeleteVpnClientUseCase();
        }
    }

    static class FakeCreateVpnClientUseCase implements CreateVpnClientUseCase {

        private CreateVpnClientResult result;
        private RuntimeException failure;

        @Override
        public CreateVpnClientResult create(CreateVpnClientCommand command) {
            if (failure != null) {
                throw failure;
            }
            return result;
        }
    }

    static class FakeDisableVpnClientUseCase implements DisableVpnClientUseCase {

        private XuiClientLifecycleResult result;
        private RuntimeException failure;

        @Override
        public XuiClientLifecycleResult disable(DisableVpnClientCommand command) {
            if (failure != null) {
                throw failure;
            }
            return result;
        }
    }

    static class FakeDeleteVpnClientUseCase implements DeleteVpnClientUseCase {

        private XuiClientLifecycleResult result;
        private RuntimeException failure;

        @Override
        public XuiClientLifecycleResult delete(DeleteVpnClientCommand command) {
            if (failure != null) {
                throw failure;
            }
            return result;
        }
    }
}
