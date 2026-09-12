---
schema_version: 3
artifact: tickets-map
change: 2026-08-30-openapi-common-module
status: completed
---

# Tickets Map: OpenAPI Common Module

- **Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/{change}/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/{change}/evidence/</Path>`
- **可选 Goal Plan：** `<Path>{roots.state}/specdev/changes/{change}/goal-plan.md</Path>`

## 1. 目标与拆分策略

十二张 Ready/Deep Ticket 共同交付一个默认关闭、显式方法开放、NAMEWTA v1 签名、Redis 失败关闭、每用户唯一凭据、标准 Sa-Token 机器 Session、全局授权失效和前端双入口的 OpenAPI 能力。切片遵循“先协议与日志安全，后权威授权/注册表/Session，再凭据与网关，最后 UI 与装配”的依赖方向。

- T-01/T-02 是 prefactor：先建立模块/SPI/协议与日志红线，不激活功能。
- T-03 至 T-06 是 expand：建立只读授权、真实 Handler 目录、Session 桥和 additive credential schema。
- T-07/T-08 是 migrate：把全部权限写入口迁移到成功返回前的机器 Session 失效语义。
- T-09/T-11 是可观察业务闭环：签名调用和 current/target 双入口。
- T-12 是 contract/release gate：唯一装配、默认关闭、启用态 fail-closed 与全量回归。

本 Map 只批准规划拓扑。用户已明确授权实现 commit 与 Lead-owned 本地 candidate/parent update；生产 DDL/DML、KEK 下发、远程动作和 `openapi.enabled=true` 始终未授权。

## 2. 执行清单

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | Contract IDs | Wave/Gate | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| T-01 | `<Path>{roots.state}/specdev/changes/{change}/ticket/01-openapi-module-protocol-core.md</Path>` | common 模块、注解/SPI、NAMEWTA v1 固定协议 | — | deep | critical | yes | codex:/root | AC-003,004,007 | W1/G-10 | done |
| T-02 | `<Path>{roots.state}/specdev/changes/{change}/ticket/02-redact-openapi-logging.md</Path>` | OpenAPI header/body/流式日志安全 | — | deep | critical | yes | codex:/root | AC-028,029 | W1/G-10 | done |
| T-03 | `<Path>{roots.state}/specdev/changes/{change}/ticket/03-global-authorization-snapshot.md</Path>` | 全局只读授权快照与停用过滤 | T-01 | deep | critical | yes | codex:/root | AC-013-017,019 | W2/G-20 | done |
| T-04 | `<Path>{roots.state}/specdev/changes/{change}/ticket/04-openapi-registry-catalog.md</Path>` | 真实 HandlerMapping/SpringDoc 注册表与目录 | T-01,T-03 | deep | high | yes | codex:/root | AC-002,014,020-022 | W3/G-30 | done |
| T-05 | `<Path>{roots.state}/specdev/changes/{change}/ticket/05-machine-session-bridge.md</Path>` | 机器 TokenSession/请求身份桥与注销 SPI | T-01,T-03 | deep | critical | yes | codex:/root | AC-012-014,017,019,030 | W3/G-30 | done |
| T-06 | `<Path>{roots.state}/specdev/changes/{change}/ticket/06-credential-lifecycle.md</Path>` | 唯一凭据、KEK 加密、self/admin API 与 SQL | T-01,T-05 | deep | critical | yes | codex:/root | AC-008-011,024,026 | W4/G-40 | done |
| T-07 | `<Path>{roots.state}/specdev/changes/{change}/ticket/07-identity-client-session-invalidation.md</Path>` | user/Client/login-domain/user-role 写后失效 | T-05 | deep | critical | yes | codex:/root | AC-018,030 | W4/G-40 | done |
| T-08 | `<Path>{roots.state}/specdev/changes/{change}/ticket/08-rbac-menu-session-invalidation.md</Path>` | role/menu/data-scope 写后失效 | T-05 | deep | critical | yes | codex:/root | AC-018,030 | W4/G-40 | done |
| T-09 | `<Path>{roots.state}/specdev/changes/{change}/ticket/09-signed-openapi-gateway.md</Path>` | 验签、nonce、双限流、授权与调用全链 | T-02,T-04,T-05,T-06 | deep | critical | yes | codex:/root | AC-001,002,004-007,012-017,019,030 | W5/G-50 | done |
| T-10 | `<Path>{roots.state}/specdev/changes/{change}/ticket/10-frontend-openapi-domain.md</Path>` | system domain 类型、收窄与请求合同 | T-04,T-06 | deep | high | yes | codex:/root | AC-009,020-022,024,026,027 | W5/G-50 | done |
| T-11 | `<Path>{roots.state}/specdev/changes/{change}/ticket/11-frontend-dual-openapi-ui.md</Path>` | admin 动态页与个人中心 tab | T-10 | deep | high | yes | codex:/root | AC-023-027 | W6/G-60 | done |
| T-12 | `<Path>{roots.state}/specdev/changes/{change}/ticket/12-openapi-assembly-release-gate.md</Path>` | default-off 装配、启动门禁与发布回归 | T-07,T-08,T-09,T-11 | deep | critical | yes | codex:/root | AC-001,018,023-026,028-030 | W7/G-70 | done |

Ticket frontmatter 是状态、依赖、深度和路径访问契约的权威；本表是同步投影，不得独立修改出另一套真相。

## 3. 依赖 DAG

```text
T-01 [READY, protocol prefactor] ─→ T-03 [authorization snapshot]
  │                                  ├─→ T-04 [registry/catalog] ─┬─→ T-09 [signed gateway]
  │                                  └─→ T-05 [session bridge] ──┼─→ T-06 [credential/schema] ─┐
  │                                                             ├─→ T-07 [identity migrate] ──┤
  │                                                             └─→ T-08 [RBAC migrate] ──────┤
  └──────────────────────────────────────────────────────────────→ T-06                       │

T-02 [READY, logging prefactor] ─────────────────────────────────→ T-09                       │
T-04 ───────────────────────────────────────────────┐                                          │
T-06 ───────────────────────────────────────────────┴─→ T-10 [frontend domain] ─→ T-11 [dual UI]

T-07 ─┐
T-08 ─┼─→ T-12 [default-off assembly + release contract]
T-09 ─┤
T-11 ─┘
```

关键汇合点是 T-09（安全调用链）、T-11（前端双入口）和 T-12（最终装配）。边只表示真实开始条件；T-06 与 T-07/T-08 可在 T-05 后并行，T-09 还必须等待 T-06，T-10 必须等待后端目录和凭据合同。

## 4. 合同覆盖矩阵

| Contract ID | 覆盖 Ticket | 验证接缝 | 状态 | 说明 |
|---|---|---|---|---|
| AC-001 | T-09, T-12 | gateway/context test | covered | 默认关闭，普通 Token 不变 |
| AC-002 | T-04, T-09 | HandlerMapping registry + E2E | covered | 仅方法级显式开放 |
| AC-003 | T-01 | fixed vectors | covered | 跨语言 canonical/signature 一致 |
| AC-004 | T-01, T-09 | signature matrix | covered | 任一字段篡改拒绝 |
| AC-005 | T-09 | Redis atomic-store test | covered | 并发 nonce 仅一次通过 |
| AC-006 | T-09 | dual rate-limit test | covered | 全局/接口额度均不可绕过 |
| AC-007 | T-01, T-09 | retry protocol test | covered | 新 nonce 可重签，旧请求拒绝 |
| AC-008 | T-06 | persistence/service concurrency test | covered | 每用户唯一且 secret 仅一次 |
| AC-009 | T-06, T-10 | controller/transport test | covered | 查询永不回显 secret/密文/Token |
| AC-010 | T-06 | lifecycle + session test | covered | reset 保留 AppKey 并注销旧 Session |
| AC-011 | T-06 | lifecycle state test | covered | enable/disable/delete/recreate 合同 |
| AC-012 | T-05, T-09 | Sa-Token bridge spy/E2E | covered | 每次验签并复用标准 LoginUser |
| AC-013 | T-03, T-05, T-09 | cache-aside module test | covered | 缺失时只读装载并写 Session |
| AC-014 | T-03, T-04, T-05, T-09 | authorization matrix | covered | 无权限拒绝且不补写权限 |
| AC-015 | T-03, T-09 | client union matrix | covered | 合法 Client 权限并集且无需 clientid |
| AC-016 | T-03, T-09 | disabled relation matrix | covered | 任一停用项不进入快照 |
| AC-017 | T-03, T-05, T-09 | superadmin module test | covered | `superadmin` 与 `*:*:*` 语义 |
| AC-018 | T-07, T-08, T-12 | invalidation interaction/release test | covered | 权威写成功前完成注销 |
| AC-019 | T-03, T-05, T-09 | negative Client dependency test | covered | 单一 Client 依赖失败关闭 |
| AC-020 | T-04, T-10 | catalog service/domain test | covered | 无凭据也可理论预览 |
| AC-021 | T-04, T-10 | target-user catalog test | covered | 目录按目标用户而非管理员过滤 |
| AC-022 | T-04, T-10 | registry/SpringDoc contract test | covered | method/path/权限/schema/调用一致 |
| AC-023 | T-11, T-12 | component/App integration | covered | 有 self 权限显示个人 tab |
| AC-024 | T-06, T-10, T-11, T-12 | permission + controller test | covered | 无 self 权限 UI 隐藏且后端拒绝 |
| AC-025 | T-11, T-12 | manifest/page integration | covered | 超管目标用户完整工作流 |
| AC-026 | T-06, T-10, T-11, T-12 | owner-scope controller/UI test | covered | 非超管 targetUserId 伪造拒绝 |
| AC-027 | T-10, T-11 | web-domain workflow test | covered | 关闭后 secret 不可恢复且复制失败安全 |
| AC-028 | T-02, T-12 | captured HTTP/operation logs | covered | header/JSON 上限内递归脱敏 |
| AC-029 | T-02, T-12 | non-JSON/file/stream log test | covered | 只记安全元数据 |
| AC-030 | T-05, T-07, T-08, T-09, T-12 | backend/frontend regression | covered | 普通浏览器 Token 不进入机器 Session |

无 `uncovered` 或 `deferred` 合同。

## 5. 并行与路径所有权

- implementation subagent 上限来自 `<Path>{roots.state}/specdev/config.json</Path>`；本 Map 不授权创建 subagent/worktree。
- review/research/test-observation 保持只读；正式实现 workspace、并发上限和动态派单由 Goal Plan 决定。
- Lead 是 SpecDev 状态与父分支 integration owner；Ticket owner 当前均为 `unassigned`。
- 项目路径契约以 Ticket frontmatter 为准；共享文件只能由下表指定 owner 写入。

| Ticket A | Ticket B | Writable 交集 | 真实依赖 | 处理 |
|---|---|---|---|---|
| T-01 | T-02 | 无 | 否 | W1 可并行 |
| T-04 | T-05 | 无 | 否 | W3 可并行，共同消费 T-03 |
| T-06 | T-07 | 无 | 否 | W4 可并行，T-06 独占 SQL |
| T-07 | T-08 | 无 | 否 | W4 可并行，按 identity/RBAC service 分界 |
| T-09 | T-10 | 无 | 否 | W5 可并行，后端网关/前端 domain 分界 |
| T-10 | T-11 | domain root 与 web-domain root 不同 | 是 | 串行，T-11 只读消费 T-10 exports |
| T-09 | T-12 | config/imports 仅 T-12 可写 | 是 | T-12 只读消费网关并完成装配 |

| 共享路径 | 唯一 Owner | 约束 |
|---|---|---|
| common aggregate/BOM/system/admin POM | T-01 | 后续票只读，full/core 装配事实由 T-12 验证 |
| `script/sql/namewta/DDL.sql`、`DML.sql` | T-06 | 只追加；不改 `ry_vue.sql`，不执行生产迁移 |
| system domain root exports/package/README | T-10 | T-11 只消费 |
| system web-domain root/runtime/tests/package/README、admin profile index | T-11 | 动态 manifest 与静态 tab 的唯一组合 owner |
| AutoConfiguration imports、admin `application*.yml`、customization map | T-12 | default-off/fail-closed 与发布记录唯一 owner |

## 6. Gate、Wave 与集成点

| Wave | 可执行 Ticket | 进入条件 | 行为/集成 Gate |
|---|---|---|---|
| W1 | T-01, T-02 | Spec/Tickets ready | 协议向量、模块边界与日志红线通过 |
| W2 | T-03 | T-01 accepted | 全局只读授权矩阵通过 |
| W3 | T-04, T-05 | T-03 accepted | registry/catalog 与机器 Session 接缝通过 |
| W4 | T-06, T-07, T-08 | 各自 blockers accepted | credential/schema 与全部写后失效通过 |
| W5 | T-09, T-10 | 各自 blockers accepted | MockMvc 签名调用闭环、前端 domain 合同通过 |
| W6 | T-11 | T-10 accepted | manifest + profile 双入口集成通过 |
| W7 | T-12 | T-07/T-08/T-09/T-11 accepted | default-off/fail-closed、full/core、前端全门禁通过 |

Goal Plan 投影固定为 `ticket_workspace_policy: required`、`integration_gate: candidate-merge`、Lead `codex:/root`、implementation subagent 上限 3、integration attempt 上限 7。source worktree 按上述 Wave 可并行，Lead candidate 集成固定串行为 `T-01→T-02→T-03→T-05→T-04→T-06→T-10→T-07→T-08→T-09→T-11→T-12`。正式 Gate 为 G-00 授权、G-10 协议/日志、G-20 授权、G-30 注册表/Session、G-40 凭据/失效迁移、G-50 垂直合同、G-60 双 UI、G-70 发布候选；详细关闭证据和恢复以 `<Path>{roots.state}/specdev/changes/{change}/goal-plan.md</Path>` 为权威。

用户已明确授权 implementation commits 与 Lead-owned local candidate integration/parent updates，G-00 授权门已关闭，Goal Plan Ready。该授权不包含 cleanup、远程或生产动作；未进入 I-implement 前不创建 source/candidate worktree。

每张实现票必须记录 implementation/source、parent before、candidate/result SHA 和父分支包含关系。T-09/T-12 的 E2E 使用 Spring 应用边界与测试 double；不得把它描述为真实 MySQL、Redis 或多节点验证。正式 Gate、candidate 集成顺序和 Lead 动态派单必须由 `<Path>{roots.workflows}/specdev/P-goal-plan/P-goal-plan.md</Path>` 生成。

## 7. 横切契约与风险

- **安全：** 只允许方法级 `@OpenApi`；dual-auth 拒绝；credential/nonce/限流/授权任一依赖异常均 fail closed；敏感 header、secret、signature、Token、canonical/body 不进入日志。
- **数据：** 每用户一条未删除凭据，AppKey 全局唯一；AES-256-GCM + versioned KEK；SQL 只追加且满足七基础字段与中文注释。
- **授权：** 快照只读复用 system 权威关系；停用 Client/login-domain/relation/role/menu 不进入结果；不支持依赖单一 Client 的 Handler。
- **失效：** 凭据和所有用户/Client/login-domain/user-role/role/menu/data-scope 权威写入必须在成功响应前完成集群机器 Session 注销。
- **兼容：** 功能默认关闭；关闭态无 OpenAPI 入口且普通浏览器 Token、Client header/path/IP、动态路由和权限行为不变。
- **前端：** App -> web-domain -> domain；self 不携带 userId，admin 明确 target user；secret 仅一次瞬态展示且不持久化。
- **迁移/恢复：** 先备份与 additive DDL/DML，再部署默认关闭代码；生产 KEK/Redis 配置和启用另行批准。恢复优先关闭开关并保留 additive schema，修复后前向恢复。
- **残余风险：** 当前门禁不要求真实 MySQL 唯一竞争、真实 Redis、多节点传播、压力测试或完整 Playwright；Evidence 必须逐项如实标记。

## 8. 同步规则

- Ticket 状态变化后同步执行清单；
- Ticket ID、路径、依赖或 frontmatter 不一致时，以 Ticket 文件为权威并修复本 Map；
- Goal Plan 存在时，Wave、Gate 和 owner 以 `<Path>{roots.state}/specdev/changes/{change}/goal-plan.md</Path>` 为编排权威；
- 依赖、合同覆盖或路径所有权变化后运行 `<Path>{roots.workflows}/specdev/common/tools/validate-specdev.mjs</Path>`；
- 内部工件不得使用相对 Markdown 链接；
- 未经新的明确批准，不得开始实现、集成或生产迁移。
