package com.parazit.panel.integration.subscription.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.parazit.panel.application.port.out.xui.XuiInboundClient;
import com.parazit.panel.application.xui.model.XuiClientSnapshot;
import com.parazit.panel.application.xui.model.XuiInboundSnapshot;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.repository.PlanRepository;
import com.parazit.panel.domain.plan.selection.PlanSelection;
import com.parazit.panel.domain.plan.selection.repository.PlanSelectionRepository;
import com.parazit.panel.domain.subscription.Subscription;
import com.parazit.panel.domain.subscription.repository.SubscriptionRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import com.parazit.panel.domain.xui.provisioning.repository.XuiClientProvisionRepository;
import com.parazit.panel.test.fixture.PlanTestData;
import com.parazit.panel.test.support.DatabaseCleaner;
import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "spring.security.user.name=test",
        "spring.security.user.password=test",
        "app.subscription.public-base-url=https://subscriptions.example.test",
        "app.subscription.endpoint.public-host=vpn.example.test",
        "app.subscription.endpoint.default-fingerprint=chrome",
        "app.subscription.qr.enabled=true"
})
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class SubscriptionDeliveryQrIntegrationTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-13T00:00:00Z");
    private static final long TELEGRAM_USER_ID = 960001L;

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final FakeXuiInboundClient inboundClient;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final PlanSelectionRepository planSelectionRepository;
    private final OrderRepository orderRepository;
    private final XuiClientProvisionRepository provisionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final JdbcTemplate jdbcTemplate;

    SubscriptionDeliveryQrIntegrationTest(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            FakeXuiInboundClient inboundClient,
            UserRepository userRepository,
            PlanRepository planRepository,
            PlanSelectionRepository planSelectionRepository,
            OrderRepository orderRepository,
            XuiClientProvisionRepository provisionRepository,
            SubscriptionRepository subscriptionRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.inboundClient = inboundClient;
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.planSelectionRepository = planSelectionRepository;
        this.orderRepository = orderRepository;
        this.provisionRepository = provisionRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPlanSelectionTables(jdbcTemplate);
        inboundClient.inbound = null;
    }

    @Test
    void subscriptionUrlAndVlessQrDeliveryPreserveTokenRulesAndLifecycle() throws Exception {
        Fixture fixture = fixture();
        inboundClient.inbound = inbound(fixture.provision);
        JsonNode created = createSubscription(fixture.provision);
        String token = created.get("accessToken").asText();
        UUID subscriptionId = UUID.fromString(created.get("subscriptionId").asText());
        String delivery = "/internal/users/" + TELEGRAM_USER_ID + "/subscriptions/" + subscriptionId + "/delivery";

        mockMvc.perform(get(delivery)
                        .with(httpBasic("test", "test")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$.subscriptionUrlAvailable").value(true))
                .andExpect(jsonPath("$.configCount").value(1))
                .andExpect(jsonPath("$.entries[0].index").value(1))
                .andExpect(content().string(not(containsString("vless://"))))
                .andExpect(content().string(not(containsString(token))));

        String expectedUrl = "https://subscriptions.example.test/sub/" + token;
        mockMvc.perform(post(delivery + "/subscription-url")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"accessToken\":\"" + token + "\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$.subscriptionUrl").value(expectedUrl));

        byte[] subscriptionQr = mockMvc.perform(post(delivery + "/subscription-url/qr")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "accessToken": "%s",
                                  "format": "PNG",
                                  "size": 512,
                                  "marginModules": 4,
                                  "errorCorrection": "MEDIUM"
                                }
                                """.formatted(token)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("image/png")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, not(containsString(token))))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(decode(subscriptionQr)).isEqualTo(expectedUrl);

        JsonNode config = json(mockMvc.perform(get(delivery + "/configs/1")
                        .with(httpBasic("test", "test")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$.uri").value(containsString("vless://")))
                .andExpect(content().string(not(containsString("PRIVATE_KEY_MUST_NOT_LEAK"))))
                .andReturn().getResponse().getContentAsString());
        String expectedVless = config.get("uri").asText();

        mockMvc.perform(get(delivery + "/content?format=plain")
                        .with(httpBasic("test", "test")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(content().string(expectedVless + "\n"));
        String renderedBase64 = mockMvc.perform(get(delivery + "/content?format=base64")
                        .with(httpBasic("test", "test")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(new String(java.util.Base64.getDecoder().decode(renderedBase64))).isEqualTo(expectedVless + "\n");

        byte[] vlessQr = mockMvc.perform(get(delivery + "/configs/1/qr?format=PNG&size=512&marginModules=4&errorCorrection=MEDIUM")
                        .with(httpBasic("test", "test")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("image/png")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, not(containsString("vless://"))))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(decode(vlessQr)).isEqualTo(expectedVless);

        Subscription before = subscriptionRepository.findByXuiClientProvisionId(fixture.provision.getId()).orElseThrow();
        assertThat(before.getAccessCount()).isZero();
        assertThat(before.getAccessTokenHash()).doesNotContain(token);

        JsonNode rotated = json(mockMvc.perform(post("/internal/users/{telegramUserId}/subscriptions/{subscriptionId}/rotate-token", TELEGRAM_USER_ID, subscriptionId)
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"reason\":\"qr rotation\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        String newToken = rotated.get("accessToken").asText();
        assertThat(newToken).isNotEqualTo(token);

        mockMvc.perform(post(delivery + "/subscription-url/qr")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"accessToken\":\"" + token + "\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(delivery + "/subscription-url/qr")
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"accessToken\":\"" + newToken + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/internal/users/{telegramUserId}/subscriptions/{subscriptionId}/revoke", TELEGRAM_USER_ID, subscriptionId)
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"reason\":\"qr done\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get(delivery + "/configs/1/qr")
                        .with(httpBasic("test", "test")))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsInvalidIndexSizeAndDisabledProvision() throws Exception {
        Fixture fixture = fixture();
        inboundClient.inbound = inbound(fixture.provision);
        JsonNode created = createSubscription(fixture.provision);
        UUID subscriptionId = UUID.fromString(created.get("subscriptionId").asText());
        String delivery = "/internal/users/" + TELEGRAM_USER_ID + "/subscriptions/" + subscriptionId + "/delivery";

        mockMvc.perform(get(delivery + "/configs/0/qr")
                        .with(httpBasic("test", "test")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(delivery + "/configs/2/qr")
                        .with(httpBasic("test", "test")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(delivery + "/configs/1/qr?size=64")
                        .with(httpBasic("test", "test")))
                .andExpect(status().isBadRequest());

        XuiClientProvision provision = provisionRepository.findById(fixture.provision.getId()).orElseThrow();
        provision.markDisabling();
        provision.markDisabled(NOW.plusSeconds(10));
        provisionRepository.save(provision);
        mockMvc.perform(get(delivery + "/configs/1/qr")
                        .with(httpBasic("test", "test")))
                .andExpect(status().isConflict());
    }

    private JsonNode createSubscription(XuiClientProvision provision) throws Exception {
        return json(mockMvc.perform(post("/internal/users/{telegramUserId}/subscriptions", TELEGRAM_USER_ID)
                        .with(httpBasic("test", "test"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"xuiClientProvisionId\":\"" + provision.getId() + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.delivery.subscriptionQrEndpoint").isNotEmpty())
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode json(String body) throws Exception {
        return objectMapper.readTree(body);
    }

    private Fixture fixture() {
        User user = userRepository.save(User.create(TELEGRAM_USER_ID, "qrflow", "Ali", null, UserLanguage.FA, NOW));
        Plan plan = planRepository.save(PlanTestData.trafficLimitedPlan("QR_FLOW_" + UUID.randomUUID(), 1));
        plan.activate();
        plan = planRepository.save(plan);
        PlanSelection selection = planSelectionRepository.save(PlanSelection.create(user.getId(), plan, NOW, Duration.ofHours(1)));
        Order order = Order.createForPlanSelection(user.getId(), plan.getId(), selection.getId(), 1000, "IRT");
        order.markPaid(NOW.plusSeconds(1));
        order.markProvisioning(NOW.plusSeconds(2));
        order.markCompleted(NOW.plusSeconds(3));
        orderRepository.save(order);
        XuiClientProvision provision = XuiClientProvision.createPending(
                user.getId(),
                plan.getId(),
                selection.getId(),
                7,
                "11111111-1111-1111-1111-111111111111",
                "client@example.test",
                "sub-test",
                5_368_709_120L,
                NOW.plus(Duration.ofDays(30)),
                2,
                NOW
        );
        provision.markProvisioning();
        provision.markActive(NOW.plusSeconds(4));
        provision = provisionRepository.save(provision);
        return new Fixture(user, provision);
    }

    private static XuiInboundSnapshot inbound(XuiClientProvision provision) {
        XuiClientSnapshot client = new XuiClientSnapshot(
                provision.getRemoteClientId(),
                provision.getRemoteEmail(),
                true,
                provision.getTrafficLimitBytes(),
                128,
                256,
                provision.getExpiresAt(),
                provision.getIpLimit(),
                provision.getRemoteSubscriptionId(),
                "xtls-rprx-vision",
                null,
                null,
                0
        );
        return new XuiInboundSnapshot(
                provision.getInboundId(),
                "Reality Main",
                "VLESS",
                443,
                true,
                "",
                provision.getTrafficLimitBytes(),
                128,
                256,
                provision.getExpiresAt(),
                List.of(client),
                "tcp",
                "reality",
                "vpn.example.test",
                "PUBLIC_KEY_TEST",
                "abcd1234"
        );
    }

    private static String decode(byte[] png) throws Exception {
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(
                ImageIO.read(new ByteArrayInputStream(png))
        )));
        return new MultiFormatReader().decode(bitmap).getText();
    }

    private record Fixture(User user, XuiClientProvision provision) {
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        FakeXuiInboundClient fakeXuiInboundClient() {
            return new FakeXuiInboundClient();
        }
    }

    static class FakeXuiInboundClient implements XuiInboundClient {

        private XuiInboundSnapshot inbound;

        @Override
        public List<XuiInboundSnapshot> getInbounds() {
            return inbound == null ? List.of() : List.of(inbound);
        }

        @Override
        public Optional<XuiInboundSnapshot> getInboundById(long inboundId) {
            return inbound != null && inbound.id() == inboundId ? Optional.of(inbound) : Optional.empty();
        }

        @Override
        public Optional<XuiClientSnapshot> findClient(long inboundId, String clientId, String email) {
            return getInboundById(inboundId)
                    .flatMap(found -> found.clients().stream()
                            .filter(client -> client.clientId().equalsIgnoreCase(clientId))
                            .findFirst());
        }
    }
}
