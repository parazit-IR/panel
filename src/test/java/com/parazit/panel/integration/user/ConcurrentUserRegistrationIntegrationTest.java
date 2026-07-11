package com.parazit.panel.integration.user;

import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import com.parazit.panel.test.support.DatabaseCleaner;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.port.in.user.RegisterUserUseCase;
import com.parazit.panel.application.user.command.RegisterUserCommand;
import com.parazit.panel.application.user.result.RegisterUserResult;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "spring.security.user.name=test",
        "spring.security.user.password=test"
})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ConcurrentUserRegistrationIntegrationTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-10T12:00:00Z");


    private final RegisterUserUseCase registerUserUseCase;
    private final JdbcTemplate jdbcTemplate;

    ConcurrentUserRegistrationIntegrationTest(
            RegisterUserUseCase registerUserUseCase,
            JdbcTemplate jdbcTemplate
    ) {
        this.registerUserUseCase = registerUserUseCase;
        this.jdbcTemplate = jdbcTemplate;
    }


    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanUserModuleTables(jdbcTemplate);
    }

    @Test
    void concurrentRegistrationsCreateOnlyOneUserAndOneSettingsRow() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        RegisterUserCommand command = new RegisterUserCommand(3001L, "same_user", "Ali", null, "fa");

        try {
            Future<RegisterUserResult> first = executorService.submit(() -> registerAfterBarrier(barrier, command));
            Future<RegisterUserResult> second = executorService.submit(() -> registerAfterBarrier(barrier, command));

            RegisterUserResult firstResult = first.get(10, TimeUnit.SECONDS);
            RegisterUserResult secondResult = second.get(10, TimeUnit.SECONDS);

            assertThat(firstResult.userId()).isEqualTo(secondResult.userId());
            assertThat(List.of(firstResult.newlyCreated(), secondResult.newlyCreated()))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(rowCount()).isEqualTo(1);
            assertThat(settingsRowCount()).isEqualTo(1);
            assertThat(referralCodes()).hasSize(1).first().asString().isNotBlank();
        } finally {
            executorService.shutdownNow();
            assertThat(executorService.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private RegisterUserResult registerAfterBarrier(
            CyclicBarrier barrier,
            RegisterUserCommand command
    ) throws Exception {
        barrier.await(5, TimeUnit.SECONDS);
        return registerUserUseCase.register(command);
    }

    private long rowCount() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        return count == null ? 0 : count;
    }

    private long settingsRowCount() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_settings", Long.class);
        return count == null ? 0 : count;
    }

    private List<String> referralCodes() {
        return jdbcTemplate.queryForList(
                "SELECT referral_code FROM users WHERE telegram_user_id = ?",
                String.class,
                3001L
        );
    }

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
