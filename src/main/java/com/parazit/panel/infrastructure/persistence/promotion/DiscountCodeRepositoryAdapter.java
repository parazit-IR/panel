package com.parazit.panel.infrastructure.persistence.promotion;

import com.parazit.panel.domain.promotion.DiscountCode;
import com.parazit.panel.domain.promotion.repository.DiscountCodeRepository;
import com.parazit.panel.infrastructure.persistence.repository.JpaRepositoryAdapter;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class DiscountCodeRepositoryAdapter extends JpaRepositoryAdapter<DiscountCode, UUID> implements DiscountCodeRepository {

    private final SpringDataDiscountCodeRepository repository;

    public DiscountCodeRepositoryAdapter(SpringDataDiscountCodeRepository repository) {
        super(repository);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public DiscountCode save(DiscountCode entity) {
        return repository.saveAndFlush(Objects.requireNonNull(entity, "entity must not be null"));
    }

    @Override
    public Optional<DiscountCode> findByCodeHash(String codeHash) {
        return repository.findByCodeHash(Objects.requireNonNull(codeHash, "codeHash must not be null"));
    }

    @Override
    public Optional<DiscountCode> findByCodeHashForUpdate(String codeHash) {
        return repository.findByCodeHashForUpdate(Objects.requireNonNull(codeHash, "codeHash must not be null"));
    }
}
