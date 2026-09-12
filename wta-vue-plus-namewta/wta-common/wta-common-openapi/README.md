# wta-common-openapi

`wta-common-openapi` provides the shared NAMEWTA v1 signed-request gateway, catalog registry,
Redis replay/rate controls, and Sa-Token machine-session bridge. Both backend bundles resolve the
same artifact. The admin application enables the feature by default; set `OPENAPI_ENABLED=false`
to explicitly disable the assembly.

## Configuration

The admin application maps these environment variables:

| Environment variable | Property | Default |
| --- | --- | --- |
| `OPENAPI_ENABLED` | `openapi.enabled` | `true` |
| `OPENAPI_CLOCK_SKEW` | `openapi.clock-skew` | `60s` |
| `OPENAPI_NONCE_TTL` | `openapi.nonce-ttl` | `60s` |
| `OPENAPI_APP_RATE_LIMIT_PER_MINUTE` | `openapi.app-rate-limit-per-minute` | `1000` |
| `OPENAPI_INTERFACE_RATE_LIMIT_PER_MINUTE` | `openapi.interface-rate-limit-per-minute` | `100` |
| `OPENAPI_MACHINE_SESSION_TTL` | `openapi.machine-session-ttl` | `8h` |
| `OPENAPI_KEK_VERSION` | `openapi.kek-version` | empty |
| `OPENAPI_KEK` | `openapi.kek` | empty |

When enabled, `OPENAPI_KEK` must be a standard Base64-encoded 32-byte key and
`OPENAPI_KEK_VERSION` must identify that active key. Supply both through the deployment secret
provider. Never commit key material or print it in logs.

Startup fails closed when an enabled deployment has invalid settings or lacks Redis, Sa-Token,
the MVC mapping registry, or a unique credential/authorization resolver. No in-memory fallback is
provided.

## Release Sequence

1. Back up the database and review the additive migration and menu DML from T-06.
2. Apply the approved migration.
3. Deploy the code with `OPENAPI_ENABLED=false`.
4. Configure Redis and deliver the KEK through the approved secret provider.
5. Enable OpenAPI only under a separate production approval.

To recover, set `OPENAPI_ENABLED=false` first. The additive schema and menu data may remain while
configuration or code is corrected and validated before a later re-enable.
