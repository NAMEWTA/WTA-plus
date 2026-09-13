---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描 AGENTS.md 与 <Path>.agents/skills/*/SKILL.md</Path>：适用 engineering-standards、namewta-fullstack-development；已排除 java-api-compatibility（本期不单独交付公开 Java API 兼容演进）、project-customization-delivery（用户未显式激活）。本票不绑 wta-module-guide（不新建 wta-sso 模块本体）；不绑 deploy。"
skill_bindings:
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9","phase":"implement","operation":"build-sso-admin-create-config","inputs":["本Ticket T-01 规划合同与路径契约","Tickets Map Skill 矩阵","上游 Spec/ADR/CONTEXT"],"outputs":["符合工程硬约束的实现落点说明","未越界路径与模块边界记录"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"namewta-fullstack-development","path":"<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>","sha256":"65d6f22d11b990de23135c5a2c0bceb3907e964c800d527d58667d12db784988","phase":"implement","operation":"build-sso-admin-create-config","inputs":["本Ticket T-01 垂直切片范围","Spec AC 与 DEC 冻结","相关前后端接缝"],"outputs":["跨层合同对齐的可观察产出清单","下一票依赖的稳定接缝说明"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9","phase":"verify","operation":"verify-sso-admin-opsflow","inputs":["本Ticket 验证矩阵","定向测试/静态检查范围"],"outputs":["含命令与退出码的 Evidence 草稿字段","残余风险与未验证项"],"required":true,"on_failure":"block-ticket","references":[]}
resource_claims:
  - "sys_client.sso-fields"
  - "sso-admin.menu"
  - "sso-admin.opsflow"
  - "sso.secret-hash"
artifact: ticket
change: 2026-09-12-wta-sso
id: T-01
title: 独立「SSO 管理」创建应用与配置交付（扩展 sys_client）
status: done
planning_depth: deep
planning_depth_reason: 新建独立 SSO 管理菜单/页/API，并扩展共享 sys_client schema/密钥哈希；后续 OAuth/context/接入态依赖其稳定；含 DDL/DML 与密钥安全。
ready: true
risk: high
blocked_by: []
contract_ids: [AC-017, AC-018]
owner: unassigned
expected_changes:
  - "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/SysClient.java</Path>"
  - "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/</Path>"
  - "<Path>frontend/packages/web-domains/system/src/</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path>"
writable_paths:
  - "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/</Path>"
  - "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/bo/</Path>"
  - "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/vo/</Path>"
  - "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/</Path>"
  - "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/mapper/</Path>"
  - "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/</Path>"
  - "<Path>frontend/packages/web-domains/system/src/</Path>"
  - "<Path>frontend/packages/domains/system/src/</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path>"
read_only_paths:
  - "<Path>frontend/packages/web-domains/system/src/client/ClientPage.vue</Path>"
  - "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path>"
  - "<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>"
shared_paths:
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path>"
shared_path_owners:
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path> => T-01"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path> => T-01"
---

# Ticket T-01: 独立「SSO 管理」创建应用与配置交付（扩展 sys_client）

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/01-sso-admin-create-and-config.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/T-01.md</Path>`

实现本 Ticket 时，Lead 与 implementation subagent 必须按顺序完整读取总体 Map，读取项目 Skill 的 frontmatter 与入口并只展开适用于 `ALL`/`T-01` 的匹配项，再读取本 Ticket 与相关上游工件。

## 1. 战略与来源

- **目标：** 在系统管理下交付**独立「SSO 管理」**，完成创建应用与配置交付；数据仍扩展 `sys_client`（D-202=A）。
- **可观察产出：** 管理员可在独立 SSO 管理完成 (a) 创建自有或外部应用 (b) 精确回调白名单（禁 `*`）(c) 获取/交付 client 与密钥（明文只一次，存 `sso_secret_hash`）；行落扩展后 `sys_client`；**不**在客户端管理完成创建。
- **来源：** `US-005`/`US-008`、`AC-017`/`AC-018`、`DEC-200`/`DEC-201`/`DEC-202`/`DEC-203`、`DEC-111`/`DEC-116`、`ADR-007`。
- **当前事实：** 工作树 `<Path>frontend/packages/web-domains/system/src/client/ClientPage.vue</Path>` 已有「SSO 接入」分组（Round3 错位主路径）；本票改为**独立菜单/页**承接创建+拿配置；客户端管理接入归 T-02。
- **Planning Depth 原因：** 新管理面 + schema/密钥 + 共享 Client 契约。

## 2. 决策状态

### 已锁定决策

- 独立 SSO 管理 = 注册+配置交付主路径（DEC-200/201）。
- 数据仍扩展 `sys_client`，不平行 OAuth 主表（DEC-202）；短寿命 code 表追加归 T-03，shared owner 仍本票。
- 外部仅 SSO 管理登记（D-111=B）；confidential 字段可建、运行时后置（D-116=B）。
- `client_secret` ≠ OAuth 密钥；用 `sso_secret_hash`。

### 已采用的低影响假设

- Origin/callback 字面量可占位；菜单路由名以实现时扫描为准。

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

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 新 SSO 管理 API/菜单/页；SysClient 字段/BO/VO；DDL `50-namewta-ddl.sql` + DML `60-namewta-dml.sql`（本票为 50/60 owner） | 现有 SysClient 基座、菜单权限模型 | 客户端管理接入 UI（T-02）；authorize/token（T-03）；sso-web（T-04）；confidential 运行时 |

## 4. 要构建什么

管理员进入系统管理 → 打开独立「SSO 管理」→ (a) 创建自有或外部应用 → (b) 配精确回调白名单（拒绝 `*`）→ (c) 获取/交付 clientId 与密钥（明文只展示一次，持久化为 hash）→ 行写入扩展后的 `sys_client`。客户端管理页**不是**创建主路径。

## 5. 实现契约

- **入口或接缝：** 系统管理下独立 SSO 管理菜单 + 对应管理 API。
- **输入与输出：** 应用元数据、回调 URI 列表、client 类型；输出 clientId、一次性明文密钥、持久化 hash。
- **公共接口变化：** 新 SSO 管理 API；SysClient 扩展字段；菜单/权限种子。
- **不变量：** 回调禁 `*`；密钥不明文落库；创建不经客户端管理。
- **状态或数据流：** UI/API → SysClient 扩展行 → 后续 context/authorize 只读。
- **错误与失败行为：** 非法回调/缺必填拒绝；轮换密钥后旧明文立即失效。
- **兼容要求：** 不破坏现有非 SSO Client 行；不要求改 notify。
- **安全与隐私要求：** 密钥仅一次明文；禁进前端包。

## 6. 执行路线

1. 扫工作树：确认 ClientPage SSO 分组仅作历史/只读参照，新菜单独立落地。
2. 先红：创建/回调/`*`/密钥只一次 验证接缝。
3. DDL/DML 扩展 `sys_client` + 菜单种子（50/60 owner）。
4. 后端 SysClient 字段/BO/VO/Service/Controller（SSO 管理 API）。
5. 前端独立 SSO 管理页与菜单挂载。
6. 定向验证；形成可验证安全落点（授权后）。

## 7. 路径访问契约

- **预计修改点：** 与 `expected_changes` 对齐。
- **可写范围：** 与 `writable_paths` 对齐；越界前必须停止。
- **只读上下文：** ClientPage 仅只读参照（接入控件归 T-02）；Spec。
- **共享路径：** 50/60 DDL/DML owner=T-01；T-03 可追加短寿命 code 表但不改本票列语义。
- **保留或不动：** notify；产品实现须等 Lead 另派 I。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| SSO 管理 (a)(b)(c) | 管理 UI/API | 创建+回调+密钥 | AC-017 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/T-01.md</Path>` |
| 外部登记 | SSO 管理 | 登记外部 App | AC-018；无运行时要求 | 同上 |
| 禁 `*` | API | 提交通配回调 | 拒绝 | 同上 |
| 创建不在客户端管理 | 客户端管理 | 打开创建主路径 | 不可用/非主路径 | 同上 |

- **Workspace checks：** 授权后 current-workspace 单元/静态。
- **E2E disposition：** not-required：管理面定向；完成态 E2E 归 T-07。
- **E2E owner/environment：** Lead / parent-candidate 或 current-workspace（按 Goal Plan）。
- **Integration evidence：** 授权后 implementation/source commit + direct-parent。

## 9. 发布、迁移与恢复

- **迁移顺序：** DDL 先于 API/UI；菜单种子可开关。
- **兼容窗口：** 旧 Client 行保持可用。
- **监控信号：** 创建失败原因分类（无 secret）。
- **回滚或前向恢复：** 关菜单权限；字段可空。
- **不可逆操作与批准点：** 密钥轮换不可逆（旧明文失效）。
- **收缩条件：** 不适用：P0 无旧独立 OAuth 目录可收缩。

## 10. 验收标准

- [x] `AC-017`：独立 SSO 管理可完成创建应用 + 精确回调 + 配置交付；行落 `sys_client`；不在客户端管理创建。
- [x] `AC-018`：外部 App 可在 SSO 管理登记并交付配置；不要求 confidential 运行时。
- [x] 实现开始前已完整读取 Tickets Map 与适用 Skill；blocked-by-auth。
- [x] 验证矩阵记录到 Evidence；未越界；未造假。

## 11. SKILL 调用计划

- **engineering-standards（implement/verify）**：实现前读硬约束；落点后跑管理面/密钥门禁验证。
- **namewta-fullstack-development（implement）**：跨层垂直切片对齐 SSO 管理前后端接缝。
- 失败均 `block-ticket`；不静默替换。

## 12. 停止、检查点与交付

- 用户交付要求与数量：无额外命名交付物（Map `requested_deliverables=[]`）。
- 未授权 / Research 未完成：停，不上 I。
- 归属冲突：暂停本票与下游，不接管他人状态。
- 完成出口：仅 Lead 另派 I 且验收通过后。
