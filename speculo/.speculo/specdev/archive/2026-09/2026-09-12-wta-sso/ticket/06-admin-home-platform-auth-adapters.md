---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描 AGENTS.md 与 <Path>.agents/skills/*/SKILL.md</Path>：适用 engineering-standards、namewta-fullstack-development；已排除 java-api-compatibility（本期不单独交付公开 Java API 兼容演进）、project-customization-delivery（用户未显式激活）。本票按需绑定 deploy-namewta-environment（Origin/callback 矩阵消费）。"
skill_bindings:
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9","phase":"implement","operation":"wire-admin-home-platform-auth","inputs":["本Ticket T-06 规划合同与路径契约","Tickets Map Skill 矩阵","上游 Spec/ADR/CONTEXT"],"outputs":["符合工程硬约束的实现落点说明","未越界路径与模块边界记录"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"namewta-fullstack-development","path":"<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>","sha256":"65d6f22d11b990de23135c5a2c0bceb3907e964c800d527d58667d12db784988","phase":"implement","operation":"wire-admin-home-platform-auth","inputs":["本Ticket T-06 垂直切片范围","Spec AC 与 DEC 冻结","相关前后端接缝"],"outputs":["跨层合同对齐的可观察产出清单","下一票依赖的稳定接缝说明"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9","phase":"verify","operation":"verify-admin-home-auth-adapters","inputs":["本Ticket 验证矩阵","定向测试/静态检查范围"],"outputs":["含命令与退出码的 Evidence 草稿字段","残余风险与未验证项"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"deploy-namewta-environment","path":"<Path>.agents/skills/deploy-namewta-environment/SKILL.md</Path>","sha256":"4f0ba7e4f618130a9c9021bdd1b3ad4b7b006fdab7819ed1a72630e5870a8ea0","phase":"verify","operation":"audit-origin-callback-matrix","inputs":["本Ticket 范围与模块/环境事实","上游 Spec/ADR"],"outputs":["Skill 约束下的落点/检查记录","失败时阻塞说明"],"required":true,"on_failure":"block-ticket","references":[]}
resource_claims:
  - "platform.auth.contract"
  - "admin-web.sso-login"
  - "home-web.sso-login"
artifact: ticket
change: 2026-09-12-wta-sso
id: T-06
title: admin-web/home-web 经 platform/auth 合同接入 SSO
status: done
planning_depth: standard
planning_depth_reason: 双 App 消费方迁移与纯合同/adapters 分层；需跨包路径所有权与回归；依赖自有接入成功。
ready: false
risk: medium
blocked_by: [T-02, T-04, T-05]
contract_ids: [AC-001, AC-014]
owner: unassigned
expected_changes:
  - "<Path>frontend/packages/platform/auth/src/index.ts</Path>"
  - "<Path>frontend/apps/admin-web/src/</Path>"
  - "<Path>frontend/apps/home-web/src/</Path>"
writable_paths:
  - "<Path>frontend/packages/platform/auth/</Path>"
  - "<Path>frontend/packages/adapters/storage-browser/</Path>"
  - "<Path>frontend/packages/adapters/axios-browser/</Path>"
  - "<Path>frontend/apps/admin-web/src/</Path>"
  - "<Path>frontend/apps/home-web/src/</Path>"
  - "<Path>frontend/packages/web-domains/admin/src/</Path>"
  - "<Path>frontend/packages/domains/admin/src/</Path>"
read_only_paths:
  - "<Path>frontend/apps/sso-web/</Path>"
  - "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path>"
  - "<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>"
shared_paths:
  - "<Path>frontend/packages/web-domains/admin/src/auth/LoginPage.vue</Path>"
shared_path_owners:
  - "<Path>frontend/packages/web-domains/admin/src/auth/LoginPage.vue</Path> => T-05"
---

# Ticket T-06: admin-web/home-web 经 platform/auth 合同接入 SSO

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/06-admin-home-platform-auth-adapters.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/T-06.md</Path>`

按 Map → Skill → 本票读取。

## 1. 战略与来源

- **目标：** admin-web / home-web 经 `platform/auth` 合同与 adapters 接入 SSO 授权码主路径。
- **可观察产出：** 两 App 登录入口可发起默认提供方路径；both 下本地 login 仍通；依赖 T-02：自有 App 必须已接入成功才能走默认提供方路径。
- **来源：** `AC-001`、`AC-014`、`DEC-110`、`ADR-004`。
- **Planning Depth 原因：** 双 App 消费方 + 合同分层。

## 2. 决策状态

### 已锁定决策

- 调用方经 platform/auth，不直调 OAuth 细节散落。
- 保留本地 `POST /auth/login`（AC-014）。
- 自有 App 未接入成功不得宣称 AC-001 默认路径已通。

### 已采用的低影响假设

- Origin/callback 占位由 deploy Skill 审计。

### 产品冻结（本票必须遵守）

- **FirstPartySsoProvider**：第三方登录目录第一、默认接通、按钮→`sso-web` 授权码。
- **三门硬验收并列**：`P0-DEFAULT-PROVIDER-PATH`(AC-001) / `P0-SSO-REUSE`(AC-002) / `P0-CLIENT-ISOLATION`(AC-003)；本轮未派 I 前不得宣称三门已通。
- **D-200/201/202/203 = A**；**DEC-200…203**：独立「SSO 管理」创建应用+拿配置；双面分工；数据仍扩展 `sys_client`；废止「创建主路径=客户端管理」。
- **D-111=B**：外部仅在 **SSO 管理** 登记+配置交付；不要求 P0 跑通外部 confidential 运行时。
- **D-116=B**：confidential 字段可建，运行时后置。
- **协议**：Authorization Code + PKCE S256；`access_token`=现有 Sa-Token；extras=`clientid`=**目标业务 Client**（禁止 `sso`）。
- **authMode**：`both`|`sso`|`local`；保留 `POST /auth/login`。
- **禁止**：OIDC / SLO / 独立进程 / Keycloak·Casdoor·Logto·Hydra / 共享业务 Token / Implicit / password grant / SAML。
- **禁止**：改动 notify 归档 change；把创建主路径塞回客户端管理；以红色「没有接入」当完成；push/PR；伪造 Evidence；触碰已删 `ruoyi-vue-plus-docs`。

### blocked-by-auth

**已解除（CTO-t316s1 / Lead 书面 I Round4）：** `implementation_commit` 已授权，范围仅 T-01…07。旧 t275u / `superseded-r3` 不算完成。仍须遵守 DAG `blocked_by`；禁止 push/CR 直至真 E2E 过门。


### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| platform/auth、adapters、admin-web、home-web 登录入口接线 | T-05 context/槽位原型；T-04 sso-web；T-02 接入成功态 | SSO 管理创建；OAuth 模块本体；三门硬 E2E（T-07） |

## 4. 要构建什么

用户打开 admin 或 home 登录页 → 经 platform/auth 读 context → 点第一 SSO 按钮发起授权码 → 回跳换票进业务会话；同时 both 模式本地密码仍可用。

## 5. 实现契约

- **入口或接缝：** `frontend/packages/platform/auth` + App 登录入口。
- **不变量：** 不共享业务 Token；clientid 隔离。
- **兼容：** 本地 login 保留。

## 6. 执行路线

1. 确认 T-02 接入成功态可用。
2. 扩展 platform/auth 合同与 adapters。
3. 接线 admin-web / home-web 登录入口。
4. deploy Skill 审计 Origin/callback。
5. 定向回归。

## 7. 路径访问契约

- LoginPage 原型 owner 仍 T-05；本票可消费/扩展调用方，不抢 T-05 shared owner。
- AuthController 只读（owner T-05）。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 默认提供方路径（调用方） | admin/home 登录 | 点第一按钮 | AC-001 可发起 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/T-06.md</Path>` |
| both+本地 login | API/UI | POST /auth/login | AC-014 | 同上 |
| 未接入不得伪通 | 管理面 | 红色「没有接入」 | 阻塞完成 | 同上 |

- **E2E disposition：** not-required：调用方接线；硬三门归 T-07（required）。
- **Workspace checks：** 授权后 current-workspace。

## 9. 发布、迁移与恢复

- 回滚：关 ssoEnabled / 恢复本地-only 入口。
- 环境占位未替换不得上线。

## 10. 验收标准

- [x] `AC-001`（调用方部分）、`AC-014`；依赖 T-02 接入成功；blocked-by-auth。

## 11. SKILL 调用计划

engineering-standards + namewta-fullstack-development + deploy-namewta-environment（verify）。失败 block-ticket。

## 12. 停止、检查点与交付

未授权即停；交付数量空。
