# Telegram Bot Architecture

Task 35 implements long polling as the only active update delivery mode. Webhook support is deferred.

```mermaid
sequenceDiagram
    participant TG as Telegram
    participant W as Long polling worker
    participant P as Update processor
    participant H as Handler
    participant C as Telegram client
    TG->>W: getUpdates(offset)
    W->>P: process update
    P->>P: claim update in short transaction
    P->>H: route command/callback
    H->>P: response plan
    P->>C: send message/photo
    P->>P: mark processed in short transaction
    W->>W: advance offset
```

The application and domain layers use internal Telegram models only. Telegram HTTP DTOs and `RestClient` stay in infrastructure.

## Task 41 Menu Routing

The persistent main menu is a Telegram application concern. Domain services do not depend on menu actions, page models, or keyboard classes.

```mermaid
sequenceDiagram
    participant TG as Telegram
    participant P as Update processor
    participant R as Text router
    participant M as Main menu handler
    participant U as Use case
    TG->>P: Persian reply keyboard text
    P->>R: normalize and exact-match
    R-->>P: TelegramMainMenuAction
    P->>M: route action
    M->>U: call existing use case when implemented
    M-->>TG: message with reply or inline keyboard
```

## Idempotency

`telegram_processed_updates` stores one row per Telegram `update_id`. A processed update is terminal and replay does not resend messages. Failed updates can be retried until the configured attempt limit, then become `DEAD`.

```mermaid
stateDiagram-v2
    [*] --> RECEIVED
    RECEIVED --> PROCESSING
    FAILED --> PROCESSING
    PROCESSING --> PROCESSED
    PROCESSING --> FAILED
    FAILED --> DEAD
```

Telegram send operations do not provide a general caller idempotency key. A timeout after Telegram accepts a message is an uncertain result, so exactly-once outbound delivery is not claimed.

## Transactions

Update claim, completion, failure, polling offset updates, and sensitive-action completion are short database transactions. Telegram HTTP calls, QR rendering, and long-poll waits happen outside those transactions.
