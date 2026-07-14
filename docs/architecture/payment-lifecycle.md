# Payment Lifecycle

Payment approval converges in `PaymentApprovalService`.

```mermaid
flowchart TD
    A[Manual receipt approval] --> C[PaymentApprovalService]
    B[Zarinpal verification] --> C
    C --> D[Mark Payment APPROVED]
    D --> E[Mark Order paid]
    E --> F[OrderPaymentApprovedDispatcher]
    F -->|NEW_SUBSCRIPTION| G[Provisioning outbox]
    F -->|RENEWAL| H[Renewal outbox]
```

Provider callbacks are verified by provider-specific code before approval. The approval service does not trust callback-carried amount, plan, subscription, or renewal data.
