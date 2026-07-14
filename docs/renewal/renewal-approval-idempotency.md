# Renewal Approval Idempotency

Idempotency keys:

- Payment approval: existing provider/manual approval request identity.
- Renewal dispatch: `paymentId + orderId`.
- Renewal outbox: `renewalOrderId + eventType`.
- Customer queued notification: emitted only when the outbox row is newly created.

```mermaid
sequenceDiagram
    participant A as Approval request
    participant P as PaymentApprovalService
    participant R as Renewal handler
    participant DB as PostgreSQL
    A->>P: approve payment
    P->>R: dispatch RENEWAL
    R->>DB: insert renewal_outbox
    DB-->>R: unique row
    A->>P: replay approve payment
    P->>R: dispatch RENEWAL
    R->>DB: find existing row
```

Correct replay result: payment remains approved, order remains renewal-pending, one renewal outbox exists, and no new-subscription provisioning outbox is created.
