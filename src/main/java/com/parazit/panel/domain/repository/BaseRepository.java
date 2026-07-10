package com.parazit.panel.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BaseRepository<T, ID> {

    Optional<T> findById(ID id);

    List<T> findAll();

    T save(T entity);

    List<T> saveAll(Collection<T> entities);

    boolean existsById(ID id);

    long count();

    void delete(T entity);

    void deleteById(ID id);
}
