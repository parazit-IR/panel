package com.parazit.panel.infrastructure.persistence.repository.fixture;

import com.parazit.panel.common.persistence.fixture.TestPersistenceEntity;
import com.parazit.panel.common.persistence.fixture.TestPersistenceRepository;
import com.parazit.panel.infrastructure.persistence.repository.JpaRepositoryAdapter;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class TestPersistenceRepositoryAdapter
        extends JpaRepositoryAdapter<TestPersistenceEntity, UUID>
        implements TestPersistenceRepository {

    public TestPersistenceRepositoryAdapter(TestPersistenceSpringDataRepository repository) {
        super(repository);
    }
}
