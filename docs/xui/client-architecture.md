# Xui Client Architecture

## Purpose

Task 21 adds the infrastructure foundation for outbound Xui HTTP communication. Later tasks add authentication and inbound discovery. It does not implement client creation, subscriptions, VPN provisioning, payments, orders, or Telegram workflows.

## Layers

- `application.port.out.xui.XuiClient` defines the outbound contract used by future application services.
- `infrastructure.xui.RestClientXuiClient` implements the contract with Spring `RestClient`.
- `infrastructure.xui.XuiRequestExecutor` centralizes low-level request execution, cookie propagation, response cookie capture, retry execution, and exception mapping.
- `infrastructure.xui.config.XuiRestClientConfiguration` creates the configured RestClient, retry executor, and exception mapper.
- `infrastructure.xui.session.XuiSessionStore` keeps the current session cookies in memory.
- `infrastructure.xui.retry.XuiRetryExecutor` owns transient retry behavior.
- `infrastructure.xui.exception` contains safe infrastructure exceptions.

No API controller or business workflow calls Xui in Task 21.

## Configuration

Configuration prefix:

```yaml
app:
  xui:
    base-url: http://localhost:2053
    username: ""
    password: ""
    connect-timeout: 5s
    read-timeout: 20s
    max-retries: 3
    retry-delay: 1s
    verify-ssl: true
```

Environment variables:

- `XUI_BASE_URL`
- `XUI_USERNAME`
- `XUI_PASSWORD`
- `XUI_CONNECT_TIMEOUT`
- `XUI_READ_TIMEOUT`
- `XUI_MAX_RETRIES`
- `XUI_RETRY_DELAY`
- `XUI_VERIFY_SSL`

Credentials are not hardcoded in application configuration. Authentication is deferred.

## RestClient

The Xui RestClient uses:

- Apache HttpComponents request factory;
- configured base URL;
- configured connect and read timeouts;
- JSON message conversion through Jackson;
- safe default headers;
- a logging interceptor that logs method, sanitized URL, status, and duration.

The logging interceptor does not log passwords, cookies, authorization values, session identifiers, or request bodies.

## Request Execution

`XuiRequestExecutor` is the low-level helper used by `RestClientXuiClient`.

Responsibilities:

- apply the current cookie header from `XuiSessionStore`;
- capture `Set-Cookie` values after successful responses;
- delegate retries to `XuiRetryExecutor`;
- translate HTTP and transport failures into Xui exceptions.

This keeps session handling out of application services and avoids embedding cookie behavior in global RestClient interceptors.

## Retry Policy

Retries are intentionally narrow.

Retried:

- timeout failures;
- connection failures;
- HTTP `502`;
- HTTP `503`;
- HTTP `504`.

Not retried:

- HTTP `400`;
- HTTP `401`;
- HTTP `403`;
- HTTP `404`;
- validation failures;
- non-transient server responses such as HTTP `500`.

`max-retries` means retry attempts after the first request. For example, `3` allows up to four total attempts.

## Exceptions

The infrastructure exception hierarchy is:

- `XuiException`
- `XuiConnectionException`
- `XuiTimeoutException`
- `XuiAuthenticationException`
- `XuiServerException`
- `XuiClientException`

Messages are safe and do not expose SQL details, raw HTTP client internals, credentials, cookies, or response bodies.

## Session Cookies

`XuiSessionStore` is memory-only and thread-safe.

It can:

- store a named cookie;
- parse and store `Set-Cookie` headers;
- provide the current `Cookie` header;
- expose the last successful login time;
- expose a snapshot for diagnostics/tests;
- clear session state.

It does not persist sessions.

## SSL

`verify-ssl=true` is the default and should be used in production.

`verify-ssl=false` configures the HttpComponents client to accept self-signed certificates and disable hostname verification. This is intended for local development or controlled test environments only.

## Current XuiClient Contract

`ping()` performs a lightweight unauthenticated GET against the configured base URL and returns `true` on success.

Task 22 implements:

- `login()`
- `logout()`
- `isLoggedIn()`
- `isAuthenticated()`
- `refreshSession()`
- `pingAuthenticated()`

The broad placeholder methods on `XuiClient` intentionally remain unsupported for mutation workflows. Focused ports are used instead:

- Task 23: `XuiInboundClient` for authenticated inbound discovery.
- Task 24: `XuiClientManagementClient` for authenticated create-client POSTs.

Update/delete, subscription generation, and VPN URI creation remain out of scope.

## Test Foundation

`XuiMockServerSupport` uses MockWebServer so tests never require a real Xui server.

Covered behavior:

- property binding;
- RestClient construction;
- headers;
- JSON serialization;
- cookie persistence;
- login and session lifecycle;
- retry behavior;
- timeout mapping;
- SSL-disabled self-signed certificate support;
- safe exception mapping;
- inbound discovery;
- idempotent create-client provisioning;
- unsupported out-of-scope update/delete/subscription operations.

## Deferred Work

Future tasks will add:

- client update/delete payloads;
- subscription generation;
- business orchestration with orders, payments, or subscriptions;
- Telegram-facing flows.

See `docs/xui/client-provisioning.md` for Task 24 create-client provisioning details.
