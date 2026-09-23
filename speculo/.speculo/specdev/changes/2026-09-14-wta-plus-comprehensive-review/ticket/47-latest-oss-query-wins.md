---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-23已枚举.agents/skills全部入口；命中scope读取入口，common入口变化已复核，历史Skill Evidence不改写"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/47-latest-oss-query-wins.md</Path>", "<Path>frontend/packages/web-domains/system/src/oss/</Path>"], "outputs": ["T-47的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/47-latest-oss-query-wins.md</Path>", "<Path>frontend/packages/web-domains/system/src/oss/</Path>"], "outputs": ["T-47的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-module-guide", "path": "<Path>.agents/skills/wta-module-guide/SKILL.md</Path>", "sha256": "bb57a781314abe316f06ba9538f62043f1bd053904c0832969e575c09731a7a3", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/47-latest-oss-query-wins.md</Path>", "<Path>frontend/packages/web-domains/system/src/oss/</Path>"], "outputs": ["T-47的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/47-latest-oss-query-wins.md</Path>", "当前diff与验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-47-replan-2026-09-23.md</Path>"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "ui:oss-list", "contract:AC-047"]
artifact: "ticket"
change: "2026-09-14-wta-plus-comprehensive-review"
id: "T-47"
title: "OSS列表在回填前验证请求代次"
status: "done"
kind: "bug"
planning_depth: "standard"
planning_depth_reason: "局部多文件可观察行为或既有实现验收"
ready: true
risk: "medium"
blocked_by: []
contract_ids: ["AC-047"]
owner: "single-agent"
expected_changes: ["<Path>frontend/packages/web-domains/system/src/oss/</Path>"]
writable_paths: ["<Path>frontend/packages/web-domains/system/src/oss/</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>", "<Path>{roots.state}/specdev/adr/</Path>", "<Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/</Path>"]
shared_paths: ["<Path>frontend/packages/web-domains/system/src/oss/</Path>"]
shared_path_owners: ["<Path>frontend/packages/web-domains/system/src/oss/</Path> => single-agent (Lead; exclusive current workspace; T-47 turn only)"]
---

# T-47：OSS列表在回填前验证请求代次

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先完整读Map→命中项目Skill入口/按scope引用→本票与上游。用户已激活Goal并授权gpt-6-sol/xhigh原生子代理；current workspace保持单产品writer、无新worktree。Lead负责状态、提交与验收。

## 1. 战略与来源

- 来源：R64-O-04；AC-047；本轮用户请求与<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>。
- 当前事实：OssPage.getList 在 withLoading 内先写 rows/total，之后才判断generation；保护只覆盖后续预览。
- 可观察产出：A慢B快最终行、total、preview、loading、error均属于B；卸载或旧失败不污染当前页面。

## 2. 决策状态

### 已锁定决策

保留工程分层、Client/权限、资源owner、安全日志、真实供应商协议和唯一六SQL基座。最新用户已授权本地实施、提交及direct-parent验收。

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

A慢B快最终行、total、preview、loading、error均属于B；卸载或旧失败不污染当前页面。 正常、失败、越权和竞争路径按本票验收断言共同交付，不把前后端或测试分成无价值空票。

## 5. 实现契约

- 入口与输入输出：OssPage受控Promise组件测试，已有presentation.test.ts回归。
- 外部行为：A慢B快最终行、total、preview、loading、error均属于B；卸载或旧失败不污染当前页面。
- 不变量：current workspace单writer；UseCase→Service→DAO→Mapper；classic保留；公开数据只经wta-api；GET查询/POST变更且安全@Log。Notify外部I/O不在结果事务内，IN_APP按确认后的短事务合同处理。
- 失败边界：不吞SQL/HTTP/Provider错误，不将UNKNOWN当成功或盲目可重试；页面旧响应不覆盖新会话。具体负向断言见第8/10节。
- 兼容：沿用用户此前明确的基座仓内直接切换决定，同步真实消费者/生成物；不放宽第三方协议。公共API技能用于调用方与影响核对，不重新增加已被用户排除的兼容桥。
- 安全：仅隔离合成测试；secret不进日志/截图/证据。对象/Client授权在后端实施，页面隐藏不替代权限。

## 6. 执行路线

1. 受控Promise同时返回配置与列表，复现A迟到覆盖B及旧错误清loading。
2. 结果留局部变量，状态写入前一次校验generation和组件存活。
3. 本代统一提交rows/total/preview标识；loading/error仅当前代可写，卸载使代次失效。
4. 覆盖过滤/翻页/刷新/卸载与预览失败，保持授权URL按需申请。

## 7. 路径访问契约

frontmatter为预计点、硬写集与共享owner权威。目录写集仅授权本票行为所需文件；新增测试在声明根内，新增生产类须符合已有层次。不存在的新文件为计划创建，不声称已实现。共享物理/语义资源由single-agent在本票轮次独占；不同票不并行，跨change冲突仅暂停相关分支。

本票状态和Evidence仅由Lead写当前change；永久ADR/context及相邻SSO change只读。越界先修订Ticket/Map，禁止先改后报。

## 8. 验证矩阵

| 场景 | 接缝/步骤 | 预期 | Evidence |
|---|---|---|---|
| 正常 | OssPage受控Promise组件测试，已有presentation.test.ts回归 | A最后返回不覆盖B的行/总数/预览；旧失败不覆盖B成功 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-47-replan-2026-09-23.md</Path> |
| 失败/竞争 | 组件卸载无回填，loading属于最新请求 | 明确失败/安全恢复，无伪成功、越权及部分提交 | 同上，记录故障注入与状态 |
| 回归 | 现有同域测试＋消费者＋适用静态门禁 | 权限、分页排序及预览生命周期不退化 | 同上，记录测试数/skip/源码 |

命令在仓根执行，`cd backend`表示该条命令切cwd；每条独立运行。以下命令已实际执行，结果见Evidence：

- `pnpm --dir frontend --filter @namewta/web-domain-system test`
- `pnpm --dir frontend --filter @namewta/web-domain-system typecheck`

- Workspace checks：current-workspace，所列命令加命中工程Skill质量门禁。
- E2E disposition：not-required: 受控Promise组件与类型检查能直接判定全部竞态；T-30执行页面组合回归。
- E2E owner/environment：single-agent（Lead）/current-workspace；真实MySQL/Redis/MinIO必须为本任务隔离资源，必要服务缺失则阻塞对应验收。
- 真实服务启用方法与零skip要求：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/verification.md</Path>；新增用例必须保存精确选择器与实际计数，不能只运行mock或test list。
- Integration evidence：非空implementation commit、parent before、clean exact HEAD/tree时点的direct-parent和适用E2E、不可变result及父链；required模式不适用，不创建candidate worktree。

## 9. 发布、迁移与恢复

- 迁移顺序：同批修改源码、消费者、测试及生成合同；无需新增数据库迁移。涉及已有库时必须Tag差异、备份、隔离演练，不重放基座。
- 兼容窗口：基座仓内同步切换，无未声明双写/双协议；外部现有协议保持。
- 监控：记录本票可观察失败/状态/耗时及资源数量，不记录敏感正文；不新增监控平台。
- 恢复：保存上个不可变候选及失败证据；停止受影响任务再核对外部副作用。不得通过恢复已披露secret、放宽权限或重发UNKNOWN恢复。
- 不可逆批准：产品commit/父分支更新及远程push、部署、轮换/真实数据操作、归档分别核对本轮授权。本地产品commit/direct-parent已授权；远程push、部署、归档不在本票授权内。
- 收缩条件：旧消费者/废弃字段/不必要配置引用清零且新合同验证通过；不适用的删除不人为增加。

## 10. 验收标准

- [x] `AC-047`：A最后返回不覆盖B的行/总数/预览；旧失败不覆盖B成功。
- [x] `AC-047`：组件卸载无回填，loading属于最新请求。
- [x] `AC-047`：权限、分页排序及预览生命周期不退化。
- [x] 实际调用已绑定Skill，记录摘要/输入/步骤/输出；不是只“读过”。
- [x] 正常、失败、回归和required E2E有当前候选证据，未运行不勾选。
- [x] 写集、共享owner、合同和生成物一致；无未批准偏差。
- [x] 真实commit/direct-parent/result出口已满足或按Goal对历史无需新实施票作有证据的取消裁决。

## 11. SKILL 调用计划

frontmatter每个必需绑定在implement阶段输入本票、真实调用方和diff，按scope执行约束检查与实现；verify阶段由engineering-standards执行适用门禁。实际Skill Execution Records写<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-47-replan-2026-09-23.md</Path>，包含id/phase/operation/sha256/status/evidence。入口摘要变化先读diff并重新绑定，不改旧历史记录。当前规划仅完成元数据/入口及相关规范路由，未伪造实施passed。

## 12. 停止、检查点与交付

交付本票完整可观察行为，数量以Map为准，不能以样例替代。缺高影响决定、必需Skill/引用/测试，或owner冲突，停止该票和依赖闭包；无依赖票仅在已获执行授权后继续。保留HEAD、diff、已跑命令、失败类别、待完成动作；相同失败无新证据或达到3次集成尝试先复盘。验收后回交Goal，全部票done仍不等于change可归档。

## 当前施工澄清（首轮规范复核）

“本代统一提交”指列表响应的rows/total/配置及空预览标识同时归属当前代，不要求等待可选图片URL。列表成功应立即可用并结束列表loading；URL按同代且alive渐进回填，挂起图片不阻塞列表或刷新完成。其余AC不变，首轮失败报告保留。

## 13. 当前验收

<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-47.md</Path>；result `7cd6fb22b28b7d464b9648ba77f2a2308e5b2016`，当前clean验收/双轴通过，731全前端与三App生产构建通过；17项新SFC回归。
