package com.parazit.panel.infrastructure.persistence.telegram;

import com.parazit.panel.domain.telegram.TelegramPollingState;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataRepository;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataTelegramPollingStateRepository
        extends SpringDataRepository<TelegramPollingState, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select state from TelegramPollingState state where state.botIdentity = :botIdentity")
    Optional<TelegramPollingState> findByBotIdentityForUpdate(@Param("botIdentity") String botIdentity);
}
