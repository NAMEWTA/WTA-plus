---
schema_version: 3
artifact: ticket
change: 2026-08-30-openapi-common-module
id: T-05
title: 建立 OpenAPI 机器 TokenSession 与请求级身份桥接
status: done
planning_depth: deep
planning_depth_reason: 复用 Sa-Token TokenSession、request Storage 和标准 LoginUser，直接影响认证上下文、缓存一致性与普通登录隔离。
ready: true
risk: critical
blocked_by: [T-01, T-03]
contract_ids: [AC-012, AC-013, AC-014, AC-017, AC-019, AC-030]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/session/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/test/java/org/dromara/common/openapi/session/**</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/session/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/test/java/org/dromara/common/openapi/session/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/src/main/java/org/dromara/common/satoken/utils/LoginHelper.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/src/main/java/org/dromara/common/satoken/core/service/SaPermissionImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/src/main/java/org/dromara/common/satoken/core/dao/PlusSaTokenDao.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/openapi/authorization/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-05: 建立 OpenAPI 机器 TokenSession 与请求级身份桥接

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/05-machine-session-bridge.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-05.md</Path>`

## 1. 战略与来源

- **目标：** 在不分叉权限链的前提下，把已验签凭据映射为服务端机器 Session 并限定到当前请求。
- **可观察产出：** 首次调用构建一次标准 LoginUser，后续 Session 命中直接复用；作用域结束后当前请求不再持有机器 Token。
- **来源：** `US-001`、`AC-012` 至 `AC-014`、`AC-017`、`AC-019`、`AC-030`、`ADR-009`、`ADR-010`。
- **当前事实：** `LoginHelper`、`SaPermissionImpl`、TokenSession 与 `PlusSaTokenDao` 已形成统一权限主链，现有公开 API 足以优先组合验证。
- **Planning Depth 原因：** 身份混淆或清理遗漏会导致跨请求权限泄漏。

## 2. 决策状态

### 已锁定决策

- 每个有效 credential 关联一个仅服务端可见、与浏览器命名空间不碰撞的机器 Token。
- TokenSession 使用 `LoginHelper.LOGIN_USER_KEY` 保存 T-03 标准 LoginUser；OpenAPI 元数据进入 token extra/session sidecar。
- 每次请求仍须先验签；本 Ticket 只接收“已验证 credential context”，不能自行信任 AppKey。
- Session miss 在分布式锁内双重检查并单写收敛；命中不重复查询 system。
- 当前请求只临时注入 Token，finally 清理并恢复先前 Storage；不得写响应 header/body。

### 已采用的低影响假设

- 优先使用 Sa-Token 1.45 公共 API；若不足，必须按 ADR-010 走 deviation，不能直接改普通核心类。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| Token 生成/恢复、Session cache-aside、request scope、机器 Session invalidator 公共能力 | T-03 resolver、LoginHelper、SaPermissionImpl、PlusSaTokenDao、Redis lock | HMAC、credential DB、HTTP filter、修改 LoginUser/普通登录 |

## 4. 要构建什么

已验证的机器请求进入 scoped bridge 后，目标方法看到与普通登录相同的 LoginUser/权限/数据权限；嵌套、异常和并发场景都能恢复原 request Storage，内部 Token 永不返回调用者。

## 5. 实现契约

- **入口或接缝：** verified credential context、authorization snapshot SPI、scoped callback、machine invalidator。
- **输入与输出：** credentialId/owner -> Session-backed LoginUser；回调结果原样返回。
- **公共接口变化：** common 提供窄桥接与按 machine channel+userId 注销能力；不暴露 Token 给业务模块。
- **不变量：** 每次验签前不可调用；Session miss 不授予新权限；clientPk/clientKey 为空；普通 Token 不匹配机器 channel。
- **状态或数据流：** credential -> existing token/session 或 locked rebuild -> request Storage -> target callback -> restore/cleanup。
- **错误与失败行为：** Redis/SPI/Session/锁/清理失败均失败关闭；部分 Session 不可见不得继续。
- **兼容要求：** 普通 Token、response token write、Client 校验和现有 SaPermissionImpl 不变。
- **安全与隐私要求：** token/credentialId 不进协议、响应、日志；Session 标识支持精确注销。

## 6. 执行路线

1. 建立首次/命中/并发/异常/嵌套/响应泄漏测试并冻结 TokenOperations 接缝。
2. 组合公开 Sa-Token API实现机器 Token 与 TokenSession cache-aside。
3. 实现 scoped request Storage 注入、finally 恢复和机器 channel+userId 注销。
4. 对权限拒绝、空 Client 下游和普通 Token 并行回归。
5. 运行 common-openapi 与现有 authorization/session 测试。

## 7. 路径访问契约

- **预计修改点/可写范围：** 新 module 的 session 子包与测试。
- **只读上下文：** Sa-Token 核心类和 T-03 resolver。
- **共享路径：** 无；现有核心安全类不得修改。
- **保留或不动：** LoginUser 字段、普通登录流程、PlusSaTokenDao、SecurityConfig。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| miss/hit/并发 | bridge spy/concurrency test | `./mvnw -pl ruoyi-common/ruoyi-common-openapi -am test -Dtest='*Session*'` | 单次构建，后续复用同一 LoginUser | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-05.md</Path>` |
| 权限与空 Client | Sa-Token bridge test | 同上 | 无权限拒绝且不补写；Client 能力失败关闭 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-05.md</Path>` |
| 清理/普通登录回归 | scoped storage + authorization tests | `./mvnw -pl ruoyi-admin -am test -Dtest='*Authorization*,*Session*'` | 异常后无残留，普通 Token 不变 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-05.md</Path>` |

- **Workspace checks：** current-workspace/source-worktree 跑模块测试；集成后由 Lead 跑普通认证回归。
- **E2E disposition：** not-required：本 Ticket 的稳定接缝是 Sa-Token module test；完整 Servlet 调用由 T-09/T-12 验证。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate。
- **Integration evidence：** implementation/source commit、parent before、candidate/result SHA 与父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** resolver 后、credential/gateway 前集成；默认无自动装配入口。
- **兼容窗口：** additive 机器 channel 与普通浏览器 channel 并存，互不匹配。
- **监控信号：** Session hit/miss/rebuild/invalidation 结果类别与耗时，禁止记录 Token。
- **回滚或前向恢复：** OpenAPI flag 关闭后停止新建；已有关联 Session 由窄 invalidator 注销。
- **不可逆操作与批准点：** 无；实现和集成需授权。
- **收缩条件：** 不适用。

## 10. 验收标准

- [x] `AC-012` 至 `AC-014`、`AC-017`、`AC-019`、`AC-030` 的桥接部分通过。
- [x] 内部 Token 不出现在响应/日志，异常与嵌套请求恢复 Storage。
- [x] Session miss 单写收敛且只从 T-03 读取已有授权。
- [x] Evidence、commit、集成 SHA、未验证真实 Redis 风险与 Map 一致。
