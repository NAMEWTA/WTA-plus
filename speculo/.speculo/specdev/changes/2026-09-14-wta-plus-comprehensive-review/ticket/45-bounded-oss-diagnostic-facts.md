---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-23已枚举.agents/skills全部入口；命中scope读取入口，common入口变化已复核，历史Skill Evidence不改写"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/45-bounded-oss-diagnostic-facts.md</Path>", "<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/client/AbstractOssClientImpl.java</Path>", "<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/model/</Path>"], "outputs": ["T-45的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/45-bounded-oss-diagnostic-facts.md</Path>", "<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/client/AbstractOssClientImpl.java</Path>", "<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/model/</Path>"], "outputs": ["T-45的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-module-guide", "path": "<Path>.agents/skills/wta-module-guide/SKILL.md</Path>", "sha256": "bb57a781314abe316f06ba9538f62043f1bd053904c0832969e575c09731a7a3", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/45-bounded-oss-diagnostic-facts.md</Path>", "<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/client/AbstractOssClientImpl.java</Path>", "<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/model/</Path>"], "outputs": ["T-45的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-common-modules-guide", "path": "<Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>", "sha256": "d7b7e105c37499e0e0f8ad9b1e2dbf379100df5b6a47e0d6affb04483f8b162c", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/45-bounded-oss-diagnostic-facts.md</Path>", "<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/client/AbstractOssClientImpl.java</Path>", "<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/model/</Path>"], "outputs": ["T-45的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "java-api-compatibility", "path": "<Path>.agents/skills/java-api-compatibility/SKILL.md</Path>", "sha256": "b90f5592e75b3f757f52649a16f78e92850fee0ccac9f12619d3d7aa94bd7aca", "phase": "implement", "operation": "audit-and-migrate-public-diagnostic-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/45-bounded-oss-diagnostic-facts.md</Path>", "<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/client/AbstractOssClientImpl.java</Path>", "<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/model/</Path>"], "outputs": ["公共OssAccessDiagnostic调用者与HTTP/UI同步切换清单"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/45-bounded-oss-diagnostic-facts.md</Path>", "当前diff与验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-45-replan-2026-09-23.md</Path>"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "oss:diagnostic-contract", "contract:AC-045"]
artifact: "ticket"
change: "2026-09-14-wta-plus-comprehensive-review"
id: "T-45"
title: "存储诊断区分允许、拒绝和未知事实"
status: "in_progress"
kind: "bug"
planning_depth: "deep"
planning_depth_reason: "公共合同/事务/安全/数据及恢复边界"
ready: true
risk: "high"
blocked_by: ["T-44"]
contract_ids: ["AC-045"]
owner: "single-agent"
expected_changes: ["<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/client/AbstractOssClientImpl.java</Path>", "<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/model/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/readiness/</Path>", "<Path>frontend/packages/web-domains/system/src/oss-config/</Path>", "<Path>frontend/packages/domains/system/src/oss-config/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/oss/readiness/</Path>", "<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/client/OssClient.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/vo/OssStorageDiagnosticVo.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/oss/config/OssConfigHttpContractUnitTest.java</Path>", "<Path>frontend/packages/domains/system/src/resource-service.ts</Path>", "<Path>frontend/packages/domains/system/src/resource-service.test.ts</Path>", "<Path>frontend/packages/domains/system/src/index.ts</Path>", "<Path>frontend/packages/api-contracts/generated/openapi.ts</Path>", "<Path>frontend/packages/api-contracts/openapi/current.json</Path>", "<Path>frontend/packages/api-contracts/openapi/revisions/</Path>", "<Path>backend/README.md</Path>", "<Path>.agents/skills/wta-module-guide/references/modules/system/domains.md</Path>", "<Path>frontend/packages/web-domains/system/src/runtime.ts</Path>", "<Path>frontend/packages/web-domains/system/src/index.test.ts</Path>", "<Path>frontend/apps/admin-web/src/router/adminManifestRegistry.ts</Path>", "<Path>frontend/apps/admin-web/src/router/adminManifestRegistry.test.ts</Path>"]
writable_paths: ["<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/client/AbstractOssClientImpl.java</Path>", "<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/model/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/readiness/</Path>", "<Path>frontend/packages/web-domains/system/src/oss-config/</Path>", "<Path>frontend/packages/domains/system/src/oss-config/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/oss/readiness/</Path>", "<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/client/OssClient.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/vo/OssStorageDiagnosticVo.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/oss/config/OssConfigHttpContractUnitTest.java</Path>", "<Path>frontend/packages/domains/system/src/resource-service.ts</Path>", "<Path>frontend/packages/domains/system/src/resource-service.test.ts</Path>", "<Path>frontend/packages/domains/system/src/index.ts</Path>", "<Path>frontend/packages/api-contracts/generated/openapi.ts</Path>", "<Path>frontend/packages/api-contracts/openapi/current.json</Path>", "<Path>frontend/packages/api-contracts/openapi/revisions/</Path>", "<Path>backend/README.md</Path>", "<Path>.agents/skills/wta-module-guide/references/modules/system/domains.md</Path>", "<Path>frontend/packages/web-domains/system/src/runtime.ts</Path>", "<Path>frontend/packages/web-domains/system/src/index.test.ts</Path>", "<Path>frontend/apps/admin-web/src/router/adminManifestRegistry.ts</Path>", "<Path>frontend/apps/admin-web/src/router/adminManifestRegistry.test.ts</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>", "<Path>{roots.state}/specdev/adr/</Path>", "<Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/</Path>"]
shared_paths: ["<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/client/AbstractOssClientImpl.java</Path>", "<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/model/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/readiness/</Path>", "<Path>frontend/packages/web-domains/system/src/oss-config/</Path>", "<Path>frontend/packages/domains/system/src/oss-config/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/oss/readiness/</Path>", "<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/client/OssClient.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/vo/OssStorageDiagnosticVo.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/oss/config/OssConfigHttpContractUnitTest.java</Path>", "<Path>frontend/packages/domains/system/src/resource-service.ts</Path>", "<Path>frontend/packages/domains/system/src/resource-service.test.ts</Path>", "<Path>frontend/packages/domains/system/src/index.ts</Path>", "<Path>frontend/packages/api-contracts/generated/openapi.ts</Path>", "<Path>frontend/packages/api-contracts/openapi/current.json</Path>", "<Path>frontend/packages/api-contracts/openapi/revisions/</Path>", "<Path>backend/README.md</Path>", "<Path>.agents/skills/wta-module-guide/references/modules/system/domains.md</Path>", "<Path>frontend/packages/web-domains/system/src/runtime.ts</Path>", "<Path>frontend/packages/web-domains/system/src/index.test.ts</Path>", "<Path>frontend/apps/admin-web/src/router/adminManifestRegistry.ts</Path>", "<Path>frontend/apps/admin-web/src/router/adminManifestRegistry.test.ts</Path>"]
shared_path_owners: ["<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/client/AbstractOssClientImpl.java</Path> => single-agent (Lead; cors_audit sole product writer; T-45 exclusive current workspace)", "<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/model/</Path> => single-agent (Lead; cors_audit sole product writer; T-45 exclusive current workspace)", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/readiness/</Path> => single-agent (Lead; cors_audit sole product writer; T-45 exclusive current workspace)", "<Path>frontend/packages/web-domains/system/src/oss-config/</Path> => single-agent (Lead; cors_audit sole product writer; T-45 exclusive current workspace)", "<Path>frontend/packages/domains/system/src/oss-config/</Path> => single-agent (Lead; cors_audit sole product writer; T-45 exclusive current workspace)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/oss/readiness/</Path> => single-agent (Lead; cors_audit sole product writer; T-45 exclusive current workspace)", "<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/client/OssClient.java</Path> => single-agent (Lead; cors_audit sole product writer; T-45 exclusive current workspace)", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/domain/vo/OssStorageDiagnosticVo.java</Path> => single-agent (Lead; cors_audit sole product writer; T-45 exclusive current workspace)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/oss/config/OssConfigHttpContractUnitTest.java</Path> => single-agent (Lead; cors_audit sole product writer; T-45 exclusive current workspace)", "<Path>frontend/packages/domains/system/src/resource-service.ts</Path> => single-agent (Lead; cors_audit sole product writer; T-45 exclusive current workspace)", "<Path>frontend/packages/domains/system/src/resource-service.test.ts</Path> => single-agent (Lead; cors_audit sole product writer; T-45 exclusive current workspace)", "<Path>frontend/packages/domains/system/src/index.ts</Path> => single-agent (Lead; cors_audit sole product writer; T-45 exclusive current workspace)", "<Path>frontend/packages/api-contracts/generated/openapi.ts</Path> => single-agent (Lead; cors_audit sole product writer; T-45 exclusive current workspace)", "<Path>frontend/packages/api-contracts/openapi/current.json</Path> => single-agent (Lead; cors_audit sole product writer; T-45 exclusive current workspace)", "<Path>frontend/packages/api-contracts/openapi/revisions/</Path> => single-agent (Lead; cors_audit sole product writer; T-45 exclusive current workspace)", "<Path>backend/README.md</Path> => single-agent (Lead; cors_audit sole product writer; T-45 exclusive current workspace)", "<Path>.agents/skills/wta-module-guide/references/modules/system/domains.md</Path> => single-agent (Lead; cors_audit sole product writer; T-45 exclusive current workspace)", "<Path>frontend/packages/web-domains/system/src/runtime.ts</Path> => single-agent (Lead; cors_audit sole product writer; T-45 exclusive current workspace)", "<Path>frontend/packages/web-domains/system/src/index.test.ts</Path> => single-agent (Lead; cors_audit sole product writer; T-45 exclusive current workspace)", "<Path>frontend/apps/admin-web/src/router/adminManifestRegistry.ts</Path> => single-agent (Lead; cors_audit sole product writer; T-45 exclusive current workspace)", "<Path>frontend/apps/admin-web/src/router/adminManifestRegistry.test.ts</Path> => single-agent (Lead; cors_audit sole product writer; T-45 exclusive current workspace)"]
---

# T-45：存储诊断区分允许、拒绝和未知事实

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先完整读Map→命中项目Skill入口/按scope引用→本票与上游。当前 Goal 已激活；Lead 串行委派 cors_audit 为唯一产品 writer，独占当前工作区，不创建 worktree；验证与提交由 Lead 执行。

## 1. 战略与来源

- 来源：R64-O-02；AC-045；本轮用户请求与<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>。
- 当前事实：Policy/ACL 403 被折叠为空，PublicAccess.from 误当否定事实；简化parser未完整处理Resource/Condition/Deny。
- 可观察产出：诊断仅报告观察事实与范围：读403为未知，单对象匿名读取只证明该对象，PRIVATE未知不能宣称全桶安全。

## 2. 决策状态

### 已锁定决策

保留工程分层、Client/权限、资源owner、安全日志、真实供应商协议和唯一六SQL基座。用户 active Goal 已授权本地实施、提交及 direct-parent；远程和生产操作不在本票范围。

### 已确认方案

2026-09-23用户已逐项接受D-002—009并确认整体共识（LOG-010—018）；本票按已接受ADR定稿。 完整决定及来源以<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>为准。

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

诊断仅报告观察事实与范围：读403为未知，单对象匿名读取只证明该对象，PRIVATE未知不能宣称全桶安全。 正常、失败、越权和竞争路径按本票验收断言共同交付，不把前后端或测试分成无价值空票。

## 5. 实现契约

- 入口与输入输出：OssAccessDiagnosticUnitTest、受限MinIO读权限与诊断展示。
- 外部行为：诊断仅报告观察事实与范围：读403为未知，单对象匿名读取只证明该对象，PRIVATE未知不能宣称全桶安全。
- 不变量：current workspace单writer；UseCase→Service→DAO→Mapper；classic保留；公开数据只经wta-api；GET查询/POST变更且安全@Log。Notify外部I/O不在结果事务内，IN_APP按确认后的短事务合同处理。
- 失败边界：不吞SQL/HTTP/Provider错误，不将UNKNOWN当成功或盲目可重试；页面旧响应不覆盖新会话。具体负向断言见第8/10节。
- 兼容：沿用用户此前明确的基座仓内直接切换决定，同步真实消费者/生成物；不放宽第三方协议。公共API技能用于调用方与影响核对，不重新增加已被用户排除的兼容桥。
- 安全：仅隔离合成测试；secret不进日志/截图/证据。对象/Client授权在后端实施，页面隐藏不替代权限。

## 6. 执行路线

1. 以policy/ACL403+匿名读取成功固定PUBLIC_READ误判；PRIVATE未知与条件策略负向样本。
2. 诊断结果明确 allowed/denied/unknown 及来源/对象/时间/能力范围，不再用空值伪造策略结论。
3. UI/管理VO显示策略不可验证与对象读事实，警告可识别危险写但不实现完整IAM解释器。
4. 保持操作只读，不用匿名PUT/DELETE探测真实业务桶，不扩大运行身份管理权限。
5. 同步诊断契约/前端映射与最小权限测试。

## 7. 路径访问契约

frontmatter为预计点、硬写集与共享owner权威。目录写集仅授权本票行为所需文件；新增测试在声明根内，新增生产类须符合已有层次。不存在的新文件为计划创建，不声称已实现。共享物理/语义资源由single-agent在本票轮次独占；不同票不并行，跨change冲突仅暂停相关分支。

本票状态和Evidence仅由Lead写当前change；永久ADR/context及相邻SSO change只读。越界先修订Ticket/Map，禁止先改后报。

## 8. 验证矩阵

| 场景 | 接缝/步骤 | 预期 | Evidence |
|---|---|---|---|
| 正常 | OssAccessDiagnosticUnitTest、受限MinIO读权限与诊断展示 | PUBLIC_READ策略不可读+对象可读不误报确定POLICY_MISMATCH | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-45-replan-2026-09-23.md</Path> |
| 失败/竞争 | PRIVATE未知不宣称匿名写已禁止；复杂策略结果有明确边界 | 明确失败/安全恢复，无伪成功、越权及部分提交 | 同上，记录故障注入与状态 |
| 回归 | 现有同域测试＋消费者＋适用静态门禁 | 全程无破坏性探测及高权限自动申请 | 同上，记录测试数/skip/源码 |

命令在仓根执行，`cd backend`表示该条命令切cwd；每条独立运行。以下为实施期命令，本轮未执行：

- `cd backend && ./mvnw -pl wta-common/wta-common-oss,wta-modules/wta-system,wta-admin -am test`
- `pnpm --dir frontend --filter @namewta/web-domain-system test`

- Workspace checks：current-workspace，所列命令加命中工程Skill质量门禁。
- E2E disposition：required: OssAccessDiagnosticUnitTest、受限MinIO读权限与诊断展示。
- E2E owner/environment：single-agent（Lead）/current-workspace；真实MySQL/Redis/MinIO必须为本任务隔离资源，必要服务缺失则阻塞对应验收。
- 真实服务启用方法与零skip要求：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/verification.md</Path>；新增用例必须保存精确选择器与实际计数，不能只运行mock或test list。
- Integration evidence：非空implementation commit、parent before、clean exact HEAD/tree时点的direct-parent和适用E2E、不可变result及父链；required模式不适用，不创建candidate worktree。

## 9. 发布、迁移与恢复

- 迁移顺序：同批修改源码、消费者、测试及生成合同；无需新增数据库迁移。涉及已有库时必须Tag差异、备份、隔离演练，不重放基座。
- 兼容窗口：基座仓内同步切换，无未声明双写/双协议；外部现有协议保持。
- 监控：记录本票可观察失败/状态/耗时及资源数量，不记录敏感正文；不新增监控平台。
- 恢复：保存上个不可变候选及失败证据；停止受影响任务再核对外部副作用。不得通过恢复已披露secret、放宽权限或重发UNKNOWN恢复。
- 不可逆批准：产品commit/父分支更新及远程push、部署、轮换/真实数据操作、归档分别核对本轮授权。本地产品 commit/direct-parent 已由 active Goal 授权；其余外部动作不由本票推定。
- 收缩条件：旧消费者/废弃字段/不必要配置引用清零且新合同验证通过；不适用的删除不人为增加。

## 10. 验收标准

- [ ] `AC-045`：PUBLIC_READ策略不可读+对象可读不误报确定POLICY_MISMATCH。
- [ ] `AC-045`：PRIVATE未知不宣称匿名写已禁止；复杂策略结果有明确边界。
- [ ] `AC-045`：全程无破坏性探测及高权限自动申请。
- [ ] 实际调用已绑定Skill，记录摘要/输入/步骤/输出；不是只“读过”。
- [ ] 正常、失败、回归和required E2E有当前候选证据，未运行不勾选。
- [ ] 写集、共享owner、合同和生成物一致；无未批准偏差。
- [ ] 真实commit/direct-parent/result出口已满足或按Goal对历史无需新实施票作有证据的取消裁决。

## 11. SKILL 调用计划

frontmatter每个必需绑定在implement阶段输入本票、真实调用方和diff，按scope执行约束检查与实现；verify阶段由engineering-standards执行适用门禁。实际Skill Execution Records写<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-45-replan-2026-09-23.md</Path>，包含id/phase/operation/sha256/status/evidence。入口摘要变化先读diff并重新绑定，不改旧历史记录。当前规划仅完成元数据/入口及相关规范路由，未伪造实施passed。

## 12. 停止、检查点与交付

交付本票完整可观察行为，数量以Map为准，不能以样例替代。缺高影响决定、必需Skill/引用/测试，或owner冲突，停止该票和依赖闭包；无依赖票仅在已获执行授权后继续。保留HEAD、diff、已跑命令、失败类别、待完成动作；相同失败无新证据或达到3次集成尝试先复盘。验收后回交Goal，全部票done仍不等于change可归档。

## revision205 — T-45启动与范围登记

revision205：T44已完成（结果fc50c1e，治理4c93a2f）；T45激活并先复现403/未知事实红灯。15done/2cancelled/1in_progress/32ready；Goal active，未完成或归档。

基线 `4c93a2fb9c7d2c1d68a0c8194197e2af1908b4d2`，main/current/direct-parent。T44 已提供单配置管理员 POST 与安全三字段 VO；本票将该 VO 扩展为有来源/范围/时间的安全事实投影，正式重新生成 OpenAPI 与前端映射。方法、权限、正ID和审计禁正文保持。策略/ACL 403不能伪装空策略；单对象匿名 HEAD/GET 仅陈述该对象，404/超时/网络异常/重定向不得推断匿名拒绝。PRIVATE未知不能宣称全桶安全或匿名写已禁止；PUBLIC_READ对象可读而策略不可读不能确定POLICY_MISMATCH。

仅解释明确、无条件且匹配目标资源的策略子集，尊重 Deny，Condition/Not*/不明Principal/Action/Resource及坏JSON保留UNKNOWN；可识别危险写以有界风险警告表达，不称实际PUT成功。保留每个独立读取的部分事实；不匿名PUT/DELETE、不提权、不影响T44业务/核心就绪解耦。每网络步骤100ms–3s/最多5步的已接受预算保持，无未回收后台任务。

公开 Java 结果按已确认仓内同步切换决定迁移全部实际调用者，java-api-compatibility用于调用清单/语义/编译核查，不新增已被用户排除的兼容桥。写集从6扩为17条：模型/Javadoc、管理VO与对应HTTP测试、domain transport/测试/出口、正式OpenAPI生成、运行文档及system模块事实。目录授权仅限本票行为，原快照不可覆盖。新增其他文件先回Lead登记。

Dispatch01A仅可改 `backend/wta-admin/src/test/java/org/namewta/test/oss/readiness/OssAccessDiagnosticUnitTest.java`，用旧公开合同可编译断言复现PUBLIC_READ+policy/ACL403误报和PRIVATE未知误称writeDenied；生产不改。Lead固定红测试提交并实际运行后再给01B写锁。legacy_audit独立只读合同审查；ops_audit仅准备隔离驱动。Lead独占治理/提交/Maven/pnpm/服务与生成。最终需受限MinIO真实零skip、HTTP权限/事实/无写调用、真实管理页面与OpenAPI同源、默认测试/full-core/前端适用门禁和cleanup。当前尚无T45实施/验收结果。

## revision206 — 红灯与Dispatch01B

revision206：T45红灯在73edcbff复现（8例/2预期failure/0error/0skip，源码前后clean）；登记三态事实投影和Dispatch01B。15done/2cancelled/1in_progress/32ready，完整候选attempts0，Goal active。

两条失败是PUBLIC_READ+策略/ACL403被误判MISMATCH，以及PRIVATE+403被误判VERIFIED；其余6项旧合同测试通过。保留red01/manifest.json，不计完整候选失败，不勾AC。

实现合同：公开OssAccessDiagnostic同批改为verification/reason/expectedAccessPolicy/checkedAt及不可变facts；每条事实包含subject、observation(ALLOWED/DENIED/UNKNOWN)、source、scope(OBJECT/BUCKET)、observedAt和固定basis。basis区分文档缺失、不可读、复杂/坏格式、超时、HTTP观察类别；不返回原始policy、bucket/key、URI、凭据、错误文本。subjects表达POLICY_READ/POLICY_WRITE限定文档声明、ACL_LIST桶列表授权、ACL_WRITE_RISK桶写入/ACL修改危险声明、OBJECT_HEAD/OBJECT_GET单对象实测。文档里的Allow不等于实际操作允许；NoSuchBucketPolicy只证明文档缺失，普通404不是同义结论。

bucket ACL READ只表示列对象而非GetObject，WRITE/WRITE_ACP/FULL_CONTROL作为危险声明警告；不因缺Allow或读文档403推断DENIED。明确支持资源/Principal/Action且无Condition/Not*的策略子集才可作限定声明，考虑Deny优先及重叠未知，不能以忽略条件的Allow宣称有效授权。PRIVATE仍有未知时不得声称全桶安全或匿名写已禁止。

匿名HEAD/GET的401/403只证明该对象该次DENIED；404/3xx/5xx/timeout/网络异常UNKNOWN，禁止自动重定向。两步独立保留部分事实，中断保留线程状态并停止后续网络操作；请求超时释放自身资源，不起遗留后台探测。总网络步骤至多5及每步100ms–3s预算保持。

管理POST/权限/正ID/审计禁正文不变；VO在status/reason/checkedAt外增加安全facts，Service私有Evaluation(entry+facts)，registry保留既有summary合同与revision fence。前端通过既有ossConfigs transport解析unknown为域模型，页面按需执行、明确事实来源范围/时间，处理失败、切换和卸载，不用旧响应覆盖当前上下文。正式OpenAPI由Lead捕获生成。

Dispatch01B：cors_audit唯一产品writer，使用Ticket17条写集，迁移全体Java构造/调用方、HTTP合同测试、domain transport与页面、文档；不写治理、不跑构建/服务/提交、不手改生成物。新增写集先回Lead。Lead固定源码后定向green/default/full、真实受限MinIO+HTTP/UI/安全与cleanup、正式OpenAPI、前端/core/静态验收；legacy_audit只读，ops_audit仅私有驱动。

协议依据：[AWS S3 ACL权限表](https://docs.aws.amazon.com/AmazonS3/latest/userguide/acl-overview.html)、[AWS策略显式Deny评估](https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_evaluation-logic_policy-eval-denyallow.html)。只用其约束解释范围，不宣称实现完整云端IAM计算。

## revision207 — 会话隔离与验证检查点

revision207：T45首轮OSS定向164例/1failure/0error/0skip；11c6e9e已修正500断言与HTTP事实依据，尚未重测。登记同用户新会话隔离修复写集，15done/2cancelled/1in_progress/32ready，完整候选attempts0，Goal active。

独立固定8dc775b审查发现：仅userId/权限相同不足以识别重新登录；Admin既有sessionGeneration和identityLoaded应以只读runtime合同投影，禁止web-domain读取token或App Store。新增写集为 `frontend/packages/web-domains/system/src/runtime.ts`, `frontend/packages/web-domains/system/src/index.test.ts`, `frontend/apps/admin-web/src/router/adminManifestRegistry.ts`, `frontend/apps/admin-web/src/router/adminManifestRegistry.test.ts`。仅扩展SystemWebRuntime、Admin装配及合同测试，不改认证Store或路由业务。页面发起时要求身份已加载；响应完成比较会话代次、用户、权限和局部请求版本；代次变化/identityLoaded变false时取消请求并清除已呈现结果。测试覆盖同一userId、同权限而会话变化，以及未加载期间拒绝发起。

Dispatch01C：cors_audit唯一产品writer，限上述4文件及原oss-config页面/测试；Lead负责提交与串行测试。green01与只读审查原始证据保留于checkpoint207/manifest.json；164例只有163通过，不记完整候选通过，不勾AC。11c6e9e同时将供应商HTTP错误按状态投影固定basis，避免误称网络错误。后续必须定向重测、真实受限MinIO+HTTP+UI、正式OpenAPI及适用质量门禁。

## revision208 — 真实公开域名回归

revision208：T45候选A1真实HTTP失败（公开域名重复桶路径，匿名HEAD/GET404，signedHEAD200，Policy/ACL403）；cleanup0。定向166全通过、前端110通过、默认1135/216skip/full通过，不能替代真实验收。15done/2cancelled/1in_progress/32ready，完整候选attempts1，Goal active。

固定候选081c75ff、完整JAR的真实受限MinIO结果e9853018a23b0d59：恰好5个只读请求，Policy/ACL403、signedHEAD200、匿名HEAD/GET404。根因anonymousReadFacts使用getBucketUrl(bucket)，path-style与已绑定桶的domainUrl一起重复拼桶；DefaultOssObjectStore.publicUrl及现有OssAccessUrlProviderUnitTest明确自定义域名已绑定桶。产品诊断应遵循相同访问地址语义，保留结构化对象键编码、尾斜杠规范化，拒绝userinfo/query/fragment；不改上传旧getBucketUrl全局合同、不放宽真实HTTP断言。

Dispatch01D：cors_audit唯一产品writer，仅原写集AbstractOssClientImpl.java及OssAccessDiagnosticUnitTest.java，修正诊断URL选择并以真实本地HTTP精确路径补测：path-style + bucket-bound domain + slash/空格/加号/片段字符键只编码一次、尾斜杠、无自定义域名回退，以及无效域名不发匿名请求。已有真实失败即回归红灯。Lead固定后重新跑受影响门禁和真实HTTP，最多3次完整候选的复盘门槛保持。

A1完整证据在candidate-a1/manifest.json。green02 34类166/0/0/0；fronttarget02 domain12+web17+Admin81全pass且前后clean。fronttarget01三个命令也pass，但Admin子集自动导入生成器删4声明导致包装源校验失败，保留差异且只恢复自身生成文件，改跑Admin全套后clean。默认reactor262类1135例，919执行/216环境skip，0failure/error；full clean package与bundle通过。独立081审查已关闭same-user generation finding，SSO callback候选反例经顶层路由卸载证据撤回。真实HTTP A1未通过，未生成新OpenAPI、未跑真实UI、未勾AC。

## revision209 — A2结果与第三候选边界

revision209：T45候选A2真实受限HTTP通过/cleanup0，168定向与默认1137（216环境skip）/full通过，正式OpenAPI仅加Fact及VO facts；前端全量typecheck发现诊断handler要求完整VO导致DefaultRow不兼容，A2尚未验收。15done/2cancelled/1in_progress/32ready，attempts2，Goal active。

backend d1fd57fb，生成候选9c567ba0。真实run6e5d5c24c38c793d确认公开匿名HEAD200/GET206、两私有桶HEAD/GET403、三个配置Policy/ACL403皆UNKNOWN，每次5个只读请求，旧A/新B对象路由及权限/审计/日志检查通过。18类凭据canary零命中、cleanup[]，默认SSE schema438路径/450 schemas已正式fetch/generate/check/typecheck，无现有路径删改。

全量前端architecture(33包)/architecture tests101/openapi/lint通过，Admin vue-tsc对OssConfigPage.vue:127报DefaultRow不能传给完整OssConfigVO。Dispatch01E由Lead唯一writer，仅原oss-config页面/测试：输入收窄Partial<OssConfigVO>并先拒绝缺失ID，保留后端权限和既有session fence，补缺失ID不发请求测试。不放宽编译器、不用断言强转绕过类型。

第三完整候选继续；若仍失败，先执行三次尝试复盘，不自动无限重试。后端输入未变时以精确tree等价复用A2默认/定向/HTTP证据，明确两个SHA；最终源码重新full打包与前端完整门禁、真实UI、core/static及两类真实JUnit（ReadinessMinio与AccessUrlMinio）必须实际通过。最后两类使用owned bootstrap兼容回归，不替代受限身份HTTP证据。A1和A2原始证据保持，不勾AC。
