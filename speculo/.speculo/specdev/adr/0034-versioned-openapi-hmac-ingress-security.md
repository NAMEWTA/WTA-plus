# ADR-0034: 版本化 OpenAPI HMAC 入口安全

- **Status:** Accepted
- **Date:** 2026-09-02
- **Source:** `<Path>{roots.state}/specdev/archive/2026-08/2026-08-30-openapi-common-module/ADR.md</Path>` ADR-002、ADR-017
- **Source:** `<Path>{roots.state}/specdev/archive/2026-08/2026-08-30-openapi-common-module/LOG.md</Path>` LOG-027

## Context

来源 MD5 方案没有绑定完整请求语义，nonce 也只能证明签名材料未被重放，不能证明写操作是否已提交。开放入口还需要同时限制单凭据总流量和热点接口流量，并在 Redis 无法提供原子安全状态时避免降级放行。

## Decision

NAMEWTA OpenAPI 使用不兼容旧方案的版本化 HMAC-SHA256 canonical request，签名绑定 AppKey、timestamp、nonce、method、path、query 和 body 摘要。Redis 在默认 60 秒窗口内原子登记 `AppKey + nonce`；重试必须生成新的 timestamp、nonce 和签名，nonce 不作为业务幂等键。入口同时执行每 AppKey 默认 1000 次/分钟和每 AppKey + 开放接口默认 100 次/分钟的可配置限流，Redis 不可用时失败关闭。HTTP 与 HTTPS 均可验签，但生产推荐 HTTPS 提供传输机密性。

## Consequences

协议版本和 canonicalization 变更必须显式演进并以跨语言固定向量验证。写接口是否可重试由各业务合同单独声明；框架不提供通用响应重放。认证完整性不能被描述为内容加密，HTTP 下被动监听风险仍然存在。
