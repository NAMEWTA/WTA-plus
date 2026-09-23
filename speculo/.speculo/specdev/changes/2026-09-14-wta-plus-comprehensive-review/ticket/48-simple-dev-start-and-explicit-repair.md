---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-23已枚举.agents/skills全部入口；命中scope读取入口，common入口变化已复核，历史Skill Evidence不改写"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/48-simple-dev-start-and-explicit-repair.md</Path>", "<Path>scripts/start-dev.sh</Path>", "<Path>scripts/lib/</Path>"], "outputs": ["T-48的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/48-simple-dev-start-and-explicit-repair.md</Path>", "当前diff与验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-48-replan-2026-09-23.md</Path>"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "dev:launch", "dev:build-guard", "contract:AC-048"]
artifact: "ticket"
change: "2026-09-14-wta-plus-comprehensive-review"
id: "T-48"
title: "将日常启动与构建修复诊断分开"
status: "ready"
kind: "bug"
planning_depth: "standard"
planning_depth_reason: "局部多文件可观察行为或既有实现验收"
ready: true
risk: "medium"
blocked_by: ["T-32"]
contract_ids: ["AC-048"]
owner: "single-agent"
expected_changes: ["<Path>scripts/start-dev.sh</Path>", "<Path>scripts/lib/</Path>", "<Path>scripts/ci/verify-dev-build-guard.sh</Path>", "<Path>scripts/README.md</Path>", "<Path>backend/README.md</Path>", "<Path>release-artifacts/tests/</Path>"]
writable_paths: ["<Path>scripts/start-dev.sh</Path>", "<Path>scripts/lib/</Path>", "<Path>scripts/ci/verify-dev-build-guard.sh</Path>", "<Path>scripts/README.md</Path>", "<Path>backend/README.md</Path>", "<Path>release-artifacts/tests/</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>", "<Path>{roots.state}/specdev/adr/</Path>", "<Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/</Path>"]
shared_paths: ["<Path>scripts/start-dev.sh</Path>", "<Path>scripts/lib/</Path>", "<Path>scripts/ci/verify-dev-build-guard.sh</Path>", "<Path>scripts/README.md</Path>", "<Path>backend/README.md</Path>", "<Path>release-artifacts/tests/</Path>"]
shared_path_owners: ["<Path>scripts/start-dev.sh</Path> => single-agent (Lead; exclusive current workspace; T-48 turn only)", "<Path>scripts/lib/</Path> => single-agent (Lead; exclusive current workspace; T-48 turn only)", "<Path>scripts/ci/verify-dev-build-guard.sh</Path> => single-agent (Lead; exclusive current workspace; T-48 turn only)", "<Path>scripts/README.md</Path> => single-agent (Lead; exclusive current workspace; T-48 turn only)", "<Path>backend/README.md</Path> => single-agent (Lead; exclusive current workspace; T-48 turn only)", "<Path>release-artifacts/tests/</Path> => single-agent (Lead; exclusive current workspace; T-48 turn only)"]
---

# T-48：将日常启动与构建修复诊断分开

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先完整读Map→命中项目Skill入口/按scope引用→本票与上游。保留单人串行、无子代理/无新worktree。本票计划已Ready；本轮没有实施或重验，等待用户自行激活Goal。

## 1. 战略与来源

- 来源：R64-D-01；AC-048；本轮用户请求与<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>。
- 当前事实：start-dev强求非空local.yml、自解析server.port并覆盖Spring、每次检查classpath及哨兵；已有build guard与安全删除值得保留。
- 可观察产出：一个脚本提供显式start/build/doctor/repair子命令（菜单仅薄包装），普通再次启动不深度修复且尊重Spring/Vite环境优先级。

## 2. 决策状态

### 已锁定决策

保留工程分层、Client/权限、资源owner、安全日志、真实供应商协议和唯一六SQL基座。最新用户仅授权计划。

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

一个脚本提供显式start/build/doctor/repair子命令（菜单仅薄包装），普通再次启动不深度修复且尊重Spring/Vite环境优先级。 正常、失败、越权和竞争路径按本票验收断言共同交付，不把前后端或测试分成无价值空票。

## 5. 实现契约

- 入口与输入输出：shell fake命令夹具＋真实Linux初次/二次启动；Windows由支持环境实际验收。
- 外部行为：一个脚本提供显式start/build/doctor/repair子命令（菜单仅薄包装），普通再次启动不深度修复且尊重Spring/Vite环境优先级。
- 不变量：current workspace单writer；UseCase→Service→DAO→Mapper；classic保留；公开数据只经wta-api；GET查询/POST变更且安全@Log。Notify外部I/O不在结果事务内，IN_APP按确认后的短事务合同处理。
- 失败边界：不吞SQL/HTTP/Provider错误，不将UNKNOWN当成功或盲目可重试；页面旧响应不覆盖新会话。具体负向断言见第8/10节。
- 兼容：沿用用户此前明确的基座仓内直接切换决定，同步真实消费者/生成物；不放宽第三方协议。公共API技能用于调用方与影响核对，不重新增加已被用户排除的兼容桥。
- 安全：仅隔离合成测试；secret不进日志/截图/证据。对象/Client授权在后端实施，页面隐藏不替代权限。

## 6. 执行路线

1. 读真实POM/reactor和脚本调用，建立fake mvnw/包管理器的命令记录夹具。
2. 保留显式build/安装与wta-admin单模块run分工，不用聚合根spring-boot:run -am。
3. 日常路径只做工具/必要配置/产物缺失提示，端口交框架；新增非交互参数并保留菜单薄包装。
4. 深度JAR/classpath/clean放doctor/repair，保留构建互斥、路径白名单、越界拒绝及占端口不乱杀。
5. 新环境首次构建→启动→正常二次启动、环境端口/路径空格、Linux/现有Windows承诺分别验收。
6. 更新唯一开发说明与CI脚本测试，未验证平台明确阻塞对应支持声明。

## 7. 路径访问契约

frontmatter为预计点、硬写集与共享owner权威。目录写集仅授权本票行为所需文件；新增测试在声明根内，新增生产类须符合已有层次。不存在的新文件为计划创建，不声称已实现。共享物理/语义资源由single-agent在本票轮次独占；不同票不并行，跨change冲突仅暂停相关分支。

本票状态和Evidence仅由Lead写当前change；永久ADR/context及相邻SSO change只读。越界先修订Ticket/Map，禁止先改后报。

## 8. 验证矩阵

| 场景 | 接缝/步骤 | 预期 | Evidence |
|---|---|---|---|
| 正常 | shell fake命令夹具＋真实Linux初次/二次启动；Windows由支持环境实际验收 | 普通二次启动无深度哨兵/依赖重装/清缓存，显式repair仍可调用 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-48-replan-2026-09-23.md</Path> |
| 失败/竞争 | SERVER_PORT不被脚本覆盖；缺工具/secret单一错误不泄密 | 明确失败/安全恢复，无伪成功、越权及部分提交 | 同上，记录故障注入与状态 |
| 回归 | 现有同域测试＋消费者＋适用静态门禁 | 安全删除拒绝越界、端口不乱杀、reactor/单模块启动正确 | 同上，记录测试数/skip/源码 |

命令在仓根执行，`cd backend`表示该条命令切cwd；每条独立运行。以下为实施期命令，本轮未执行：

- `bash -n scripts/start-dev.sh`
- `bash scripts/ci/verify-dev-build-guard.sh`
- `bash release-artifacts/scripts/verify-release.sh`

- Workspace checks：current-workspace，所列命令加命中工程Skill质量门禁。
- E2E disposition：required: shell fake命令夹具＋真实Linux初次/二次启动；Windows由支持环境实际验收。
- E2E owner/environment：single-agent（Lead）/current-workspace；真实MySQL/Redis/MinIO必须为本任务隔离资源，必要服务缺失则阻塞对应验收。
- 真实服务启用方法与零skip要求：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/verification.md</Path>；新增用例必须保存精确选择器与实际计数，不能只运行mock或test list。
- Integration evidence：非空implementation commit、parent before、clean exact HEAD/tree时点的direct-parent和适用E2E、不可变result及父链；required模式不适用，不创建candidate worktree。

## 9. 发布、迁移与恢复

- 迁移顺序：保留repair显式入口与必要build lock。跨平台承诺以现有脚本支持范围为准，不新建进程平台。
- 兼容窗口：基座仓内同步切换，无未声明双写/双协议；外部现有协议保持。
- 监控：记录本票可观察失败/状态/耗时及资源数量，不记录敏感正文；不新增监控平台。
- 恢复：保存上个不可变候选及失败证据；停止受影响任务再核对外部副作用。不得通过恢复已披露secret、放宽权限或重发UNKNOWN恢复。
- 不可逆批准：产品commit/父分支更新及远程push、部署、轮换/真实数据操作、归档分别核对本轮授权。当前均未授权。
- 收缩条件：旧消费者/废弃字段/不必要配置引用清零且新合同验证通过；不适用的删除不人为增加。

## 10. 验收标准

- [ ] `AC-048`：普通二次启动无深度哨兵/依赖重装/清缓存，显式repair仍可调用。
- [ ] `AC-048`：SERVER_PORT不被脚本覆盖；缺工具/secret单一错误不泄密。
- [ ] `AC-048`：安全删除拒绝越界、端口不乱杀、reactor/单模块启动正确。
- [ ] 实际调用已绑定Skill，记录摘要/输入/步骤/输出；不是只“读过”。
- [ ] 正常、失败、回归和required E2E有当前候选证据，未运行不勾选。
- [ ] 写集、共享owner、合同和生成物一致；无未批准偏差。
- [ ] 真实commit/direct-parent/result出口已满足或按Goal对历史无需新实施票作有证据的取消裁决。

## 11. SKILL 调用计划

frontmatter每个必需绑定在implement阶段输入本票、真实调用方和diff，按scope执行约束检查与实现；verify阶段由engineering-standards执行适用门禁。实际Skill Execution Records写<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-48-replan-2026-09-23.md</Path>，包含id/phase/operation/sha256/status/evidence。入口摘要变化先读diff并重新绑定，不改旧历史记录。当前规划仅完成元数据/入口及相关规范路由，未伪造实施passed。

## 12. 停止、检查点与交付

交付本票完整可观察行为，数量以Map为准，不能以样例替代。缺高影响决定、必需Skill/引用/测试，或owner冲突，停止该票和依赖闭包；无依赖票仅在已获执行授权后继续。保留HEAD、diff、已跑命令、失败类别、待完成动作；相同失败无新证据或达到3次集成尝试先复盘。验收后回交Goal，全部票done仍不等于change可归档。
