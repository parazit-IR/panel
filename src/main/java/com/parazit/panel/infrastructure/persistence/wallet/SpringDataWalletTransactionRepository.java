package com.parazit.panel.infrastructure.persistence.wallet;

import com.parazit.panel.domain.wallet.WalletTransaction;
import com.parazit.panel.domain.wallet.WalletTransactionDirection;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataWalletTransactionRepository extends SpringDataUuidRepository<WalletTransaction> {

    Optional<WalletTransaction> findByWalletIdAndIdempotencyKey(UUID walletId, String idempotencyKey);

    List<WalletTransaction> findAllByWalletIdOrderByOccurredAtDescIdDesc(UUID walletId, Pageable pageable);

    long countByWalletId(UUID walletId);

    @Query("select max(t.occurredAt) from WalletTransaction t where t.walletId = :walletId")
    Optional<Instant> findLastOccurredAtByWalletId(@Param("walletId") UUID walletId);

    @Query("select coalesce(sum(t.amount), 0) from WalletTransaction t where t.walletId = :walletId and t.direction = :direction")
    long sumByWalletIdAndDirection(@Param("walletId") UUID walletId, @Param("direction") WalletTransactionDirection direction);
}
