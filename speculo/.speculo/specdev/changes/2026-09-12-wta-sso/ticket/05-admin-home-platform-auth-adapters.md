---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描 AGENTS.md 与 <Path>.agents/skills/*/SKILL.md</Path>：适用 engineering-standards、namewta-fullstack-development；已排除 java-api-compatibility（本期不单独交付公开 Java API 兼容演进）、project-customization-delivery（用户未显式激活）。本票按需绑定 deploy-namewta-environment（回调/独立 Origin 矩阵消费）。"
skill_bindings:
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9","phase":"implement","operation":"integrate-apps-platform-auth-sso","inputs":["本Ticket T-05 规划合同与路径契约","Tickets Map Skill 矩阵","上游 Spec/ADR/CONTEXT"],"outputs":["符合工程硬约束的实现落点说明","未越界路径与模块边界记录"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"namewta-fullstack-development","path":"<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>","sha256":"65d6f22d11b990de23135c5a2c0bceb3907e964c800d527d58667d12db784988","phase":"implement","operation":"integrate-apps-platform-auth-sso","inputs":["本Ticket T-05 垂直切片范围","Spec AC 与 DEC 冻结","相关前后端接缝"],"outputs":["跨层合同对齐的可观察产出清单","下一票依赖的稳定接缝说明"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9","phase":"verify","operation":"verify-app-sso-entry-regression","inputs":["本Ticket 验证矩阵","定向测试/静态检查范围"],"outputs":["含命令与退出码的 Evidence 草稿字段","残余风险与未验证项"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"deploy-namewta-environment","path":"<Path>.agents/skills/deploy-namewta-environment/SKILL.md</Path>","sha256":"4f0ba7e4f618130a9c9021bdd1b3ad4b7b006fdab7819ed1a72630e5870a8ea0","phase":"implement","operation":"consume-origin-callback-matrix","inputs":["本Ticket 范围与模块/环境事实","上游 Spec/ADR"],"outputs":["Skill 约束下的落点/检查记录","失败时阻塞说明"],"required":true,"on_failure":"block-ticket","references":[]}
resource_claims:
  - "platform.auth.sso-contract"
  - "adapters.browser.sso"
  - "admin-web.sso-entry"
  - "home-web.sso-entry"
artifact: ticket
change: 2026-09-12-wta-sso
id: T-05
title: admin-web/home-web 经 platform/auth 合同接入 SSO
status: done
planning_depth: standard
planning_depth_reason: 双 App 消费方迁移与纯合同/adapters 分层；需跨包路径所有权与回归。
ready: true
risk: medium
blocked_by: [T-03, T-04]
contract_ids: [AC-001, AC-014]
owner: unassigned
expected_changes:
  - "<Path>frontend/packages/platform/auth/src/index.ts</Path>"
  - "<Path>frontend/apps/admin-web/src/</Path>"
  - "<Path>frontend/apps/home-web/src/</Path>"
  - "<Path>frontend/apps/home-web/src/router/index.ts</Path>"
  - "<Path>frontend/apps/home-web/src/router/homeManifestRegistry.ts</Path>"
writable_paths:
  - "<Path>frontend/packages/platform/auth/</Path>"
  - "<Path>frontend/packages/adapters/storage-browser/</Path>"
  - "<Path>frontend/packages/adapters/axios-browser/</Path>"
  - "<Path>frontend/apps/admin-web/src/</Path>"
  - "<Path>frontend/apps/home-web/src/</Path>"
  - "<Path>frontend/packages/web-domains/admin/src/</Path>"
  - "<Path>frontend/packages/domains/admin/src/</Path>"
read_only_paths:
  - "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path>"
  - "<Path>frontend/apps/sso-web/</Path>"
  - "<Path>frontend/e2e/client-auth-context.spec.ts</Path>"
  - "<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>"
shared_paths:
  - "<Path>frontend/packages/platform/auth/</Path>"
  - "<Path>frontend/packages/web-domains/admin/src/</Path>"
  - "<Path>frontend/packages/domains/admin/src/</Path>"
shared_path_owners:
  - "<Path>frontend/packages/platform/auth/</Path> => T-05"
  - "<Path>frontend/packages/web-domains/admin/src/</Path> => T-05"
  - "<Path>frontend/packages/domains/admin/src/</Path> => T-05"
---

# Ticket T-05: admin-web/home-web 经 platform/auth 合同接入 SSO

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/05-admin-home-platform-auth-adapters.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/T-05.md</Path>`

按 Map → Skill（含 deploy）→ 本票读取。

## 1. 战略与来源

- **目标：** 在 `packages/platform/auth` 提供无浏览器依赖的 `startSsoLogin`/`handleCallback`；browser 侧 adapters；admin-web/home-web 调用方接入。
- **可观察产出：** 两 App 登录页可走默认 SSO 按钮并完成回调换票；both 下本地 login 仍通。home-web 登录页同样暴露第三方区第一 SSO 按钮并完成回调换票（AC-001 调用方）。
- **来源：** `AC-001`/`AC-014`（调用方）、`DEC-014`/`DEC-015`、`ADR-003`/`ADR-004`。
- **Planning Depth 原因：** 跨 App 消费迁移。

## 2. 决策状态

### 已锁定决策

- platform/auth 纯合同；browser 在 adapters。
- 第一方 SPA=public+PKCE；禁持 secret。
- 保留 POST /auth/login。

### 已采用的低影响假设

- home 登录组件经 identity-access 注册点接入既有壳。

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
| platform/auth SSO API；adapters；admin/home 接入 | T-03 sso-web；T-04 context/槽位 | 三门硬 E2E 证据权威（T-06）；后端 OAuth |

## 4. 要构建什么

用户在 admin 或 home 点第一 SSO 按钮 → startSsoLogin（PKCE+state）→ sso-web → 回调 handleCallback 换票 → 业务 Bearer 存储键写入；本地 login 并存。

## 5. 实现契约

- **入口：** 登录页按钮；callback 路由。
- **不变量：** 无 secret 进包；state CSRF；Token 存业务键。
- **兼容：** 现有 client context E2E 先例可扩展。

## 6. 执行路线

1. 合同测试：platform/auth 无 DOM。
2. 实现 startSsoLogin/handleCallback + adapters。
3. admin-web / home-web 接线。
4. 消费 Origin/callback 矩阵占位。
5. 回归本地 login + 构建。

## 7. 路径访问契约

- platform/auth、admin/home 调用方与 `domains/admin` / `web-domains/admin` 目录 owner=T-05（承接 T-04 槽位后的调用方）。
- T-04 仅保留 `LoginPage.vue` 原型与 context 契约；home-web 登录槽位在本票完成。
- 不改 sso-web / 后端。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 默认按钮调用方 | 两 App 登录页 | 点击 SSO | 进入授权码路径 AC-001 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/T-05.md</Path>` |
| both 本地 | POST /auth/login | 密码登录 | AC-014 | 同上 |
| 合同纯度 | 静态/单测 | platform/auth | 无 browser import | 同上 |
| 回归 | 既有 auth context | e2e 先例扩展准备 | 不破坏 | 同上 |

- **E2E disposition：** required（调用方路径）：Lead / current-workspace；三门完整场景仍以 T-06 为准。
- **Workspace checks：** FE test/build；current-workspace。

## 9. 发布、迁移与恢复

- both 默认可回退本地；监控回调失败。
- 收缩条件：旧仅本地入口流量可观测后无强制收缩。

## 10. 验收标准

- [ ] `AC-001`/`AC-014` 调用方；blocked-by-auth；不造假 Evidence。

## 11. SKILL 调用计划

engineering-standards、namewta-fullstack-development、deploy-namewta-environment（矩阵消费）。失败 block-ticket。

## 12. 停止、检查点与交付

未授权即停；交付数量空。

