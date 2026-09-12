# ADR-0043: 本地 MinIO CORS 默认允许任意 Origin

- **Status:** Accepted
- **Date:** 2026-09-08
- **Source:** `<Path>{roots.state}/specdev/archive/2026-09/2026-09-08-richtext-third-oss/spec.md</Path>` DEC-003 / T-03

## Context

本地和联调时前端 Origin 经常随端口、主机名和 `localhost`/`127.0.0.1` 变化。把 MinIO CORS 写成少数固定 IP/端口后，预签名直传会在浏览器预检失败，开发者反复改容器环境变量。生产仍需要稳定 HTTPS 域名和最小白名单。

## Decision

`release-artifacts` 里 MinIO 的 `MINIO_API_CORS_ALLOW_ORIGIN` 本地默认值为 `*`，仅用于开发/联调 compose。共享或生产环境必须用该环境变量显式覆盖为稳定 HTTPS Origin 白名单，不得沿用 `*`。本决策不改变 Bucket policy、对象访问类型，也不改变 `endpoint` 与 `domainUrl` 分离。

## Consequences

本地 Origin 变化不再要求改 MinIO IP 白名单。若把 `.env.example` 或 compose 默认值直接带进共享/生产，会放宽浏览器直传 Origin。部署和排障必须以显式白名单验收生产 CORS。
