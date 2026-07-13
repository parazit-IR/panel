package com.parazit.panel.domain.telegram.repository;

import com.parazit.panel.domain.telegram.TelegramPollingState;
import java.util.Optional;

public interface TelegramPollingStateRepository {

    TelegramPollingState save(TelegramPollingState state);

    Optional<TelegramPollingState> findByBotIdentity(String botIdentity);

    Optional<TelegramPollingState> findByBotIdentityForUpdate(String botIdentity);
}
