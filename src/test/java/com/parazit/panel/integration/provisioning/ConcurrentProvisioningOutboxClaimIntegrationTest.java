package com.parazit.panel.integration.provisioning;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.provisioning.outbox.ClaimProvisioningOutboxTransaction;
import com.parazit.panel.application.provisioning.outbox.ProvisioningOutboxPayloadSerializer;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.OrderStatus;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanType;
import com.parazit.panel.domain.plan.repository.PlanRepository;
import com.parazit.panel.domain.plan.selection.PlanSelection;
import com.parazit.panel.domain.plan.selection.repository.PlanSelectionRepository;
import com.parazit.panel.domain.provisioning.outbox.ProvisioningOutbox;
import com.parazit.panel.domain.provisioning.outbox.ProvisioningOutboxStatus;
import com.parazit.panel.domain.provisioning.outbox.ProvisioningOutboxType;
import com.parazit.panel.domain.provisioning.outbox.repository.ProvisioningOutboxRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.test.support.DatabaseCleaner;
import com.parazit.panel.test.support.MutableClockTestConfiguration;
import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = "spring.profiles.active=local")
@Import(MutableClockTestConfiguration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ConcurrentProvisioningOutboxClaimIntegrationTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = MutableClockTestConfiguration.DEFAULT_INSTANT;

    private final ClaimProvisioningOutboxTransaction claimTransaction;
    private final ProvisioningOutboxRepository outboxRepository;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final PlanSelectionRepository planSelectionRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final TransactionTemplate transactionTemplate;
    private final JdbcTemplate jdbcTemplate;

    ConcurrentProvisioningOutboxClaimIntegrationTest(
            ClaimProvisioningOutboxTransaction claimTransaction,
            ProvisioningOutboxRepository outboxRepository,
            UserRepository userRepository,
            PlanRepository planRepository,
            PlanSelectionRepository planSelectionRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            TransactionTemplate transactionTemplate,
            JdbcTemplate jdbcTemplate
    ) {
        this.claimTransaction = claimTransaction;
        this.outboxRepository = outboxRepository;
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.planSelectionRepository = planSelectionRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.transactionTemplate = transactionTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPlanSelectionTables(jdbcTemplate);
    }

    @Test
    void onlyOneWorkerClaimsOutboxEventWhenTransactionsOverlap() throws Exception {
        ProvisioningOutbox outbox = pendingOutbox();
        CountDownLatch firstClaimed = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<Optional<ProvisioningOutbox>> first = executor.submit(() -> transactionTemplate.execute(status -> {
            Optional<ProvisioningOutbox> claimed = claimTransaction.claim(outbox.getEventId());
            firstClaimed.countDown();
            await(releaseFirst);
            return claimed;
        }));

        assertThat(firstClaimed.await(10, TimeUnit.SECONDS)).isTrue();
        Future<Optional<ProvisioningOutbox>> second = executor.submit(() -> claimTransaction.claim(outbox.getEventId()));

        try {
            assertThat(second.get(5, TimeUnit.SECONDS)).isEmpty();
        } finally {
            releaseFirst.countDown();
        }

        assertThat(first.get(5, TimeUnit.SECONDS)).isPresent();
        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        ProvisioningOutbox reloadedOutbox = outboxRepository.findByEventId(outbox.getEventId()).orElseThrow();
        Order reloadedOrder = orderRepository.findById(outbox.getOrderId()).orElseThrow();
        assertThat(reloadedOutbox.getStatus()).isEqualTo(ProvisioningOutboxStatus.PROCESSING);
        assertThat(reloadedOrder.getStatus()).isEqualTo(OrderStatus.PROVISIONING);
    }

    private ProvisioningOutbox pendingOutbox() {
        User user = userRepository.save(User.create(940_001L, null, "Provision", null, UserLanguage.EN, NOW));
        Plan plan = Plan.create(
                "PROVISION_BASIC",
                "Provision Basic",
                null,
                PlanType.TRAFFIC_LIMITED,
                100_000L,
                CurrencyCode.IRT,
                30,
                50_000_000_000L,
                2,
                1
        );
        plan.activate();
        plan = planRepository.save(plan);
        PlanSelection selection = planSelectionRepository.save(PlanSelection.create(
                user.getId(),
                plan,
                NOW,
                Duration.ofMinutes(30)
        ));
        Order order = Order.createForPlanSelection(user.getId(), plan.getId(), selection.getId(), 100_000L, "IRT");
        order.markPaid(NOW);
        order = orderRepository.save(order);
        Payment payment = Payment.create(
                order.getId(),
                user.getId(),
                PaymentMethod.CARD_TO_CARD,
                100_000L,
                100_000L,
                "IRT",
                NOW.plus(Duration.ofMinutes(30))
        );
        payment.markWaitingForPayment();
        payment.markApproved(NOW, "manual-review", null);
        payment = paymentRepository.save(payment);
        return outboxRepository.save(ProvisioningOutbox.create(
                UUID.randomUUID(),
                order.getId(),
                payment.getId(),
                user.getId(),
                plan.getId(),
                selection.getId(),
                ProvisioningOutboxType.CREATE_VPN_CLIENT,
                ProvisioningOutboxPayloadSerializer.CREATE_VPN_CLIENT_V1,
                "{}",
                NOW
        ));
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting for latch");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for latch", exception);
        }
    }
}
