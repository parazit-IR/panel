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
    C -->|WALLET_TOP_UP| I[Wallet top-up credit]
    I --> J[WalletTransaction TOP_UP]
```

Provider callbacks are verified by provider-specific code before approval. The approval service does not trust callback-carried amount, plan, subscription, or renewal data.

Task 49 extends `Payment` with a typed target:

- `ORDER` payments keep the existing `order_id` path.
- `WALLET_TOP_UP` payments reference `wallet_top_up_request_id` and never create an `Order`.

Wallet top-up approval credits the wallet through the wallet ledger use case with an idempotency key derived from the top-up request.
