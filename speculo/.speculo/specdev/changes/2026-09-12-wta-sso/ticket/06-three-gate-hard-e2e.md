---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描 AGENTS.md 与 <Path>.agents/skills/*/SKILL.md</Path>：适用 engineering-standards、namewta-fullstack-development；已排除 java-api-compatibility（本期不单独交付公开 Java API 兼容演进）、project-customization-delivery（用户未显式激活）。本票按需绑定 deploy-namewta-environment（E2E 运行时 Origin/callback 审计）。"
skill_bindings:
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9","phase":"implement","operation":"prove-three-hard-gates-e2e","inputs":["本Ticket T-06 规划合同与路径契约","Tickets Map Skill 矩阵","上游 Spec/ADR/CONTEXT"],"outputs":["符合工程硬约束的实现落点说明","未越界路径与模块边界记录"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"namewta-fullstack-development","path":"<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>","sha256":"65d6f22d11b990de23135c5a2c0bceb3907e964c800d527d58667d12db784988","phase":"implement","operation":"prove-three-hard-gates-e2e","inputs":["本Ticket T-06 垂直切片范围","Spec AC 与 DEC 冻结","相关前后端接缝"],"outputs":["跨层合同对齐的可观察产出清单","下一票依赖的稳定接缝说明"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9","phase":"verify","operation":"verify-three-hard-gates-evidence","inputs":["本Ticket 验证矩阵","定向测试/静态检查范围"],"outputs":["含命令与退出码的 Evidence 草稿字段","残余风险与未验证项"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"deploy-namewta-environment","path":"<Path>.agents/skills/deploy-namewta-environment/SKILL.md</Path>","sha256":"4f0ba7e4f618130a9c9021bdd1b3ad4b7b006fdab7819ed1a72630e5870a8ea0","phase":"verify","operation":"audit-e2e-origin-callback-runtime","inputs":["本Ticket 范围与模块/环境事实","上游 Spec/ADR"],"outputs":["Skill 约束下的落点/检查记录","失败时阻塞说明"],"required":true,"on_failure":"block-ticket","references":[]}
resource_claims:
  - "p0.default-provider-path"
  - "p0.sso-reuse"
  - "p0.client-isolation"
  - "e2e.sso-admin-home"
artifact: ticket
change: 2026-09-12-wta-sso
id: T-06
title: 三门硬 E2E：默认提供方路径 + SSO 复用 + Client 隔离
status: blocked
planning_depth: deep
planning_depth_reason: change 级硬验收汇合；跨 Origin/双 App/Token 隔离，需发布恢复与 Lead E2E。
ready: false
risk: critical
blocked_by: [T-05]
contract_ids: [AC-001, AC-002, AC-003]
owner: unassigned
expected_changes:
  - "<Path>frontend/e2e/</Path>"
writable_paths:
  - "<Path>frontend/e2e/</Path>"
  - "<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/</Path>"
read_only_paths:
  - "<Path>backend/wta-common/wta-common-security/src/main/java/org/namewta/common/security/config/SecurityConfig.java</Path>"
  - "<Path>frontend/apps/admin-web/</Path>"
  - "<Path>frontend/apps/home-web/</Path>"
  - "<Path>frontend/apps/sso-web/</Path>"
  - "<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>"
shared_paths:
[]
shared_path_owners:
[]
---

# Ticket T-06: 三门硬 E2E：默认提供方路径 + SSO 复用 + Client 隔离

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/06-three-gate-hard-e2e.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/T-06.md</Path>`

按 Map → Skill（含 deploy verify）→ 本票读取。

## 1. 战略与来源

- **目标：** 以同浏览器 SSO→admin→home 证明三门硬验收并列通过。
- **可观察产出：** AC-001 默认路径可走通；AC-002 第二次不要求本仓密码；AC-003 两 Token clientid 不同且互拒。
- **来源：** `US-002`/`US-003`/`US-004`、`AC-001`/`AC-002`/`AC-003`、`DEC-115`/`DEC-011`、`ADR-002`、`<Path>frontend/e2e/client-auth-context.spec.ts</Path>`。
- **Planning Depth 原因：** change 级硬门+发布证据。

## 2. 决策状态

### 已锁定决策

- 三门并列，缺一不可宣称 P0 完成。
- 登录归一 ≠ Token 共用。
- E2E 由 Lead 在 current-workspace 执行（Goal Plan current/direct-parent）。

### 已采用的低影响假设

- 环境矩阵上线前填真值；本票验证前必须可解析。

### 产品冻结（本票必须遵守）

- **FirstPartySsoProvider**：第三方登录目录第一、默认接通、按钮→`sso-web` 授权码。
- **三门硬验收并列**：`P0-DEFAULT-PROVIDER-PATH`(AC-001) / `P0-SSO-REUSE`(AC-002) / `P0-CLIENT-ISOLATION`(AC-003)；未授权实施前不得宣称三门已通。
- **D-111=B**：外部 App **仅管理面可登记**；不验收 confidential 运行时（D-116=B / AC-018/019）。
- **协议**：Authorization Code + PKCE S256；`access_token`=现有 Sa-Token；extras=`clientid`=**目标业务 Client**（禁止 `sso`）。
- **authMode**：`both`|`sso`|`local`；保留 `POST /auth/login`。
- **禁止**：OIDC / SLO / 独立进程 / Keycloak·Casdoor·Logto·Hydra / 共享业务 Token / Implicit / password grant / SAML。
- **禁止**：改动 notify 归档 change；push/PR；伪造 Evidence；触碰已删 `ruoyi-vue-plus-docs` 路径。
- **授权**：`implementation_commit=not-authorized`；本票 `status: blocked` / `ready: false`。

### blocked-by-auth

**本票不可立即实施。** 在 `.status.json` 的 `execution_authorization.implementation_commit` 书面翻转前，禁止改产品树、将本票标 ready/in_progress、或造假 Evidence。本文仅为 SpecDev 规划合同（§1–12）。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| E2E 场景与 Evidence；clientid 互拒断言 | 全部上游票能力；SecurityConfig | 新协议功能；改产品业务逻辑（仅测/证据） |

## 4. 要构建什么

同浏览器：点默认 SSO → sso-web 密码一次 → admin 换票 → 再进 home **不再要密码** → 断言两 Token clientid 不同且互打被拒。

## 5. 实现契约

- **入口：** E2E 规格与运行命令。
- **不变量：** 三门全过；失败不得降级为“部分通过”。
- **安全：** 不把失败改成共享 Token 伪绿。

## 6. 执行路线

1. 固化三门场景用例（先红）。
2. 审计 Origin/callback 可达。
3. 跑同浏览器主路径。
4. 断言 SSO-REUSE 与 CLIENT-ISOLATION。
5. 写 Evidence（授权后）；对照 Spec §9。

## 7. 路径访问契约

- 可写仅 e2e/ 与本 change evidence/；产品代码只读。
- 本轮禁止新建假 `evidence/T-*.md`。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 默认提供方路径 | E2E | 第一按钮→sso-web→双端登录 | AC-001 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/T-06.md</Path>` |
| SSO 复用 | E2E | 第二次业务 App | AC-002 无密码 | 同上 |
| Client 隔离 | API/E2E | 互打 | AC-003 拒绝 | 同上 |
| 回归 | SecurityConfig | clientid 不一致 | 仍拒绝 | 同上 |

- **Workspace checks：** 非 E2E 静态/类型按需。
- **E2E disposition：** **required**；owner=Lead；environment=**current-workspace**（非 source-worktree 冒充通过）。
- **Integration evidence：** 授权后 commit + direct-parent + 父分支 result。

## 9. 发布、迁移与恢复

- **迁移顺序：** 先上游票全绿再跑本票。
- **兼容窗口：** both 保留本地回退。
- **监控信号：** E2E 三门结果；互拒码。
- **回滚：** 关 SSO 开关；本地 login。
- **不可逆：** 无。
- **收缩条件：** 不适用：硬门不可收缩掉。

## 10. 验收标准

- [ ] `AC-001`、`AC-002`、`AC-003` 同场证据。
- [ ] blocked-by-auth 直至授权；不造假 Evidence。
- [ ] 未放宽隔离；未用共享 Token 伪 SSO。

## 11. SKILL 调用计划

engineering-standards、namewta-fullstack-development、deploy-namewta-environment（verify 审计环境）。失败 block-ticket。

## 12. 停止、检查点与交付

任一硬门失败→阻塞 change complete；未授权实施则本票保持 blocked；交付数量空。

