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

- Buy routes to the existing purchase entry point.
- Tariffs route to a read-only active plan tariff catalog.
- My services routes to the existing subscription list.
- Tutorials route to configured platform instructions and trusted download links.
- Support routes to FAQ and configured Telegram support URL.
- Renewal, trial, and wallet remain visible but return localized unavailable messages until those domains are implemented.

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

```mermaid
flowchart TD
    Tariffs[💵 تعرفه اشتراک‌ها] --> ActivePlans[List active plans]
    Tutorials[📚 آموزش] --> TutorialMenu[Platform tutorial menu]
    Support[☎️ پشتیبانی] --> SupportPage[FAQ and support URL]
```
