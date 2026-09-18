---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-18枚举.agents/skills入口并按本票真实路径/领域绑定；Map为最低集合"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/22-atomic-notify-result.md</Path>", "<Path>backend/wta-modules/wta-notify/src/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>"], "outputs": ["T-22的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/22-atomic-notify-result.md</Path>", "<Path>backend/wta-modules/wta-notify/src/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>"], "outputs": ["T-22的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-module-guide", "path": "<Path>.agents/skills/wta-module-guide/SKILL.md</Path>", "sha256": "441de2ccc513e09820ed3d7d2faf559eeaa7466202e4fbb0c8dd3eabf09510c9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/22-atomic-notify-result.md</Path>", "<Path>backend/wta-modules/wta-notify/src/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>"], "outputs": ["T-22的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/22-atomic-notify-result.md</Path>", "current-workspace实际diff及本票验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-22.md</Path>：命令、退出码、测试数、AC与Skill Execution Records"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "finding:B-04", "contract:AC-022"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-22
title: 原子提交通知投递结果与lease fence
status: "ready"
planning_depth: "deep"
planning_depth_reason: "安全/鉴权、公共合同、数据一致性或共享核心路径变更：原子提交通知投递结果与lease fence"
ready: true
risk: high
blocked_by: []
contract_ids: [AC-022]
owner: single-agent
expected_changes: ["<Path>backend/wta-modules/wta-notify/src/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/DispatchNotificationService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotificationApplicationUseCase.java</Path>"]
writable_paths: ["<Path>backend/wta-modules/wta-notify/src/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/DispatchNotificationService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotificationApplicationUseCase.java</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: ["<Path>backend/wta-modules/wta-notify/src/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/DispatchNotificationService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotificationApplicationUseCase.java</Path>"]
shared_path_owners: ["<Path>backend/wta-modules/wta-notify/src/</Path> => single-agent (Lead; serial T-22 turn)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path> => single-agent (Lead; serial T-22 turn)", "<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path> => single-agent (Lead; serial T-22 turn)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/DispatchNotificationService.java</Path> => single-agent (Lead; serial T-22 turn)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotificationApplicationUseCase.java</Path> => single-agent (Lead; serial T-22 turn)"]
---

# T-22：原子提交通知投递结果与lease fence

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先读Map，再按Skill矩阵读取适用入口/引用，再读本票。禁止任何implementation/review/research子代理。Ready仅表示计划合同就绪，不表示已经授权实施或已验证通过。

## 1. 战略与来源

- 目标与可观察产出：任意一条SQL失败不会留下Delivery/Attempt/Outbox不一致。
- 来源：B-04；AC-022；USER-DECISION: 全面完善计划、无兼容、单人串行。
- 当前事实与调用链：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/re-review.md</Path>中T-22行及对应专项报告；源码导航为frontmatter预计修改点。
- 规划深度：deep；安全/鉴权、公共合同、数据一致性或共享核心路径变更：原子提交通知投递结果与lease fence。

## 2. 决策状态

### 已锁定决策

ADR-CR-009保留Outbox/Redis wake，不退回同步发送；外部投递exactly-once不作无法证明承诺。

### 已采用的低影响假设

沿用当前仓库版本与既有模块命名；实施前回读实际源码，路径变化由本票修订，不猜测不存在的实现。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| provider I/O结果→短事务→Delivery/Attempt/Outbox/Intent | 已有deliveryId幂等键/Outbox/DAO；I/O留在事务外 | 本票之外的模块重写、兼容桥、远程发布与重要数据操作 |

## 4. 要构建什么

provider I/O结果→短事务→Delivery/Attempt/Outbox/Intent。调用者可观察到：任意一条SQL失败不会留下Delivery/Attempt/Outbox不一致。失败时：锁后验证owner/token/状态/lease_until；过期owner零写入；finish=0整笔回滚。

## 5. 实现契约

- 入口、输入输出与数据流：provider I/O结果→短事务→Delivery/Attempt/Outbox/Intent。
- 不变量及失败语义：锁后验证owner/token/状态/lease_until；过期owner零写入；finish=0整笔回滚。
- 公共合同：本票只按以上行为及执行路线变更；同步全部仓内调用/生成物，沿用已有权限/Client/owner校验。未列出的接口保持原语义。
- 兼容：用户明确无需旧版兼容；仓内一次切换，不加双路由/版本等待。实际供应商协议仍须遵守。
- 安全与隐私：凭据不进入日志/UI证据；越权/过期/无owner拒绝；数据库与资源约束不能为前端成功而放宽。

用代理调用的UseCase短DSTransactional原子提交Delivery/Attempt/Outbox/Intent；Provider I/O在外。取得锁后校验owner/token/状态及租约有效期；finish影响0行时回滚先前写入。Callback与dispatch共享单调状态推进和锁序，避免DELIVERED倒退；不同delivery的聚合须串行或CAS重试。站内信已有幂等持久化和实时推送失败隔离需保留。

## 6. 执行路线

1. 复现provider成功后Delivery更新成功/Attempt插入失败的部分状态，以及旧worker写回。
2. 保持provider I/O在数据库事务外，固化稳定幂等key。
3. 当前结果写回位于 <Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/DispatchNotificationService.java</Path>，该类当前没有事务注解；新增/接入正式 UseCase 短 `DSTransactional`，把结果、Attempt、Outbox finish 和 Intent 聚合合为一次提交。
4. 完成入口验证owner/token/期望状态并让所有相关写入遵循同一fence；旧worker只能no-op/失效。
5. 崩溃后reclaim不能把半完成当DONE；设计provider已接受但DB未提交的重试/人工核对语义。
6. 只有测试证明需要的唯一约束才加入10-cde-base-ddl.sql；当前deliveryId已作为Provider requestId和idempotencyKey，不新增同义key。
7. renew/finish当前SQL只比owner/token，没有lease_until截止条件；明确租约过期后旧owner不可复活，数据库时间判断与结果事务使用同一锁序。

## 7. 路径访问契约

预计点、可写范围、只读上下文及共享项以frontmatter为唯一权威。共享owner固定single-agent；只有当前票轮次可写，下一票须回读前一结果。跨模块目录只允许本票行为必需的文件，目录授权不意味着重写全部模块。新增测试位于同模块测试目录；未覆盖的新路径先修订本票/Map再写。
保留当前用户未提交改动、永久ADR/context、供应商源码与运行数据；生成物通过正式工具更新。

## 8. 验证矩阵

| 行为或风险 | 验证接缝/步骤 | 预期结果 | Evidence |
|---|---|---|---|
| 正常路径 | provider I/O结果→短事务→Delivery/Attempt/Outbox/Intent；执行下列定向命令及对应场景 | 任意一条SQL失败不会留下Delivery/Attempt/Outbox不一致 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-22.md</Path> |
| 失败路径 | 锁后验证owner/token/状态/lease_until；过期owner零写入；finish=0整笔回滚；固定时序/故障注入，记录输入与最终可观察状态 | 无越权、错误状态或部分提交；可按定义恢复 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-22.md</Path> |
| 回归 | 运行所属包原有测试及受影响调用者；逐项核对下列AC | 旧lease不能覆盖新owner结果；双worker只产生符合合同的结果记录；provider去重能力不足时风险明确且不宣称exactly-once | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-22.md</Path> |

Workspace checks：current-workspace；`frontend:`/`backend:`表示先进入该目录，其他命令cwd为仓根，逐条串行执行。新增用例实施时登记精确选择器、实际测试数与跳过项，零测试/required跳过不算通过。

- `backend: ./mvnw -pl wta-modules/wta-notify,wta-admin -am test`

- E2E disposition：required: 真实MySQL/Redis双worker、过期/reclaim、Attempt失败与finish冲突注入。
- E2E owner/environment：single-agent（Lead）/current-workspace；使用隔离MySQL/Redis/OSS及必要真实HTTP/浏览器，禁止连生产。场景步骤以上表、本票AC为准；需新用例时在写集内创建后记录精确命令。
- Integration evidence：记录parent before、implementation commit及direct-parent检查；result SHA等于通过验证的implementation commit，candidate不适用。当前全部产品检查not-run。

## 9. 发布、迁移与恢复

- 顺序：前置票产生已验证合同后实施本票；源码、仓内消费者、测试和生成物同批交付。涉及DDL只编辑10-cde-base-ddl.sql，新环境按六文件基座初始化；不增加存量迁移工程。
- 兼容窗口：无；不保留旧接口或数据格式桥。生产部署不是本票自动步骤。
- 监控/诊断：观察本票AC的成功/错误状态、耗时及资源/持久化结果，日志只含安全元数据；复用现有观测入口，不新建监控平台。
- 恢复：停止受影响worker并核对provider已受理但DB未提交记录；不承诺外部exactly-once。
- 不可逆批准点：提交、推送、部署、运行数据删除/修复分别需授权；本轮只有计划文档授权。
- 收缩条件：本票替代的旧调用/配置引用归零且仓内回归通过；无被替代入口时不适用，不为凑清单扩大删除范围。

## 10. 验收标准

- [ ] `AC-022`：任意一条SQL失败不会留下Delivery/Attempt/Outbox不一致。
- [ ] `AC-022`：旧lease不能覆盖新owner结果。
- [ ] `AC-022`：双worker只产生符合合同的结果记录。
- [ ] `AC-022`：provider去重能力不足时风险明确且不宣称exactly-once。
- [ ] 按Map→适用Skill→本票完成读取及实际调用；所有required Skill记录passed并可回读。
- [ ] 正常/失败/回归及required E2E均完成，证据写入<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-22.md</Path>，未执行不得标通过。
- [ ] 修改不超出写集，共享项只有single-agent当前票轮次写入。
- [ ] 获得授权后形成非空implementation commit，Lead完成direct-parent验收并记录parent result SHA；未获授权不提交、不标Done。
- [ ] Ticket、Map、Goal与Evidence一致；不存在未批准偏差。

## 11. SKILL 调用计划

frontmatter绑定的项目Skill在implementation阶段接收本票路径和上游合同，产出适用分层、权限、数据/资源边界及实现diff；engineering-standards在verify阶段根据本节命令选择受影响门禁并输出AC/退出码/E2E记录。按入口scope展开引用，不以“已读”代替实际操作。
必需入口缺失或sha256漂移时阻塞本票；Lead回读差异后更新绑定及Map，不能自动接受新摘要。实际记录归<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-22.md</Path>的`## Skill Execution Records`，本次规划不伪造实现调用记录。

## 12. 停止、检查点与交付

交付本票完整可观察行为及验收证据；数量以Map为准。缺依赖/测试环境/Skill、越界或高影响事实变化时停止受影响票，保留checkpoint和失败证据，其他独立票仍可串行推进。恢复先读Goal、Map、本票、状态及最新Evidence；记录实际HEAD/dirty差异，禁止覆盖用户修改。
依赖：无。单票完成条件为全部AC、实际Skill证据和获授权的direct-parent出口；仅补文档不能标Done。
