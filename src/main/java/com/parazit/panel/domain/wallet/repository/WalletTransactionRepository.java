package com.parazit.panel.domain.wallet.repository;

import com.parazit.panel.domain.repository.UuidRepository;
import com.parazit.panel.domain.wallet.WalletTransaction;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletTransactionRepository extends UuidRepository<WalletTransaction> {

    Optional<WalletTransaction> findByWalletIdAndIdempotencyKey(UUID walletId, String idempotencyKey);

    List<WalletTransaction> findAllByWalletIdOrderByOccurredAtDesc(UUID walletId, int offset, int limit);

    long countByWalletId(UUID walletId);

    Optional<Instant> findLastOccurredAtByWalletId(UUID walletId);

    long sumCreditsByWalletId(UUID walletId);

    long sumDebitsByWalletId(UUID walletId);
}
