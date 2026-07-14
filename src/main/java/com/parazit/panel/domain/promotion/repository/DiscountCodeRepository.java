package com.parazit.panel.domain.promotion.repository;

import com.parazit.panel.domain.promotion.DiscountCode;
import com.parazit.panel.domain.repository.UuidRepository;
import java.util.Optional;
import java.util.UUID;

public interface DiscountCodeRepository extends UuidRepository<DiscountCode> {

    Optional<DiscountCode> findByCodeHash(String codeHash);

    Optional<DiscountCode> findByCodeHashForUpdate(String codeHash);
}
