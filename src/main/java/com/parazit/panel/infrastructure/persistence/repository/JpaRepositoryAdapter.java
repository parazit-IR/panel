package com.parazit.panel.infrastructure.persistence.repository;

import com.parazit.panel.domain.repository.BaseRepository;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class JpaRepositoryAdapter<T, ID> implements BaseRepository<T, ID> {

    private final SpringDataRepository<T, ID> repository;

    protected JpaRepositoryAdapter(SpringDataRepository<T, ID> repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Optional<T> findById(ID id) {
        return repository.findById(requireId(id));
    }

    @Override
    public List<T> findAll() {
        return repository.findAll();
    }

    @Override
    public T save(T entity) {
        return repository.save(requireEntity(entity));
    }

    @Override
    public List<T> saveAll(Collection<T> entities) {
        Objects.requireNonNull(entities, "entities must not be null");
        entities.forEach(this::requireEntity);

        return repository.saveAll(entities);
    }

    @Override
    public boolean existsById(ID id) {
        return repository.existsById(requireId(id));
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public void delete(T entity) {
        repository.delete(requireEntity(entity));
    }

    @Override
    public void deleteById(ID id) {
        repository.deleteById(requireId(id));
    }

    private ID requireId(ID id) {
        return Objects.requireNonNull(id, "id must not be null");
    }

    private T requireEntity(T entity) {
        return Objects.requireNonNull(entity, "entity must not be null");
    }
}
