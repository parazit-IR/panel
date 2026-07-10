package com.parazit.panel.common.persistence.fixture;

import com.parazit.panel.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "test_persistence_entities")
public class TestPersistenceEntity extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    protected TestPersistenceEntity() {
    }

    public TestPersistenceEntity(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
