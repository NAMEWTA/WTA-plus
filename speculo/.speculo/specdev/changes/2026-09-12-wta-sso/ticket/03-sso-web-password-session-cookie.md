---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描 AGENTS.md 与 <Path>.agents/skills/*/SKILL.md</Path>：适用 engineering-standards、namewta-fullstack-development；已排除 java-api-compatibility（本期不单独交付公开 Java API 兼容演进）、project-customization-delivery（用户未显式激活）。本票不绑 wta-module-guide/common-modules/deploy（模块已由 T-02；环境矩阵归 T-05/T-06）。"
skill_bindings:
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9","phase":"implement","operation":"build-sso-web-session-cookie","inputs":["本Ticket T-03 规划合同与路径契约","Tickets Map Skill 矩阵","上游 Spec/ADR/CONTEXT"],"outputs":["符合工程硬约束的实现落点说明","未越界路径与模块边界记录"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"namewta-fullstack-development","path":"<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>","sha256":"65d6f22d11b990de23135c5a2c0bceb3907e964c800d527d58667d12db784988","phase":"implement","operation":"build-sso-web-session-cookie","inputs":["本Ticket T-03 垂直切片范围","Spec AC 与 DEC 冻结","相关前后端接缝"],"outputs":["跨层合同对齐的可观察产出清单","下一票依赖的稳定接缝说明"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9","phase":"verify","operation":"verify-sso-cookie-owner","inputs":["本Ticket 验证矩阵","定向测试/静态检查范围"],"outputs":["含命令与退出码的 Evidence 草稿字段","残余风险与未验证项"],"required":true,"on_failure":"block-ticket","references":[]}
resource_claims:
  - "sso-web.app"
  - "sso.session.cookie"
  - "sso.login.password-only"
artifact: ticket
change: 2026-09-12-wta-sso
id: T-03
title: 交付 sso-web（仅密码）与后端 SSO 域 Set-Cookie 会话
status: done
planning_depth: standard
planning_depth_reason: 独立 Origin 登录页 + 后端 Set-Cookie 两层会话；需完整实现契约与验证，决策已锁定。
ready: true
risk: high
blocked_by: [T-02]
contract_ids: [AC-016, AC-020]
owner: unassigned
expected_changes:
  - "<Path>frontend/apps/sso-web/</Path>"
  - "<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/session/</Path>"
writable_paths:
  - "<Path>frontend/apps/sso-web/</Path>"
  - "<Path>frontend/pnpm-workspace.yaml</Path>"
  - "<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/session/</Path>"
  - "<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/web/</Path>"
  - "<Path>backend/wta-modules/wta-sso/src/test/java/org/namewta/sso/session/</Path>"
read_only_paths:
  - "<Path>backend/wta-admin/src/main/java/org/namewta/web/service/impl/PasswordAuthStrategy.java</Path>"
  - "<Path>frontend/packages/platform/auth/src/index.ts</Path>"
  - "<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>"
shared_paths:
  - "<Path>frontend/pnpm-workspace.yaml</Path>"
shared_path_owners:
  - "<Path>frontend/pnpm-workspace.yaml</Path> => T-03"
---

# Ticket T-03: 交付 sso-web（仅密码）与后端 SSO 域 Set-Cookie 会话

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/03-sso-web-password-session-cookie.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/T-03.md</Path>`

按 Map → 适用 Skill → 本票顺序读取。

## 1. 战略与来源

- **目标：** 交付 `sso-web`（ClientId=`sso`，会话键=`Sso-Token`）只收本仓密码；SSO 域会话由后端 Set-Cookie。
- **可观察产出：** 登录页无 Mask/GitHub；成功后网络面板可见后端 Set-Cookie；业务 App 不把 SSO Cookie 当 API Token。
- **来源：** `US-001`、`AC-016`、`AC-020`、`DEC-112`/`DEC-013`/`DEC-004`、`ADR-005`。
- **当前事实：** 无 `frontend/apps/sso-web`；业务 Cookie 默认关闭、走 Bearer。
- **Planning Depth 原因：** 两层会话边界+独立 Origin UX。

## 2. 决策状态

### 已锁定决策

- sso-web 只密码；social 仅业务 App 且排在自建 SSO 后。
- SSO HttpOnly Cookie 后端 Set-Cookie；禁 JS storage 冒充 SSO 会话。
- 同进程组装 + sso-web 独立 Web Origin。

### 已采用的低影响假设

- Cookie 名/Domain 随环境矩阵占位，语义锁定 D-013。

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
| sso-web 应用；SSO 密码登录+Set-Cookie 会话 | Password 校验能力；T-02 authorize 会话检查钩子 | OAuth token 实现；业务 App 接入；OIDC；social 在 sso-web |

## 4. 要构建什么

用户被 redirect 到 sso-web → 仅账号密码 → 后端 Set-Cookie 建 SSO 域会话 → 回到 authorize 续发 code；无 SSO 会话时才要密码。

## 5. 实现契约

- **入口或接缝：** sso-web 登录页 + SSO session API。
- **输入与输出：** 本仓用户名密码；Set-Cookie。
- **不变量：** TwoLayerSession；AC-020 无 social。
- **错误与失败行为：** 密码错误不建会话；不泄露是否用户存在的敏感细节按现有策略。
- **安全与隐私要求：** HttpOnly；业务仅 Bearer。

## 6. 执行路线

1. 负向：sso-web 不应出现 social 按钮。
2. 建 sso-web 应用骨架与路由。
3. 后端 session/login Set-Cookie。
4. 与 T-02 authorize 无会话→跳登录衔接。
5. 浏览器网络面板验证 Cookie owner。

## 7. 路径访问契约

- 可写 sso-web + wta-sso session/web 包；不改 platform/auth 合同（归 T-05）。
- 与 T-02 串行依赖，允许在 wta-sso 会话包落点。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 只密码 | UI | 打开 sso-web | AC-020 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/T-03.md</Path>` |
| Cookie owner | 网络面板 | 登录成功 | AC-016 后端 Set-Cookie | 同上 |
| 失败路径 | 错密码 | 登录 | 无有效 SSO 会话 | 同上 |
| 回归 | authorize 无会话 | 跳登录 | 可续 | 同上 |

- **Workspace checks：** 授权后 FE build + 后端 session 测。
- **E2E disposition：** required（Cookie 域）：Lead / current-workspace；完整三门仍归 T-06。
- **Integration evidence：** 授权后 commit + direct-parent。

## 9. 发布、迁移与恢复

- 不适用深度迁移：随 T-02 开关；回滚关 SSO 或摘 sso-web 路由。
- 监控：SSO 登录失败率。
- 收缩条件：不适用。

## 10. 验收标准

- [ ] `AC-016`、`AC-020`；blocked-by-auth；不造假 Evidence。

## 11. SKILL 调用计划

engineering-standards + namewta-fullstack-development（implement/verify）。失败 block-ticket。

## 12. 停止、检查点与交付

未授权即停；交付数量空；完成出口仅授权后。

