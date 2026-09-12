---
schema_version: 3
artifact: ticket
change: 2026-09-04-third-party-http-integration
id: T-08
title: 增加强制超时、幂等重试与双层 Redisson 限制
status: done
planning_depth: deep
planning_depth_reason: 超时、重试、限流和并发控制决定是否实际发送及发送次数，需防止非幂等重复副作用并在 Redis 未知时关闭。
ready: true
risk: high
blocked_by: [T-07]
contract_ids: [AC-009, AC-010]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/adapter/resilience/**</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/adapter/resilience/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/port/ThirdResiliencePort.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/support/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/test/java/org/dromara/third/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-redis/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/adapter/gateway/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/http/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-08: 增加强制超时、幂等重试与双层 Redisson 限制

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/08-resilience-and-limits.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-08.md</Path>`

## 1. 战略与来源

- **目标：** 在发送前实施 Provider/Endpoint 双层容量约束，并为实际发送提供有界超时与只限幂等的有限重试。
- **可观察产出：** 非幂等失败只发一次；幂等 Endpoint 在固定上限内重试；任一层达到限流/并发上限或 Redis 未知时零 HTTP 并返回稳定错误。
- **来源：** `US-006`、`US-007`、`AC-009`、`AC-010`、`ADR-007`。
- **当前事实：** Gateway pipeline 可发送，但尚无强制 connect/read timeout、retry 或 Redisson 门禁。
- **Planning Depth 原因：** 错误重试可能重复推送，错误 fail-open 会放大外部事故与配额消耗。

## 2. 决策状态

### 已锁定决策

- connect/read timeout 强制非零有界；Endpoint 可收紧但不得超过 Provider/静态上限。
- 只有显式幂等 Endpoint 对明确可重试传输失败执行有限重试；沿用 requestId，attempt 递增。
- Provider 与 Endpoint 两层限流/并发都必须通过；Endpoint 上限受 Provider 约束。
- 限流/并发/Redis 未知在发送前返回 RATE_LIMITED 或 CONFIG_UNAVAILABLE；quota 首期仅展示，不阻断。

### 已采用的低影响假设

- 复用 `RedisUtils.rateLimiter`/Redisson primitives，固定算法与 key 版本，不引入动态脚本策略。

### 未决问题

无。

## 2.1 必须加载的 Skill 与工程基线

- **必须加载：** <Path>.agents/skills/engineering-standards/SKILL.md</Path>、<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>、<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>、<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>。
- **必须先读的参考：** <Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>、<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path>、<Path>.agents/skills/engineering-standards/references/project/03-backend-module-modes.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/backend/framework-usage.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/backend/security-and-data.md</Path>、<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>。
- **目录与代码最低要求：** policy 作为 Pipeline 的明确 stage/support，不让 Controller/UseCase 直连 Redisson；配置/错误分类和 limiter key 由 Service 端口封装。测试与实现遵守 layered 五层、GET/POST、@Log/@DSTransactional 和准确 common Redis FQN。
- **韧性要求：** timeout 必须有界；只有显式幂等 endpoint 才允许固定次数重试；Provider/Endpoint gate 均在发送前，permit 在 finally 释放，quota 不得被误作阻断策略。
- **执行停止条件：** 无限重试/动态脚本、非幂等重试、Redis fail-open、发送后才限流、permit 泄漏、或把熔断/自动降级作为默认实现时立即停止。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| timeout、retry classifier、provider/endpoint rate/concurrency gates | T-07 Pipeline 扩展点、RedisUtils/Redisson | 熔断、自动降级、无限策略、quota 阻断 |

## 4. 要构建什么

每次逻辑调用在真正发送前依次取得 Provider 与 Endpoint 限流/并发许可；任何一层拒绝即释放已得许可并返回，HTTP server 看不到请求。发送使用受上限约束的连接/读取超时，只有 endpoint.idempotent 为真且错误属于固定集合时有限重试，所有许可最终可靠释放。

## 5. 实现契约

- **入口或接缝：** Pipeline policy stages、RestClient factory timeout key、retry classifier、Redisson gates。
- **输入与输出：** 有效快照 + requestId/attempt → permit 或稳定拒绝；每次 attempt 产生结果。
- **公共接口变化：** 无。
- **不变量：** 非幂等最多一次；attempt 上限固定；两层许可都在发送前；并发许可 finally 释放。
- **状态或数据流：** preflight → provider gate → endpoint gate → send/optional retry → release。
- **错误与失败行为：** RATE_LIMITED、CONFIG_UNAVAILABLE、TIMEOUT、TRANSPORT 可区分；Redis 未知不 fail-open。
- **兼容要求：** 静态默认值向后可收紧，不允许 DB 配置超过安全 cap。
- **安全与隐私要求：** limiter key 仅使用稳定编码/fingerprint，不含凭据或请求值。

## 6. 执行路线

1. 建立非幂等一次、幂等有限次数、零请求拒绝和 permit 释放测试。
2. 实现超时配置归一与 client factory 复用 key。
3. 实现固定 retry classifier/backoff 和 requestId/attempt 传播。
4. 实现 Provider/Endpoint Redisson 限流与并发 stage。
5. 运行本地 HTTP+真实 Redis 故障/并发集成测试。

## 7. 路径访问契约

- **预计修改点/可写范围：** timeout/retry/limit policy 与测试子树。
- **只读上下文：** common Redis、T-07 pipeline/execution。
- **共享路径：** 无；通过已定义 stage 扩展，不改 T-07 owner 路径。
- **保留或不动：** 熔断、quota 阻断、动态脚本和全自动降级。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | local HTTP/Redis | 幂等传输失败后成功，双层许可可用 | requestId 相同、attempt 有界、许可释放 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-08.md</Path>` |
| 失败路径 | zero-request/concurrency | 非幂等失败、任一层限额、Redis down、timeout | 次数符合合同；拒绝零 HTTP；错误码准确 | 同上 |
| 回归 | Maven policy suite | 并发重复运行并检查残留 permit | 无许可泄漏，Gateway 既有行为通过 | 同上 |

- **Workspace checks：** source/current workspace 运行 policy 单测、并发测试和 Maven 模块回归。
- **E2E disposition：** required：真实 Redis 原子门禁、并发释放与 socket 超时/重试次数属于跨边界行为。
- **E2E owner/environment：** Lead / parent-candidate 或 current-workspace；真实 Redis 加本地可计数 HTTP server。
- **Integration evidence：** implementation/source commit、parent before、candidate/result SHA、实际发送计数和父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 先配置静态 cap，再部署 policy，最后启用 Endpoint。
- **兼容窗口：** 未配置时使用安全静态默认；非法 DB 值 fail-closed。
- **监控信号：** rate/concurrency rejection、Redis error、timeout、retry attempts、permit leak。
- **回滚或前向恢复：** 禁用 Endpoint；修复 policy 后清理仅 third limiter key，不放开 fail-closed。
- **不可逆操作与批准点：** 无生产限额变更；生产参数调整另行批准。
- **收缩条件：** 不适用：无旧 retry/limit 行为。

## 10. 验收标准

- [x] `AC-009`：幂等有限重试、非幂等一次与 requestId/attempt 语义成立。
- [x] `AC-010`：双层限制/Redis 未知零请求且错误码准确。
- [x] timeout、permit 释放和发送次数 Evidence 完整。
- [x] required E2E、提交和集成合同已执行。
