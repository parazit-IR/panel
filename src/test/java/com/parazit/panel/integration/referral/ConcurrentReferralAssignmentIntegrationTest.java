package com.parazit.panel.integration.referral;

import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import com.parazit.panel.test.support.DatabaseCleaner;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.port.in.referral.AssignReferralUseCase;
import com.parazit.panel.application.port.in.user.RegisterUserUseCase;
import com.parazit.panel.application.referral.ReferralAlreadyAssignedException;
import com.parazit.panel.application.referral.command.AssignReferralCommand;
import com.parazit.panel.application.referral.result.AssignReferralResult;
import com.parazit.panel.application.user.command.RegisterUserCommand;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "spring.security.user.name=test",
        "spring.security.user.password=test"
})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ConcurrentReferralAssignmentIntegrationTest extends PostgreSqlContainerSupport {


    private final RegisterUserUseCase registerUserUseCase;
    private final AssignReferralUseCase assignReferralUseCase;
    private final JdbcTemplate jdbcTemplate;

    ConcurrentReferralAssignmentIntegrationTest(
            RegisterUserUseCase registerUserUseCase,
            AssignReferralUseCase assignReferralUseCase,
            JdbcTemplate jdbcTemplate
    ) {
        this.registerUserUseCase = registerUserUseCase;
        this.assignReferralUseCase = assignReferralUseCase;
        this.jdbcTemplate = jdbcTemplate;
    }


    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanUserModuleTables(jdbcTemplate);
    }

    @Test
    void sameReferrerConcurrentAssignmentReturnsSameReferral() throws Exception {
        register(8101L);
        register(8102L);
        String code = referralCodeFor(8101L);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        try {
            Future<AssignReferralResult> first = executorService.submit(() -> assignAfterBarrier(barrier, 8102L, code));
            Future<AssignReferralResult> second = executorService.submit(() -> assignAfterBarrier(barrier, 8102L, code));

            AssignReferralResult firstResult = first.get(10, TimeUnit.SECONDS);
            AssignReferralResult secondResult = second.get(10, TimeUnit.SECONDS);

            assertThat(firstResult.referralId()).isEqualTo(secondResult.referralId());
            assertThat(List.of(firstResult.newlyAssigned(), secondResult.newlyAssigned()))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(referralRowCount()).isEqualTo(1);
        } finally {
            executorService.shutdownNow();
            assertThat(executorService.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void differentReferrerConcurrentAssignmentCreatesOneReferralAndOneConflict() throws Exception {
        register(8201L);
        register(8202L);
        register(8203L);
        String firstCode = referralCodeFor(8201L);
        String secondCode = referralCodeFor(8202L);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        try {
            Future<Object> first = executorService.submit(() -> assignObjectAfterBarrier(barrier, 8203L, firstCode));
            Future<Object> second = executorService.submit(() -> assignObjectAfterBarrier(barrier, 8203L, secondCode));

            List<Object> results = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));

            assertThat(results.stream().filter(AssignReferralResult.class::isInstance)).hasSize(1);
            assertThat(results.stream().filter(ReferralAlreadyAssignedException.class::isInstance)).hasSize(1);
            assertThat(referralRowCount()).isEqualTo(1);
        } finally {
            executorService.shutdownNow();
            assertThat(executorService.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private AssignReferralResult assignAfterBarrier(CyclicBarrier barrier, Long telegramUserId, String code) throws Exception {
        barrier.await(5, TimeUnit.SECONDS);
        return assignReferralUseCase.assign(new AssignReferralCommand(telegramUserId, code));
    }

    private Object assignObjectAfterBarrier(CyclicBarrier barrier, Long telegramUserId, String code) throws Exception {
        try {
            return assignAfterBarrier(barrier, telegramUserId, code);
        } catch (ReferralAlreadyAssignedException exception) {
            return exception;
        }
    }

    private void register(Long telegramUserId) {
        registerUserUseCase.register(new RegisterUserCommand(telegramUserId, null, "Ali", null, "fa"));
    }

    private String referralCodeFor(Long telegramUserId) {
        return jdbcTemplate.queryForObject(
                "SELECT referral_code FROM users WHERE telegram_user_id = ?",
                String.class,
                telegramUserId
        );
    }

    private long referralRowCount() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM referrals", Long.class);
        return count == null ? 0 : count;
    }
}
