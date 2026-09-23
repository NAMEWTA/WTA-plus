---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-23已枚举.agents/skills全部入口；命中scope读取入口，common入口变化已复核，历史Skill Evidence不改写"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/30-integrated-upgrade-acceptance.md</Path>", "<Path>frontend/e2e/</Path>", "<Path>backend/wta-admin/src/test/</Path>"], "outputs": ["T-30的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/30-integrated-upgrade-acceptance.md</Path>", "<Path>frontend/e2e/</Path>", "<Path>backend/wta-admin/src/test/</Path>"], "outputs": ["T-30的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-module-guide", "path": "<Path>.agents/skills/wta-module-guide/SKILL.md</Path>", "sha256": "bb57a781314abe316f06ba9538f62043f1bd053904c0832969e575c09731a7a3", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/30-integrated-upgrade-acceptance.md</Path>", "<Path>frontend/e2e/</Path>", "<Path>backend/wta-admin/src/test/</Path>"], "outputs": ["T-30的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/30-integrated-upgrade-acceptance.md</Path>", "当前diff与验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-30-replan-2026-09-23.md</Path>"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "contract:AC-001", "contract:AC-002", "contract:AC-003", "contract:AC-004", "contract:AC-005", "contract:AC-006", "contract:AC-007", "contract:AC-008", "contract:AC-009", "contract:AC-010", "contract:AC-011", "contract:AC-012", "contract:AC-013", "contract:AC-014", "contract:AC-015", "contract:AC-016", "contract:AC-017", "contract:AC-018", "contract:AC-019", "contract:AC-020", "contract:AC-021", "contract:AC-022", "contract:AC-023", "contract:AC-024", "contract:AC-025", "contract:AC-026", "contract:AC-027", "contract:AC-028", "contract:AC-029", "contract:AC-030", "contract:AC-031", "contract:AC-032", "contract:AC-033", "contract:AC-034", "contract:AC-035", "contract:AC-036", "contract:AC-037", "contract:AC-038", "contract:AC-039", "contract:AC-040", "contract:AC-041", "contract:AC-042", "contract:AC-043", "contract:AC-044", "contract:AC-045", "contract:AC-046", "contract:AC-047", "contract:AC-048", "contract:AC-049", "contract:AC-050"]
artifact: "ticket"
change: "2026-09-14-wta-plus-comprehensive-review"
id: "T-30"
title: "完成升级整体验收与可审查交付"
status: "ready"
kind: "review"
planning_depth: "deep"
planning_depth_reason: "公共合同/事务/安全/数据及恢复边界"
ready: true
risk: "high"
blocked_by: ["T-01", "T-02", "T-03", "T-04", "T-05", "T-06", "T-07", "T-08", "T-09", "T-10", "T-11", "T-12", "T-13", "T-14", "T-15", "T-16", "T-17", "T-18", "T-19", "T-20", "T-21", "T-22", "T-23", "T-24", "T-25", "T-26", "T-27", "T-28", "T-29", "T-31", "T-32", "T-33", "T-34", "T-35", "T-36", "T-37", "T-38", "T-39", "T-40", "T-41", "T-42", "T-43", "T-44", "T-45", "T-46", "T-47", "T-48", "T-49", "T-50"]
contract_ids: ["AC-001", "AC-002", "AC-003", "AC-004", "AC-005", "AC-006", "AC-007", "AC-008", "AC-009", "AC-010", "AC-011", "AC-012", "AC-013", "AC-014", "AC-015", "AC-016", "AC-017", "AC-018", "AC-019", "AC-020", "AC-021", "AC-022", "AC-023", "AC-024", "AC-025", "AC-026", "AC-027", "AC-028", "AC-029", "AC-030", "AC-031", "AC-032", "AC-033", "AC-034", "AC-035", "AC-036", "AC-037", "AC-038", "AC-039", "AC-040", "AC-041", "AC-042", "AC-043", "AC-044", "AC-045", "AC-046", "AC-047", "AC-048", "AC-049", "AC-050"]
owner: "single-agent"
expected_changes: ["<Path>frontend/e2e/</Path>", "<Path>backend/wta-admin/src/test/</Path>", "<Path>scripts/ci/</Path>", "<Path>release-artifacts/tests/</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/src/test/java/org/namewta/profile/enterprise/service/impl/EnterpriseApplicationMySqlE2ETest.java</Path>"]
writable_paths: ["<Path>frontend/e2e/</Path>", "<Path>backend/wta-admin/src/test/</Path>", "<Path>scripts/ci/</Path>", "<Path>release-artifacts/tests/</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/src/test/java/org/namewta/profile/enterprise/service/impl/EnterpriseApplicationMySqlE2ETest.java</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>", "<Path>{roots.state}/specdev/adr/</Path>", "<Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/</Path>"]
shared_paths: ["<Path>frontend/e2e/</Path>", "<Path>backend/wta-admin/src/test/</Path>", "<Path>scripts/ci/</Path>", "<Path>release-artifacts/tests/</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/src/test/java/org/namewta/profile/enterprise/service/impl/EnterpriseApplicationMySqlE2ETest.java</Path>"]
shared_path_owners: ["<Path>frontend/e2e/</Path> => single-agent (Lead; exclusive current workspace; T-30 turn only)", "<Path>backend/wta-admin/src/test/</Path> => single-agent (Lead; exclusive current workspace; T-30 turn only)", "<Path>scripts/ci/</Path> => single-agent (Lead; exclusive current workspace; T-30 turn only)", "<Path>release-artifacts/tests/</Path> => single-agent (Lead; exclusive current workspace; T-30 turn only)", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/src/test/java/org/namewta/profile/enterprise/service/impl/EnterpriseApplicationMySqlE2ETest.java</Path> => single-agent (Lead; exclusive current workspace; T-30 turn only)"]
---

# T-30：完成升级整体验收与可审查交付

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先完整读Map→命中项目Skill入口/按scope引用→本票与上游。保留单人串行、无子代理/无新worktree。本票计划已Ready；本轮没有实施或重验，等待用户自行激活Goal。

## 1. 战略与来源

- 来源：；AC-001, AC-002, AC-003, AC-004, AC-005, AC-006, AC-007, AC-008, AC-009, AC-010, AC-011, AC-012, AC-013, AC-014, AC-015, AC-016, AC-017, AC-018, AC-019, AC-020, AC-021, AC-022, AC-023, AC-024, AC-025, AC-026, AC-027, AC-028, AC-029, AC-030, AC-031, AC-032, AC-033, AC-034, AC-035, AC-036, AC-037, AC-038, AC-039, AC-040, AC-041, AC-042, AC-043, AC-044, AC-045, AC-046, AC-047, AC-048, AC-049, AC-050；本轮用户请求与<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>。
- 当前事实：扩展为AC-001—050同候选集成、历史票闭合、可发布候选及归档准备；旧全绿不再代表当前完成。
- 可观察产出：全部已接受AC均有实际命令/退出码/环境/源码checkpoint

## 2. 决策状态

### 已锁定决策

保留工程分层、Client/权限、资源owner、安全日志、真实供应商协议和唯一六SQL基座。最新用户仅授权计划。

### 已确认方案

既有合同保持；本轮重新评审与验收；扩展为AC-001—050同候选集成、历史票闭合、可发布候选及归档准备；旧全绿不再代表当前完成。 完整决定及来源以<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>为准。

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

全部已接受AC均有实际命令/退出码/环境/源码checkpoint 正常、失败、越权和竞争路径按本票验收断言共同交付，不把前后端或测试分成无价值空票。

## 5. 实现契约

- 入口与输入输出：required: 同一候选完整运行SSO/Profile/workflow/Notify/Third/树/三App发布及失败恢复。
- 外部行为：全部已接受AC均有实际命令/退出码/环境/源码checkpoint
- 不变量：current workspace单writer；UseCase→Service→DAO→Mapper；classic保留；公开数据只经wta-api；GET查询/POST变更且安全@Log。Notify外部I/O不在结果事务内，IN_APP按确认后的短事务合同处理。
- 失败边界：不吞SQL/HTTP/Provider错误，不将UNKNOWN当成功或盲目可重试；页面旧响应不覆盖新会话。具体负向断言见第8/10节。
- 兼容：沿用用户此前明确的基座仓内直接切换决定，同步真实消费者/生成物；不放宽第三方协议。公共API技能用于调用方与影响核对，不重新增加已被用户排除的兼容桥。
- 安全：仅隔离合成测试；secret不进日志/截图/证据。对象/Client授权在后端实施，页面隐藏不替代权限。

## 6. 执行路线

1. 冻结最终代码/依赖/配置与新的验证矩阵；逐票核对历史证据失效闭包及未决决策。
2. 完成所有旧票的复验与合法关闭记录；新票完成真实实现commit/direct-parent，不使用共同旧result伪造逐票完成。
3. 按相同候选运行后端/前端/真实依赖/浏览器/权限/失败注入及全部新合同；对默认skip逐项显式启用。
4. full产物校验后再core clean；保存三个App模式、完整release manifest、SHA及失败恢复证明。
5. 合并source/report finding→AC→ticket→test→commit矩阵，检查所有非目标仍独立归属且没有隐藏未验证。
6. 按change-completion完成clean HEAD状态证明与完成治理提交；形成归档准备清单，由A在单独批准后归档/知识提升。

## 7. 路径访问契约

frontmatter为预计点、硬写集与共享owner权威。目录写集仅授权本票行为所需文件；新增测试在声明根内，新增生产类须符合已有层次。不存在的新文件为计划创建，不声称已实现。共享物理/语义资源由single-agent在本票轮次独占；不同票不并行，跨change冲突仅暂停相关分支。

本票状态和Evidence仅由Lead写当前change；永久ADR/context及相邻SSO change只读。越界先修订Ticket/Map，禁止先改后报。

## 8. 验证矩阵

| 场景 | 接缝/步骤 | 预期 | Evidence |
|---|---|---|---|
| 正常 | required: 同一候选完整运行SSO/Profile/workflow/Notify/Third/树/三App发布及失败恢复 | AC-001—AC-050全部有当前候选通过证据，报告18项全部有关闭结论 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-30-replan-2026-09-23.md</Path> |
| 失败/竞争 | 历史共同result及worktrees空缺已按事实处置，未伪造验收时点或新空提交 | 明确失败/安全恢复，无伪成功、越权及部分提交 | 同上，记录故障注入与状态 |
| 回归 | 现有同域测试＋消费者＋适用静态门禁 | 真实MySQL/Redis/MinIO及浏览器所需场景无required skip，full/core/三App同源候选完整；G-security-external和所有归档必需条件关闭，未批准风险不消失；部署、永久知识、归档移动按授权执行 | 同上，记录测试数/skip/源码 |

命令在仓根执行，`cd backend`表示该条命令切cwd；每条独立运行。以下为实施期命令，本轮未执行：

- `cd frontend && pnpm architecture:check`
- `cd frontend && pnpm lint`
- `cd frontend && pnpm typecheck`
- `cd frontend && pnpm test`
- `cd frontend && pnpm test:e2e`
- `cd frontend && pnpm build:dev`
- `cd frontend && pnpm build:prod`
- `cd frontend && pnpm exec playwright test --config playwright.sso.config.ts`
- `cd backend && ./mvnw test`
- `cd backend && ./mvnw clean package -DskipTests`
- `bash scripts/ci/verify-admin-bundle.sh full`
- `cd backend && ./mvnw clean package -Pbundle-core -Dmaven.test.skip=true`
- `bash scripts/ci/verify-admin-bundle.sh core`
- `bash release-artifacts/scripts/verify-release.sh`
- `bash scripts/ci/run-external-services.sh`

- Workspace checks：current-workspace，所列命令加命中工程Skill质量门禁。
- E2E disposition：required: 同一候选完整运行SSO/Profile/workflow/Notify/Third/树/三App发布及失败恢复。
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

- [ ] `AC-030`：全部已接受AC均有实际命令/退出码/环境/源码checkpoint。
- [ ] `AC-030`：required E2E与失败注入全部完成，not-run不能被标通过。
- [ ] `AC-030`：未增加安全/类型豁免或删除测试制造绿色。
- [ ] `AC-030`：用户能据artifact digest批准明确候选，未自动发布。

- [ ] `AC-030`：AC-001—AC-050全部有当前候选通过证据，报告18项全部有关闭结论。
- [ ] `AC-030`：历史共同result及worktrees空缺已按事实处置，未伪造验收时点或新空提交。
- [ ] `AC-030`：真实MySQL/Redis/MinIO及浏览器所需场景无required skip，full/core/三App同源候选完整。
- [ ] `AC-030`：G-security-external和所有归档必需条件关闭，未批准风险不消失；部署、永久知识、归档移动按授权执行。
- [ ] 实际调用已绑定Skill，记录摘要/输入/步骤/输出；不是只“读过”。
- [ ] 正常、失败、回归和required E2E有当前候选证据，未运行不勾选。
- [ ] 写集、共享owner、合同和生成物一致；无未批准偏差。
- [ ] 真实commit/direct-parent/result出口已满足或按Goal对历史无需新实施票作有证据的取消裁决。

## 11. SKILL 调用计划

frontmatter每个必需绑定在implement阶段输入本票、真实调用方和diff，按scope执行约束检查与实现；verify阶段由engineering-standards执行适用门禁。实际Skill Execution Records写<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-30-replan-2026-09-23.md</Path>，包含id/phase/operation/sha256/status/evidence。入口摘要变化先读diff并重新绑定，不改旧历史记录。当前规划仅完成元数据/入口及相关规范路由，未伪造实施passed。

## 12. 停止、检查点与交付

交付本票完整可观察行为，数量以Map为准，不能以样例替代。缺高影响决定、必需Skill/引用/测试，或owner冲突，停止该票和依赖闭包；无依赖票仅在已获执行授权后继续。保留HEAD、diff、已跑命令、失败类别、待完成动作；相同失败无新证据或达到3次集成尝试先复盘。验收后回交Goal，全部票done仍不等于change可归档。

## 历史实现保留及本轮处置

历史implementation commit：`91d4c2ea6356c74d3174f63f2665485484f0a103`；历史result：`6c8764cca97bb6057fcb90ccdfe635c7efbf502a`。提交存在及祖先关系见<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/legacy-ticket-audit.json</Path>。

<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-30.md</Path>及其引用日志是历史证据，本轮未重跑业务测试。原Ticket全文见<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/replan-2026-09-23-before/ticket/30-integrated-upgrade-acceptance.md</Path>，不得按旧“尚未实现/提交暂缓”描述重复执行。

旧计划把所有票result设为同一整批提交且当前worktrees为空，不满足现行逐票验收记录合同；Lead须查原始记录。不能补造当时clean状态，不能为关闭历史票创建空commit。若现代码满足合同且无需新实现，经当前行为证据及明确处置可cancelled并保留AC由T-30覆盖；否则按真实修复重新形成产品提交，既有历史证据仍不删。
