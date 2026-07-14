package com.parazit.panel.infrastructure.persistence.promotion;

import com.parazit.panel.domain.promotion.GiftCode;
import com.parazit.panel.domain.promotion.repository.GiftCodeRepository;
import com.parazit.panel.infrastructure.persistence.repository.JpaRepositoryAdapter;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class GiftCodeRepositoryAdapter extends JpaRepositoryAdapter<GiftCode, UUID> implements GiftCodeRepository {

    private final SpringDataGiftCodeRepository repository;

    public GiftCodeRepositoryAdapter(SpringDataGiftCodeRepository repository) {
        super(repository);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public GiftCode save(GiftCode entity) {
        return repository.saveAndFlush(Objects.requireNonNull(entity, "entity must not be null"));
    }

    @Override
    public Optional<GiftCode> findByCodeHash(String codeHash) {
        return repository.findByCodeHash(Objects.requireNonNull(codeHash, "codeHash must not be null"));
    }

    @Override
    public Optional<GiftCode> findByCodeHashForUpdate(String codeHash) {
        return repository.findByCodeHashForUpdate(Objects.requireNonNull(codeHash, "codeHash must not be null"));
    }
}
