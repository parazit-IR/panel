# Telegram Main Menu

The customer-facing Telegram bot uses a persistent Persian `ReplyKeyboardMarkup` for global navigation.

```text
[ 🔐 خرید اشتراک ] [ ♻️ تمدید سرویس ]
[ 🛍 سرویس‌های من ] [ 🔑 اکانت تست ]
[ 💵 تعرفه اشتراک‌ها ] [ 💰 کیف پول + شارژ ]
[ 📚 آموزش ]
[ ☎️ پشتیبانی ]
```

Visible text is resolved through localization keys. Handlers use `TelegramMainMenuAction`, not raw Persian text.

Reply keyboard policy:

- Use reply keyboard only for persistent global navigation.
- Use inline keyboard for page actions, service details, config delivery actions, Back, and Home.
- Do not place sensitive one-time confirmations in the persistent keyboard.

Feature behavior:

- Buy and tariffs route to the active plan catalog.
- My services routes to the existing subscription list.
- Renewal, trial, wallet, tutorials, and support remain visible but return localized unavailable messages until those domains are implemented.

```mermaid
flowchart TD
    Start[/start] --> Welcome[Welcome message]
    Welcome --> ReplyKeyboard[Persistent reply keyboard]
    ReplyKeyboard --> Text[Exact menu text]
    Text --> Action[TelegramMainMenuAction]
    Action --> Enabled{Enabled?}
    Enabled -->|yes| Route[Route to existing handler]
    Enabled -->|no| Placeholder[Localized unavailable message]
```
