# Telegram Payment Flow

Telegram payment screens remain generic. For renewal orders, approval routes through the same payment architecture and then shows renewal-specific status text.

```mermaid
flowchart TD
    A[Payment method selected] --> B[Manual or Zarinpal flow]
    B --> C[Payment approved]
    C --> D{Order type}
    D -->|NEW_SUBSCRIPTION| E[Provisioning queued]
    D -->|RENEWAL| F[Renewal queued]
    F --> G[Telegram renewal status refresh]
```

Telegram callbacks never carry trusted financial values, subscription ownership, provider authority, or renewal payload data.
