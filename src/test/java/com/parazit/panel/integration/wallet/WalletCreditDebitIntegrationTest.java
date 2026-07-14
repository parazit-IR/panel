package com.parazit.panel.integration.wallet;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.port.in.wallet.CreditWalletUseCase;
import com.parazit.panel.application.port.in.wallet.DebitWalletUseCase;
import com.parazit.panel.application.port.in.wallet.GetCustomerWalletUseCase;
import com.parazit.panel.application.port.in.wallet.ReconcileWalletBalanceUseCase;
import com.parazit.panel.application.wallet.command.CreditWalletCommand;
import com.parazit.panel.application.wallet.command.DebitWalletCommand;
import com.parazit.panel.application.wallet.command.GetCustomerWalletCommand;
import com.parazit.panel.application.wallet.command.ReconcileWalletBalanceCommand;
import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.wallet.WalletOperationOutcome;
import com.parazit.panel.domain.wallet.WalletTransactionType;
import com.parazit.panel.test.support.DatabaseCleaner;
import com.parazit.panel.test.support.MutableClockTestConfiguration;
import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "app.wallet.enabled=true"
})
@Import(MutableClockTestConfiguration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class WalletCreditDebitIntegrationTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-14T00:00:00Z");

    private final CreditWalletUseCase creditWalletUseCase;
    private final DebitWalletUseCase debitWalletUseCase;
    private final GetCustomerWalletUseCase getCustomerWalletUseCase;
    private final ReconcileWalletBalanceUseCase reconcileWalletBalanceUseCase;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    WalletCreditDebitIntegrationTest(
            CreditWalletUseCase creditWalletUseCase,
            DebitWalletUseCase debitWalletUseCase,
            GetCustomerWalletUseCase getCustomerWalletUseCase,
            ReconcileWalletBalanceUseCase reconcileWalletBalanceUseCase,
            UserRepository userRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.creditWalletUseCase = creditWalletUseCase;
        this.debitWalletUseCase = debitWalletUseCase;
        this.getCustomerWalletUseCase = getCustomerWalletUseCase;
        this.reconcileWalletBalanceUseCase = reconcileWalletBalanceUseCase;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanUserModuleTables(jdbcTemplate);
    }

    @Test
    void createsWalletAppliesLedgerAndReconcilesBalance() {
        User user = userRepository.save(User.create(48_001L, "wallet_user", "Wallet", null, UserLanguage.FA, NOW));

        var empty = getCustomerWalletUseCase.get(new GetCustomerWalletCommand(48_001L));
        assertThat(empty.balance()).isEqualTo(new Money(0, CurrencyCode.IRT));
        assertThat(walletRowCount()).isEqualTo(1);

        UUID creditReferenceId = UUID.randomUUID();
        var credit = creditWalletUseCase.credit(new CreditWalletCommand(
                user.getId(),
                new Money(500_000, CurrencyCode.IRT),
                WalletTransactionType.SYSTEM_CREDIT,
                "integration-test",
                creditReferenceId,
                "credit-key",
                "wallet.integration.credit"
        ));
        var replay = creditWalletUseCase.credit(new CreditWalletCommand(
                user.getId(),
                new Money(500_000, CurrencyCode.IRT),
                WalletTransactionType.SYSTEM_CREDIT,
                "integration-test",
                creditReferenceId,
                "credit-key",
                "wallet.integration.credit"
        ));
        var debit = debitWalletUseCase.debit(new DebitWalletCommand(
                user.getId(),
                new Money(200_000, CurrencyCode.IRT),
                WalletTransactionType.SYSTEM_DEBIT,
                "integration-test",
                UUID.randomUUID(),
                "debit-key",
                "wallet.integration.debit"
        ));
        var rejected = debitWalletUseCase.debit(new DebitWalletCommand(
                user.getId(),
                new Money(400_000, CurrencyCode.IRT),
                WalletTransactionType.SYSTEM_DEBIT,
                "integration-test",
                UUID.randomUUID(),
                "overspend-key",
                "wallet.integration.debit"
        ));

        assertThat(credit.outcome()).isEqualTo(WalletOperationOutcome.APPLIED);
        assertThat(replay.outcome()).isEqualTo(WalletOperationOutcome.REPLAYED);
        assertThat(debit.outcome()).isEqualTo(WalletOperationOutcome.APPLIED);
        assertThat(rejected.outcome()).isEqualTo(WalletOperationOutcome.REJECTED_INSUFFICIENT_BALANCE);
        assertThat(transactionRowCount()).isEqualTo(2);

        var summary = getCustomerWalletUseCase.get(new GetCustomerWalletCommand(48_001L));
        assertThat(summary.balance()).isEqualTo(new Money(300_000, CurrencyCode.IRT));
        assertThat(summary.transactionCount()).isEqualTo(2);

        var reconciliation = reconcileWalletBalanceUseCase.reconcile(new ReconcileWalletBalanceCommand(summary.walletId()));
        assertThat(reconciliation.consistent()).isTrue();
        assertThat(reconciliation.storedBalance()).isEqualTo(new Money(300_000, CurrencyCode.IRT));
        assertThat(reconciliation.ledgerCalculatedBalance()).isEqualTo(new Money(300_000, CurrencyCode.IRT));
    }

    @Test
    void concurrentDebitsCannotOverspendWallet() throws Exception {
        User user = userRepository.save(User.create(48_002L, "wallet_user_2", "Wallet", null, UserLanguage.FA, NOW));
        creditWalletUseCase.credit(new CreditWalletCommand(
                user.getId(),
                new Money(100, CurrencyCode.IRT),
                WalletTransactionType.SYSTEM_CREDIT,
                "integration-test",
                UUID.randomUUID(),
                "initial-credit",
                "wallet.integration.credit"
        ));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<WalletOperationOutcome> first = concurrentDebit(user.getId(), "debit-a", ready, start);
            Callable<WalletOperationOutcome> second = concurrentDebit(user.getId(), "debit-b", ready, start);
            var futures = List.of(executor.submit(first), executor.submit(second));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<WalletOperationOutcome> outcomes = List.of(
                    futures.get(0).get(10, TimeUnit.SECONDS),
                    futures.get(1).get(10, TimeUnit.SECONDS)
            );

            assertThat(outcomes).containsExactlyInAnyOrder(
                    WalletOperationOutcome.APPLIED,
                    WalletOperationOutcome.REJECTED_INSUFFICIENT_BALANCE
            );
        }

        var summary = getCustomerWalletUseCase.get(new GetCustomerWalletCommand(48_002L));
        assertThat(summary.balance()).isEqualTo(new Money(20, CurrencyCode.IRT));
        assertThat(summary.transactionCount()).isEqualTo(2);
    }

    private Callable<WalletOperationOutcome> concurrentDebit(
            UUID userId,
            String key,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        return () -> {
            ready.countDown();
            assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
            return debitWalletUseCase.debit(new DebitWalletCommand(
                    userId,
                    new Money(80, CurrencyCode.IRT),
                    WalletTransactionType.SYSTEM_DEBIT,
                    "integration-test",
                    UUID.randomUUID(),
                    key,
                    "wallet.integration.debit"
            )).outcome();
        };
    }

    private Integer walletRowCount() {
        return jdbcTemplate.queryForObject("select count(*) from wallets", Integer.class);
    }

    private Integer transactionRowCount() {
        return jdbcTemplate.queryForObject("select count(*) from wallet_transactions", Integer.class);
    }
}
