---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-23已枚举.agents/skills全部入口；命中scope读取入口，common入口变化已复核，历史Skill Evidence不改写"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/14-profile-self-materials.md</Path>", "<Path>frontend/packages/web-domains/profile/src/self/</Path>", "<Path>frontend/packages/web-domains/profile/src/self/runtime.ts</Path>"], "outputs": ["T-14的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/14-profile-self-materials.md</Path>", "<Path>frontend/packages/web-domains/profile/src/self/</Path>", "<Path>frontend/packages/web-domains/profile/src/self/runtime.ts</Path>"], "outputs": ["T-14的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-module-guide", "path": "<Path>.agents/skills/wta-module-guide/SKILL.md</Path>", "sha256": "bb57a781314abe316f06ba9538f62043f1bd053904c0832969e575c09731a7a3", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/14-profile-self-materials.md</Path>", "<Path>frontend/packages/web-domains/profile/src/self/</Path>", "<Path>frontend/packages/web-domains/profile/src/self/runtime.ts</Path>"], "outputs": ["T-14的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-common-modules-guide", "path": "<Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>", "sha256": "d7b7e105c37499e0e0f8ad9b1e2dbf379100df5b6a47e0d6affb04483f8b162c", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/14-profile-self-materials.md</Path>", "<Path>frontend/packages/web-domains/profile/src/self/</Path>", "<Path>frontend/packages/web-domains/profile/src/self/runtime.ts</Path>"], "outputs": ["T-14的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/14-profile-self-materials.md</Path>", "当前diff与验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-14-replan-2026-09-23.md</Path>"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "contract:AC-014"]
artifact: "ticket"
change: "2026-09-14-wta-plus-comprehensive-review"
id: "T-14"
title: "补齐个人与企业自助认证材料闭环"
status: "cancelled"
kind: "review"
planning_depth: "deep"
planning_depth_reason: "公共合同/事务/安全/数据及恢复边界"
ready: true
risk: "high"
blocked_by: ["T-18"]
contract_ids: ["AC-014"]
owner: "single-agent"
expected_changes: ["<Path>frontend/packages/web-domains/profile/src/self/</Path>", "<Path>frontend/packages/web-domains/profile/src/self/runtime.ts</Path>", "<Path>frontend/packages/domains/profile/src/person/application/service.ts</Path>", "<Path>frontend/packages/domains/profile/src/enterprise/application/service.ts</Path>", "<Path>frontend/apps/home-web/src/router/homeManifestRegistry.ts</Path>", "<Path>backend/wta-modules/wta-profile/</Path>", "<Path>frontend/e2e/profile-management.spec.ts</Path>", "<Path>frontend/apps/home-web/src/application/services.ts</Path>", "<Path>frontend/apps/home-web/package.json</Path>", "<Path>frontend/e2e/profile-self-materials.spec.ts</Path>", "<Path>frontend/packages/domains/profile/src/material-tags/</Path>", "<Path>frontend/packages/web-domains/profile/package.json</Path>", "<Path>frontend/pnpm-lock.yaml</Path>", "<Path>frontend/playwright.profile.config.ts</Path>", "<Path>frontend/playwright.config.ts</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/profile/material/ProfileSelfMaterialsBrowserIntegrationTest.java</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/50-cde-base-dml.sql</Path>", "<Path>.agents/skills/wta-module-guide/references/modules/profile/index.md</Path>", "<Path>frontend/apps/home-web/AGENTS.md</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path>", "<Path>backend/wta-common/wta-common-satoken/src/main/java/org/namewta/common/satoken/handler/SaTokenExceptionHandler.java</Path>"]
writable_paths: ["<Path>frontend/packages/web-domains/profile/src/self/</Path>", "<Path>frontend/packages/web-domains/profile/src/self/runtime.ts</Path>", "<Path>frontend/packages/domains/profile/src/person/application/service.ts</Path>", "<Path>frontend/packages/domains/profile/src/enterprise/application/service.ts</Path>", "<Path>frontend/apps/home-web/src/router/homeManifestRegistry.ts</Path>", "<Path>backend/wta-modules/wta-profile/</Path>", "<Path>frontend/e2e/profile-management.spec.ts</Path>", "<Path>frontend/apps/home-web/src/application/services.ts</Path>", "<Path>frontend/apps/home-web/package.json</Path>", "<Path>frontend/e2e/profile-self-materials.spec.ts</Path>", "<Path>frontend/packages/domains/profile/src/material-tags/</Path>", "<Path>frontend/packages/web-domains/profile/package.json</Path>", "<Path>frontend/pnpm-lock.yaml</Path>", "<Path>frontend/playwright.profile.config.ts</Path>", "<Path>frontend/playwright.config.ts</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/profile/material/ProfileSelfMaterialsBrowserIntegrationTest.java</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/50-cde-base-dml.sql</Path>", "<Path>.agents/skills/wta-module-guide/references/modules/profile/index.md</Path>", "<Path>frontend/apps/home-web/AGENTS.md</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path>", "<Path>backend/wta-common/wta-common-satoken/src/main/java/org/namewta/common/satoken/handler/SaTokenExceptionHandler.java</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>", "<Path>{roots.state}/specdev/adr/</Path>", "<Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/</Path>"]
shared_paths: ["<Path>frontend/packages/web-domains/profile/src/self/</Path>", "<Path>frontend/packages/web-domains/profile/src/self/runtime.ts</Path>", "<Path>frontend/packages/domains/profile/src/person/application/service.ts</Path>", "<Path>frontend/packages/domains/profile/src/enterprise/application/service.ts</Path>", "<Path>frontend/apps/home-web/src/router/homeManifestRegistry.ts</Path>", "<Path>backend/wta-modules/wta-profile/</Path>", "<Path>frontend/e2e/profile-management.spec.ts</Path>", "<Path>frontend/apps/home-web/src/application/services.ts</Path>", "<Path>frontend/apps/home-web/package.json</Path>", "<Path>frontend/e2e/profile-self-materials.spec.ts</Path>", "<Path>frontend/packages/domains/profile/src/material-tags/</Path>", "<Path>frontend/packages/web-domains/profile/package.json</Path>", "<Path>frontend/pnpm-lock.yaml</Path>", "<Path>frontend/playwright.profile.config.ts</Path>", "<Path>frontend/playwright.config.ts</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/profile/material/ProfileSelfMaterialsBrowserIntegrationTest.java</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/50-cde-base-dml.sql</Path>", "<Path>.agents/skills/wta-module-guide/references/modules/profile/index.md</Path>", "<Path>frontend/apps/home-web/AGENTS.md</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path>", "<Path>backend/wta-common/wta-common-satoken/src/main/java/org/namewta/common/satoken/handler/SaTokenExceptionHandler.java</Path>"]
shared_path_owners: ["<Path>frontend/packages/web-domains/profile/src/self/</Path> => single-agent (Lead; exclusive current workspace; T-14 turn only)", "<Path>frontend/packages/web-domains/profile/src/self/runtime.ts</Path> => single-agent (Lead; exclusive current workspace; T-14 turn only)", "<Path>frontend/packages/domains/profile/src/person/application/service.ts</Path> => single-agent (Lead; exclusive current workspace; T-14 turn only)", "<Path>frontend/packages/domains/profile/src/enterprise/application/service.ts</Path> => single-agent (Lead; exclusive current workspace; T-14 turn only)", "<Path>frontend/apps/home-web/src/router/homeManifestRegistry.ts</Path> => single-agent (Lead; exclusive current workspace; T-14 turn only)", "<Path>backend/wta-modules/wta-profile/</Path> => single-agent (Lead; exclusive current workspace; T-14 turn only)", "<Path>frontend/e2e/profile-management.spec.ts</Path> => single-agent (Lead; exclusive current workspace; T-14 turn only)", "<Path>frontend/apps/home-web/src/application/services.ts</Path> => single-agent (Lead; exclusive current workspace; T-14 turn only)", "<Path>frontend/apps/home-web/package.json</Path> => single-agent (Lead; exclusive current workspace; T-14 turn only)", "<Path>frontend/e2e/profile-self-materials.spec.ts</Path> => single-agent (Lead; exclusive current workspace; T-14 turn only)", "<Path>frontend/packages/domains/profile/src/material-tags/</Path> => single-agent (Lead; exclusive current workspace; T-14 turn only)", "<Path>frontend/packages/web-domains/profile/package.json</Path> => single-agent (Lead; exclusive current workspace; T-14 turn only)", "<Path>frontend/pnpm-lock.yaml</Path> => single-agent (Lead; exclusive current workspace; T-14 turn only)", "<Path>frontend/playwright.profile.config.ts</Path> => single-agent (Lead; exclusive current workspace; T-14 turn only)", "<Path>frontend/playwright.config.ts</Path> => single-agent (Lead; exclusive current workspace; T-14 turn only)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/profile/material/ProfileSelfMaterialsBrowserIntegrationTest.java</Path> => single-agent (Lead; exclusive current workspace; T-14 turn only)", "<Path>release-artifacts/docker/infrastructure/mysql/init/50-cde-base-dml.sql</Path> => single-agent (Lead; exclusive current workspace; T-14 turn only)", "<Path>.agents/skills/wta-module-guide/references/modules/profile/index.md</Path> => single-agent (Lead; exclusive current workspace; T-14 turn only)", "<Path>frontend/apps/home-web/AGENTS.md</Path> => single-agent (Lead; exclusive current workspace; T-14 turn only)", "<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path> => single-agent (Lead; exclusive current workspace; T-14 turn only)", "<Path>backend/wta-common/wta-common-satoken/src/main/java/org/namewta/common/satoken/handler/SaTokenExceptionHandler.java</Path> => single-agent (Lead; exclusive current workspace; T-14 turn only)"]
---

# T-14：补齐个人与企业自助认证材料闭环

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先完整读Map→命中项目Skill入口/按scope引用→本票与上游。保留单人串行、无子代理/无新worktree。本票计划已Ready；本轮没有实施或重验，等待用户自行激活Goal。

## 1. 战略与来源

- 来源：F-02；AC-014；本轮用户请求与<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>。
- 当前事实：材料owner及必填规则保持；OSS诊断解耦后仍须真实上传与提交验证。
- 可观察产出：新个人CN_RESIDENT_ID上传正反面后完成提交

## 2. 决策状态

### 已锁定决策

保留工程分层、Client/权限、资源owner、安全日志、真实供应商协议和唯一六SQL基座。最新用户仅授权计划。

### 已确认方案

既有合同保持；本轮重新评审与验收；材料owner及必填规则保持；OSS诊断解耦后仍须真实上传与提交验证。 完整决定及来源以<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>为准。

### 已采用的低影响假设

沿用当前模块与测试基座；测试样本为隔离合成数据，不作为生产容量或SLO。

### 执行前置

G整体共识已确认；执行前仍需复核当前HEAD/归属、实际实施与commit授权及必要测试环境。Ready不代替Goal激活。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 本票可观察产出及所列消费者、验证、文档 | 既有wta-api/common、模块模式、事务/权限/生命周期与测试 | 无关模块重写、新队列/锁平台、真实数据修复、远程发布及相邻OIDC实现 |

## 4. 要构建什么

新个人CN_RESIDENT_ID上传正反面后完成提交 正常、失败、越权和竞争路径按本票验收断言共同交付，不把前后端或测试分成无价值空票。

## 5. 实现契约

- 入口与输入输出：required: 新个人身份证双面、企业条件材料经真实MySQL/OSS提交，刷新/失败/越权覆盖。
- 外部行为：新个人CN_RESIDENT_ID上传正反面后完成提交
- 不变量：current workspace单writer；UseCase→Service→DAO→Mapper；classic保留；公开数据只经wta-api；GET查询/POST变更且安全@Log。Notify外部I/O不在结果事务内，IN_APP按确认后的短事务合同处理。
- 失败边界：不吞SQL/HTTP/Provider错误，不将UNKNOWN当成功或盲目可重试；页面旧响应不覆盖新会话。具体负向断言见第8/10节。
- 兼容：沿用用户此前明确的基座仓内直接切换决定，同步真实消费者/生成物；不放宽第三方协议。公共API技能用于调用方与影响核对，不重新增加已被用户排除的兼容桥。
- 安全：仅隔离合成测试；secret不进日志/截图/证据。对象/Client授权在后端实施，页面隐藏不替代权限。

## 6. 执行路线

1. 回读本票历史Evidence和实际实现提交，使用当前源码核对本票验收合同；旧施工步骤仅在before快照保存，不重复实施。
2. 比较历史候选与当前写集；对后续提交或新票触及的行为逐一标记需要重跑的测试。
3. 执行本票正常/失败/回归及E2E要求；零用例或required skip不算通过，结果写新的带日期证据，不覆盖原始记录。
4. 若发现退化，先在本票写集内固定红灯；超出范围或与新票重叠时由Lead修订owner，禁止重复改动。
5. 核验历史非空提交和父链；缺少真实clean exact-HEAD证明不得事后补造。按Goal历史票关闭程序处置。

## 7. 路径访问契约

frontmatter为预计点、硬写集与共享owner权威。目录写集仅授权本票行为所需文件；新增测试在声明根内，新增生产类须符合已有层次。不存在的新文件为计划创建，不声称已实现。共享物理/语义资源由single-agent在本票轮次独占；不同票不并行，跨change冲突仅暂停相关分支。

本票状态和Evidence仅由Lead写当前change；永久ADR/context及相邻SSO change只读。越界先修订Ticket/Map，禁止先改后报。

## 8. 验证矩阵

| 场景 | 接缝/步骤 | 预期 | Evidence |
|---|---|---|---|
| 正常 | required: 新个人身份证双面、企业条件材料经真实MySQL/OSS提交，刷新/失败/越权覆盖 | 新个人CN_RESIDENT_ID上传正反面后完成提交 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-14-replan-2026-09-23.md</Path> |
| 失败/竞争 | 企业必填及条件材料齐备时完成提交，缺项定位准确 | 明确失败/安全恢复，无伪成功、越权及部分提交 | 同上，记录故障注入与状态 |
| 回归 | 现有同域测试＋消费者＋适用静态门禁 | 取消/失败/过期OSS/刷新不会伪造完成或越owner访问；后端必填校验不被关闭，真实浏览器+MySQL+OSS验收通过 | 同上，记录测试数/skip/源码 |

命令在仓根执行，`cd backend`表示该条命令切cwd；每条独立运行。以下为实施期命令，本轮未执行：

- `cd frontend && pnpm --filter @namewta/web-domain-profile test`
- `cd frontend && pnpm typecheck`
- `cd frontend && pnpm test:e2e`
- `cd backend && ./mvnw -pl wta-modules/wta-profile -am test`

- Workspace checks：current-workspace，所列命令加命中工程Skill质量门禁。
- E2E disposition：required: 新个人身份证双面、企业条件材料经真实MySQL/OSS提交，刷新/失败/越权覆盖。
- E2E owner/environment：single-agent（Lead）/current-workspace；真实MySQL/Redis/MinIO必须为本任务隔离资源，必要服务缺失则阻塞对应验收。
- 真实服务启用方法与零skip要求：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/verification.md</Path>；新增用例必须保存精确选择器与实际计数，不能只运行mock或test list。
- Integration evidence：非空implementation commit、parent before、clean exact HEAD/tree时点的direct-parent和适用E2E、不可变result及父链；required模式不适用，不创建candidate worktree。

## 9. 发布、迁移与恢复

- 迁移顺序：保持历史已交付行为；仅对当前复验发现的真实退化实施最小修复。已有数据不得重放基座。
- 兼容窗口：基座仓内同步切换，无未声明双写/双协议；外部现有协议保持。
- 监控：记录本票可观察失败/状态/耗时及资源数量，不记录敏感正文；不新增监控平台。
- 恢复：保存上个不可变候选及失败证据；停止受影响任务再核对外部副作用。不得通过恢复已披露secret、放宽权限或重发UNKNOWN恢复。
- 不可逆批准：产品commit/父分支更新及远程push、部署、轮换/真实数据操作、归档分别核对本轮授权。当前均未授权。
- 收缩条件：旧消费者/废弃字段/不必要配置引用清零且新合同验证通过；不适用的删除不人为增加。

## 10. 验收标准

- [x] `AC-014`：新个人CN_RESIDENT_ID上传正反面后完成提交。
- [x] `AC-014`：企业必填及条件材料齐备时完成提交，缺项定位准确。
- [x] `AC-014`：取消/失败/过期OSS/刷新不会伪造完成或越owner访问。
- [x] `AC-014`：后端必填校验不被关闭，真实浏览器+MySQL+OSS验收通过。
- [x] 实际调用已绑定Skill，记录摘要/输入/步骤/输出；不是只“读过”。
- [x] 正常、失败、回归和required E2E有当前候选证据，未运行不勾选。
- [x] 写集、共享owner、合同和生成物一致；无未批准偏差。
- [x] 真实commit/direct-parent/result出口已满足或按Goal对历史无需新实施票作有证据的取消裁决。

当前源码已满足 AC-014，无新实现。真实 MySQL/Redis/MinIO Chrome 8 项，0 skip。证据见 `evidence/T-14-replan-2026-09-23.md`。AC 仍由 T-30 组合复验。

## 11. SKILL 调用计划

frontmatter每个必需绑定在implement阶段输入本票、真实调用方和diff，按scope执行约束检查与实现；verify阶段由engineering-standards执行适用门禁。实际Skill Execution Records写<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-14-replan-2026-09-23.md</Path>，包含id/phase/operation/sha256/status/evidence。入口摘要变化先读diff并重新绑定，不改旧历史记录。当前规划仅完成元数据/入口及相关规范路由，未伪造实施passed。

## 12. 停止、检查点与交付

交付本票完整可观察行为，数量以Map为准，不能以样例替代。缺高影响决定、必需Skill/引用/测试，或owner冲突，停止该票和依赖闭包；无依赖票仅在已获执行授权后继续。保留HEAD、diff、已跑命令、失败类别、待完成动作；相同失败无新证据或达到3次集成尝试先复盘。验收后回交Goal，全部票done仍不等于change可归档。

## 历史实现保留及本轮处置

历史implementation commit：`65bd6d38d67ddaef02d83a45db528347e5487109`；历史result：`6c8764cca97bb6057fcb90ccdfe635c7efbf502a`。提交存在及祖先关系见<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/legacy-ticket-audit.json</Path>。

<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-14.md</Path>及其引用日志是历史证据，本轮未重跑业务测试。原Ticket全文见<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/replan-2026-09-23-before/ticket/14-profile-self-materials.md</Path>，不得按旧“尚未实现/提交暂缓”描述重复执行。

旧计划把所有票result设为同一整批提交且当前worktrees为空，不满足现行逐票验收记录合同；Lead须查原始记录。不能补造当时clean状态，不能为关闭历史票创建空commit。若现代码满足合同且无需新实现，经当前行为证据及明确处置可cancelled并保留AC由T-30覆盖；否则按真实修复重新形成产品提交，既有历史证据仍不删。
