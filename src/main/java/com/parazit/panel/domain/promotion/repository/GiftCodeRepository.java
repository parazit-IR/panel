package com.parazit.panel.domain.promotion.repository;

import com.parazit.panel.domain.promotion.GiftCode;
import com.parazit.panel.domain.repository.UuidRepository;
import java.util.Optional;

public interface GiftCodeRepository extends UuidRepository<GiftCode> {

    Optional<GiftCode> findByCodeHash(String codeHash);

    Optional<GiftCode> findByCodeHashForUpdate(String codeHash);
}
