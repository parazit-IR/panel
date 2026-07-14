package com.parazit.panel.infrastructure.persistence.promotion;

import com.parazit.panel.domain.promotion.DiscountCode;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataDiscountCodeRepository extends SpringDataUuidRepository<DiscountCode> {

    Optional<DiscountCode> findByCodeHash(String codeHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DiscountCode d where d.codeHash = :codeHash")
    Optional<DiscountCode> findByCodeHashForUpdate(@Param("codeHash") String codeHash);
}
