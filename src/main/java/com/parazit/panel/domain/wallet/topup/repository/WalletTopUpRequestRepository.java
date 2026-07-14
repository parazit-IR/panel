package com.parazit.panel.domain.wallet.topup.repository;

import com.parazit.panel.domain.repository.UuidRepository;
import com.parazit.panel.domain.wallet.topup.WalletTopUpRequest;
import com.parazit.panel.domain.wallet.topup.WalletTopUpStatus;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface WalletTopUpRequestRepository extends UuidRepository<WalletTopUpRequest> {

    Optional<WalletTopUpRequest> findByIdForUpdate(UUID id);

    Optional<WalletTopUpRequest> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

    Optional<WalletTopUpRequest> findByPaymentId(UUID paymentId);

    long countByUserIdAndStatusIn(UUID userId, Collection<WalletTopUpStatus> statuses);
}
