---
module: wta-third
scope: backend
---

# wta-third module guide

`wta-third` is the layered outbound HTTP integration module. It owns provider and endpoint configuration, credential envelopes, cache snapshots, the synchronous gateway pipeline, adapter SPI, outbound audit records, and administration APIs.

## Layout

- `controller/admin`: authenticated management endpoints only.
- `usecase`: application orchestration and transaction boundaries.
- `service`: domain policies, configuration resolution, crypto, cache, gateway and observability services.
- `dao`: persistence ports owned by the module.
- `mapper` and `resources/mapper/third`: MyBatis persistence adapters and XML.
- `spi`: explicit provider adapters for signing, non-standard encryption, pagination, and business error mapping.
- `http`: RestClient factory and typed `@HttpExchange` marker.
- `domain`: entity, read-row, BO and VO models; entities never cross the module boundary.
- `wta-api`: only `ThirdPartyGateway`, `ThirdPartyRequest`, `ThirdPartyResponse`, and failure categories are public to other business modules.

## Runtime boundaries

Provider configuration supplies a trusted HTTP(S) origin. Endpoint metadata may supply only a validated relative path and declared query/header/body names. Dynamic requests use the fixed RestClient pipeline; fixed typed contracts use `@HttpExchange`. No database script, SpEL, arbitrary class name, full URL, cross-host redirect, or arbitrary header is executable.

Provider disabled state takes precedence over endpoint state. Redis is a second-level cache only; an unavailable or unverifiable configuration is fail-closed. Credentials are provider/endpoint scoped, AES-GCM encrypted, and never returned to callers.

## Verification

From `wta-vue-plus-namewta` run:

```text
./mvnw -P local -pl wta-modules/wta-third -am test
```

The module must also be included in both `wta-admin` full and core bundle checks. Real MySQL 8.4, Redis, HTTP mock, and browser evidence belong to the change release gate; tests must record when those external prerequisites are unavailable.
