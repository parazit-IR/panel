# Manual Card Payment

Task 29 adds manual card-to-card payment instructions. This is not a bank integration and it does not approve a payment.

## Purpose

Manual card payment gives a user a destination card and an exact payable amount. A temporary suffix is added to the original Payment amount so operators can later match transfers more easily.

Example:

- Base amount: `100000 IRT`
- Unique suffix: `1638 IRT`
- Payable amount: `101638 IRT`

The unique amount is only a matching aid. It is not proof of payment.

## Provider Method

The project uses the existing `PaymentMethod.CARD_TO_CARD` enum value. No second `CARD_TO_CARD_MANUAL` value is introduced.

Manual payment requires user transfer, receipt submission, operator review, and explicit approval in a later task. Task 29 never marks a Payment as `APPROVED`. Task 30 adds receipt submission and review queueing, but still does not approve a Payment.

## Instruction Lifecycle

`ManualCardPaymentInstruction` stores instruction history with the Payment ID, request ID, amount snapshot, destination snapshots, masked card number, and lifecycle timestamps.

```mermaid
stateDiagram-v2
    [*] --> CREATED
    CREATED --> ACTIVE
    ACTIVE --> EXPIRED
    ACTIVE --> CANCELLED
    ACTIVE --> RECEIPT_PENDING: Task 30
    RECEIPT_PENDING --> COMPLETED: Task 31
```

Task 29 uses `CREATED`, `ACTIVE`, `EXPIRED`, and `CANCELLED`. Task 30 uses `RECEIPT_PENDING` after a valid receipt is stored and queued. `COMPLETED` remains reserved for the future approval workflow.

## Payment Lifecycle

Creating an active instruction moves the Payment to `WAITING_FOR_PAYMENT`. A successful receipt upload moves it through `RECEIPT_SUBMITTED` to `WAITING_FOR_REVIEW`. Instruction expiry or cancellation does not approve, reject, or fail the Payment. The Payment remains reusable until its own expiry and lifecycle rules prevent it.

## Destination Card Security

The active destination is configuration-backed:

- `MANUAL_CARD_DESTINATION_ID`
- `MANUAL_CARD_BANK_NAME`
- `MANUAL_CARD_HOLDER_NAME`
- `MANUAL_CARD_NUMBER`

The full card number is not persisted in the instruction table. Only a masked snapshot such as `6037-****-****-0014` is stored. The full card number can be returned only by the controlled internal instruction endpoint from configuration at response time.

No CVV2, PIN, OTP, internet-bank credential, or receipt data is stored.

## Amount Reservation

The database is the correctness authority. PostgreSQL partial unique indexes enforce one active instruction per Payment and one active `(currency, payable_amount)` reservation.

Active reservation statuses are `CREATED`, `ACTIVE`, and `RECEIPT_PENDING`. During receipt review the reservation remains held. Expired and cancelled instructions retain history but release the active reservation.

```mermaid
sequenceDiagram
    participant API
    participant Service
    participant DB
    API->>Service: initialize(paymentId, requestId)
    Service->>Service: generate suffix
    Service->>DB: insert ACTIVE instruction
    alt amount free
        DB-->>Service: saved
    else active amount conflict
        DB-->>Service: unique violation
        Service->>Service: generate another suffix
        Service->>DB: retry bounded insert
    end
```

## Idempotency

`instructionRequestId` is globally unique. Same request ID and same Payment returns the same instruction. Same request ID and another Payment returns conflict. A new request ID while a valid active instruction exists returns the existing instruction.

```mermaid
sequenceDiagram
    participant Client
    participant API
    participant DB
    Client->>API: initialize(requestId=A)
    API->>DB: create instruction
    DB-->>API: instruction X
    Client->>API: initialize(requestId=A)
    API->>DB: find by requestId
    DB-->>API: instruction X
```

## Expiration And Reissue

Instruction TTL is configured by `MANUAL_CARD_PAYMENT_TTL`. Expiration is lazy in Task 29 during initialize, get current instruction, or cancel. Expired instructions are marked `EXPIRED` and remain in history. A new instruction may be issued when reissue is enabled and cooldown has passed.

```mermaid
sequenceDiagram
    participant Client
    participant API
    participant DB
    Client->>API: get current
    API->>DB: load active instruction
    API->>API: now >= expiresAt
    API->>DB: mark EXPIRED
    API-->>Client: 404
    Client->>API: initialize new request
    API->>DB: create new ACTIVE instruction
```

## Concurrency

Concurrent requests are protected by database partial unique indexes, not JVM locks.

```mermaid
sequenceDiagram
    participant A
    participant B
    participant DB
    A->>DB: reserve IRT 101638
    B->>DB: reserve IRT 101638
    DB-->>A: success
    DB-->>B: unique violation
    B->>DB: reserve IRT 101921
    DB-->>B: success
```

## Internal API

- `POST /internal/payments/{paymentId}/manual-card/initialize`
- `GET /internal/payments/{paymentId}/manual-card?telegramUserId=...`
- `POST /internal/payments/{paymentId}/manual-card/cancel`
- `POST /internal/payments/{paymentId}/manual-card/receipt`
- `GET /internal/payments/{paymentId}/manual-card/receipt?telegramUserId=...`
- `GET /internal/admin/manual-payments/review-queue`
- `GET /internal/admin/manual-payments/receipts/{receiptId}/content`

Instruction responses include display information, exact amounts, masked card number, and controlled full card number. Receipt responses include safe file metadata and never expose storage keys or filesystem paths. Error responses must not contain card numbers or receipt storage details.

## Deferred Work

Task 30 does not implement OCR, operator approval or rejection, automatic bank verification, Payment approval, VPN provisioning, Telegram handlers, or reminders.
