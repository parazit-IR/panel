# Renewal Retry And Recovery

The renewal outbox stores `execution_step` and a versioned deterministic target payload. Retries reuse the same absolute expiry and traffic values, so a renewal is not extended twice.

```mermaid
flowchart TD
  A[Remote timeout] --> B[Classify transient]
  B --> C[Status FAILED]
  C --> D[available_at = now + backoff]
  D --> E[Retry later]
  E --> F[Reload target payload]
```

Permanent failures such as missing remote client or identity mismatch move the order to renewal review and do not create a replacement client.
