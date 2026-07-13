# Telegram Subscription Delivery

Task 35 supports:

- `/start`
- `/menu`
- `/subscriptions`
- `/help`
- main menu
- subscription list
- subscription metadata
- individual VLESS text delivery
- VLESS QR delivery
- explicit token rotation before subscription URL and subscription QR delivery

It does not implement purchases, payment initiation, receipt upload, operator review, referrals, reminders, admin commands, or broadcasts.

```mermaid
flowchart TD
    Start[/start] --> Menu[Main menu]
    Menu --> Subs[My subscriptions]
    Subs --> Details[Subscription details]
    Details --> Config[VLESS text]
    Details --> ConfigQr[VLESS QR]
    Details --> LinkWarn[Generate new link warning]
    LinkWarn --> Confirm[Confirm rotation]
    Confirm --> NewToken[Rotate token once]
    NewToken --> Url[Send subscription URL]
    NewToken --> UrlQr[Send subscription URL QR]
```

VLESS text and QR payloads are sensitive client credentials. The bot sends them only in private chats and does not store the URI or QR bytes.

If a subscription has multiple config entries, callbacks use a one-based config index and the subscription module validates ownership and state for every action.

