# Dependency Injection

## Rules

- Use constructor injection only.
- Store dependencies in `final` fields.
- Do not use `@Autowired` on a constructor when the class has one constructor.
- Do not use field injection.
- Do not use setter injection except for framework-required cases.
- Do not use `ApplicationContext.getBean` as a service locator.
- Do not use static mutable state for dependency access.

## Dependency Direction

```text
API -> Application -> Domain interfaces

Infrastructure -> Domain/Application ports

Configuration -> wires implementations to interfaces
```

Domain classes must remain Spring-free. Application services may depend on
domain interfaces and application ports. Infrastructure adapters may implement
domain or application ports. API controllers depend on application services, not
infrastructure classes.

## Ports And Adapters

Ports define what the application needs. Adapters implement those ports using a
specific technology.

Example in this project:

```text
SystemInfoService -> SystemClockPort -> SystemClockAdapter -> Clock
```

`SystemClockPort` is an application port. `SystemClockAdapter` is infrastructure.
`ClockConfiguration` wires the adapter to the port with explicit `@Bean`
definitions.

## Stereotypes

Use `@Service` for application services that express an application operation.
Use `@Component` sparingly for framework integrations such as filters. Use
`@Repository` for concrete persistence adapters when Spring should discover
them. Prefer explicit `@Bean` methods for infrastructure adapters when wiring an
interface to an implementation is clearer.

## Optional Integrations

Optional modules such as Telegram, Panel, and Payment use enable flags:

```text
app.telegram.bot.enabled
app.panel.enabled
app.payment.enabled
```

Future module configuration should use `@ConditionalOnProperty` so disabled
modules do not require credentials or client beans.

## Tests

Infrastructure dependencies should be replaceable in tests. Use test-specific
configuration with `@TestConfiguration` and `@Primary` when a bean such as
`Clock` must be deterministic.

Do not enable global bean overriding to make tests pass.

## Circular Dependencies

Do not enable circular references and do not use `@Lazy` to hide cycles. If a
cycle appears, split responsibilities or introduce a narrower port.

Avoid designs such as:

```text
Controller -> Service -> Adapter -> Controller
Service A -> Service B -> Service A
```

Circular dependencies are design problems and should be fixed at the dependency
direction level.
