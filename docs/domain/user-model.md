# User Model

## Purpose

`User` represents a Telegram person known to the system. It stores only identity
and lifecycle data needed by the core domain. Registration, authentication,
referral processing, subscription state, payment data, and panel access are
deferred to later tasks.

## External Identity

`telegramUserId` is the required external identity from Telegram. It is unique,
positive, and immutable after creation. The internal identifier remains the UUID
from `BaseEntity`.

## Fields

- `telegramUserId`: required Telegram identity.
- `username`: optional Telegram username.
- `firstName`: required Telegram first name.
- `lastName`: optional Telegram last name.
- `language`: required language preference. Initial values are `FA` and `EN`.
- `status`: required business lifecycle state. Initial values are `ACTIVE`,
  `INACTIVE`, and `SUSPENDED`.
- `blocked`: required operational flag. When `true`, the bot or system must not
  serve the user.
- `lastInteractionAt`: optional timestamp of the last Telegram interaction.
- `createdAt` and `updatedAt`: inherited audited timestamps.

## Normalization

Strings are trimmed before storage. Blank optional values are stored as `null`.
Usernames have a leading `@` removed. `firstName` must remain nonblank.

Current length limits:

- `username`: 64 characters
- `firstName`: 128 characters
- `lastName`: 128 characters

## State Transitions

The entity exposes explicit mutation methods:

- `updateTelegramProfile`
- `changeLanguage`
- `activate`
- `deactivate`
- `suspend`
- `block`
- `unblock`
- `recordInteraction`

There is no generic update method and no DTO is accepted by the entity.

## Blocked Versus Status

`blocked` is an operational flag. If it is `true`, the bot or system must not
serve the user.

`status` is the business lifecycle state, such as active, inactive, or
suspended.

## Repository

The domain repository exposes UUID repository operations plus Telegram identity
lookups:

- `findByTelegramUserId`
- `existsByTelegramUserId`

Spring Data remains hidden in the infrastructure adapter.

## Database Constraints

Flyway creates the `users` table. The schema includes:

- UUID primary key
- unique `telegram_user_id`
- non-null constraints for required fields
- enum check constraints
- positive Telegram ID check
- indexes for Telegram ID lookup, status filtering, and created timestamp access

Hibernate validates the schema but does not create it.

## Deferred Work

Subscription and payment data are not stored on `User` because they belong to
separate aggregates. Telegram handlers, registration flow, authentication,
authorization, referral logic, and profile API endpoints are deferred to Task 10
or later tasks.
