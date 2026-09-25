---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-23已枚举.agents/skills全部入口；命中scope读取入口，common入口变化已复核，历史Skill Evidence不改写"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/43-measure-notify-fanout.md</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotificationApplicationRuntimeService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotifyDispatchResultService.java</Path>"], "outputs": ["T-43的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/43-measure-notify-fanout.md</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotificationApplicationRuntimeService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotifyDispatchResultService.java</Path>"], "outputs": ["T-43的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-module-guide", "path": "<Path>.agents/skills/wta-module-guide/SKILL.md</Path>", "sha256": "bb57a781314abe316f06ba9538f62043f1bd053904c0832969e575c09731a7a3", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/43-measure-notify-fanout.md</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotificationApplicationRuntimeService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotifyDispatchResultService.java</Path>"], "outputs": ["T-43的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-common-modules-guide", "path": "<Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>", "sha256": "d7b7e105c37499e0e0f8ad9b1e2dbf379100df5b6a47e0d6affb04483f8b162c", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/43-measure-notify-fanout.md</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotificationApplicationRuntimeService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotifyDispatchResultService.java</Path>"], "outputs": ["T-43的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/43-measure-notify-fanout.md</Path>", "当前diff与验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-43-replan-2026-09-23.md</Path>"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "notify:result-tx", "notify:batch-submit", "contract:AC-043"]
artifact: "ticket"
change: "2026-09-14-wta-plus-comprehensive-review"
id: "T-43"
title: "测量公告批量写入并控制聚合成本"
status: "in_progress"
kind: "bug"
planning_depth: "standard"
planning_depth_reason: "局部多文件可观察行为或既有实现验收"
ready: true
risk: "medium"
blocked_by: ["T-36", "T-38", "T-39"]
contract_ids: ["AC-043"]
owner: "single-agent"
expected_changes: ["<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotificationApplicationRuntimeService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotifyDispatchResultService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/NotifyNotificationDao.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/mapper/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/resources/mapper/notify/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>", "<Path>backend/wta-modules/wta-notify/src/test/</Path>"]
writable_paths: ["<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotificationApplicationRuntimeService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotifyDispatchResultService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/NotifyNotificationDao.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/mapper/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/resources/mapper/notify/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>", "<Path>backend/wta-modules/wta-notify/src/test/</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>", "<Path>{roots.state}/specdev/adr/</Path>", "<Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/</Path>"]
shared_paths: ["<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotificationApplicationRuntimeService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotifyDispatchResultService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/NotifyNotificationDao.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/mapper/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/resources/mapper/notify/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>", "<Path>backend/wta-modules/wta-notify/src/test/</Path>"]
shared_path_owners: ["<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotificationApplicationRuntimeService.java</Path> => single-agent (Lead; exclusive current workspace; T-43 turn only)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotifyDispatchResultService.java</Path> => single-agent (Lead; exclusive current workspace; T-43 turn only)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/NotifyNotificationDao.java</Path> => single-agent (Lead; exclusive current workspace; T-43 turn only)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/mapper/</Path> => single-agent (Lead; exclusive current workspace; T-43 turn only)", "<Path>backend/wta-modules/wta-notify/src/main/resources/mapper/notify/</Path> => single-agent (Lead; exclusive current workspace; T-43 turn only)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path> => single-agent (Lead; exclusive current workspace; T-43 turn only)", "<Path>backend/wta-modules/wta-notify/src/test/</Path> => single-agent (Lead; exclusive current workspace; T-43 turn only)"]
---

# T-43：测量公告批量写入并控制聚合成本

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
Lead已按Map→项目Skill与当前源码→本票/上游复核。用户已激活全部50票Goal并授权本地实施、提交与direct-parent及gpt-6-sol/xhigh子代理；current-workspace保持唯一产品writer，不创建worktree。T36/T38/T39已Done，当前激活T43。

## 1. 战略与来源

- 来源：R64-N-10；AC-043；本轮用户请求与<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>。
- 当前事实：接收人/delivery/outbox逐行插入；每条结果锁 intent 后读其所有 delivery，存在二次读取规模风险，尚无性能事故实测。
- 可观察产出：代表性规模有可重复 SQL/时延/锁等待基线；保持现有总量上限与持久聚合语义，优先减少插入往返，测量不足不引入新计数状态机。

## 2. 决策状态

### 已锁定决策

保留工程分层、Client/权限、资源owner、安全日志、真实供应商协议和唯一六SQL基座。当前Goal已授权本地实施与验收，远程推送、部署及归档不在本票授权内。

### 已确认方案

2026-09-23用户已逐项接受D-002—009并确认整体共识（LOG-010—018）；本票按已接受ADR定稿。 完整决定及来源以<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>为准。

### 已采用的低影响假设

沿用当前模块与测试基座；测试样本为隔离合成数据，不作为生产容量或SLO。

### 执行前置

G整体共识与Goal激活已确认；基线dd250b947576505425b67ebac0a559c56616952c、clean当前工作区，先建立真实MySQL测量基线再改生产。隔离测试资源由Lead创建和清理。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 本票可观察产出及所列消费者、验证、文档 | 既有wta-api/common、模块模式、事务/权限/生命周期与测试 | 无关模块重写、新队列/锁平台、真实数据修复、远程发布及相邻OIDC实现 |

## 4. 要构建什么

代表性规模有可重复 SQL/时延/锁等待基线；保持现有总量上限与持久聚合语义，优先减少插入往返，测量不足不引入新计数状态机。 正常、失败、越权和竞争路径按本票验收断言共同交付，不把前后端或测试分成无价值空票。

## 5. 实现契约

- 入口与输入输出：真实 MySQL代表规模、SQL计数、现有原子结果/fence回归。
- 外部行为：代表性规模有可重复 SQL/时延/锁等待基线；保持现有总量上限与持久聚合语义，优先减少插入往返，测量不足不引入新计数状态机。
- 不变量：current workspace单writer；UseCase→Service→DAO→Mapper；classic保留；公开数据只经wta-api；GET查询/POST变更且安全@Log。Notify外部I/O不在结果事务内，IN_APP按确认后的短事务合同处理。
- 失败边界：不吞SQL/HTTP/Provider错误，不将UNKNOWN当成功或盲目可重试；页面旧响应不覆盖新会话。具体负向断言见第8/10节。
- 兼容：沿用用户此前明确的基座仓内直接切换决定，同步真实消费者/生成物；不放宽第三方协议。公共API技能用于调用方与影响核对，不重新增加已被用户排除的兼容桥。
- 安全：仅隔离合成测试；secret不进日志/截图/证据。对象/Client授权在后端实施，页面隐藏不替代权限。

## 6. 执行路线

1. 用合成100/1000/10000人（均非生产SLO）记录R/D、SQL次数、耗时、锁等待、堆使用及数据库版本。
2. 查实际状态消费者，保留 Profile/query/monitor 依赖的聚合真相；不得擅自改只读时聚合。
3. 复用 MyBatis batch 插入收件人/delivery/outbox，在相同事务内验证中途失败全回滚与ID唯一。
4. 结果聚合先按同批已提交结果合并、或测量证明无需优化则保留上限与预算证据；禁止仅GROUP BY后宣称消除二次扫描。
5. 对比同输入前后结果与成本；若需增量计数/新状态机，回到G裁决，不在本票随意扩展。

## 7. 路径访问契约

frontmatter为预计点、硬写集与共享owner权威。目录写集仅授权本票行为所需文件；新增测试在声明根内，新增生产类须符合已有层次。不存在的新文件为计划创建，不声称已实现。共享物理/语义资源由single-agent在本票轮次独占；不同票不并行，跨change冲突仅暂停相关分支。

本票状态和Evidence仅由Lead写当前change；永久ADR/context及相邻SSO change只读。越界先修订Ticket/Map，禁止先改后报。

## 8. 验证矩阵

| 场景 | 接缝/步骤 | 预期 | Evidence |
|---|---|---|---|
| 正常 | 真实 MySQL代表规模、SQL计数、现有原子结果/fence回归 | 报告含同环境同样本前后SQL/时延/锁等待和正确性，未测量不写性能提升 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-43-replan-2026-09-23.md</Path> |
| 失败/竞争 | 无漏收件人/重复关系/错误聚合，回滚与租约测试不退化 | 明确失败/安全恢复，无伪成功、越权及部分提交 | 同上，记录故障注入与状态 |
| 回归 | 现有同域测试＋消费者＋适用静态门禁 | 单次上限、超限失败和压力证据可定位；不强设未经用户确认的SLA | 同上，记录测试数/skip/源码 |

命令在仓根执行，`cd backend`表示该条命令切cwd；每条独立运行。以下为实施期命令；各次实际执行结果单独保存，不预先声称通过：

- `cd backend && ./mvnw -pl wta-modules/wta-notify,wta-admin -am test`

- Workspace checks：current-workspace，所列命令加命中工程Skill质量门禁。
- E2E disposition：required: 真实 MySQL代表规模、SQL计数、现有原子结果/fence回归。
- E2E owner/environment：single-agent（Lead）/current-workspace；真实MySQL/Redis必须为本任务隔离资源；无附件纯IN_APP测量不访问OSS，若新增附件场景再启owned MinIO；必要服务缺失则阻塞对应验收。
- 真实服务启用方法与零skip要求：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/verification.md</Path>；新增用例必须保存精确选择器与实际计数，不能只运行mock或test list。
- Integration evidence：非空implementation commit、parent before、clean exact HEAD/tree时点的direct-parent和适用E2E、不可变result及父链；required模式不适用，不创建candidate worktree。

## 9. 发布、迁移与恢复

- 迁移顺序：同批修改源码、消费者、测试及生成合同；无需新增数据库迁移。涉及已有库时必须Tag差异、备份、隔离演练，不重放基座。
- 兼容窗口：基座仓内同步切换，无未声明双写/双协议；外部现有协议保持。
- 监控：记录本票可观察失败/状态/耗时及资源数量，不记录敏感正文；不新增监控平台。
- 恢复：保存上个不可变候选及失败证据；停止受影响任务再核对外部副作用。不得通过恢复已披露secret、放宽权限或重发UNKNOWN恢复。
- 不可逆批准：产品commit/父分支更新及远程push、部署、轮换/真实数据操作、归档分别核对本轮授权。本地产品commit/direct-parent已授权；不推送、部署、重复轮换、操作真实存量数据或归档。
- 收缩条件：旧消费者/废弃字段/不必要配置引用清零且新合同验证通过；不适用的删除不人为增加。

## 10. 验收标准

- [ ] `AC-043`：报告含同环境同样本前后SQL/时延/锁等待和正确性，未测量不写性能提升。
- [ ] `AC-043`：无漏收件人/重复关系/错误聚合，回滚与租约测试不退化。
- [ ] `AC-043`：单次上限、超限失败和压力证据可定位；不强设未经用户确认的SLA。
- [ ] 实际调用已绑定Skill，记录摘要/输入/步骤/输出；不是只“读过”。
- [ ] 正常、失败、回归和required E2E有当前候选证据，未运行不勾选。
- [ ] 写集、共享owner、合同和生成物一致；无未批准偏差。
- [ ] 真实commit/direct-parent/result出口已满足或按Goal对历史无需新实施票作有证据的取消裁决。

## 11. SKILL 调用计划

frontmatter每个必需绑定在implement阶段输入本票、真实调用方和diff，按scope执行约束检查与实现；verify阶段由engineering-standards执行适用门禁。实际Skill Execution Records写<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-43-replan-2026-09-23.md</Path>，包含id/phase/operation/sha256/status/evidence。入口摘要变化先读diff并重新绑定，不改旧历史记录。当前规划仅完成元数据/入口及相关规范路由，未伪造实施passed。

## 12. 停止、检查点与交付

交付本票完整可观察行为，数量以Map为准，不能以样例替代。缺高影响决定、必需Skill/引用/测试，或owner冲突，停止该票和依赖闭包；无依赖票仅在已获执行授权后继续。保留HEAD、diff、已跑命令、失败类别、待完成动作；相同失败无新证据或达到3次集成尝试先复盘。验收后回交Goal，全部票done仍不等于change可归档。

## revision239 — Dispatch01 测量基线

基线 `dd250b947576505425b67ebac0a559c56616952c`。首轮只新增已登记测试根内 `NotifyFanoutMeasurementIntegrationTest.java`，生产写入与聚合不改；Lead提交治理后将该单文件产品写锁交给cors_audit，Lead独占命令、服务、提交及治理。legacy_audit只读核查batch事务与聚合消费者，ops_audit只在/tmp准备owned驱动。100/1000/10000各三次fresh库，真实公告UseCase发布、固定10次IN_APP结果、SQL/JDBC batch/返回行数、时间/内存/锁等待及回滚控制；无数据不写提升，10次结果不冒充全量成本。同字节探针与驱动用于后续A/B，三候选失败先四项复盘。精确方案见 `evidence/T-43-current-2026-09-24/dispatch01-measurement-packet.md`；所有AC待真实验收。

### Dispatch01 Lead 接管与测量口径细化（实测前）

cors_audit已交还唯一产品写锁；测量类摘要 `94ae510b968474abc30a34fbd8a8a7a9431b90ecfd9fcfc0ed36b0f64e78186a`。Lead追加已登记Notify测试根内精确文件 `backend/wta-modules/wta-notify/src/test/java/org/namewta/notify/service/runtime/NotificationAllRecipientLimitTest.java`，仅确认ALL 100000到达首次Intent写入口、100001写前拒绝，不称十万人真实持久化或性能；私有稿已独立静态复核。生产代码仍不改。

初稿双层MyBatis/JDBC探针改为真实JDBC层计数及正控制，减少测量干扰；记录executeQuery/update/batch/addBatch与实返行、FOR UPDATE、物理commit/rollback，不把executeBatch调用次数称网络往返/服务端语句数。AOP仅观察生产OSS和提交后Wake，均有正控制；不替换业务Bean。预热为三次只读公告查询，结果是新JVM中的首次业务发布或固定十次结果，不能称充分JIT预热后的稳态性能或全量完成成本。各堆池阶段内峰值之和也不称同一时刻堆峰值。

锁等待取owned MySQL全局状态前后差量，无竞争时零仅说明本样本未观察到等待；无法读取记null而非伪零，最终验收需实际可用值或补充证据。批量优化前必须完成A；B保持本测量类和驱动逐字节相同，额外批次原子性/混合目标验证另设测试。独占驱动v2在/tmp，SHA256 `93edf8f32d861365ea9140da31ea9304f8c3409493b514de0e957561329e33bb`；v1未运行且原字节保留。先编译和100人发布smoke，再21个独立fresh库矩阵（发布/十次结果各三档三重复，加10k回滚三重复）。离线驱动12项只证明驱动自身安全检查，不是产品验收。当前所有业务AC未勾。

### A 基线已固化，AC 仍未关闭

2026-09-24 将 `/tmp/wta-t43/runs/af951e784d5c79e4` 的 21 次接受结果抄入 `evidence/T-43-current-2026-09-24/a-baseline/`。固定源码 `7933bdff61a5dbd620b869be7171b61b83fed845`。100/1000/10000 发布的 JDBC 写执行分别约为 305/3005/30005，`executeBatch` 为 0；10k 三次插入失败均 1 次回滚且收件人/Delivery/Outbox 为 0。这只是优化前证据。AC-043 三个复选框保持未勾。聚合二次扫描在 10 次结果上不是发布耗时的主要部分，本票不因此新增计数状态机。
