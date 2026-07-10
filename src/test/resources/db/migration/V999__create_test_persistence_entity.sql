-- Test-only schema used by BaseEntityPersistenceTest.
-- Production migrations must not create test tables.
CREATE TABLE test_persistence_entities (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
