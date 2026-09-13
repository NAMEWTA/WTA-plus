---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描 AGENTS.md 与 <Path>.agents/skills/*/SKILL.md</Path>：适用 engineering-standards、namewta-fullstack-development；已排除 java-api-compatibility（本期不单独交付公开 Java API 兼容演进）、project-customization-delivery（用户未显式激活）。本票不绑 module-guide/deploy。"
skill_bindings:
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9","phase":"implement","operation":"wire-first-party-provider-context","inputs":["本Ticket T-04 规划合同与路径契约","Tickets Map Skill 矩阵","上游 Spec/ADR/CONTEXT"],"outputs":["符合工程硬约束的实现落点说明","未越界路径与模块边界记录"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"namewta-fullstack-development","path":"<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>","sha256":"65d6f22d11b990de23135c5a2c0bceb3907e964c800d527d58667d12db784988","phase":"implement","operation":"wire-first-party-provider-context","inputs":["本Ticket T-04 垂直切片范围","Spec AC 与 DEC 冻结","相关前后端接缝"],"outputs":["跨层合同对齐的可观察产出清单","下一票依赖的稳定接缝说明"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9","phase":"verify","operation":"verify-authmode-and-slot","inputs":["本Ticket 验证矩阵","定向测试/静态检查范围"],"outputs":["含命令与退出码的 Evidence 草稿字段","残余风险与未验证项"],"required":true,"on_failure":"block-ticket","references":[]}
resource_claims:
  - "first-party-sso-provider.slot"
  - "auth.client.context.sso"
  - "authMode.both-sso-local"
artifact: ticket
change: 2026-09-12-wta-sso
id: T-04
title: 接通 FirstPartySsoProvider 槽位、client context 与 authMode
status: done
planning_depth: standard
planning_depth_reason: 改变登录入口默认 UX 与 /auth/client/context 公共合同；决策已由 Spec 锁定。
ready: true
risk: high
blocked_by: [T-01, T-02]
contract_ids: [AC-001, AC-014, AC-015, AC-023]
owner: unassigned
expected_changes:
  - "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path>"
  - "<Path>frontend/packages/web-domains/admin/src/auth/LoginPage.vue</Path>"
writable_paths:
  - "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path>"
  - "<Path>backend/wta-admin/src/main/java/org/namewta/web/service/</Path>"
  - "<Path>backend/wta-admin/src/test/java/org/namewta/web/</Path>"
  - "<Path>frontend/packages/domains/admin/src/</Path>"
  - "<Path>frontend/packages/web-domains/admin/src/auth/</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path>"
read_only_paths:
  - "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/SysClient.java</Path>"
  - "<Path>backend/wta-admin/src/main/java/org/namewta/web/service/impl/SocialAuthStrategy.java</Path>"
  - "<Path>backend/wta-modules/wta-sso/</Path>"
  - "<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>"
shared_paths:
  - "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path>"
  - "<Path>frontend/packages/web-domains/admin/src/auth/LoginPage.vue</Path>"
shared_path_owners:
  - "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path> => T-04"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path> => T-01"
  - "<Path>frontend/packages/web-domains/admin/src/auth/LoginPage.vue</Path> => T-04"
---

# Ticket T-04: 接通 FirstPartySsoProvider 槽位、client context 与 authMode

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/04-first-party-provider-context-authmode.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/T-04.md</Path>`

按 Map → Skill → 本票读取。

## 1. 战略与来源

- **目标：** 预置 FirstPartySsoProvider 为第三方登录目录第一且默认启用；扩展 `GET /auth/client/context`；落实 authMode。
- **可观察产出：** context 含 `ssoEnabled`/`ssoAuthorizeUrl`/`authMode`；both 下本地框+第一 SSO 按钮；sso 入口可直达授权码路径。
- **来源：** `US-001`/`US-006`、`AC-001`/`AC-014`/`AC-015`/`AC-023`、`DEC-100`/`DEC-003`/`DEC-110`/`DEC-114`、`ADR-001`/`ADR-004`、`<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path>`。
- **Planning Depth 原因：** 公共 context 合同+默认 UX。

## 2. 决策状态

### 已锁定决策

- FirstPartySsoProvider 同目录第一、默认接通（废止 NoExternalIdP 口号）。
- 默认 admin/home=`both`；可部分入口 `sso`；保留 `POST /auth/login`。
- 按钮→授权码到 sso-web（D-110=C）。

### 已采用的低影响假设

- Provider 物理表可为 social 配置扩展或等价目录（D-114）。

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
| Provider 预置/排序；context 扩展；authMode 行为；登录页槽位 | IAuthStrategy/social；SysClient 字段；T-02 authorize URL | admin/home 全量 adapters（T-05）；三门 E2E（T-06）；OIDC |

## 4. 要构建什么

业务登录页第三方区第一项为 WTA SSO → 点按走授权码；context 免配可读；both 保留本地密码；sso 模式可不展示本地框。

## 5. 实现契约

- **入口或接缝：** `/auth/client/context`；登录页提供方目录。
- **输出：** ssoEnabled/ssoAuthorizeUrl/authMode；第一按钮行为。
- **不变量：** ThirdPartySlotFirst；本地 login 保留。
- **兼容：** social 仍在其后。

## 6. 执行路线

1. context 契约测试（缺字段失败）。
2. 扩展 AuthController context。
3. Provider 预置种子与排序。
4. 登录页第一按钮绑定 authorize URL。
5. authMode both/sso/local 分支。
6. 定向验证。

## 7. 路径访问契约

- AuthController owner=T-04；DML 种子写时尊重 T-01 owner。
- T-04 只稳定 context 契约 + 槽位种子 + admin 登录页第一按钮原型（`LoginPage.vue`）。
- home-web 登录槽位与 platform 调用方归 T-05；本票不把 home App 列入完成条件。调用方/home 接线后 **owner 迁到 T-05**。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 默认路径槽位 | context+登录页 | 读配置/打开页 | AC-001/023 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/T-04.md</Path>` |
| both | 登录页+API | 本地 login | AC-014 | 同上 |
| sso 入口 | 配置入口 | 直达授权码 | AC-015 | 同上 |
| 回归 | social 顺序 | 打开第三方区 | Mask/GitHub 在后 | 同上 |

- **E2E disposition：** not-required：槽位/合同；完整默认路径 E2E 归 T-06（可与 T-05 联调）。
- **Workspace checks：** 授权后 API+组件测；current-workspace。

## 9. 发布、迁移与恢复

- 默认 both 降低切断风险；回滚关 ssoEnabled。
- 监控：context 拉取失败。
- 收缩条件：不适用。

## 10. 验收标准

- [ ] `AC-001`（槽位/context 部分）、`AC-014`、`AC-015`、`AC-023`；blocked-by-auth。

## 11. SKILL 调用计划

engineering-standards + namewta-fullstack-development。失败 block-ticket。

## 12. 停止、检查点与交付

未授权即停；交付数量空。

