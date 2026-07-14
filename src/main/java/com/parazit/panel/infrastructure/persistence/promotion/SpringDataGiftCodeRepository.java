package com.parazit.panel.infrastructure.persistence.promotion;

import com.parazit.panel.domain.promotion.GiftCode;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataGiftCodeRepository extends SpringDataUuidRepository<GiftCode> {

    Optional<GiftCode> findByCodeHash(String codeHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GiftCode g where g.codeHash = :codeHash")
    Optional<GiftCode> findByCodeHashForUpdate(@Param("codeHash") String codeHash);
}
