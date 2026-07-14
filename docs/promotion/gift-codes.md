# Gift Codes

Gift codes credit the customer wallet through the immutable wallet ledger.

```mermaid
flowchart TD
  A[Wallet] --> B[Enter gift code]
  B --> C[Normalize and hash code]
  C --> D[Lock gift code and wallet]
  D --> E{Eligible?}
  E -- no --> F[Safe rejection]
  E -- yes --> G[Create redemption]
  G --> H[Credit wallet]
  H --> I[GIFT_CODE ledger row]
  I --> J[Show updated balance]
```

Rules:

- Gift codes never create orders or payments.
- Wallet credit goes through `CreditWalletUseCase`.
- The ledger transaction type is `GIFT_CODE`.
- The ledger reference type is `GIFT_CODE_REDEMPTION`.
- One redemption produces at most one wallet credit.

