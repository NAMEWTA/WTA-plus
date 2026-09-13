---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描 AGENTS.md 与 <Path>.agents/skills/*/SKILL.md</Path>：适用 engineering-standards、namewta-fullstack-development；已排除 java-api-compatibility（本期不单独交付公开 Java API 兼容演进）、project-customization-delivery（用户未显式激活）。本票不绑 wta-module-guide/deploy；写面仅客户端管理接入控件。"
skill_bindings:
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9","phase":"implement","operation":"client-admin-own-app-sso-access","inputs":["本Ticket T-02 规划合同与路径契约","Tickets Map Skill 矩阵","上游 Spec/ADR/CONTEXT"],"outputs":["符合工程硬约束的实现落点说明","未越界路径与模块边界记录"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"namewta-fullstack-development","path":"<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>","sha256":"65d6f22d11b990de23135c5a2c0bceb3907e964c800d527d58667d12db784988","phase":"implement","operation":"client-admin-own-app-sso-access","inputs":["本Ticket T-02 垂直切片范围","Spec AC 与 DEC 冻结","相关前后端接缝"],"outputs":["跨层合同对齐的可观察产出清单","下一票依赖的稳定接缝说明"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9","phase":"verify","operation":"verify-own-app-access-success-state","inputs":["本Ticket 验证矩阵","定向测试/静态检查范围"],"outputs":["含命令与退出码的 Evidence 草稿字段","残余风险与未验证项"],"required":true,"on_failure":"block-ticket","references":[]}
resource_claims:
  - "client-admin.sso-access"
  - "client-admin.authmode"
  - "own-app.access-success-state"
artifact: ticket
change: 2026-09-12-wta-sso
id: T-02
title: 客户端管理完成自有 App SSO 接入并呈现成功态
status: done
planning_depth: standard
planning_depth_reason: 双面分工下客户端管理仅承接自有接入成功态；需与 SSO 管理登记结果对齐，且禁止「没有接入」伪绿。
ready: false
risk: high
blocked_by: [T-01]
contract_ids: [AC-019]
owner: unassigned
expected_changes:
  - "<Path>frontend/packages/web-domains/system/src/client/ClientPage.vue</Path>"
  - "<Path>frontend/packages/domains/system/src/client/</Path>"
writable_paths:
  - "<Path>frontend/packages/web-domains/system/src/client/</Path>"
  - "<Path>frontend/packages/domains/system/src/client/</Path>"
  - "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysClientController.java</Path>"
  - "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/</Path>"
  - "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/bo/</Path>"
  - "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/vo/</Path>"
read_only_paths:
  - "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/SysClient.java</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>"
  - "<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-02: 客户端管理完成自有 App SSO 接入并呈现成功态

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/02-client-admin-own-app-access-success.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/T-02.md</Path>`

按 Map → 适用 Skill → 本票顺序读取。

## 1. 战略与来源

- **目标：** 对已在 SSO 管理登记的自有 Client，在**客户端管理**完成接入并呈现可判定成功态。
- **可观察产出：** 可开关/配置 `authMode` 并与 context 对齐；呈现可区分于红色「没有接入」的接入成功态；confidential 字段可建但 P0 不验收运行时。
- **来源：** `AC-019`、`DEC-201`/`DEC-203`/`DEC-116`、`ADR-007`。
- **当前事实：** ClientPage 已有 SSO 分组控件（工作树事实）；本票将其收敛为**接入控件**（非创建主路径），并强制成功态可观测。
- **Planning Depth 原因：** 管理面成功态合同 + 与 T-01 双面分离。

## 2. 决策状态

### 已锁定决策

- 客户端管理 = 仅自有 App 接入（DEC-201）；创建/外部建站归 SSO 管理。
- 禁止以红色「没有接入」为完成证据（AC-019/AC-024）。
- D-116=B：字段可建，运行时后置。

### 已采用的低影响假设

- 「接入成功态」文案/图标以实现为准，但必须可自动/人工与「没有接入」区分。

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
| ClientPage / client domain 接入控件；接入成功态呈现；authMode 与 context 对齐 | T-01 已登记 Client 与字段 | 创建应用/拿配置主路径；外部 App 建在客户端管理；authorize/token；三门 E2E |

## 4. 要构建什么

管理员在客户端管理打开已在 SSO 管理登记的自有 Client → 完成 SSO 开关/`authMode` 等接入控件 → UI 呈现接入成功态（非红色「没有接入」）→ context 可读对齐字段，使后续默认提供方路径可走。

## 5. 实现契约

- **入口或接缝：** 客户端管理 Client 详情/编辑中的接入控件。
- **输入与输出：** 已登记 Client；输出接入成功态与 authMode。
- **不变量：** 非创建主路径；成功态 ≠ 「没有接入」。
- **错误与失败行为：** 未在 SSO 管理登记的 Client 不得伪造成功。
- **安全与隐私要求：** 不在此面重复明文密钥交付主路径。

## 6. 执行路线

1. 先红：仍显示「没有接入」时不得标 done。
2. 收敛 ClientPage SSO 分组为接入控件（移除/禁用创建主路径语义）。
3. 对齐后端接入状态字段与 context。
4. 验证成功态可区分；定向回归。

## 7. 路径访问契约

- 可写 ClientPage/client domain 与必要 client service/VO；SysClient 列定义只读（owner T-01）。
- 不写独立 SSO 管理页（T-01）。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 自有接入成功 | 客户端管理 UI | 对已登记 Client 接入 | AC-019 成功态 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/T-02.md</Path>` |
| 禁伪绿 | UI | 观察「没有接入」 | 不得作为完成证据 | 同上 |
| 非创建主路径 | UI | 尝试在客户端管理创建 | 不可用/非主路径 | 同上 |

- **E2E disposition：** not-required：管理面定向；完成态归 T-07。
- **Workspace checks：** 授权后组件/静态；current-workspace。

## 9. 发布、迁移与恢复

- 不适用深度迁移：UI 态与字段开关可回滚。
- 监控：接入态与 context 不一致告警（授权后）。

## 10. 验收标准

- [x] `AC-019`：自有接入成功态可判定；禁「没有接入」伪绿；confidential 字段可建不验收运行时。
- [x] blocked-by-auth；Map/Skill 已读；Evidence 完整。

## 11. SKILL 调用计划

engineering-standards + namewta-fullstack-development（implement/verify）。失败 block-ticket。

## 12. 停止、检查点与交付

未授权即停；无额外命名交付物。
