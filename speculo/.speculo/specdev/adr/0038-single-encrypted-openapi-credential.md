# ADR-0038: 单一加密 OpenAPI 凭据

- **Status:** Accepted
- **Date:** 2026-09-02
- **Source:** `<Path>{roots.state}/specdev/archive/2026-08/2026-08-30-openapi-common-module/ADR.md</Path>` ADR-006、ADR-007

## Context

开放凭据代表用户统一的机器调用入口，不是每个 Client 的独立登录身份。AppSecret 必须可用于验签，但数据库泄露时不能直接暴露明文，也不能通过重复创建绕过单凭据生命周期。

## Decision

每个用户最多拥有一条有效凭据，由公开 AppKey 和 CSPRNG 生成的 AppSecret 组成。Secret 使用外部注入、带版本的 KEK 进行 AES-256-GCM 加密保存，只在创建或重置成功时显示一次。重复创建返回现有状态而不产生第二条；重置立即切换为单一新 secret，禁用或删除立即使旧凭据不可用。

## Consequences

有效 owner 必须有数据库唯一约束，管理员代管也不能创建第二条凭据。该模型不支持多第三方独立撤销或双 secret 无停机轮换。OpenAPI 启用时 KEK 缺失、版本无效或密文不可验证必须失败关闭，任何读取 API 和日志都不得回显 secret。
