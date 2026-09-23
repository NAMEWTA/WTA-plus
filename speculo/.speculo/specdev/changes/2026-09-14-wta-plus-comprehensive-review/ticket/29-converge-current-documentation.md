---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-23已枚举.agents/skills全部入口；命中scope读取入口，common入口变化已复核，历史Skill Evidence不改写"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/29-converge-current-documentation.md</Path>", "<Path>backend/AGENTS.md</Path>", "<Path>backend/AGENTS.md</Path>"], "outputs": ["T-29的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-module-guide", "path": "<Path>.agents/skills/wta-module-guide/SKILL.md</Path>", "sha256": "bb57a781314abe316f06ba9538f62043f1bd053904c0832969e575c09731a7a3", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/29-converge-current-documentation.md</Path>", "<Path>backend/AGENTS.md</Path>", "<Path>backend/AGENTS.md</Path>"], "outputs": ["T-29的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/29-converge-current-documentation.md</Path>", "<Path>backend/AGENTS.md</Path>", "<Path>backend/AGENTS.md</Path>"], "outputs": ["T-29的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/29-converge-current-documentation.md</Path>", "当前diff与验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-29-replan-2026-09-23.md</Path>"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "contract:AC-029"]
artifact: "ticket"
change: "2026-09-14-wta-plus-comprehensive-review"
id: "T-29"
title: "清理过时文档与重复AGENTS权威"
status: "ready"
kind: "review"
planning_depth: "deep"
planning_depth_reason: "公共合同/事务/安全/数据及恢复边界"
ready: true
risk: "medium"
blocked_by: ["T-01"]
contract_ids: ["AC-029"]
owner: "single-agent"
expected_changes: ["<Path>backend/AGENTS.md</Path>", "<Path>backend/wta-common/AGENTS.md</Path>", "<Path>backend/wta-extend/AGENTS.md</Path>", "<Path>backend/wta-modules/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-demo/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-notify/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-profile/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-person/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-sso/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-system/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-third/AGENTS.md</Path>", "<Path>scripts/README.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/02-decisions-and-exceptions.md</Path>", "<Path>README.md</Path>", "<Path>frontend/README.md</Path>", "<Path>frontend/AGENTS.md</Path>", "<Path>frontend/apps/README.md</Path>", "<Path>frontend/docs/architecture-baseline.md</Path>", "<Path>backend/README.md</Path>", "<Path>docs/README.md</Path>", "<Path>docs/namewta-enhancements.md</Path>", "<Path>docs/runtime-nacos-hard-cut.md</Path>", "<Path>docs/oss-public-private-operations.md</Path>", "<Path>release-artifacts/README.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/03-backend-module-modes.md</Path>", "<Path>.agents/skills/wta-module-guide/SKILL.md</Path>", "<Path>.agents/skills/engineering-standards/references/rules/review-and-delivery.md</Path>", "<Path>.agents/skills/engineering-standards/references/rules/testing.md</Path>", "<Path>.agents/skills/engineering-standards/references/rules/architecture-and-boundaries.md</Path>", "<Path>.agents/skills/namewta-fullstack-development/references/frontend/naming-and-layout.md</Path>", "<Path>scripts/ci/verify-agent-handbooks.mjs</Path>", "<Path>scripts/ci/verify-agent-handbooks.test.mjs</Path>", "<Path>.github/workflows/quality-gates.yml</Path>", "<Path>frontend/packages/domains/third/AGENTS.md</Path>", "<Path>frontend/packages/web-domains/third/AGENTS.md</Path>"]
writable_paths: ["<Path>backend/AGENTS.md</Path>", "<Path>backend/wta-common/AGENTS.md</Path>", "<Path>backend/wta-extend/AGENTS.md</Path>", "<Path>backend/wta-modules/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-demo/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-notify/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-profile/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-person/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-sso/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-system/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-third/AGENTS.md</Path>", "<Path>scripts/README.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/02-decisions-and-exceptions.md</Path>", "<Path>README.md</Path>", "<Path>frontend/README.md</Path>", "<Path>frontend/AGENTS.md</Path>", "<Path>frontend/apps/README.md</Path>", "<Path>frontend/docs/architecture-baseline.md</Path>", "<Path>backend/README.md</Path>", "<Path>docs/README.md</Path>", "<Path>docs/namewta-enhancements.md</Path>", "<Path>docs/runtime-nacos-hard-cut.md</Path>", "<Path>docs/oss-public-private-operations.md</Path>", "<Path>release-artifacts/README.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/03-backend-module-modes.md</Path>", "<Path>.agents/skills/wta-module-guide/SKILL.md</Path>", "<Path>.agents/skills/engineering-standards/references/rules/review-and-delivery.md</Path>", "<Path>.agents/skills/engineering-standards/references/rules/testing.md</Path>", "<Path>.agents/skills/engineering-standards/references/rules/architecture-and-boundaries.md</Path>", "<Path>.agents/skills/namewta-fullstack-development/references/frontend/naming-and-layout.md</Path>", "<Path>scripts/ci/verify-agent-handbooks.mjs</Path>", "<Path>scripts/ci/verify-agent-handbooks.test.mjs</Path>", "<Path>.github/workflows/quality-gates.yml</Path>", "<Path>frontend/packages/domains/third/AGENTS.md</Path>", "<Path>frontend/packages/web-domains/third/AGENTS.md</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>", "<Path>{roots.state}/specdev/adr/</Path>", "<Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/</Path>"]
shared_paths: ["<Path>backend/AGENTS.md</Path>", "<Path>backend/wta-common/AGENTS.md</Path>", "<Path>backend/wta-extend/AGENTS.md</Path>", "<Path>backend/wta-modules/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-demo/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-notify/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-profile/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-person/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-sso/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-system/AGENTS.md</Path>", "<Path>backend/wta-modules/wta-third/AGENTS.md</Path>", "<Path>scripts/README.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/02-decisions-and-exceptions.md</Path>", "<Path>README.md</Path>", "<Path>frontend/README.md</Path>", "<Path>frontend/AGENTS.md</Path>", "<Path>frontend/apps/README.md</Path>", "<Path>frontend/docs/architecture-baseline.md</Path>", "<Path>backend/README.md</Path>", "<Path>docs/README.md</Path>", "<Path>docs/namewta-enhancements.md</Path>", "<Path>docs/runtime-nacos-hard-cut.md</Path>", "<Path>docs/oss-public-private-operations.md</Path>", "<Path>release-artifacts/README.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/03-backend-module-modes.md</Path>", "<Path>.agents/skills/wta-module-guide/SKILL.md</Path>", "<Path>.agents/skills/engineering-standards/references/rules/review-and-delivery.md</Path>", "<Path>.agents/skills/engineering-standards/references/rules/testing.md</Path>", "<Path>.agents/skills/engineering-standards/references/rules/architecture-and-boundaries.md</Path>", "<Path>.agents/skills/namewta-fullstack-development/references/frontend/naming-and-layout.md</Path>", "<Path>scripts/ci/verify-agent-handbooks.mjs</Path>", "<Path>scripts/ci/verify-agent-handbooks.test.mjs</Path>", "<Path>.github/workflows/quality-gates.yml</Path>", "<Path>frontend/packages/domains/third/AGENTS.md</Path>", "<Path>frontend/packages/web-domains/third/AGENTS.md</Path>"]
shared_path_owners: ["<Path>backend/AGENTS.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>backend/wta-common/AGENTS.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>backend/wta-extend/AGENTS.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>backend/wta-modules/AGENTS.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>backend/wta-modules/wta-demo/AGENTS.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>backend/wta-modules/wta-notify/AGENTS.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>backend/wta-modules/wta-profile/AGENTS.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/AGENTS.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>backend/wta-modules/wta-profile/wta-profile-person/AGENTS.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>backend/wta-modules/wta-sso/AGENTS.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>backend/wta-modules/wta-system/AGENTS.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>backend/wta-modules/wta-third/AGENTS.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>scripts/README.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>.agents/skills/engineering-standards/references/project/02-decisions-and-exceptions.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>README.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>frontend/README.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>frontend/AGENTS.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>frontend/apps/README.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>frontend/docs/architecture-baseline.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>backend/README.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>docs/README.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>docs/namewta-enhancements.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>docs/runtime-nacos-hard-cut.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>docs/oss-public-private-operations.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>release-artifacts/README.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>.agents/skills/engineering-standards/references/project/03-backend-module-modes.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>.agents/skills/wta-module-guide/SKILL.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>.agents/skills/engineering-standards/references/rules/review-and-delivery.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>.agents/skills/engineering-standards/references/rules/testing.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>.agents/skills/engineering-standards/references/rules/architecture-and-boundaries.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>.agents/skills/namewta-fullstack-development/references/frontend/naming-and-layout.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>scripts/ci/verify-agent-handbooks.mjs</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>scripts/ci/verify-agent-handbooks.test.mjs</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>.github/workflows/quality-gates.yml</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>frontend/packages/domains/third/AGENTS.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)", "<Path>frontend/packages/web-domains/third/AGENTS.md</Path> => single-agent (Lead; exclusive current workspace; T-29 turn only)"]
---

# T-29：清理过时文档与重复AGENTS权威

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先完整读Map→命中项目Skill入口/按scope引用→本票与上游。保留单人串行、无子代理/无新worktree。本票计划已Ready；本轮没有实施或重验，等待用户自行激活Goal。

## 1. 战略与来源

- 来源：D-05, D-06, B-17；AC-029；本轮用户请求与<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>。
- 当前事实：旧删除37份重复手册的证据保持；本轮重新审查所有当前文档，旧清单不再作为待删除任务。
- 可观察产出：每个删除文件有owner、内容迁移落点和无丢失硬约束证据

## 2. 决策状态

### 已锁定决策

保留工程分层、Client/权限、资源owner、安全日志、真实供应商协议和唯一六SQL基座。最新用户仅授权计划。

### 已确认方案

既有合同保持；本轮重新评审与验收；旧删除37份重复手册的证据保持；本轮重新审查所有当前文档，旧清单不再作为待删除任务。 完整决定及来源以<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>为准。

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

每个删除文件有owner、内容迁移落点和无丢失硬约束证据 正常、失败、越权和竞争路径按本票验收断言共同交付，不把前后端或测试分成无价值空票。

## 5. 实现契约

- 入口与输入输出：not-required: 逐文件hash/规则去向/路径引用和事实检查直接覆盖文档交付。
- 外部行为：每个删除文件有owner、内容迁移落点和无丢失硬约束证据
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
| 正常 | not-required: 逐文件hash/规则去向/路径引用和事实检查直接覆盖文档交付 | 每个删除文件有owner、内容迁移落点和无丢失硬约束证据 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-29-replan-2026-09-23.md</Path> |
| 失败/竞争 | 50POM/3App/247后端测试基线与最终源码变化一致（最终重算） | 明确失败/安全恢复，无伪成功、越权及部分提交 | 同上，记录故障注入与状态 |
| 回归 | 现有同域测试＋消费者＋适用静态门禁 | 所有当前引用和cwd命令可解析；文档不把候选CI、未跑服务或未批准设计写成已完成 | 同上，记录测试数/skip/源码 |

命令在仓根执行，`cd backend`表示该条命令切cwd；每条独立运行。以下为实施期命令，本轮未执行：

- `node .agents/skills/engineering-standards/scripts/validate-skill-facts.mjs`
- `node docs/fm/scripts/validate.mjs`

- Workspace checks：current-workspace，所列命令加命中工程Skill质量门禁。
- E2E disposition：not-required: 逐文件hash/规则去向/路径引用和事实检查直接覆盖文档交付。
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

- [ ] `AC-029`：每个删除文件有owner、内容迁移落点和无丢失硬约束证据。
- [ ] `AC-029`：50POM/3App/247后端测试基线与最终源码变化一致（最终重算）。
- [ ] `AC-029`：所有当前引用和cwd命令可解析。
- [ ] `AC-029`：文档不把候选CI、未跑服务或未批准设计写成已完成。
- [ ] 实际调用已绑定Skill，记录摘要/输入/步骤/输出；不是只“读过”。
- [ ] 正常、失败、回归和required E2E有当前候选证据，未运行不勾选。
- [ ] 写集、共享owner、合同和生成物一致；无未批准偏差。
- [ ] 真实commit/direct-parent/result出口已满足或按Goal对历史无需新实施票作有证据的取消裁决。

## 11. SKILL 调用计划

frontmatter每个必需绑定在implement阶段输入本票、真实调用方和diff，按scope执行约束检查与实现；verify阶段由engineering-standards执行适用门禁。实际Skill Execution Records写<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-29-replan-2026-09-23.md</Path>，包含id/phase/operation/sha256/status/evidence。入口摘要变化先读diff并重新绑定，不改旧历史记录。当前规划仅完成元数据/入口及相关规范路由，未伪造实施passed。

## 12. 停止、检查点与交付

交付本票完整可观察行为，数量以Map为准，不能以样例替代。缺高影响决定、必需Skill/引用/测试，或owner冲突，停止该票和依赖闭包；无依赖票仅在已获执行授权后继续。保留HEAD、diff、已跑命令、失败类别、待完成动作；相同失败无新证据或达到3次集成尝试先复盘。验收后回交Goal，全部票done仍不等于change可归档。

## 历史实现保留及本轮处置

历史implementation commit：`70eed51acb703ed19a48a47d182f2476f3eabcf7`；历史result：`6c8764cca97bb6057fcb90ccdfe635c7efbf502a`。提交存在及祖先关系见<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/legacy-ticket-audit.json</Path>。

<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-29.md</Path>及其引用日志是历史证据，本轮未重跑业务测试。原Ticket全文见<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/replan-2026-09-23-before/ticket/29-converge-current-documentation.md</Path>，不得按旧“尚未实现/提交暂缓”描述重复执行。

旧计划把所有票result设为同一整批提交且当前worktrees为空，不满足现行逐票验收记录合同；Lead须查原始记录。不能补造当时clean状态，不能为关闭历史票创建空commit。若现代码满足合同且无需新实现，经当前行为证据及明确处置可cancelled并保留AC由T-30覆盖；否则按真实修复重新形成产品提交，既有历史证据仍不删。

## 当前执行新增事实（2026-09-23）

T-34附加facts检查exit1：部署Skill仍明确固定本地私有报告为temp/relase，现场凭据维护按该规范已生成0600报告/恢复状态；validate-skill-facts.mjs又禁止此目录，同时仅对deploy Skill排除旧文本扫描。需核对规范权威，统一Skill/引用/检查器的当前路径合同并安全迁移本轮私有资料（如采用temp/release需冲突检查、保留权限/哈希和服务器报告对应关系）。不得删除恢复资料或只放宽检查求绿。此问题属T29文档/事实同步，执行前扩精确写集并重绑实际变更Skill，历史证据不改写。
