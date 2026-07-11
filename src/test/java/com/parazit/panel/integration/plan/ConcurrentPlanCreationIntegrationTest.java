package com.parazit.panel.integration.plan;

import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import com.parazit.panel.test.support.DatabaseCleaner;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.plan.admin.PlanCodeAlreadyExistsException;
import com.parazit.panel.application.plan.admin.command.CreatePlanCommand;
import com.parazit.panel.application.plan.result.PlanResult;
import com.parazit.panel.application.port.in.plan.admin.CreatePlanUseCase;
import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.plan.PlanType;
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
class ConcurrentPlanCreationIntegrationTest extends PostgreSqlContainerSupport {

    private final CreatePlanUseCase createPlanUseCase;
    private final JdbcTemplate jdbcTemplate;

    ConcurrentPlanCreationIntegrationTest(CreatePlanUseCase createPlanUseCase, JdbcTemplate jdbcTemplate) {
        this.createPlanUseCase = createPlanUseCase;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPlanTables(jdbcTemplate);
    }

    @Test
    void concurrentCreateWithSameNormalizedCodeCreatesOnePlanAndOneConflict() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        try {
            Future<Object> first = executorService.submit(() -> createAfterBarrier(barrier, "monthly_unlimited"));
            Future<Object> second = executorService.submit(() -> createAfterBarrier(barrier, " MONTHLY_UNLIMITED "));

            List<Object> results = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));

            assertThat(results.stream().filter(PlanResult.class::isInstance)).hasSize(1);
            assertThat(results.stream().filter(PlanCodeAlreadyExistsException.class::isInstance)).hasSize(1);
            assertThat(planRowCount()).isEqualTo(1);
            assertThat(codeRowCount("MONTHLY_UNLIMITED")).isEqualTo(1);
        } finally {
            executorService.shutdownNow();
            assertThat(executorService.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private Object createAfterBarrier(CyclicBarrier barrier, String code) throws Exception {
        barrier.await(5, TimeUnit.SECONDS);
        try {
            return createPlanUseCase.create(new CreatePlanCommand(
                    code,
                    "Monthly Unlimited",
                    null,
                    PlanType.UNLIMITED,
                    900_000L,
                    CurrencyCode.IRT,
                    30,
                    null,
                    null,
                    1
            ));
        } catch (PlanCodeAlreadyExistsException exception) {
            return exception;
        }
    }

    private long planRowCount() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM plans", Long.class);
        return count == null ? 0 : count;
    }

    private long codeRowCount(String code) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM plans WHERE code = ?", Long.class, code);
        return count == null ? 0 : count;
    }
}
