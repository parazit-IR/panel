# Telegram Navigation

Routing priority:

1. Private-chat validation.
2. Processed-update idempotency claim.
3. Callback query handling.
4. Slash command handling.
5. Exact main-menu text routing.
6. Generic unknown-message fallback.
7. Unsupported update ignore.

Home sends a new main-menu message and restores the persistent reply keyboard. It does not cancel orders, payments, subscriptions, sensitive actions, or plan selections.

Back uses explicit signed callback actions and reloads server-side resources. Callback data remains signed, user-bound, and time-bounded.

Close is modeled as `TelegramNavigationAction.CLOSE`; it is reserved for pages where removing inline controls is safe.

```mermaid
flowchart TD
    Update[Telegram update] --> Private{Private chat?}
    Private -->|no| PrivateOnly[Private-chat message]
    Private -->|yes| Claim[Claim update id]
    Claim --> Callback{Callback?}
    Callback -->|yes| Decode[Decode signed callback]
    Decode --> Page[Reload page/action]
    Callback -->|no| Command{Slash command?}
    Command -->|yes| CommandHandler[Command handler]
    Command -->|no| MenuText{Exact menu label?}
    MenuText -->|yes| MenuHandler[Main menu handler]
    MenuText -->|no| Unknown[Unknown-message fallback]
```

```mermaid
sequenceDiagram
    participant User
    participant Bot
    participant Domain
    User->>Bot: Home
    Bot->>Bot: clear UI navigation only
    Bot-->>User: main menu + reply keyboard
    Note over Domain: orders, payments, subscriptions unchanged
```
