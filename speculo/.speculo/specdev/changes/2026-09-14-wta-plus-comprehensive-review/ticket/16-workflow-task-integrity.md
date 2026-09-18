---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-18枚举.agents/skills入口并按本票真实路径/领域绑定；Map为最低集合"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/16-workflow-task-integrity.md</Path>", "<Path>frontend/packages/web-domains/workflow/src/components/ProcessActionDialog.vue</Path>", "<Path>frontend/packages/web-domains/workflow/src/</Path>"], "outputs": ["T-16的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/16-workflow-task-integrity.md</Path>", "<Path>frontend/packages/web-domains/workflow/src/components/ProcessActionDialog.vue</Path>", "<Path>frontend/packages/web-domains/workflow/src/</Path>"], "outputs": ["T-16的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-module-guide", "path": "<Path>.agents/skills/wta-module-guide/SKILL.md</Path>", "sha256": "441de2ccc513e09820ed3d7d2faf559eeaa7466202e4fbb0c8dd3eabf09510c9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/16-workflow-task-integrity.md</Path>", "<Path>frontend/packages/web-domains/workflow/src/components/ProcessActionDialog.vue</Path>", "<Path>frontend/packages/web-domains/workflow/src/</Path>"], "outputs": ["T-16的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/16-workflow-task-integrity.md</Path>", "current-workspace实际diff及本票验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-16.md</Path>：命令、退出码、测试数、AC与Skill Execution Records"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "finding:F-03", "finding:B-10", "contract:AC-016"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-16
title: 保证流程任务读取和办理对象一致
status: "ready"
planning_depth: "deep"
planning_depth_reason: "安全/鉴权、公共合同、数据一致性或共享核心路径变更：保证流程任务读取和办理对象一致"
ready: true
risk: high
blocked_by: []
contract_ids: [AC-016]
owner: single-agent
expected_changes: ["<Path>frontend/packages/web-domains/workflow/src/components/ProcessActionDialog.vue</Path>", "<Path>frontend/packages/web-domains/workflow/src/</Path>", "<Path>backend/wta-modules/wta-workflow/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/</Path>", "<Path>frontend/e2e/</Path>"]
writable_paths: ["<Path>frontend/packages/web-domains/workflow/src/components/ProcessActionDialog.vue</Path>", "<Path>frontend/packages/web-domains/workflow/src/</Path>", "<Path>backend/wta-modules/wta-workflow/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/</Path>", "<Path>frontend/e2e/</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: ["<Path>frontend/packages/web-domains/workflow/src/</Path>", "<Path>backend/wta-modules/wta-workflow/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/</Path>", "<Path>frontend/e2e/</Path>"]
shared_path_owners: ["<Path>frontend/packages/web-domains/workflow/src/</Path> => single-agent (Lead; serial T-16 turn)", "<Path>backend/wta-modules/wta-workflow/</Path> => single-agent (Lead; serial T-16 turn)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/</Path> => single-agent (Lead; serial T-16 turn)", "<Path>frontend/e2e/</Path> => single-agent (Lead; serial T-16 turn)"]
---

# T-16：保证流程任务读取和办理对象一致

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先读Map，再按Skill矩阵读取适用入口/引用，再读本票。禁止任何implementation/review/research子代理。Ready仅表示计划合同就绪，不表示已经授权实施或已验证通过。

## 1. 战略与来源

- 目标与可观察产出：B失败绝不发出A的审批请求。
- 来源：F-03, B-10；AC-016；USER-DECISION: 全面完善计划、无兼容、单人串行。
- 当前事实与调用链：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/re-review.md</Path>中T-16行及对应专项报告；源码导航为frontmatter预计修改点。
- 规划深度：deep；安全/鉴权、公共合同、数据一致性或共享核心路径变更：保证流程任务读取和办理对象一致。

## 2. 决策状态

### 已锁定决策

复用既有checkTaskReadAccess及WarmFlow锁；先用实际引擎权限测试确定调用接入点，B-10仍标likely直到运行证实。

### 已采用的低影响假设

沿用当前仓库版本与既有模块命名；实施前回读实际源码，路径变化由本票修订，不猜测不存在的实现。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 当前task打开/加载→捕获taskId/payload→确认→办理 | WarmFlow现有checkTaskReadAccess、任务锁与状态校验；不新增taskVersion | 本票之外的模块重写、兼容桥、远程发布与重要数据操作 |

## 4. 要构建什么

当前task打开/加载→捕获taskId/payload→确认→办理。调用者可观察到：B失败绝不发出A的审批请求。失败时：generation失效不更新/提交；陌生人读节点被拒；旧任务不二次推进。

## 5. 实现契约

- 入口、输入输出与数据流：当前task打开/加载→捕获taskId/payload→确认→办理。
- 不变量及失败语义：generation失效不更新/提交；陌生人读节点被拒；旧任务不二次推进。
- 公共合同：本票只按以上行为及执行路线变更；同步全部仓内调用/生成物，沿用已有权限/Client/owner校验。未列出的接口保持原语义。
- 兼容：用户明确无需旧版兼容；仓内一次切换，不加双路由/版本等待。实际供应商协议仍须遵守。
- 安全与隐私：凭据不进入日志/UI证据；越权/过期/无owner拒绝；数据库与资源约束不能为前端成功而放宽。

CompleteTaskBo没有现成taskVersion合同，FlwTaskServiceImpl.completeTask已有Lock4j。节点读取缺显式checkTaskReadAccess为静态事实，真实WarmFlow调用效果仍待集成验证；listVariable不是本finding定位的两个入口，不把它写成已确认缺陷。

## 6. 执行路线

1. 用受控Promise复现A加载后打开B失败或乱序响应；open立即清task/nextNodes/附属动作并递增generation。
2. 只有当前generation可提交结果、错误和loading终态；关闭/卸载使generation失效。当前HTTP端口无signal，generation即可修复本问题，不强制扩展所有transport。
3. 提交前同步阻止重复提交，捕获当前taskId及payload；await确认后再次核对generation，禁止闭包读取已被切换的task.value。
4. 分别检查getNextNodeList与getBackTaskNode的实际读取权限，复用已有checkTaskReadAccess；保留WarmFlow办理handler、现有任务锁和状态校验。
5. 不新增前后端task version字段来修前端竞态；只有定向后端测试证明现有状态/锁不足时才扩大合同。

## 7. 路径访问契约

预计点、可写范围、只读上下文及共享项以frontmatter为唯一权威。共享owner固定single-agent；只有当前票轮次可写，下一票须回读前一结果。跨模块目录只允许本票行为必需的文件，目录授权不意味着重写全部模块。新增测试位于同模块测试目录；未覆盖的新路径先修订本票/Map再写。
保留当前用户未提交改动、永久ADR/context、供应商源码与运行数据；生成物通过正式工具更新。

## 8. 验证矩阵

| 行为或风险 | 验证接缝/步骤 | 预期结果 | Evidence |
|---|---|---|---|
| 正常路径 | 当前task打开/加载→捕获taskId/payload→确认→办理；执行下列定向命令及对应场景 | B失败绝不发出A的审批请求 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-16.md</Path> |
| 失败路径 | generation失效不更新/提交；陌生人读节点被拒；旧任务不二次推进；固定时序/故障注入，记录输入与最终可观察状态 | 无越权、错误状态或部分提交；可按定义恢复 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-16.md</Path> |
| 回归 | 运行所属包原有测试及受影响调用者；逐项核对下列AC | 陌生用户读取任务节点/变量被拒，合法办理人/发起人按既有规则可读；双击/过期任务不重复推进流程；快速切换、关闭、网络乱序与服务端失败可恢复 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-16.md</Path> |

Workspace checks：current-workspace；`frontend:`/`backend:`表示先进入该目录，其他命令cwd为仓根，逐条串行执行。新增用例实施时登记精确选择器、实际测试数与跳过项，零测试/required跳过不算通过。

- `frontend: pnpm test`
- `frontend: pnpm exec playwright test e2e/workflow-runtime.spec.ts e2e/workflow-definition.spec.ts`
- `backend: ./mvnw -pl wta-modules/wta-workflow,wta-admin -am test`

- E2E disposition：required: 真引擎授权矩阵及快速A/B切换、B失败、确认期间切任务、重复办理。
- E2E owner/environment：single-agent（Lead）/current-workspace；使用隔离MySQL/Redis/OSS及必要真实HTTP/浏览器，禁止连生产。场景步骤以上表、本票AC为准；需新用例时在写集内创建后记录精确命令。
- Integration evidence：记录parent before、implementation commit及direct-parent检查；result SHA等于通过验证的implementation commit，candidate不适用。当前全部产品检查not-run。

## 9. 发布、迁移与恢复

- 顺序：前置票产生已验证合同后实施本票；源码、仓内消费者、测试和生成物同批交付。涉及DDL只编辑10-cde-base-ddl.sql，新环境按六文件基座初始化；不增加存量迁移工程。
- 兼容窗口：无；不保留旧接口或数据格式桥。生产部署不是本票自动步骤。
- 监控/诊断：观察本票AC的成功/错误状态、耗时及资源/持久化结果，日志只含安全元数据；复用现有观测入口，不新建监控平台。
- 恢复：恢复前后端候选；已发生流程动作不靠数据库手改撤销。
- 不可逆批准点：提交、推送、部署、运行数据删除/修复分别需授权；本轮只有计划文档授权。
- 收缩条件：本票替代的旧调用/配置引用归零且仓内回归通过；无被替代入口时不适用，不为凑清单扩大删除范围。

## 10. 验收标准

- [ ] `AC-016`：B失败绝不发出A的审批请求。
- [ ] `AC-016`：陌生用户读取任务节点/变量被拒，合法办理人/发起人按既有规则可读。
- [ ] `AC-016`：双击/过期任务不重复推进流程。
- [ ] `AC-016`：快速切换、关闭、网络乱序与服务端失败可恢复。
- [ ] 按Map→适用Skill→本票完成读取及实际调用；所有required Skill记录passed并可回读。
- [ ] 正常/失败/回归及required E2E均完成，证据写入<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-16.md</Path>，未执行不得标通过。
- [ ] 修改不超出写集，共享项只有single-agent当前票轮次写入。
- [ ] 获得授权后形成非空implementation commit，Lead完成direct-parent验收并记录parent result SHA；未获授权不提交、不标Done。
- [ ] Ticket、Map、Goal与Evidence一致；不存在未批准偏差。

## 11. SKILL 调用计划

frontmatter绑定的项目Skill在implementation阶段接收本票路径和上游合同，产出适用分层、权限、数据/资源边界及实现diff；engineering-standards在verify阶段根据本节命令选择受影响门禁并输出AC/退出码/E2E记录。按入口scope展开引用，不以“已读”代替实际操作。
必需入口缺失或sha256漂移时阻塞本票；Lead回读差异后更新绑定及Map，不能自动接受新摘要。实际记录归<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-16.md</Path>的`## Skill Execution Records`，本次规划不伪造实现调用记录。

## 12. 停止、检查点与交付

交付本票完整可观察行为及验收证据；数量以Map为准。缺依赖/测试环境/Skill、越界或高影响事实变化时停止受影响票，保留checkpoint和失败证据，其他独立票仍可串行推进。恢复先读Goal、Map、本票、状态及最新Evidence；记录实际HEAD/dirty差异，禁止覆盖用户修改。
依赖：无。单票完成条件为全部AC、实际Skill证据和获授权的direct-parent出口；仅补文档不能标Done。
