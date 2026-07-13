package com.parazit.panel.application.telegram;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.config.properties.TelegramBotProperties;
import com.parazit.panel.domain.telegram.TelegramSensitiveAction;
import com.parazit.panel.domain.telegram.TelegramSensitiveActionStatus;
import com.parazit.panel.domain.telegram.TelegramSensitiveActionType;
import com.parazit.panel.domain.telegram.repository.TelegramSensitiveActionRepository;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelegramSensitiveActionService {

    private final TelegramSensitiveActionRepository repository;
    private final SystemClockPort clock;
    private final TelegramBotProperties properties;

    public TelegramSensitiveActionService(
            TelegramSensitiveActionRepository repository,
            SystemClockPort clock,
            TelegramBotProperties properties
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Transactional
    public TelegramSensitiveAction createRotation(long telegramUserId, UUID subscriptionId) {
        TelegramSensitiveAction action = TelegramSensitiveAction.pendingRotation(
                telegramUserId,
                subscriptionId,
                clock.now().plus(properties.callbackTtl())
        );
        return repository.save(action);
    }

    @Transactional
    public Optional<TelegramSensitiveAction> claimRotation(UUID actionId, long telegramUserId, String resultFingerprint) {
        Instant now = clock.now();
        TelegramSensitiveAction action = repository.findByIdForUpdate(actionId)
                .orElseThrow(() -> new IllegalArgumentException("sensitive action not found"));
        if (!Objects.equals(action.getTelegramUserId(), telegramUserId)
                || action.getType() != TelegramSensitiveActionType.ROTATE_SUBSCRIPTION_TOKEN) {
            throw new IllegalArgumentException("sensitive action ownership mismatch");
        }
        if (action.getStatus() == TelegramSensitiveActionStatus.COMPLETED) {
            return Optional.empty();
        }
        action.expire(now);
        if (!action.isPendingAt(now)) {
            repository.save(action);
            return Optional.empty();
        }
        action.complete(now, resultFingerprint);
        return Optional.of(repository.save(action));
    }

    @Transactional
    public void cancel(UUID actionId, long telegramUserId) {
        TelegramSensitiveAction action = repository.findByIdForUpdate(actionId)
                .orElseThrow(() -> new IllegalArgumentException("sensitive action not found"));
        if (!Objects.equals(action.getTelegramUserId(), telegramUserId)) {
            throw new IllegalArgumentException("sensitive action ownership mismatch");
        }
        action.cancel();
        repository.save(action);
    }
}
