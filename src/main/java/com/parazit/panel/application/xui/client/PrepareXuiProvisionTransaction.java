package com.parazit.panel.application.xui.client;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.port.out.xui.XuiClientIdGenerator;
import com.parazit.panel.application.port.out.xui.XuiRemoteEmailGenerator;
import com.parazit.panel.application.port.out.xui.XuiSubscriptionIdGenerator;
import com.parazit.panel.application.user.UserNotFoundException;
import com.parazit.panel.domain.plan.PlanType;
import com.parazit.panel.domain.plan.selection.PlanSelection;
import com.parazit.panel.domain.plan.selection.PlanSelectionStatus;
import com.parazit.panel.domain.plan.selection.repository.PlanSelectionRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserStatus;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import com.parazit.panel.domain.xui.provisioning.XuiProvisionStatus;
import com.parazit.panel.domain.xui.provisioning.repository.XuiClientProvisionRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PrepareXuiProvisionTransaction {

    private final UserRepository userRepository;
    private final PlanSelectionRepository planSelectionRepository;
    private final XuiClientProvisionRepository provisionRepository;
    private final XuiClientIdGenerator clientIdGenerator;
    private final XuiRemoteEmailGenerator emailGenerator;
    private final XuiSubscriptionIdGenerator subscriptionIdGenerator;
    private final SystemClockPort clock;

    public PrepareXuiProvisionTransaction(
            UserRepository userRepository,
            PlanSelectionRepository planSelectionRepository,
            XuiClientProvisionRepository provisionRepository,
            XuiClientIdGenerator clientIdGenerator,
            XuiRemoteEmailGenerator emailGenerator,
            XuiSubscriptionIdGenerator subscriptionIdGenerator,
            SystemClockPort clock
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.planSelectionRepository = Objects.requireNonNull(planSelectionRepository, "planSelectionRepository must not be null");
        this.provisionRepository = Objects.requireNonNull(provisionRepository, "provisionRepository must not be null");
        this.clientIdGenerator = Objects.requireNonNull(clientIdGenerator, "clientIdGenerator must not be null");
        this.emailGenerator = Objects.requireNonNull(emailGenerator, "emailGenerator must not be null");
        this.subscriptionIdGenerator = Objects.requireNonNull(subscriptionIdGenerator, "subscriptionIdGenerator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public PreparedXuiProvision prepare(Long telegramUserId, UUID planSelectionId, long inboundId) {
        Instant now = clock.now();
        User user = userRepository.findByTelegramUserId(telegramUserId)
                .orElseThrow(() -> new UserNotFoundException(telegramUserId));
        verifyUser(user);
        XuiClientProvision existing = provisionRepository.findByPlanSelectionId(planSelectionId).orElse(null);
        if (existing != null) {
            return new PreparedXuiProvision(existing, false);
        }

        PlanSelection selection = planSelectionRepository.findById(planSelectionId)
                .orElseThrow(() -> new XuiClientProvisionNotAllowedException("Plan selection not found"));
        verifySelection(user, selection, now);
        String remoteClientId = clientIdGenerator.generateClientId();
        UUID stableProvisionKey = UUID.nameUUIDFromBytes((planSelectionId + ":" + remoteClientId).getBytes(StandardCharsets.UTF_8));
        String remoteEmail = emailGenerator.generate(user.getId(), stableProvisionKey);
        XuiClientProvision provision = XuiClientProvision.createPending(
                user.getId(),
                selection.getPlanId(),
                selection.getId(),
                inboundId,
                remoteClientId,
                remoteEmail,
                subscriptionIdGenerator.generate(),
                trafficLimit(selection),
                now.plusSeconds(selection.getDurationDaysSnapshot() * 86_400L),
                selection.getMaxDevicesSnapshot() == null ? 0 : selection.getMaxDevicesSnapshot(),
                now
        );
        try {
            return new PreparedXuiProvision(provisionRepository.save(provision), true);
        } catch (DataIntegrityViolationException exception) {
            return new PreparedXuiProvision(
                    provisionRepository.findByPlanSelectionId(planSelectionId)
                            .orElseThrow(() -> exception),
                    false
            );
        }
    }

    private static void verifyUser(User user) {
        if (user.getStatus() != UserStatus.ACTIVE || Boolean.TRUE.equals(user.getBlocked())) {
            throw new XuiClientProvisionNotAllowedException("User is not eligible for Xui provisioning");
        }
    }

    private void verifySelection(User user, PlanSelection selection, Instant now) {
        if (!selection.getUserId().equals(user.getId())) {
            throw new XuiClientProvisionNotAllowedException("Plan selection does not belong to user");
        }
        if (selection.getStatus() != PlanSelectionStatus.ACTIVE) {
            throw new XuiClientProvisionNotAllowedException("Plan selection is not active");
        }
        if (selection.isExpiredAt(now)) {
            selection.expire(now);
            planSelectionRepository.save(selection);
            throw new XuiClientProvisionNotAllowedException("Plan selection is expired");
        }
    }

    private static long trafficLimit(PlanSelection selection) {
        if (selection.getPlanTypeSnapshot() == PlanType.UNLIMITED) {
            return 0;
        }
        return Objects.requireNonNull(selection.getTrafficLimitBytesSnapshot(), "trafficLimitBytesSnapshot must not be null");
    }
}
