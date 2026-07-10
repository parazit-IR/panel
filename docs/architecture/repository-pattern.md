# Repository Pattern

## Purpose

The repository abstraction keeps application and domain code independent from
Spring Data JPA. Domain-facing code depends on small repository interfaces, while
infrastructure code adapts those interfaces to Spring Data repositories.

## Domain Repository

Domain repositories live under:

```text
com.parazit.panel.domain.repository
```

`BaseRepository<T, ID>` defines the small set of persistence operations that are
currently justified by the project. `UuidRepository<T>` specializes it for the
project-wide UUID identifier strategy.

These interfaces do not extend Spring Data types.

## Spring Data Repository

Spring Data repositories live under infrastructure:

```text
com.parazit.panel.infrastructure.persistence.repository
```

`SpringDataRepository<T, ID>` and `SpringDataUuidRepository<T>` are internal
adapter-side abstractions. They extend `JpaRepository` and are marked with
`@NoRepositoryBean`.

Application and domain packages must not depend on these interfaces.

## JpaRepositoryAdapter

`JpaRepositoryAdapter<T, ID>` implements the domain repository abstraction and
delegates to the matching Spring Data repository. It performs basic null checks
and then lets Spring Data handle persistence and exception translation.

The generic adapter is not a Spring bean by itself. Concrete adapters for future
business repositories should extend it and be registered as infrastructure beans.

## Transaction Boundaries

Repository adapters do not define broad business transactions. Transaction
boundaries belong primarily in application services. Repository calls rely on
Spring Data repository transaction behavior.

Read-only transaction tuning can be added later where a real use case requires
it, but it should not be applied broadly without measurement or need.

## Null Handling

Repository adapters reject null IDs, null entities, null collections, and null
collection elements with `NullPointerException` and clear messages.

Repository methods never return null. `findById` uses `Optional`, and collection
methods return lists.

## Business-Specific Repositories

Create a business-specific repository when the domain or application layer has a
real persistence need for an entity. Do not create repositories speculatively.

Future example:

```java
package com.parazit.panel.domain.repository;

public interface UserRepository extends UuidRepository<User> {
}
```

Infrastructure side:

```java
package com.parazit.panel.infrastructure.persistence.repository;

interface SpringDataUserRepository extends SpringDataUuidRepository<User> {
}
```

Concrete adapter:

```java
package com.parazit.panel.infrastructure.persistence.repository;

@Repository
class UserRepositoryAdapter extends JpaRepositoryAdapter<User, UUID>
        implements UserRepository {

    UserRepositoryAdapter(SpringDataUserRepository repository) {
        super(repository);
    }
}
```

## Keep Generic Repositories Small

The generic repository should remain intentionally small. Pagination, sorting,
specifications, projections, and query-by-example should be added only when a
specific business case needs them.
