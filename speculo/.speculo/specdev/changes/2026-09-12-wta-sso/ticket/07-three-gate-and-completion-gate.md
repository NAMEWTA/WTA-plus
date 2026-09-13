---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描 AGENTS.md 与 <Path>.agents/skills/*/SKILL.md</Path>：适用 engineering-standards、namewta-fullstack-development；已排除 java-api-compatibility（本期不单独交付公开 Java API 兼容演进）、project-customization-delivery（用户未显式激活）。本票按需绑定 deploy-namewta-environment（E2E 运行时 Origin/callback 审计）。"
skill_bindings:
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9","phase":"implement","operation":"prove-three-hard-gates-and-ac024","inputs":["本Ticket T-07 规划合同与路径契约","Tickets Map Skill 矩阵","上游 Spec/ADR/CONTEXT"],"outputs":["符合工程硬约束的实现落点说明","未越界路径与模块边界记录"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"namewta-fullstack-development","path":"<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>","sha256":"65d6f22d11b990de23135c5a2c0bceb3907e964c800d527d58667d12db784988","phase":"implement","operation":"prove-three-hard-gates-and-ac024","inputs":["本Ticket T-07 垂直切片范围","Spec AC 与 DEC 冻结","相关前后端接缝"],"outputs":["跨层合同对齐的可观察产出清单","下一票依赖的稳定接缝说明"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9","phase":"verify","operation":"verify-three-gates-and-completion-evidence","inputs":["本Ticket 验证矩阵","定向测试/静态检查范围"],"outputs":["含命令与退出码的 Evidence 草稿字段","残余风险与未验证项"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"deploy-namewta-environment","path":"<Path>.agents/skills/deploy-namewta-environment/SKILL.md</Path>","sha256":"4f0ba7e4f618130a9c9021bdd1b3ad4b7b006fdab7819ed1a72630e5870a8ea0","phase":"verify","operation":"audit-e2e-origin-callback-runtime","inputs":["本Ticket 范围与模块/环境事实","上游 Spec/ADR"],"outputs":["Skill 约束下的落点/检查记录","失败时阻塞说明"],"required":true,"on_failure":"block-ticket","references":[]}
resource_claims:
  - "p0.default-provider-path"
  - "p0.sso-reuse"
  - "p0.client-isolation"
  - "p0.completion-gate-ac024"
  - "e2e.sso-admin-home"
artifact: ticket
change: 2026-09-12-wta-sso
id: T-07
title: 三门硬 E2E + 双面管理成功态（AC-024）
status: done
planning_depth: deep
planning_depth_reason: change 级硬验收汇合；跨 Origin/双 App/Token 隔离 + 双面管理完成态门禁；需发布恢复与 Lead E2E。
ready: false
risk: critical
blocked_by: [T-06]
contract_ids: [AC-001, AC-002, AC-003, AC-024]
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
shared_paths: []
shared_path_owners: []
---

# Ticket T-07: 三门硬 E2E + 双面管理成功态（AC-024）

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/07-three-gate-and-completion-gate.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/T-07.md</Path>`

按 Map → Skill → 本票读取。

## 1. 战略与来源

- **目标：** 同浏览器证明三门硬验收，**并且** Evidence 同时包含双面管理成功态（AC-024）。
- **可观察产出：** SSO→admin→home 证明 AC-001/002/003；Evidence **必须**同时包含：SSO 管理 (a)(b)(c) 成功 + 自有接入成功态 + 三门。若仅 Token 隔离、或管理面仍红色「没有接入」→ **本票不得 done**。
- **来源：** `AC-001`/`AC-002`/`AC-003`/`AC-024`、`DEC-115`/`DEC-011`、`ADR-007`。
- **Planning Depth 原因：** change 级汇合 + 完成态门禁。

## 2. 决策状态

### 已锁定决策

- 三门并列硬验收，不可互相替代。
- AC-024：缺少管理成功或接入成功或三门任一 → 不合格。
- 历史截图/旧 Evidence 不当完成证据。

### 已采用的低影响假设

无。

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
| `frontend/e2e/` 场景；本 change evidence/ 完成态清单 | 上游全部功能票产出 | 新功能实现；改产品协议；OIDC/SLO |

## 4. 要构建什么

在真实/约定环境跑：SSO 管理完成创建+回调+配置 → 客户端管理呈现自有接入成功态 → 同浏览器 SSO 登录 → admin 换票 → home 复用无再输密码 → Token clientid 互拒 → 将上述全部写入 Evidence；缺任一则失败。

## 5. 实现契约

- **入口或接缝：** E2E 套件 + Evidence 评审清单。
- **不变量：** AC-024 三要素齐全；禁「没有接入」伪绿。
- **错误与失败行为：** 任一门失败 → 票不得 done。

## 6. 执行路线

1. 编写/对齐 E2E：管理面成功 + 接入成功 + 三门。
2. deploy Skill 审计 Origin/callback 运行时。
3. Lead 在 parent-candidate/current（按 Goal Plan）执行 required E2E。
4. 写 Evidence；评审 AC-024 清单。
5. 任一缺口 → 保持 blocked/非 done。

## 7. 路径访问契约

- 可写 `frontend/e2e/` 与本 change `evidence/`。
- 只读业务 App 与 SecurityConfig。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 默认提供方路径 | E2E | SSO→admin | AC-001 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/T-07.md</Path>` |
| SSO 复用 | E2E | 同浏览器→home | AC-002 不再输密码 | 同上 |
| Client 隔离 | API/E2E | Token 互打 | AC-003 | 同上 |
| 完成态门禁 | Evidence 评审 | 清单三要素 | AC-024；缺一失败 | 同上 |
| 伪绿禁入 | 管理面 | 「没有接入」 | 不得 done | 同上 |

- **Workspace checks：** 非 E2E 静态可在 current-workspace。
- **E2E disposition：** **required**：三门+管理成功态；不得在 Ticket source worktree 宣称通过。
- **E2E owner/environment：** Lead / parent-candidate 或 current（Goal Plan）。
- **Integration evidence：** 授权后 commit + 父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 全部上游票完成后才跑本票。
- **监控信号：** E2E 失败分类。
- **回滚：** 关 `namewta.sso.enabled` / 恢复本地登录。
- **不可逆：** 无。
- **收缩条件：** 不适用。

## 10. 验收标准

- [x] `AC-001`、`AC-002`、`AC-003` 硬证据齐全。
- [x] `AC-024`：Evidence 同时含 SSO 管理 (a)(b)(c) + 自有接入成功态 + 三门；禁「没有接入」伪绿。
- [x] blocked-by-auth；required E2E 由 Lead 在正确环境执行。

## 11. SKILL 调用计划

engineering-standards + namewta-fullstack-development + deploy-namewta-environment（verify）。失败 block-ticket。

## 12. 停止、检查点与交付

未授权即停；缺 AC-024 任一要素不得标 done；交付数量空。
