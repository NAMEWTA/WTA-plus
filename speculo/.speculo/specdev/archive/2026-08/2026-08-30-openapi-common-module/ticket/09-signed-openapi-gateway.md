---
schema_version: 3
artifact: ticket
change: 2026-08-30-openapi-common-module
id: T-09
title: 实现完整签名验签、重放防护、双限流与机器调用网关
status: done
planning_depth: deep
planning_depth_reason: 该请求边界同时承担密码学验证、Redis 原子状态、双认证拒绝、限流、授权桥接与清理，属于最高安全风险路径。
ready: true
risk: critical
blocked_by: [T-02, T-04, T-05, T-06]
contract_ids: [AC-001, AC-002, AC-004, AC-005, AC-006, AC-007, AC-012, AC-013, AC-014, AC-015, AC-016, AC-017, AC-019, AC-030]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/gateway/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/nonce/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/ratelimit/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-security/src/main/java/org/dromara/common/security/config/SecurityConfig.java</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/gateway/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/nonce/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/ratelimit/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/event/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/test/java/org/dromara/common/openapi/gateway/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/openapi/gateway/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-security/src/main/java/org/dromara/common/security/config/SecurityConfig.java</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/protocol/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/registry/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/session/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/openapi/credential/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-09: 实现完整签名验签、重放防护、双限流与机器调用网关

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/09-signed-openapi-gateway.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-09.md</Path>`

## 1. 战略与来源

- **目标：** 在 Servlet 请求边界完成 NAMEWTA v1 签名调用的全部安全校验，并仅向显式公开的 Handler 放行。
- **可观察产出：** 正确签名可调用；篡改、过期、重放、超限、双认证、禁用凭据、未公开接口和 Redis 故障均稳定拒绝。
- **来源：** `US-001`、`AC-001` 至 `AC-007`、`AC-012` 至 `AC-017`、`AC-019`、`AC-030`、相关 ADR。
- **当前事实：** T-01 提供协议/SPI，T-04 提供真实 Handler registry，T-05 提供机器身份桥，T-06 提供唯一 credential resolver。
- **Planning Depth 原因：** 任一校验顺序、原子性或上下文清理错误都可能造成认证绕过或跨请求身份污染。

## 2. 决策状态

### 已锁定决策

- 只有请求携带完整签名头时进入机器认证；普通请求维持原 Sa-Token 链路。
- 同时携带普通 Authorization/Token 与签名头一律拒绝，不选择其中一种继续。
- 先定位真实 Handler 并确认 `@OpenApi`，再做 credential/version/window/signature/nonce/rate/authorization。
- nonce 与 AppKey/IP 双限流均使用 Redis 原子操作；Redis 不可用时 fail closed。
- 机器上下文必须在 `finally` 清理；调用事件记录失败不得改变已确定的业务响应。

### 已采用的低影响假设

- 具体 Servlet Filter/Interceptor 组合由 Spring MVC 可可靠取得 Handler 的最小实现决定，但外部顺序合同不变。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 请求识别、canonical 验签、时间窗、nonce、AppKey/IP 限流、dual-auth、授权桥接、事件和清理 | T-01/T-02/T-04/T-05/T-06 SPI 与统一错误模型 | 自动配置/总开关（T-12）、credential UI、OAuth/Webhook/IP 白名单 |

## 4. 要构建什么

构建一个默认尚未装配的安全网关：解析 NAMEWTA v1 请求，按固定顺序完成 Handler allowlist、凭据、签名、重放、双限流和权限判断，建立请求级机器身份后调用原 Controller，并在所有结果下清理上下文与发布脱敏调用事件。

## 5. 实现契约

- **入口或接缝：** Servlet 请求边界、T-04 registry、T-06 credential resolver、T-05 identity/session bridge。
- **输入与输出：** 签名 headers + method/path/query/body -> 原业务响应或统一 OpenAPI error。
- **公共接口变化：** 实现 T-01 已定义 SPI；不新增业务 Controller。
- **不变量：** canonical bytes 与 T-01 完全一致；nonce 单次；两类限流都通过；只调用显式公开 Handler。
- **状态或数据流：** detect -> reject dual auth -> resolve handler -> credential -> verify -> nonce -> rate limits -> snapshot/session -> invoke -> event -> cleanup。
- **错误与失败行为：** 所有认证/授权/限流错误使用稳定 code 且不泄露 credential 是否存在；Redis/KEK/SPI 异常 fail closed。
- **兼容要求：** 无完整签名头的普通请求不经过机器链路；总开关仍由 T-12 默认关闭。
- **安全与隐私要求：** 不记录 raw signature、secret、Token、完整 body 或 canonical string；遵循 T-02 redaction。

## 6. 执行路线

1. 先建立顺序驱动和 canonical/signature 的正反例测试。
2. 实现 Handler allowlist、dual-auth 与 credential/window/signature 校验。
3. 实现 Redis 原子 nonce、AppKey/IP 双限流及失败关闭。
4. 接入机器身份/权限，保证异常与异步边界的上下文清理。
5. 增加调用事件与 MockMvc 应用边界 E2E，跑 common/admin 定向测试。

## 7. 路径访问契约

- **预计修改点/可写范围：** common-openapi 的 gateway/nonce/ratelimit/event 子包及其测试、admin 边界测试。
- **只读上下文：** protocol、registry、session 与 system credential 实现。
- **共享路径：** 无。
- **保留或不动：** AutoConfiguration imports、admin application 配置、system credential 与现有业务 Controller。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 验签/篡改/时间窗/重放 | common module tests | `./mvnw -pl ruoyi-common/ruoyi-common-openapi -am test` | 正例通过，全部反例给稳定错误且无泄漏 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-09.md</Path>` |
| 双限流与 Redis 故障 | atomic-store tests | 同上 | AppKey/IP 任一超限拒绝，Redis 异常 fail closed | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-09.md</Path>` |
| Handler/身份/清理全链 | MockMvc E2E | `./mvnw -pl ruoyi-admin -am test -Dtest='*OpenApiGatewayE2E*'` | 仅公开接口执行，权限正确，上下文无串扰 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-09.md</Path>` |
| 日志与事件脱敏 | captured log/event assertions | 同上 | secret/signature/token/body 敏感内容不存在 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-09.md</Path>` |

- **Workspace checks：** current-workspace 跑 common 单测；parent-candidate 跑 Spring MockMvc 边界 E2E。
- **E2E disposition：** required：Filter/Handler/Controller/身份清理需应用边界 E2E；使用测试 Redis/credential double，不冒充真实 MySQL、Redis 或多节点验证。
- **E2E owner/environment：** Lead / parent-candidate。
- **Integration evidence：** implementation/source、parent before、candidate/result SHA、父分支包含关系及测试基础设施类型。

## 9. 发布、迁移与恢复

- **迁移顺序：** 前置 SPI/credential 完成后合入；T-12 装配前保持不可达。
- **兼容窗口：** 普通认证链不变；无旧签名协议兼容负担。
- **监控信号：** error code 分布、nonce 冲突、两类限流、Redis/KEK/SPI 故障与上下文泄漏断言。
- **回滚或前向恢复：** 关闭总开关立即停止机器调用；修复后前向恢复，不放宽 fail-closed。
- **不可逆操作与批准点：** 无；生产启用另行批准。
- **收缩条件：** 全部安全反例与应用边界 E2E 通过。

## 10. 验收标准

- [x] 所列 `AC-001` 至 `AC-019` 相关网关合同通过，未公开 Handler 永不执行。
- [x] nonce 与 AppKey/IP 双限流原子且 Redis 故障失败关闭。
- [x] dual-auth、权限快照、Session 状态和请求清理行为可重复验证。
- [x] 日志/事件不含凭据材料，Evidence 如实标记测试 double 与真实基础设施缺口。
