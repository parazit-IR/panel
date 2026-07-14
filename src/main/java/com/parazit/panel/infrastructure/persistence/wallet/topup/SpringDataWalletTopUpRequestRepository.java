package com.parazit.panel.infrastructure.persistence.wallet.topup;

import com.parazit.panel.domain.wallet.topup.WalletTopUpRequest;
import com.parazit.panel.domain.wallet.topup.WalletTopUpStatus;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataWalletTopUpRequestRepository extends SpringDataUuidRepository<WalletTopUpRequest> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from WalletTopUpRequest r where r.id = :id")
    Optional<WalletTopUpRequest> findByIdForUpdate(@Param("id") UUID id);

    Optional<WalletTopUpRequest> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

    Optional<WalletTopUpRequest> findByPaymentId(UUID paymentId);

    long countByUserIdAndStatusIn(UUID userId, Collection<WalletTopUpStatus> statuses);
}
