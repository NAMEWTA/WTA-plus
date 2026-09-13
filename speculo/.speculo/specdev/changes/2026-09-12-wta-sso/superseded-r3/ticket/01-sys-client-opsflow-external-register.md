---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描 AGENTS.md 与 <Path>.agents/skills/*/SKILL.md</Path>：适用 engineering-standards、namewta-fullstack-development；已排除 java-api-compatibility（本期不单独交付公开 Java API 兼容演进）、project-customization-delivery（用户未显式激活）。本票不绑 wta-module-guide（不新建 wta-sso 模块本体）；不绑 deploy。"
skill_bindings:
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9","phase":"implement","operation":"extend-sys-client-opsflow","inputs":["本Ticket T-01 规划合同与路径契约","Tickets Map Skill 矩阵","上游 Spec/ADR/CONTEXT"],"outputs":["符合工程硬约束的实现落点说明","未越界路径与模块边界记录"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"namewta-fullstack-development","path":"<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>","sha256":"65d6f22d11b990de23135c5a2c0bceb3907e964c800d527d58667d12db784988","phase":"implement","operation":"extend-sys-client-opsflow","inputs":["本Ticket T-01 垂直切片范围","Spec AC 与 DEC 冻结","相关前后端接缝"],"outputs":["跨层合同对齐的可观察产出清单","下一票依赖的稳定接缝说明"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9","phase":"verify","operation":"verify-client-opsflow-gates","inputs":["本Ticket 验证矩阵","定向测试/静态检查范围"],"outputs":["含命令与退出码的 Evidence 草稿字段","残余风险与未验证项"],"required":true,"on_failure":"block-ticket","references":[]}
resource_claims:
  - "sys_client.sso-fields"
  - "opsflow.client-register"
  - "sso.secret-hash"
artifact: ticket
change: 2026-09-12-wta-sso
id: T-01
title: 扩展 sys_client SSO 字段与 OpsFlow；外部 App 管理面可登记
status: done
planning_depth: deep
planning_depth_reason: 扩展一层应用目录 schema/管理面/密钥哈希与 OpsFlow；改变共享 Client 契约，后续 OAuth/context 依赖其稳定；含迁移与密钥安全。
ready: true
risk: high
blocked_by: []
contract_ids: [AC-017, AC-018, AC-019, AC-023]
owner: unassigned
expected_changes:
  - "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/SysClient.java</Path>"
  - "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysClientController.java</Path>"
  - "<Path>frontend/packages/web-domains/system/src/client/ClientPage.vue</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path>"
writable_paths:
  - "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/</Path>"
  - "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/bo/</Path>"
  - "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/vo/</Path>"
  - "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/</Path>"
  - "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/mapper/</Path>"
  - "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysClientController.java</Path>"
  - "<Path>frontend/packages/web-domains/system/src/client/</Path>"
  - "<Path>frontend/packages/domains/system/src/client/</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path>"
read_only_paths:
  - "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path>"
  - "<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>"
shared_paths:
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path>"
shared_path_owners:
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path> => T-01"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path> => T-01"
---

# Ticket T-01: 扩展 sys_client SSO 字段与 OpsFlow；外部 App 管理面可登记

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/01-sys-client-opsflow-external-register.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/T-01.md</Path>`

实现本 Ticket 时，Lead 与 implementation subagent 必须按顺序完整读取总体 Map，读取项目 Skill 的 frontmatter 与入口并只展开适用于 `ALL`/`T-01` 的匹配项，再读取本 Ticket 与相关上游工件。

## 1. 战略与来源

- **目标：** 扩展 `sys_client` 为一层 SSO 应用目录，支持 OpsFlow (a)(b)(c) 与外部 App 管理面登记。
- **可观察产出：** 可创建 Client、精确回调、密钥明文只一次；外部可登记；confidential 字段可建；**不**宣称运行时 confidential 已通。
- **来源：** `US-005`/`US-008`、`AC-017`/`AC-018`/`AC-019`/`AC-023`(部分)、`DEC-102`/`DEC-111`/`DEC-116`/`DEC-021`、`ADR-003`、`<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/SysClient.java</Path>`。
- **当前事实：** SysClient 无 SSO 分组；ClientPage 无 OpsFlow SSO；DDL/DML 在 50/60 基座。
- **Planning Depth 原因：** schema+密钥+共享契约。

## 2. 决策状态

### 已锁定决策

- 扩展 `sys_client`，不平行 OAuth 主表；短寿命 code 表归 T-02。
- D-111=B / D-116=B：登记深度 ≠ 运行时打通。
- `client_secret` ≠ OAuth 密钥；用 `sso_secret_hash`。

### 已采用的低影响假设

- Origin/callback 字面量可占位（D-002）；Provider 种子可与 T-04 协调。

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
| SSO 字段 DDL/DML；OpsFlow 管理 API/页；外部登记；confidential 可建 | 现有 SysClient CRUD；现有 client_secret 语义 | authorize/token；sso-web；context 接线；confidential 运行时；OIDC；notify |

## 4. 要构建什么

管理员按 (a) 创建应用 → (b) 精确回调白名单（禁 `*`）→ (c) 交付/轮换密钥（明文只一次）。外部 App 同样可登记成功，但不要求跑通 Code 流。

## 5. 实现契约

- **入口或接缝：** SysClient 管理 API + ClientPage SSO 分组。
- **输入与输出：** 回调、authMode、ssoEnabled、public/confidential、secret 写时明文。
- **公共接口变化：** Client VO/BO SSO 字段；无 OAuth 运行时接口。
- **不变量：** 精确回调；secret 不回显；登记≠运行时。
- **状态或数据流：** 管理写 → sys_client → T-02/T-04 读。
- **错误与失败行为：** `*`/非法回调拒绝；日志无 secret。
- **兼容要求：** 非 SSO Client 与本地登录不变。
- **安全与隐私要求：** hash 存储；明文只一次。

## 6. 执行路线

1. 建立负向接缝（`*` 回调、secret 回显）。
2. DDL/DML + domain/bo/vo/service。
3. 管理 API + ClientPage OpsFlow。
4. 外部登记与 confidential 字段路径。
5. 安全落点：定向测绿；不接 OAuth。
6. 授权后写 Evidence。

## 7. 路径访问契约

- **预计修改点 / 可写 / 只读 / 共享：** 见 frontmatter；DDL/DML owner=T-01。
- **保留或不动：** notify 归档；已删 docs 路径。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| OpsFlow | 管理 API/UI | 建应用+回调+密钥 | AC-017 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/T-01.md</Path>` |
| 外部登记 | 管理 API | 登记外部 App | AC-018 | 同上 |
| confidential 字段 | 管理 API | 存 hash | AC-019 无运行时 | 同上 |
| 回调负向 | API | `*` | 拒绝 | 同上 |
| 回归 | Client CRUD | 定向测 | 旧行为不变 | 同上 |

- **Workspace checks：** 授权后 current-workspace 定向测/编译。
- **E2E disposition：** not-required：管理面契约；三门归 T-06。
- **E2E owner/environment：** 不适用。
- **Integration evidence：** 授权后 implementation commit + direct-parent。

## 9. 发布、迁移与恢复

- **迁移顺序：** 可空列 → 读新列 → 强制回调校验。
- **兼容窗口：** 旧 Client 默认 SSO 关闭。
- **监控信号：** 保存失败；密钥轮换审计。
- **回滚或前向恢复：** 保留可空列；不删 hash。
- **不可逆操作与批准点：** 密钥轮换使旧 secret 失效。
- **收缩条件：** 不适用：无旧 OAuth 主表。

## 10. 验收标准

- [ ] `AC-017`/`AC-018`/`AC-019`；`AC-023` 配置面部分。
- [ ] 已读 Map→Skill→本票；未越界；保持 blocked-by-auth。
- [ ] 不造假 Evidence；不翻转 implementation 授权。

## 11. SKILL 调用计划

按 frontmatter 绑定：`engineering-standards` implement/verify + `namewta-fullstack-development` implement。失败 block-ticket。扫描排除见 skill_scan。

## 12. 停止、检查点与交付

- **交付数量：** `requested_deliverables=[]`。
- **Skill/资源冲突：** 阻塞本票并上报；不改 notify。
- **检查点：** 源版本+绑定摘要；恢复前确认仍 not-authorized。
- **完成出口：** 仅授权后；当前=规划完成且 blocked。

