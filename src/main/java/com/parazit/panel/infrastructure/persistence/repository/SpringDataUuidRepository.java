package com.parazit.panel.infrastructure.persistence.repository;

import java.util.UUID;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface SpringDataUuidRepository<T> extends SpringDataRepository<T, UUID> {
}
