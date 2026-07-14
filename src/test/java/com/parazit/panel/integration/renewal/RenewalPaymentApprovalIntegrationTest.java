package com.parazit.panel.integration.renewal;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.payment.ApprovePaymentCommand;
import com.parazit.panel.application.payment.PaymentApprovalResult;
import com.parazit.panel.application.payment.PaymentApprovalService;
import com.parazit.panel.application.payment.PaymentApprovalSource;
import com.parazit.panel.application.port.out.telegram.TelegramBotClient;
import com.parazit.panel.application.telegram.command.AnswerTelegramCallbackCommand;
import com.parazit.panel.application.telegram.command.EditTelegramMessageCommand;
import com.parazit.panel.application.telegram.command.GetTelegramUpdatesCommand;
import com.parazit.panel.application.telegram.command.SendTelegramMessageCommand;
import com.parazit.panel.application.telegram.command.SendTelegramPhotoCommand;
import com.parazit.panel.application.telegram.result.TelegramEditResult;
import com.parazit.panel.application.telegram.result.TelegramMessageKind;
import com.parazit.panel.application.telegram.result.TelegramSendResult;
import com.parazit.panel.application.telegram.result.TelegramUpdatesResult;
import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.OrderStatus;
import com.parazit.panel.domain.order.RenewalSnapshot;
import com.parazit.panel.domain.order.RenewalTrafficPolicy;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanType;
import com.parazit.panel.domain.plan.repository.PlanRepository;
import com.parazit.panel.domain.plan.selection.PlanSelection;
import com.parazit.panel.domain.plan.selection.repository.PlanSelectionRepository;
import com.parazit.panel.domain.provisioning.outbox.ProvisioningOutboxType;
import com.parazit.panel.domain.provisioning.outbox.repository.ProvisioningOutboxRepository;
import com.parazit.panel.domain.renewal.RenewalOutbox;
import com.parazit.panel.domain.renewal.repository.RenewalOutboxRepository;
import com.parazit.panel.domain.subscription.Subscription;
import com.parazit.panel.domain.subscription.repository.SubscriptionRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import com.parazit.panel.domain.xui.provisioning.repository.XuiClientProvisionRepository;
import com.parazit.panel.test.support.DatabaseCleaner;
import com.parazit.panel.test.support.MutableClockTestConfiguration;
import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "app.renewal.enabled=true",
        "app.telegram.bot.callback-signing-secret=0123456789abcdef0123456789abcdef"
})
@Import({
        MutableClockTestConfiguration.class,
        RenewalPaymentApprovalIntegrationTest.TelegramTestConfiguration.class
})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class RenewalPaymentApprovalIntegrationTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-14T12:00:00Z");

    private final PaymentApprovalService paymentApprovalService;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final PlanSelectionRepository selectionRepository;
    private final XuiClientProvisionRepository provisionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RenewalOutboxRepository renewalOutboxRepository;
    private final ProvisioningOutboxRepository provisioningOutboxRepository;
    private final JdbcTemplate jdbcTemplate;

    RenewalPaymentApprovalIntegrationTest(
            PaymentApprovalService paymentApprovalService,
            UserRepository userRepository,
            PlanRepository planRepository,
            PlanSelectionRepository selectionRepository,
            XuiClientProvisionRepository provisionRepository,
            SubscriptionRepository subscriptionRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            RenewalOutboxRepository renewalOutboxRepository,
            ProvisioningOutboxRepository provisioningOutboxRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.paymentApprovalService = paymentApprovalService;
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.selectionRepository = selectionRepository;
        this.provisionRepository = provisionRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.renewalOutboxRepository = renewalOutboxRepository;
        this.provisioningOutboxRepository = provisioningOutboxRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPlanSelectionTables(jdbcTemplate);
    }

    @Test
    void approvedRenewalPaymentQueuesOneRenewalOutboxAndReplayReusesIt() {
        Fixture fixture = fixture(920001L);

        PaymentApprovalResult first = paymentApprovalService.approve(new ApprovePaymentCommand(
                fixture.payment().getId(),
                PaymentApprovalSource.MANUAL_OPERATOR_REVIEW,
                "manual-review-1",
                null,
                NOW
        ));
        PaymentApprovalResult replay = paymentApprovalService.approve(new ApprovePaymentCommand(
                fixture.payment().getId(),
                PaymentApprovalSource.MANUAL_OPERATOR_REVIEW,
                "manual-review-1",
                null,
                NOW.plusSeconds(10)
        ));

        assertThat(first.newlyApproved()).isTrue();
        assertThat(first.provisioningRequired()).isFalse();
        assertThat(replay.newlyApproved()).isFalse();
        assertThat(replay.provisioningRequired()).isFalse();
        assertThat(paymentRepository.findById(fixture.payment().getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.APPROVED);
        assertThat(orderRepository.findById(fixture.renewalOrder().getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.RENEWAL_PENDING);
        RenewalOutbox outbox = renewalOutboxRepository
                .findByRenewalOrderIdAndEventType(fixture.renewalOrder().getId(), RenewalOutbox.APPLY_REQUESTED_EVENT_TYPE)
                .orElseThrow();
        assertThat(outbox.getPaymentId()).isEqualTo(fixture.payment().getId());
        assertThat(outbox.getTargetSubscriptionId()).isEqualTo(fixture.subscription().getId());
        assertThat(outbox.getPayload()).contains("\"renewalOrderId\"");
        assertThat(renewalOutboxCount()).isEqualTo(1);
        assertThat(provisioningOutboxRepository.existsByOrderIdAndType(
                fixture.renewalOrder().getId(),
                ProvisioningOutboxType.CREATE_VPN_CLIENT
        )).isFalse();
        assertThat(subscriptionRepository.findAll()).hasSize(1);
        assertThat(subscriptionRepository.findById(fixture.subscription().getId()).orElseThrow().getExpiresAt())
                .isEqualTo(fixture.subscription().getExpiresAt());
    }

    @Test
    void concurrentDuplicateRenewalApprovalCreatesOneOutbox() throws Exception {
        Fixture fixture = fixture(920002L);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<PaymentApprovalResult> approval = () -> {
            ready.countDown();
            assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
            return paymentApprovalService.approve(new ApprovePaymentCommand(
                    fixture.payment().getId(),
                    PaymentApprovalSource.MANUAL_OPERATOR_REVIEW,
                    "manual-review-concurrent",
                    null,
                    NOW
            ));
        };

        try {
            Future<PaymentApprovalResult> first = executor.submit(approval);
            Future<PaymentApprovalResult> second = executor.submit(approval);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<PaymentApprovalResult> results = List.of(
                    first.get(10, TimeUnit.SECONDS),
                    second.get(10, TimeUnit.SECONDS)
            );

            assertThat(results).extracting(PaymentApprovalResult::provisioningRequired)
                    .containsOnly(false);
            assertThat(results).extracting(PaymentApprovalResult::newlyApproved)
                    .containsExactlyInAnyOrder(true, false);
            assertThat(renewalOutboxCount()).isEqualTo(1);
            assertThat(orderRepository.findById(fixture.renewalOrder().getId()).orElseThrow().getStatus())
                    .isEqualTo(OrderStatus.RENEWAL_PENDING);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private Fixture fixture(long telegramUserId) {
        User user = userRepository.save(User.create(telegramUserId, null, "Renew", null, UserLanguage.FA, NOW));
        Plan plan = Plan.create("RENEW30" + telegramUserId, "Renew 30", "Renewal plan", PlanType.TRAFFIC_LIMITED,
                500_000L, CurrencyCode.IRT, 30, 30L * 1024 * 1024 * 1024, 2, 1);
        plan.activate();
        plan.enableRenewal();
        plan = planRepository.save(plan);
        PlanSelection originalSelection = selectionRepository.save(PlanSelection.create(
                user.getId(),
                plan,
                NOW.minus(Duration.ofDays(5)),
                Duration.ofMinutes(30)
        ));
        Order originalOrder = orderRepository.save(Order.createForPlanSelection(
                user.getId(),
                plan.getId(),
                originalSelection.getId(),
                500_000L,
                "IRT"
        ));
        String remoteClientId = UUID.randomUUID().toString();
        XuiClientProvision provision = XuiClientProvision.createPending(
                user.getId(),
                plan.getId(),
                originalSelection.getId(),
                1001L,
                remoteClientId,
                "renew-" + telegramUserId + "@example.test",
                "sub-" + telegramUserId,
                30L * 1024 * 1024 * 1024,
                NOW.plus(Duration.ofDays(25)),
                2,
                NOW.minus(Duration.ofDays(5))
        );
        provision.markProvisioning();
        provision.markActive(NOW.minus(Duration.ofDays(5)));
        provision = provisionRepository.save(provision);
        Subscription subscription = subscriptionRepository.save(Subscription.activate(
                user.getId(),
                originalOrder.getId(),
                originalSelection.getId(),
                provision.getId(),
                provision.getInboundId(),
                UUID.fromString(remoteClientId),
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                "012345",
                NOW.minus(Duration.ofDays(5)),
                NOW.plus(Duration.ofDays(25)),
                "Renew service",
                "v1"
        ));
        originalSelection.consume(NOW.minus(Duration.ofDays(5)));
        selectionRepository.save(originalSelection);
        PlanSelection renewalSelection = selectionRepository.save(PlanSelection.createRenewal(
                user.getId(),
                subscription.getId(),
                plan,
                NOW,
                Duration.ofMinutes(15)
        ));
        RenewalSnapshot snapshot = new RenewalSnapshot(
                subscription.getId(),
                provision.getId(),
                "Renew service",
                provision.getRemoteEmail(),
                subscription.getExpiresAt(),
                provision.getTrafficLimitBytes(),
                provision.getLastKnownTotalBytes(),
                Duration.ofDays(30),
                30L * 1024 * 1024 * 1024,
                RenewalTrafficPolicy.RESET_TO_PLAN_LIMIT,
                new Money(500_000L, CurrencyCode.IRT),
                new Money(500_000L, CurrencyCode.IRT),
                plan.getName(),
                plan.getDescription(),
                plan.getId(),
                NOW
        );
        Order renewalOrder = orderRepository.save(Order.createRenewal(
                user.getId(),
                plan.getId(),
                renewalSelection.getId(),
                subscription.getId(),
                snapshot,
                500_000L,
                "IRT"
        ));
        Payment payment = Payment.create(
                renewalOrder.getId(),
                user.getId(),
                PaymentMethod.CARD_TO_CARD,
                500_000L,
                500_000L,
                "IRT",
                NOW.plus(Duration.ofMinutes(30))
        );
        payment.markWaitingForPayment();
        payment = paymentRepository.save(payment);
        return new Fixture(subscription, renewalOrder, payment);
    }

    private int renewalOutboxCount() {
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM renewal_outbox", Integer.class);
        return count == null ? 0 : count;
    }

    private record Fixture(Subscription subscription, Order renewalOrder, Payment payment) {
    }

    @TestConfiguration
    static class TelegramTestConfiguration {

        @Bean
        @Primary
        TelegramBotClient telegramBotClient() {
            return new TelegramBotClient() {
                @Override
                public TelegramSendResult sendMessage(SendTelegramMessageCommand command) {
                    return new TelegramSendResult(command.chatId(), 1L, NOW, TelegramMessageKind.TEXT, true);
                }

                @Override
                public TelegramSendResult sendPhoto(SendTelegramPhotoCommand command) {
                    return new TelegramSendResult(command.chatId(), 1L, NOW, TelegramMessageKind.PHOTO, true);
                }

                @Override
                public TelegramEditResult editMessage(EditTelegramMessageCommand command) {
                    return new TelegramEditResult(command.chatId(), command.messageId(), true);
                }

                @Override
                public void answerCallbackQuery(AnswerTelegramCallbackCommand command) {
                }

                @Override
                public TelegramUpdatesResult getUpdates(GetTelegramUpdatesCommand command) {
                    return new TelegramUpdatesResult(java.util.List.of());
                }
            };
        }
    }
}
