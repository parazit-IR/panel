package com.parazit.panel.integration.user.settings;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.port.in.user.RegisterUserUseCase;
import com.parazit.panel.application.port.in.user.settings.GetUserSettingsUseCase;
import com.parazit.panel.application.user.command.RegisterUserCommand;
import com.parazit.panel.application.user.settings.query.GetUserSettingsQuery;
import com.parazit.panel.application.user.settings.result.UserSettingsResult;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.test.support.DatabaseCleaner;
import com.parazit.panel.test.support.PostgreSqlContainerSupport;
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
class ConcurrentUserSettingsIntegrationTest extends PostgreSqlContainerSupport {

    private final RegisterUserUseCase registerUserUseCase;
    private final GetUserSettingsUseCase getUserSettingsUseCase;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    ConcurrentUserSettingsIntegrationTest(
            RegisterUserUseCase registerUserUseCase,
            GetUserSettingsUseCase getUserSettingsUseCase,
            UserRepository userRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.registerUserUseCase = registerUserUseCase;
        this.getUserSettingsUseCase = getUserSettingsUseCase;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanUserModuleTables(jdbcTemplate);
    }

    @Test
    void concurrentSettingsReadsCreateExactlyOneDefaultSettingsRow() throws Exception {
        registerUserUseCase.register(new RegisterUserCommand(9101L, null, "Ali", null, "fa"));
        var user = userRepository.findByTelegramUserId(9101L).orElseThrow();
        jdbcTemplate.update("DELETE FROM user_settings WHERE user_id = ?", user.getId());

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        try {
            Future<UserSettingsResult> first = executorService.submit(() -> getSettingsAfterBarrier(barrier));
            Future<UserSettingsResult> second = executorService.submit(() -> getSettingsAfterBarrier(barrier));

            UserSettingsResult firstResult = first.get(10, TimeUnit.SECONDS);
            UserSettingsResult secondResult = second.get(10, TimeUnit.SECONDS);

            assertThat(firstResult.userId()).isEqualTo(user.getId());
            assertThat(secondResult.userId()).isEqualTo(user.getId());
            assertThat(settingsRowCount(user.getId())).isEqualTo(1);
        } finally {
            executorService.shutdownNow();
            assertThat(executorService.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private UserSettingsResult getSettingsAfterBarrier(CyclicBarrier barrier) throws Exception {
        barrier.await(5, TimeUnit.SECONDS);
        return getUserSettingsUseCase.getSettings(new GetUserSettingsQuery(9101L));
    }

    private long settingsRowCount(Object userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_settings WHERE user_id = ?",
                Long.class,
                userId
        );
        return count == null ? 0 : count;
    }
}
