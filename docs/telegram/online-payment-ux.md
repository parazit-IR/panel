# Online Payment UX

Online payment reuses the existing Zarinpal initialization flow.

```mermaid
flowchart TD
    A[Select online payment] --> B[Create or reuse payment]
    B --> C[Initialize or replay Zarinpal request]
    C --> D[Show URL button]
    D --> E[Customer opens provider page]
```

The bot displays a URL button for the provider payment page. Callback data never contains the URL or authority.

The message does not expose:

- Merchant ID
- Authority
- Raw provider response
- Internal payment ID

Duplicate callback delivery reuses existing payment/provider request state where the underlying payment flow supports idempotency.
