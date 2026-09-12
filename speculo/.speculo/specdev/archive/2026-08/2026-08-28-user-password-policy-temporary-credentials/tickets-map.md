---
schema_version: 3
artifact: tickets-map
change: 2026-08-28-user-password-policy-temporary-credentials
status: completed
---

# Tickets Map: 统一密码策略、临时凭据与 Client 精确失效

- **Map：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/tickets-map.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/spec.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/evidence/</Path>`
- **Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/goal-plan.md</Path>`（complete：G-00 至 G-50 全部关闭）

## 1. 目标与拆分策略

八个 Ticket 共同交付 `US-001` 至 `US-009` 与 `AC-001` 至 `AC-024`。拆分以可观察纵向行为为中心，而不是按数据库/后端/前端水平分层：T-02 独立交付授权变化后的 Client 精确失效；T-03/T-04/T-06 分别交付策略公共合同、全部后端写入收敛、注册/个人改密体验；T-05/T-07 分别交付临时凭据认证与用户管理操作；T-08 负责真实数据启用、发布与长期合同。

T-01 是必要 prefactor。当前 Spring Cache 与 Sa-Token 都有进程私有 Caffeine，T-02 的立即授权回收和 T-03 的配置 last-known-good 刷新都需要同一跨 JVM 失效协议；由 common 单一 owner 先建立可测试合同，避免两个业务 Ticket 各自发明 Redis channel 或直接碰静态缓存。

本 change 不使用 expand-contract 替换现有登录或 reset API：新增字段/endpoint 是 additive，现有 reset/login 请求形状保持。兼容收缩只针对 `sys.user.initPassword` 读取和旧前端弱静态规则，由 T-08 最终扫描和迁移 Gate 证明为零。

## 2. 执行清单

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | Contract IDs | Wave/Gate | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| T-01 | `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/ticket/01-build-cluster-cache-invalidation.md</Path>` | 两个 JVM 的 Spring/Sa-Token 本地缓存可被立即、可确认失效 | — | deep | critical | yes | codex:lead | AC-022 | Wave 0 / G-10 / integration 1 | done |
| T-02 | `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/ticket/02-enforce-client-authorization-invalidation.md</Path>` | 角色权限与用户角色变化按 Client 精确强制重新登录 | T-01 | deep | critical | yes | codex:lead | AC-020/021/022 | Wave 1 / G-30 / integration 3 | done |
| T-03 | `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/ticket/03-publish-password-policy-contract.md</Path>` | 类型化策略、详细错误、生成器和安全公开投影成为共享合同 | T-01 | deep | high | yes | codex:lead | AC-001/003/015/019 | Wave 1 / G-20 / integration 2 | done |
| T-04 | `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/ticket/04-converge-password-write-paths.md</Path>` | 五类后端密码写入统一收敛并支持可编辑 reset candidate | T-03 | deep | high | yes | codex:lead | AC-001/002/003/004/016/017/018 | Wave 2 / G-30 / integration 4 | done |
| T-05 | `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/ticket/05-implement-temporary-password-authentication.md</Path>` | 一分钟、覆盖、单次 CAS 的临时凭据可签发并登录普通会话 | T-03 | deep | critical | yes | codex:lead | AC-005..014/023/024 | Wave 2 / G-30 / integration 5 | done |
| T-06 | `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/ticket/06-adopt-policy-in-registration-profile.md</Path>` | OpenAPI 同步，注册与个人改密按公开策略即时反馈 | T-04, T-05 | deep | high | yes | codex:lead | AC-003/015/016/018 | Wave 3 / G-40A / integration 6 | done |
| T-07 | `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/ticket/07-deliver-admin-user-credential-workflows.md</Path>` | 用户管理可编辑重置候选并独立签发/复制一次性临时密码 | T-04, T-05, T-06 | deep | high | yes | codex:lead | AC-001/002/003/005/006/017/023 | Wave 4 / G-40B / integration 7 | done |
| T-08 | `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/ticket/08-publish-password-migration-and-contracts.md</Path>` | fresh/upgrade DML、发布回滚与长期认证边界可演练 | T-02, T-06, T-07 | deep | critical | yes | codex:lead | AC-005/006/019/023 | Wave 5 / G-50 / integration 8 | done |

Ticket frontmatter 是状态、依赖、深度和路径访问合同的权威；本表是同步投影。

## 3. 依赖 DAG

```text
T-01 [DONE: cluster invalidation prefactor]
  ├─→ T-02 [DONE: Client-scoped authorization invalidation] ────────────────┐
  └─→ T-03 [DONE: password policy shared contract]                         │
        ├─→ T-04 [DONE: all password writes] ───┐                           │
        └─→ T-05 [DONE: temporary auth] ────────┴─→ T-06 [DONE: public UI] │
                                                   └─→ T-07 [DONE: admin UI]
T-04 ────────────────────────────────────────────────────↗                 │
T-05 ────────────────────────────────────────────────────↗                 │
T-02 + T-06 + T-07 ───────────────────────────────────────→ T-08 [DONE] ──┘
```

- **根/prefactor：** T-01。
- **第一扇出：** T-02 与 T-03 无产品路径交集，在已选择的 required worktree 模式并行形成 source；Lead 先集成 T-03 打开密码功能关键路径，再集成 T-02。
- **第二扇出：** T-04 与 T-05 只读同一 T-03 合同，精确 writable paths 不相交，并行形成 source；Lead 按 T-04、T-05 串行重建 candidate。
- **前端汇合：** T-06 等两个 backend 公共 API 都进入父状态后一次生成完整 OpenAPI；T-07 在 T-06 后只读该生成物。
- **最终汇合：** T-08 等授权、公共 UI、管理员 UI 都有实现证据后，才写生产 DML 和运行最终发布 Gate。

## 4. 合同覆盖矩阵

| Contract ID | 覆盖 Ticket | 验证接缝 | 状态 | 说明 |
|---|---|---|---|---|
| AC-001 | T-03, T-04, T-07 | policy properties + user API/UI | covered | 默认 random candidate 与 add/reset 展示 |
| AC-002 | T-04, T-07 | reset API + browser dialog | covered | 编辑最终值才写入 |
| AC-003 | T-03, T-04, T-06, T-07 | structured error + component/API | covered | 全部稳定 violations |
| AC-004 | T-04 | session compatibility integration | covered | reset 不踢既有会话 |
| AC-005 | T-05, T-07, T-08 | permission API/UI/SQL | covered | 独立权限拒绝与配置 |
| AC-006 | T-05, T-07, T-08 | issue API/Redis/browser/migration | covered | 60 秒合规且永久密码不变 |
| AC-007 | T-05 | real Redis overwrite | covered | reissue 覆盖与 TTL 重置 |
| AC-008 | T-05 | wrong-then-correct Redis auth | covered | 错误不消费、正确一次 |
| AC-009 | T-05 | concurrent CAS | covered | 双并发仅一成功 |
| AC-010 | T-05 | expiry/consumed/overwritten login API | covered | 通用凭据错误不泄露状态 |
| AC-011 | T-05 | multi-Client auth matrix | covered | 未准入不消费，获准首次消费 |
| AC-012 | T-05 | LoginUser/token equivalence | covered | 普通无标记会话 |
| AC-013 | T-05 | permanent-first auth test | covered | 永久成功不消费临时值 |
| AC-014 | T-05 | Redis/policy fault injection | covered | fail-close 且永久登录保持 |
| AC-015 | T-03, T-06 | Auth context API + transport narrowing | covered | 最小公开投影 |
| AC-016 | T-04, T-06 | direct API + registration browser | covered | 绕过前端仍拒绝弱注册 |
| AC-017 | T-04, T-07 | add/import/profile/reset service/UI | covered | 所有写入口同一策略 |
| AC-018 | T-04, T-06 | legacy login + next-write tests | covered | 存量弱登录兼容、改密收敛 |
| AC-019 | T-03, T-08 | config cache + MySQL migration | covered | random/fixed、坏配置、刷新 |
| AC-020 | T-02 | role permission multi-Client integration | covered | 角色所属 Client 全失效 |
| AC-021 | T-02 | user-role multi-token integration | covered | user+Client 精确失效 |
| AC-022 | T-01, T-02 | two-JVM fault injection | covered | 全层确认、失败可重试 |
| AC-023 | T-05, T-07, T-08 | audit/log/UI/SQL review | covered | 可审计且无业务日志明文 |
| AC-024 | T-05 | login counter integration | covered | 一次 grant 一次成败计数 |

不存在 `uncovered` 或 `deferred` 合同。

## 5. 并行与路径所有权

- implementation subagent 上限来自 `<Path>{roots.state}/specdev/config.json</Path>`，当前为 3；Goal Plan 可降低且不含 Lead。
- T-01 唯一拥有 common-redis 与 PlusSaTokenDao；T-03 唯一拥有 common 结构化错误和 system password policy；T-06 唯一拥有本 change 的 api-contracts snapshot/generated；T-08 唯一拥有 DML/SQL README/customization map。
- T-04 与 T-05 对新 BO/VO 采用精确文件路径，避免在同一目录使用重叠 glob。
- T-06 拥有 `domain-admin` 公开策略投影以及 Admin 私有 register/profile/`src/lang`；T-07 拥有 `domain-system` 用户类型/服务、`web-domain-system` 用户页与 manifest 权限，并只在 Admin manifest registry 注入宿主 port。两者都不得把业务能力写回 `router/index.ts` 静态路由。
- Lead 始终拥有 SpecDev 状态、Evidence、candidate/direct-parent、父分支与两个 submodule gitlink 集成。

| Ticket A | Ticket B | Writable 交集 | 真实依赖 | 处理 |
|---|---|---|---|---|
| T-02 | T-03 | 无 | 否 | required 可并行；current 串行 |
| T-04 | T-05 | 无 | 否 | required 可并行；共同只读 T-03 |
| T-06 | T-07 | 无，但 T-07 消费 T-06 OpenAPI/policy | 是 | T-06 先集成，T-07 后开始 |
| T-01 | T-02/T-03 | shared contract 被消费 | 是 | prefactor Gate 后扇出 |
| T-08 | 所有前序 | 无产品源码交集，但依赖已验证行为 | 是 | 最终迁移 owner 与发布 Gate |

## 6. Gate、Wave 与集成点

- **G-00 / Authorization + Baseline：** required worktree 已选择；implementation commit 与 local candidate integration/parent update 获得明确授权，并重新冻结三个仓库 predecessor 后才创建 T-01 worktree。
- **Wave 0 / G-10 Shared Invalidation：** T-01 真实 Redis 双实例绿色，公共失效协议进入 backend `main`。
- **Wave 1 / G-20 Policy Contract：** T-02/T-03 并行 source；T-03 先 candidate 集成并打开 T-04/T-05，T-02 随后在 G-30 前进入 backend `main`。
- **Wave 2 / G-30 Backend Feature：** T-04/T-05 并行 source、依序 candidate；真实 MySQL/Redis、认证和会话矩阵绿色，冻结 backend OpenAPI provenance SHA。
- **Wave 3-4 / G-40 Frontend Verticals：** T-06 唯一生成 OpenAPI并完成 App 私有 register/profile UI；T-07 再按 domain -> web-domain manifest -> App runtime composition 完成 Admin 凭据 UI 和敏感交互 E2E，架构门禁证明无静态业务路由回流。
- **Wave 5 / G-50 Migration + Final（已关闭）：** T-08 在真实 MySQL 演练 fresh/upgrade/重复/冲突/补偿，backend full/core、frontend architecture/lint/typecheck/test/build 与 48 项 E2E 全绿；Lead 已更新长期文档和两个已验证 gitlink。

Goal Plan 按 `required + candidate-merge` 完成。八张 Ticket 均有 clean source、通过的 candidate、Lead Evidence 和已晋级 result；最终 backend 为 `42e06c0f713e0d724813800505e5bb5b40ab563b`，frontend 为 `8aa184b353c5a37ee555feb8be808fe9ba885297`，G-00 至 G-50 全部关闭。

## 7. 横切契约与风险

- 服务端是密码校验、生成、Client 准入和授权失效权威；前端提示/隐藏不能替代。
- fixedValue、generator internals、提交密码、临时 hash 和 Token 不进入普通日志、数据库审计、Redis 消息或 SpecDev Evidence；永久 ADR-0017 的 raw HTTP 例外保持且不得扩大。
- OAuth `clientId` 字符串与 Long `clientPk` 分离；临时凭据 user-scoped，授权失效 Client-scoped。
- 永久密码 reset 不踢会话；只有角色权限/用户角色关系按 Spec 强制重建快照。
- 所有 real Redis/MySQL tests 必须记录是否执行/skip；mock 不能替代 CAS、跨 JVM 或 migration Evidence。
- 后端公共合同与 commits 先行，OpenAPI 由 T-06 从已集成 SHA 获取，前端之后发布；T-08 DML 实际执行仍需外部写入授权。
- 前后端分别在子模块形成逻辑 commit，父仓库最后更新 gitlink/文档；不得把三仓库改动压成不可审查的单一状态。

## 8. 同步规则

- Ticket frontmatter 是状态、依赖、owner 和路径合同权威；状态变化同步本 Map。
- Goal Plan 建立后，Wave、Gate、workspace、owner 和集成顺序以 `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/goal-plan.md</Path>` 为编排权威。
- 任一 owner 需要写其他 Ticket/shared owner 路径时先停止并走 deviation，不能先改后报。
- 依赖、合同覆盖或路径所有权变化后重新运行 `<Path>{roots.workflows}/specdev/common/tools/validate-specdev.mjs</Path>`。
- source-worktree 不运行或声明最终 E2E；required E2E 仅由 Lead 在 parent-candidate，current 模式由 Lead 在 current-workspace。
