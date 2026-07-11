package com.parazit.panel.integration.referral;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.port.in.referral.AssignReferralUseCase;
import com.parazit.panel.application.port.in.referral.GetReferralOverviewUseCase;
import com.parazit.panel.application.port.in.user.RegisterUserUseCase;
import com.parazit.panel.application.referral.ReferralAlreadyAssignedException;
import com.parazit.panel.application.referral.ReferralCodeNotFoundException;
import com.parazit.panel.application.referral.SelfReferralNotAllowedException;
import com.parazit.panel.application.referral.command.AssignReferralCommand;
import com.parazit.panel.application.referral.query.GetReferralOverviewQuery;
import com.parazit.panel.application.referral.result.AssignReferralResult;
import com.parazit.panel.application.referral.result.ReferralOverviewResult;
import com.parazit.panel.application.user.command.RegisterUserCommand;
import com.parazit.panel.domain.referral.ReferralStatus;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.user.settings.repository.UserSettingsRepository;
import com.parazit.panel.test.support.DatabaseCleaner;
import com.parazit.panel.test.support.PostgreSqlContainerSupport;
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
class ReferralIntegrationTest extends PostgreSqlContainerSupport {

    private final RegisterUserUseCase registerUserUseCase;
    private final GetReferralOverviewUseCase getReferralOverviewUseCase;
    private final AssignReferralUseCase assignReferralUseCase;
    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final JdbcTemplate jdbcTemplate;

    ReferralIntegrationTest(
            RegisterUserUseCase registerUserUseCase,
            GetReferralOverviewUseCase getReferralOverviewUseCase,
            AssignReferralUseCase assignReferralUseCase,
            UserRepository userRepository,
            UserSettingsRepository userSettingsRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.registerUserUseCase = registerUserUseCase;
        this.getReferralOverviewUseCase = getReferralOverviewUseCase;
        this.assignReferralUseCase = assignReferralUseCase;
        this.userRepository = userRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanUserModuleTables(jdbcTemplate);
    }

    @Test
    void assignsReferralIdempotentlyAndPreservesUserState() {
        register(9201L, "fa");
        register(9202L, "en");
        register(9203L, "fa");
        String firstCode = userRepository.findByTelegramUserId(9201L).orElseThrow().getReferralCode();
        String secondCode = userRepository.findByTelegramUserId(9203L).orElseThrow().getReferralCode();
        var referredBefore = userRepository.findByTelegramUserId(9202L).orElseThrow();
        var settingsBefore = userSettingsRepository.findByUserId(referredBefore.getId()).orElseThrow();

        AssignReferralResult first = assignReferralUseCase.assign(new AssignReferralCommand(9202L, firstCode.toLowerCase()));
        AssignReferralResult repeated = assignReferralUseCase.assign(new AssignReferralCommand(9202L, firstCode));
        ReferralOverviewResult referrerOverview = getReferralOverviewUseCase.getOverview(new GetReferralOverviewQuery(9201L));
        ReferralOverviewResult referredOverview = getReferralOverviewUseCase.getOverview(new GetReferralOverviewQuery(9202L));

        assertThat(first.status()).isEqualTo(ReferralStatus.PENDING);
        assertThat(first.newlyAssigned()).isTrue();
        assertThat(repeated.referralId()).isEqualTo(first.referralId());
        assertThat(repeated.newlyAssigned()).isFalse();
        assertThat(referrerOverview.referralCount()).isEqualTo(1);
        assertThat(referredOverview.referrerUserId()).isEqualTo(first.referrerUserId());
        assertThat(referredOverview.referrerTelegramUserId()).isEqualTo(9201L);
        assertThat(referralRowCount()).isEqualTo(1);

        var referredAfter = userRepository.findByTelegramUserId(9202L).orElseThrow();
        var settingsAfter = userSettingsRepository.findByUserId(referredAfter.getId()).orElseThrow();
        assertThat(referredAfter.getLanguage()).isEqualTo(UserLanguage.EN);
        assertThat(referredAfter.getUsername()).isNull();
        assertThat(settingsAfter.getId()).isEqualTo(settingsBefore.getId());
        assertThat(settingsAfter.isNotificationsEnabled()).isEqualTo(settingsBefore.isNotificationsEnabled());

        assertThatThrownBy(() -> assignReferralUseCase.assign(new AssignReferralCommand(9202L, secondCode)))
                .isInstanceOf(ReferralAlreadyAssignedException.class);
        assertThatThrownBy(() -> assignReferralUseCase.assign(new AssignReferralCommand(9201L, firstCode)))
                .isInstanceOf(SelfReferralNotAllowedException.class);
        assertThatThrownBy(() -> assignReferralUseCase.assign(new AssignReferralCommand(9203L, "ABCDEFGH")))
                .isInstanceOf(ReferralCodeNotFoundException.class);
        assertThat(referralRowCount()).isEqualTo(1);
    }

    private void register(Long telegramUserId, String languageCode) {
        registerUserUseCase.register(new RegisterUserCommand(telegramUserId, null, "Ali", null, languageCode));
    }

    private long referralRowCount() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM referrals", Long.class);
        return count == null ? 0 : count;
    }
}
