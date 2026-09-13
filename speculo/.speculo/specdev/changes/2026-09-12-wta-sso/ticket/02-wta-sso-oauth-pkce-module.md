---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描 AGENTS.md 与 <Path>.agents/skills/*/SKILL.md</Path>：适用 engineering-standards、namewta-fullstack-development；已排除 java-api-compatibility（本期不单独交付公开 Java API 兼容演进）、project-customization-delivery（用户未显式激活）。本票另绑 wta-module-guide 与 wta-common-modules-guide；不绑 deploy。"
skill_bindings:
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9","phase":"implement","operation":"build-wta-sso-oauth-pkce","inputs":["本Ticket T-02 规划合同与路径契约","Tickets Map Skill 矩阵","上游 Spec/ADR/CONTEXT"],"outputs":["符合工程硬约束的实现落点说明","未越界路径与模块边界记录"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"namewta-fullstack-development","path":"<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>","sha256":"65d6f22d11b990de23135c5a2c0bceb3907e964c800d527d58667d12db784988","phase":"implement","operation":"build-wta-sso-oauth-pkce","inputs":["本Ticket T-02 垂直切片范围","Spec AC 与 DEC 冻结","相关前后端接缝"],"outputs":["跨层合同对齐的可观察产出清单","下一票依赖的稳定接缝说明"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9","phase":"verify","operation":"verify-oauth-pkce-negatives","inputs":["本Ticket 验证矩阵","定向测试/静态检查范围"],"outputs":["含命令与退出码的 Evidence 草稿字段","残余风险与未验证项"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"wta-module-guide","path":"<Path>.agents/skills/wta-module-guide/SKILL.md</Path>","sha256":"441de2ccc513e09820ed3d7d2faf559eeaa7466202e4fbb0c8dd3eabf09510c9","phase":"implement","operation":"scaffold-wta-sso-module","inputs":["本Ticket 范围与模块/环境事实","上游 Spec/ADR"],"outputs":["Skill 约束下的落点/检查记录","失败时阻塞说明"],"required":true,"on_failure":"block-ticket","references":[]}
  - {"id":"wta-common-modules-guide","path":"<Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>","sha256":"e92775ce47af41bd33c1b3293d7f8a3185fdd739b9b588519e043c654f678d98","phase":"implement","operation":"select-common-security-ports","inputs":["本Ticket 范围与模块/环境事实","上游 Spec/ADR"],"outputs":["Skill 约束下的落点/检查记录","失败时阻塞说明"],"required":true,"on_failure":"block-ticket","references":[]}
resource_claims:
  - "wta-sso.module"
  - "oauth.authorize"
  - "oauth.token"
  - "oauth.revoke"
  - "pkce.s256"
  - "token.extras.business-client"
artifact: ticket
change: 2026-09-12-wta-sso
id: T-02
title: 新建 wta-sso：authorize/token/revoke + PKCE 负向 + 模块边界
status: blocked
planning_depth: deep
planning_depth_reason: 新模块与 OAuth 公共接缝、PKCE/code 安全负向全进 P0、Token extras 不变量与模块边界硬约束。
ready: false
risk: critical
blocked_by: [T-01]
contract_ids: [AC-004, AC-005, AC-006, AC-007, AC-008, AC-009, AC-010, AC-011, AC-012, AC-013, AC-021, AC-022]
owner: unassigned
expected_changes:
  - "<Path>backend/wta-modules/wta-sso/</Path>"
  - "<Path>backend/wta-api/src/main/java/org/namewta/sso/</Path>"
  - "<Path>backend/wta-modules/pom.xml</Path>"
  - "<Path>backend/wta-admin/pom.xml</Path>"
writable_paths:
  - "<Path>backend/wta-modules/wta-sso/</Path>"
  - "<Path>backend/wta-api/src/main/java/org/namewta/sso/</Path>"
  - "<Path>backend/wta-modules/pom.xml</Path>"
  - "<Path>backend/wta-admin/pom.xml</Path>"
  - "<Path>backend/pom.xml</Path>"
  - "<Path>backend/wta-admin/src/main/resources/application.yml</Path>"
  - "<Path>backend/wta-admin/src/main/resources/application-dev.yml</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path>"
read_only_paths:
  - "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/SysClient.java</Path>"
  - "<Path>backend/wta-common/wta-common-security/src/main/java/org/namewta/common/security/config/SecurityConfig.java</Path>"
  - "<Path>backend/wta-common/wta-common-satoken/src/main/java/org/namewta/common/satoken/utils/LoginHelper.java</Path>"
  - "<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>"
shared_paths:
  - "<Path>backend/wta-modules/pom.xml</Path>"
  - "<Path>backend/wta-admin/pom.xml</Path>"
  - "<Path>backend/pom.xml</Path>"
  - "<Path>backend/wta-admin/src/main/resources/application.yml</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path>"
shared_path_owners:
  - "<Path>backend/wta-modules/pom.xml</Path> => T-02"
  - "<Path>backend/wta-admin/pom.xml</Path> => T-02"
  - "<Path>backend/pom.xml</Path> => T-02"
  - "<Path>backend/wta-admin/src/main/resources/application.yml</Path> => T-02"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path> => T-01"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path> => T-01"
---

# Ticket T-02: 新建 wta-sso：authorize/token/revoke + PKCE 负向 + 模块边界

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/ticket/02-wta-sso-oauth-pkce-module.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/T-02.md</Path>`

实现本 Ticket 时，Lead 与 implementation subagent 必须按顺序完整读取总体 Map，读取适用于 `ALL`/`T-02` 的项目 Skill，再读本票。

## 1. 战略与来源

- **目标：** 落地 `wta-sso` 模块，提供 Authorization Code + PKCE S256 的 authorize/token/revoke，并强制负向合同与模块边界。
- **可观察产出：** 合法 PKCE 流可换得**目标业务 Client** 的 Sa-Token；AC-004…012 负向全拒绝；revoke 仅废本令牌；模块不直连 system Mapper/`ISys*`。
- **来源：** `US-007`、`AC-004`…`AC-013`、`AC-021`、`AC-022`、`DEC-001`/`DEC-010`/`DEC-012`/`DEC-015`/`DEC-016`/`DEC-020`、`ADR-001`/`ADR-002`/`ADR-003`。
- **当前事实：** 无 `wta-sso` 目录；安全隔离见 `<Path>backend/wta-common/wta-common-security/src/main/java/org/namewta/common/security/config/SecurityConfig.java</Path>`。
- **Planning Depth 原因：** 新模块+安全负向+Token 不变量。

## 2. 决策状态

### 已锁定决策

- 唯一 P0 授权=Code+PKCE S256；禁 Implicit/password/SAML。
- access_token=Sa-Token；extras=目标业务 Client 非 `sso`。
- 仅经 wta-api / Port/SPI 取用户与 Client（AC-013）。
- revoke≠SLO；第一方 SPA=public 强制 PKCE。

### 已采用的低影响假设

- 短寿命 code 表可新建；Cookie 登录会话 API 表面可与 T-03 衔接（本票可留 Port）。

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
| wta-sso 模块；authorize/token/revoke；PKCE 负向；code 表；组装开关 | Sa-Token/LoginHelper；SysClient 字段(T-01)；SecurityConfig 隔离 | sso-web UI；Set-Cookie 登录页；OIDC/SLO；外置 IdP；context UI |

## 4. 要构建什么

Client 带齐 PKCE 发起 authorize → 校验 challenge/S256/白名单/绑定 → 发一次性 code → token 用 verifier 换得业务 Client 的 Sa-Token → 复用/过期/错绑定/并发至多一成功均拒绝 → revoke 只废提交令牌。

## 5. 实现契约

- **入口或接缝：** OAuth2 authorize/token/revoke（wta-sso）。
- **输入与输出：** code_challenge/S256、verifier、client_id、redirect_uri、state；access_token。
- **公共接口变化：** 新增 OAuth 端点；短寿命持久化。
- **不变量：** TokenInvariant；PKCE 强制；日志无 code/verifier/secret。
- **错误与失败行为：** AC-004…012 全拒绝路径。
- **兼容要求：** 不放宽 SecurityConfig clientid 校验。
- **安全与隐私要求：** NFR-001；禁 browser 持 secret。

## 6. 执行路线

1. 先红：PKCE/code 负向矩阵测试骨架。
2. 按 wta-module-guide 建模块与 API Port。
3. 实现 authorize/token/revoke + code TTL/单次/并发。
4. 断言 extras.clientid=业务 Client（AC-021）；revoke（AC-022）。
5. 静态/架构检查模块边界（AC-013）。
6. 组装开关 `namewta.sso.enabled`；安全落点。

## 7. 路径访问契约

- frontmatter 为准；DDL 短寿命表追加时不破坏 T-01 列语义；pom/application.yml owner=T-02。
- **保留或不动：** system Mapper 直连；notify。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| PKCE 负向 | API | AC-004…012 | 全拒绝 | `<Path>{roots.state}/specdev/changes/2026-09-12-wta-sso/evidence/T-02.md</Path>` |
| Token extras | Token 断言 | 换票 | AC-021 | 同上 |
| revoke | API | 撤一令牌 | AC-022 非 SLO | 同上 |
| 模块边界 | 静态/架构 | 依赖规则 | AC-013 | 同上 |
| 日志 | 抽查 | 失败路径 | AC-012 | 同上 |

- **Workspace checks：** 授权后 current-workspace 后端测。
- **E2E disposition：** not-required：API/集成为主；浏览器三门归 T-06。
- **Integration evidence：** 授权后 commit + direct-parent。

## 9. 发布、迁移与恢复

- **迁移顺序：** 模块开关默认关 → 开后仅影响 SSO 路径。
- **兼容窗口：** 本地登录保留。
- **监控信号：** 授权失败原因分类（无 secret）。
- **回滚：** 关 `namewta.sso.enabled`。
- **不可逆：** 无强制数据删除。
- **收缩条件：** 不适用：P0 无旧 OAuth 协议可收缩。

## 10. 验收标准

- [ ] `AC-004`…`AC-013`、`AC-021`、`AC-022`。
- [ ] Map→Skill(含 module/common)→本票；blocked-by-auth。
- [ ] 不造假 Evidence；不引入外置 IdP SDK。

## 11. SKILL 调用计划

frontmatter：engineering-standards、namewta-fullstack-development、wta-module-guide、wta-common-modules-guide。verify 阶段跑负向矩阵。失败 block-ticket。

## 12. 停止、检查点与交付

- 交付数量空；未授权即停；冲突上报；完成出口仅授权后。

