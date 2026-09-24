---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-23已枚举.agents/skills全部入口；命中scope读取入口，common入口变化已复核，历史Skill Evidence不改写"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/46-serialize-oss-restore-cleanup.md</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/migration/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/mapper/SysOssMapper.java</Path>"], "outputs": ["T-46的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/46-serialize-oss-restore-cleanup.md</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/migration/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/mapper/SysOssMapper.java</Path>"], "outputs": ["T-46的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-module-guide", "path": "<Path>.agents/skills/wta-module-guide/SKILL.md</Path>", "sha256": "bb57a781314abe316f06ba9538f62043f1bd053904c0832969e575c09731a7a3", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/46-serialize-oss-restore-cleanup.md</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/migration/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/mapper/SysOssMapper.java</Path>"], "outputs": ["T-46的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-common-modules-guide", "path": "<Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>", "sha256": "d7b7e105c37499e0e0f8ad9b1e2dbf379100df5b6a47e0d6affb04483f8b162c", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/46-serialize-oss-restore-cleanup.md</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/migration/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/mapper/SysOssMapper.java</Path>"], "outputs": ["T-46的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "java-api-compatibility", "path": "<Path>.agents/skills/java-api-compatibility/SKILL.md</Path>", "sha256": "b90f5592e75b3f757f52649a16f78e92850fee0ccac9f12619d3d7aa94bd7aca", "phase": "implement", "operation": "audit-bounded-oss-io-contract-and-consumers", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/46-serialize-oss-restore-cleanup.md</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/migration/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/mapper/SysOssMapper.java</Path>"], "outputs": ["有界HEAD/DELETE公共方法、迁移调用方与超时语义验证"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/46-serialize-oss-restore-cleanup.md</Path>", "当前diff与验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-46-replan-2026-09-23.md</Path>"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "oss:storage-pointer", "oss:migration-item", "contract:AC-046"]
artifact: "ticket"
change: "2026-09-14-wta-plus-comprehensive-review"
id: "T-46"
title: "恢复与清理共享对象锁以保护当前来源"
status: "in_progress"
kind: "bug"
planning_depth: "deep"
planning_depth_reason: "公共合同/事务/安全/数据及恢复边界"
ready: true
risk: "high"
blocked_by: []
contract_ids: ["AC-046"]
owner: "single-agent"
expected_changes: ["<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/migration/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/mapper/SysOssMapper.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/resources/mapper/system/SysOssMapper.xml</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/oss/migration/</Path>", "<Path>frontend/packages/web-domains/system/src/oss/OssPage.vue</Path>", "<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/client/OssClient.java</Path>", "<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/client/AbstractOssClientImpl.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/oss/client/</Path>", "<Path>backend/README.md</Path>", "<Path>.agents/skills/wta-module-guide/references/modules/system/domains.md</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/mapper/SysOssConfigMapper.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysOssConfigServiceImpl.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/oss/config/</Path>", "<Path>backend/wta-modules/wta-system/AGENTS.md</Path>"]
writable_paths: ["<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/migration/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/mapper/SysOssMapper.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/resources/mapper/system/SysOssMapper.xml</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/oss/migration/</Path>", "<Path>frontend/packages/web-domains/system/src/oss/OssPage.vue</Path>", "<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/client/OssClient.java</Path>", "<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/client/AbstractOssClientImpl.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/oss/client/</Path>", "<Path>backend/README.md</Path>", "<Path>.agents/skills/wta-module-guide/references/modules/system/domains.md</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/mapper/SysOssConfigMapper.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysOssConfigServiceImpl.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/oss/config/</Path>", "<Path>backend/wta-modules/wta-system/AGENTS.md</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>", "<Path>{roots.state}/specdev/adr/</Path>", "<Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/</Path>"]
shared_paths: ["<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/migration/</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/mapper/SysOssMapper.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/resources/mapper/system/SysOssMapper.xml</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/oss/migration/</Path>", "<Path>frontend/packages/web-domains/system/src/oss/OssPage.vue</Path>", "<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/client/OssClient.java</Path>", "<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/client/AbstractOssClientImpl.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/oss/client/</Path>", "<Path>backend/README.md</Path>", "<Path>.agents/skills/wta-module-guide/references/modules/system/domains.md</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/mapper/SysOssConfigMapper.java</Path>", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysOssConfigServiceImpl.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/oss/config/</Path>", "<Path>backend/wta-modules/wta-system/AGENTS.md</Path>"]
shared_path_owners: ["<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/migration/</Path> => single-agent (Lead; cors_audit sole product writer; T-46 exclusive current workspace)", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/mapper/SysOssMapper.java</Path> => single-agent (Lead; cors_audit sole product writer; T-46 exclusive current workspace)", "<Path>backend/wta-modules/wta-system/src/main/resources/mapper/system/SysOssMapper.xml</Path> => single-agent (Lead; cors_audit sole product writer; T-46 exclusive current workspace)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/oss/migration/</Path> => single-agent (Lead; cors_audit sole product writer; T-46 exclusive current workspace)", "<Path>frontend/packages/web-domains/system/src/oss/OssPage.vue</Path> => single-agent (Lead; cors_audit sole product writer; T-46 exclusive current workspace)", "<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/client/OssClient.java</Path> => single-agent (Lead; cors_audit sole product writer; T-46 exclusive current workspace)", "<Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/client/AbstractOssClientImpl.java</Path> => single-agent (Lead; cors_audit sole product writer; T-46 exclusive current workspace)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/oss/client/</Path> => single-agent (Lead; cors_audit sole product writer; T-46 exclusive current workspace)", "<Path>backend/README.md</Path> => single-agent (Lead; cors_audit sole product writer; T-46 exclusive current workspace)", "<Path>.agents/skills/wta-module-guide/references/modules/system/domains.md</Path> => single-agent (Lead; cors_audit sole product writer; T-46 exclusive current workspace)", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/mapper/SysOssConfigMapper.java</Path> => single-agent (Lead; cors_audit sole product writer; T-46 exclusive current workspace)", "<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysOssConfigServiceImpl.java</Path> => single-agent (Lead; cors_audit sole product writer; T-46 exclusive current workspace)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/oss/config/</Path> => single-agent (Lead; cors_audit sole product writer; T-46 exclusive current workspace)", "<Path>backend/wta-modules/wta-system/AGENTS.md</Path> => single-agent (Lead; cors_audit sole product writer; T-46 exclusive current workspace)"]
---

# T-46：恢复与清理共享对象锁以保护当前来源

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先完整读Map→命中项目Skill入口/按scope引用→本票与上游。当前Goal已激活；Lead串行委派cors_audit为唯一产品writer，独占当前工作区，不创建worktree；验证与提交由Lead执行。

## 1. 战略与来源

- 来源：R64-O-03；AC-046；本轮用户请求与<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>。
- 当前事实：cleanup依据预读工单删除来源，unpublish不共锁；saveItem未检查updateById影响行数。
- 可观察产出：所有切指针/删来源入口共用对象→工单锁序。恢复先成功则清理不得删来源；清理已获得合法执行权则恢复明确拒绝。

## 2. 决策状态

### 已锁定决策

保留工程分层、Client/权限、资源owner、安全日志、真实供应商协议和唯一六SQL基座。active Goal已授权本地实施、提交及direct-parent；本票不包含远程或生产操作。

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

所有切指针/删来源入口共用对象→工单锁序。恢复先成功则清理不得删来源；清理已获得合法执行权则恢复明确拒绝。 正常、失败、越权和竞争路径按本票验收断言共同交付，不把前后端或测试分成无价值空票。

## 5. 实现契约

- 入口与输入输出：OssStorageMigrationIntegrationTest：真实MySQL两连接＋阻塞替身＋MinIO对象存在验收。
- 外部行为：所有切指针/删来源入口共用对象→工单锁序。恢复先成功则清理不得删来源；清理已获得合法执行权则恢复明确拒绝。
- 不变量：current workspace单writer；UseCase→Service→DAO→Mapper；classic保留；公开数据只经wta-api；GET查询/POST变更且安全@Log。Notify外部I/O不在结果事务内，IN_APP按确认后的短事务合同处理。
- 失败边界：不吞SQL/HTTP/Provider错误，不将UNKNOWN当成功或盲目可重试；页面旧响应不覆盖新会话。具体负向断言见第8/10节。
- 兼容：沿用用户此前明确的基座仓内直接切换决定，同步真实消费者/生成物；不放宽第三方协议。公共API技能用于调用方与影响核对，不重新增加已被用户排除的兼容桥。
- 安全：仅隔离合成测试；secret不进日志/截图/证据。对象/Client授权在后端实施，页面隐藏不替代权限。

## 6. 执行路线

1. 真实双连接/屏障和可阻塞ObjectStore固定报告时序，覆盖unpublish/cleanup/rollback/process切指针。
2. 实现单对象被代理DSTransactional边界，锁后重读sys_oss及工单，检查目标指针、状态、安全窗口及version。
3. 来源存在验证与幂等删除使用有界I/O；仅该对象持锁，批量逐项短事务，不持整批锁。
4. 检查所有条件更新影响行数，指针失败不得把工单标成功；删除成功DB提交失败以同目标不存在重试收敛。
5. 遍历所有改service入口确认共同互斥，测试重复清理、超时、跨进程和版本冲突。
6. 保留公开副本提示，不将恢复私有指针写成撤销所有公开URL。

## 7. 路径访问契约

frontmatter为预计点、硬写集与共享owner权威。目录写集仅授权本票行为所需文件；新增测试在声明根内，新增生产类须符合已有层次。不存在的新文件为计划创建，不声称已实现。共享物理/语义资源由single-agent在本票轮次独占；不同票不并行，跨change冲突仅暂停相关分支。

本票状态和Evidence仅由Lead写当前change；永久ADR/context及相邻SSO change只读。越界先修订Ticket/Map，禁止先改后报。

## 8. 验证矩阵

| 场景 | 接缝/步骤 | 预期 | Evidence |
|---|---|---|---|
| 正常 | OssStorageMigrationIntegrationTest：真实MySQL两连接＋阻塞替身＋MinIO对象存在验收 | 两种竞争顺序均不删当前来源，恢复/清理结果明确 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-46-replan-2026-09-23.md</Path> |
| 失败/竞争 | 指针/工单更新冲突回滚；删除确认丢失能安全幂等重试 | 明确失败/安全恢复，无伪成功、越权及部分提交 | 同上，记录故障注入与状态 |
| 回归 | 现有同域测试＋消费者＋适用静态门禁 | 单对象超时/失败不锁住整个批次，真实MinIO目标对象仍可读取 | 同上，记录测试数/skip/源码 |

命令在仓根执行，`cd backend`表示该条命令切cwd；每条独立运行。以下为实施期命令，本轮未执行：

- `cd backend && ./mvnw -pl wta-modules/wta-system,wta-admin -am test`

- Workspace checks：current-workspace，所列命令加命中工程Skill质量门禁。
- E2E disposition：required: OssStorageMigrationIntegrationTest：真实MySQL两连接＋阻塞替身＋MinIO对象存在验收。
- E2E owner/environment：single-agent（Lead）/current-workspace；真实MySQL/Redis/MinIO必须为本任务隔离资源，必要服务缺失则阻塞对应验收。
- 真实服务启用方法与零skip要求：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/verification.md</Path>；新增用例必须保存精确选择器与实际计数，不能只运行mock或test list。
- Integration evidence：非空implementation commit、parent before、clean exact HEAD/tree时点的direct-parent和适用E2E、不可变result及父链；required模式不适用，不创建candidate worktree。

## 9. 发布、迁移与恢复

- 迁移顺序：先部署共同互斥再启用真实管理清理；不自动清旧来源。单对象IO预算需小于锁/请求预算并在超时测试证明；不宣称DSTransactional可配置不存在的timeout属性。
- 兼容窗口：基座仓内同步切换，无未声明双写/双协议；外部现有协议保持。
- 监控：记录本票可观察失败/状态/耗时及资源数量，不记录敏感正文；不新增监控平台。
- 恢复：保存上个不可变候选及失败证据；停止受影响任务再核对外部副作用。不得通过恢复已披露secret、放宽权限或重发UNKNOWN恢复。
- 不可逆批准：产品commit/父分支更新及远程push、部署、轮换/真实数据操作、归档分别核对本轮授权。本地产品commit/direct-parent已授权，其余外部动作不由本票推定。
- 收缩条件：旧消费者/废弃字段/不必要配置引用清零且新合同验证通过；不适用的删除不人为增加。

## 10. 验收标准

- [ ] `AC-046`：两种竞争顺序均不删当前来源，恢复/清理结果明确。
- [ ] `AC-046`：指针/工单更新冲突回滚；删除确认丢失能安全幂等重试。
- [ ] `AC-046`：单对象超时/失败不锁住整个批次，真实MinIO目标对象仍可读取。
- [ ] 实际调用已绑定Skill，记录摘要/输入/步骤/输出；不是只“读过”。
- [ ] 正常、失败、回归和required E2E有当前候选证据，未运行不勾选。
- [ ] 写集、共享owner、合同和生成物一致；无未批准偏差。
- [ ] 真实commit/direct-parent/result出口已满足或按Goal对历史无需新实施票作有证据的取消裁决。

## 11. SKILL 调用计划

frontmatter每个必需绑定在implement阶段输入本票、真实调用方和diff，按scope执行约束检查与实现；verify阶段由engineering-standards执行适用门禁。实际Skill Execution Records写<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-46-replan-2026-09-23.md</Path>，包含id/phase/operation/sha256/status/evidence。入口摘要变化先读diff并重新绑定，不改旧历史记录。当前规划仅完成元数据/入口及相关规范路由，未伪造实施passed。

## 12. 停止、检查点与交付

交付本票完整可观察行为，数量以Map为准，不能以样例替代。缺高影响决定、必需Skill/引用/测试，或owner冲突，停止该票和依赖闭包；无依赖票仅在已获执行授权后继续。保留HEAD、diff、已跑命令、失败类别、待完成动作；相同失败无新证据或达到3次集成尝试先复盘。验收后回交Goal，全部票done仍不等于change可归档。

## revision213 — T-46启动与共同互斥合同

revision213：T45已验收完成（产品09be6db、治理4a8fea8c）；T46激活，以恢复/清理竞争红灯起步。16done/2cancelled/1in_progress/31ready，Goal active，未完成或归档。

基线`4a8fea8c19392972ee5cfef4263962ff6b0e3bfb`，main/current/direct-parent。实现仍复用sys_oss对象锁及现有工单，不新增schema、队列/分布式状态机。所有unpublish/rollback/process切指针及cleanup统一Object→Item锁序，在被Spring代理的public DSTransactional边界重读对象/工单，核对ACTIVE、当前service、source/target/key、最新工单、版本/状态和安全窗口；指针与工单条件更新均须恰1行且同事务，不能吞CAS失败。批次逐对象处理，不持整批锁。

严格超时不能证明供应商DELETE立即取消。为满足“清理获得合法执行权后恢复拒绝”和“删除已成功但DB提交失败可安全重试”，先在对象锁事务中用既有FAILED + lastErrorStage=COMPLETED + 固定CLEANUP_OUTCOME_UNKNOWN持久化执行/未知栅栏，提交已知后才发有界DELETE；不确定提交不发DELETE。完成再按Object→Item与同版本收敛COMPLETED。超时、响应/提交确认丢失保留栅栏，所有恢复/切指针路径拒绝绕过；后续显式cleanup可用有界HEAD确认来源缺失、目标有效后仅finalize，来源仍在或读取未知不盲重发DELETE或恢复来源。这是已有工单字段的保守执行权，不引入新的分布式状态平台；SDK cancel不等于撤销远端副作用。

来源存在与核对HEAD、DELETE均采用明确Duration预算，复用OssClient/Abstract最小重载及迁移ObjectStore端口；普通OSS调用语义保持。读写I/O不跨整批持锁，副作用前后只锁单对象；copy/verify不借此扩张重写。写集从5扩到10：两common API/实现、client测试目录、backend运行文档和system事实。java-api-compatibility用于新增有界操作及仓内消费者核对，不新建旧API兼容桥。HTTP形状/权限/公开副本提示原则上保持；实际合同变化须先报Lead。

Dispatch01A仅修改既有OssStorageMigrationServiceUnitTest.java，使用可阻塞对象删除和两个调用者固定旧时序，证明旧cleanup已进入删除时unpublish仍能恢复来源而后被删；锁获胜顺序和条件更新失败后续用真实MySQL双连接/真实DSTransactional代理及MinIO补验。当前生产代码不动，不宣称内存替身能证明DB锁。Lead固定红灯后派01B完整实现。cors_audit唯一writer；legacy_audit只读合同审查；ops_audit仅私有隔离驱动准备。Lead独占治理、提交、构建、真实服务。

必需门禁：受影响OSS单元/合同、默认后端、full/core；真实MySQL两物理连接证明共同锁、两竞争顺序、指针/工单CAS0与回滚、旧工单/未知栅栏、超时及晚到DELETE、提交确认丢失和安全finalize；第二对象仍能推进，MinIO目标字节可读。只有fresh XML零skip/ownedcleanup及双轴审查完成才勾AC；最多3次完整候选失败先四项复盘。当前attempts0。

## revision214 — 红灯与Dispatch01B

revision214：T46红灯在0f8729d复现，15例/1预期failure/0error/0skip，源码前后clean；DELETE已进入后旧unpublish返回成功。Dispatch01B开始共同锁、持久UNKNOWN和有界I/O实现。16done/2cancelled/1in_progress/31ready，完整候选attempts0，Goal active。

权威实施包为 `evidence/dispatch-T-46-implementation.md`；红灯和写入口清单见 `evidence/T-46-current-2026-09-23/red01/manifest.json`。没有真实服务或完整候选通过记录，AC未勾。

## revision215 — 配置身份与红灯证据边界

revision215：T46补充配置物理身份保护，写集10扩14；未结束迁移保护source/target配置，关闭编辑/删除与新工单创建竞态。实现仍由cors_audit独占，尚未完整候选验证；16done/2cancelled/1in_progress/31ready，attempts0，Goal active。

配置引用不能只计算当前sys_oss.service：迁移切换后仍须保护未终结工单的source/target，FAILED（含CLEANUP_OUTCOME_UNKNOWN）继续持有引用，不能改名、删除或改向另一物理存储后把404误判为原来源已删除。已有对象引用保护同步覆盖影响物理寻址的endpoint/isHttps/region及原configKey/bucket/accessPolicy；凭据按同一存储身份轮换仍允许，不能把普通密钥轮换等同身份迁移。域名等字段按实际调用面审查，避免无关冻结。

创建工单须与配置编辑/删除形成真实互斥，配置锁统一稳定顺序且先于Object→Item；其它既有工单事务保持Object→Item，不引入逆序Config锁。配置批删须避免首项普通读建立RR快照后、后续配置锁等待期间新增引用漏检；先取得全部配置锁再读引用，或采用有证据的等价当前读方案。真实MySQL验证创建与编辑/删除竞争、切换后源配置引用、UNKNOWN及正常凭据更新；不以Mapper字符串或mock断言替代竞争证据。

产品写集新增SysOssConfigMapper.java、SysOssConfigServiceImpl.java、oss/config测试目录、System AGENTS.md，共14根；只补本票安全边界与事实，不扩展配置架构。既有common/事务/Java API技能绑定仍适用。

证据精度更正：revision213“恢复来源而后被删”是风险描述，red01 FakeObjects.delete仅计数与等待，没有物理删除字节。该红灯只实证DELETE进入后旧unpublish成功；不能当作来源已不存在证据。真实MinIO HEAD/GET将在最终集成补验，旧记录原字节保留。

## revision216 — 有界缺失证明与Dispatch02

revision216：T46候选4f8c4b4a定向174例1error（Region空值误拒轮换）；窄修f6f8dd8后174例全通过零skip。独立审查发现源桶404可误归对象缺失，Dispatch02补有界桶存在核对。完整候选attempts0，16done/2cancelled/1in_progress/31ready，Goal active。

只强化新Duration版headObject：对象HEAD404之后在同一剩余总预算内HEAD Bucket；桶可达才将对象404作为OBJECT_NOT_FOUND。桶不存在、拒绝、超时或未知均保守报告PROVIDER_ERROR，迁移保留UNKNOWN且不finalize/重DELETE。SDK两请求各以剩余预算限制总/attempt timeout并有界await；普通旧OSS调用语义保持。不增加接口/schema/路径，14根写集不变。补受控404/403/timeout单元负例和owned真实缺桶负例；更新运行限制说明。cors_audit仅获上述窄修产品写锁；Lead继续独占构建/服务/提交。原失败与后续绿灯分开保留，均不代表真实集成验收。
