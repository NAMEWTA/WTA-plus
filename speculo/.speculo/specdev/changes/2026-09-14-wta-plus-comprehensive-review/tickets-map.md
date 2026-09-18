---
schema_version: 3
plan_contract_version: 1
plan_revision: 3
requested_deliverables: [{"name":"完整Tickets Map","count":1},{"name":"Goal Plan","count":1}]
deliverable_policy: "用户请求完整完善既有change；保留31票追溯编号，未将31误写为用户指定数量；T/P各交付一份完整主工件。"
artifact: tickets-map
change: 2026-09-14-wta-plus-comprehensive-review
status: draft
---

# Tickets Map

## 1. 目标与拆分策略

### 总体实施背景

以2026-09-18当前源码复核为依据，修复安全、状态、通知、树与交付真实问题。保留31票可观察行为及现有编号；不因无兼容要求削弱供应商协议、权限或数据不变量。T-01是可信门禁准备，T-09是发布构建准备，其余按行为跨最小层次闭环，不拆成纯前端/后端/SQL水平票。

29票计划Ready；T-03、T-23真实参数未闭合保持blocked。Ready表示决策完备，不表示依赖已Done、产品验证通过或实施授权成立。Spec总体仍draft，Goal执行Gate关闭。本轮用户已授权自主完善计划，无需为发布这些文档重复确认。

唯一执行者按Map→适用Skill入口及命中引用→Ticket读取；禁止所有子代理，任何命令/实现/验证严格串行。当前工作树含用户未提交内容，不能直接创建干净源码发布候选或混入implementation commit。

### 项目 Skill 读取矩阵

| Applies To | Project Skill | Trigger / Scope | Read Timing | Purpose |
|---|---|---|---|---|
| ALL | <Path>.agents/skills/engineering-standards/SKILL.md</Path> | 架构、分层、API、事务、SQL和质量门禁 | Map后、本票实现前；验证时复用适用规范 | 架构、分层、API、事务、SQL和质量门禁 |
| T-02, T-03, T-04, T-05, T-11 | <Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path> | 日志/正文/IP/防重/加密的现有common入口 | Map后、本票实现前；验证时复用适用规范 | 日志/正文/IP/防重/加密的现有common入口 |
| T-06, T-07, T-12, T-13, T-14, T-15, T-16, T-17, T-18, T-19, T-22, T-23, T-24, T-25, T-26, T-27, T-28, T-30, T-31 | <Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path> | 业务切片、权限、App组合与数据边界 | Map后、本票实现前；验证时复用适用规范 | 业务切片、权限、App组合与数据边界 |
| T-14, T-15, T-16, T-22, T-23, T-24, T-25, T-28, T-30, T-31 | <Path>.agents/skills/wta-module-guide/SKILL.md</Path> | Profile/System/Workflow/Notify/Third公开API事实 | Map后、本票实现前；验证时复用适用规范 | Profile/System/Workflow/Notify/Third公开API事实 |

矩阵是最低集合；真实调用、入口摘要、阶段、输入输出与失败动作由单票frontmatter拥有。无兼容演进，不绑定java-api-compatibility；本轮无部署，不激活环境接管技能。Skill更新先由唯一Lead核对影响再更新受影响票绑定。

## 2. 执行清单

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | AC | 顺序 | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| T-01 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/01-restore-trustworthy-gates.md</Path> | 干净clone不创建temp/release也通过事实检查 | — | standard | medium | true | single-agent | AC-001 | 01 | ready |
| T-02 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/02-unify-log-redaction.md</Path> | canary不出现在HTTP sink、OperLogEvent、数据库或错误日志 | — | deep | high | true | single-agent | AC-002 | 02 | ready |
| T-03 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/03-bound-request-capture.md</Path> | 大小边界前/等于/超限一字节结果可判定 | T-02 | deep | high | false | single-agent | AC-003 | 03 | blocked |
| T-04 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/04-trusted-client-address.md</Path> | 任意外来XFF不改变直连或正常入口的授权结果 | — | deep | high | true | single-agent | AC-004 | 04 | ready |
| T-05 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/05-repeat-submit-lease.md</Path> | A失败不得删除B的键 | — | deep | high | true | single-agent | AC-005 | 05 | ready |
| T-06 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/06-secure-sso-session.md</Path> | 生产实现不含ThreadLocalRandom/雪花ID作为bearer | — | deep | high | true | single-agent | AC-006 | 06 | ready |
| T-07 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/07-sso-callback-journey.md</Path> | 复杂state往返相等且无重复code/state参数 | T-06 | deep | high | true | single-agent | AC-007 | 07 | ready |
| T-08 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/08-complete-sso-release.md</Path> | 每个shipped App均有且仅有完整配套，缺项在promotion前失败 | T-07, T-09, T-10 | deep | high | true | single-agent | AC-008 | 10 | ready |
| T-09 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/09-coherent-build-matrix.md</Path> | build:dev最终三个App均development，build:prod均production | T-01 | deep | high | true | single-agent | AC-009 | 08 | ready |
| T-10 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/10-atomic-release-provenance.md</Path> | manifest每个artifact的digest和source可追溯 | T-09 | deep | high | true | single-agent | AC-010 | 09 | ready |
| T-11 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/11-remove-browser-shared-private-key.md</Path> | 生产bundle不再携带该共享响应私钥或ECB路径 | — | deep | high | true | single-agent | AC-011 | 11 | ready |
| T-12 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/12-session-navigation-lifecycle.md</Path> | logout超时/401/离线时本地token和动态路由仍清空 | T-07 | standard | high | true | single-agent | AC-012 | 12 | ready |
| T-13 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/13-recoverable-registration.md</Path> | 服务端关闭注册时入口与页面均准确，后端仍拒绝直接调用 | T-12 | standard | medium | true | single-agent | AC-013 | 13 | ready |
| T-14 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/14-profile-self-materials.md</Path> | 新个人CN_RESIDENT_ID上传正反面后完成提交 | T-18 | deep | high | true | single-agent | AC-014 | 15 | ready |
| T-15 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/15-contract-profile-legacy-bridges.md</Path> | 旧入口引用为零且替代能力覆盖完整 | — | deep | high | true | single-agent | AC-015 | 16 | ready |
| T-16 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/16-workflow-task-integrity.md</Path> | B失败绝不发出A的审批请求 | — | deep | high | true | single-agent | AC-016 | 17 | ready |
| T-17 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/17-trusted-designer-messages.md</Path> | 错误origin、错误source、未知payload不能关闭标签 | — | standard | medium | true | single-agent | AC-017 | 18 | ready |
| T-18 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/18-upload-ownership-lifecycle.md</Path> | 下载URL失败不生成无人回收的Blob URL，也不把已完成上传误报失败 | — | deep | high | true | single-agent | AC-018 | 14 | ready |
| T-19 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/19-system-page-state-locality.md</Path> | 快速筛选时旧响应不能覆盖新列表，失败/loading可恢复 | T-18 | standard | medium | true | single-agent | AC-019 | 19 | ready |
| T-20 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/20-strict-contract-target.md</Path> | 受影响边界类型检查通过，nullable与非法transport样本有明确处理 | T-12, T-14, T-19 | deep | medium | true | single-agent | AC-020 | 20 | ready |
| T-21 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/21-accessible-public-apps.md</Path> | 只用键盘可完成公开流程，焦点可见且错误能被读屏发现 | T-13 | standard | medium | true | single-agent | AC-021 | 21 | ready |
| T-22 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/22-atomic-notify-result.md</Path> | 任意一条SQL失败不会留下Delivery/Attempt/Outbox不一致 | — | deep | high | true | single-agent | AC-022 | 22 | ready |
| T-23 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/23-durable-provider-callback.md</Path> | 回滚后相同事件可重试成功 | T-22 | deep | high | false | single-agent | AC-023 | 23 | blocked |
| T-24 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/24-recoverable-third-resilience.md</Path> | 崩溃后permit在规定上限内恢复，不依赖人工删key | — | deep | high | true | single-agent | AC-024 | 26 | ready |
| T-25 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/25-acyclic-department-moves.md</Path> | 自父/后代父/并发互移均不能形成环 | — | deep | high | true | single-agent | AC-025 | 27 | ready |
| T-26 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/26-cut-over-crud-contracts.md</Path> | 每个候选有迁移或保留理由，不遗漏调用者 | T-02, T-16, T-25 | deep | high | true | single-agent | AC-026 | 28 | ready |
| T-27 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/27-honest-demo-tree-baseline.md</Path> | 保存/删除路径不再有虚假校验TODO | T-26 | standard | medium | true | single-agent | AC-027 | 29 | ready |
| T-28 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/28-bounded-notify-wake.md</Path> | 慢provider不阻塞业务提交线程 | T-22 | deep | high | true | single-agent | AC-028 | 24 | ready |
| T-29 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/29-converge-current-documentation.md</Path> | 每个删除文件有owner、内容迁移落点和无丢失硬约束证据 | T-01 | deep | medium | true | single-agent | AC-029 | 30 | ready |
| T-30 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/30-integrated-upgrade-acceptance.md</Path> | 全部已接受AC均有实际命令/退出码/环境/源码checkpoint | T-01, T-02, T-03, T-04, T-05, T-06, T-07, T-08, T-09, T-10, T-11, T-12, T-13, T-14, T-15, T-16, T-17, T-18, T-19, T-20, T-21, T-22, T-23, T-24, T-25, T-26, T-27, T-28, T-29, T-31 | deep | high | true | single-agent | AC-030 | 31 | ready |
| T-31 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/31-enterprise-transfer-queued-contract.md</Path> | 真实Notify返回QUEUED时send不抛DELIVERY_FAILED，transfer与通知同事务提交 | T-22 | deep | critical | true | single-agent | AC-031 | 25 | ready |

## 3. 依赖 DAG

blocked_by权威见各Ticket，上表为完整边表。以下为串行调度顺序，不额外制造代码依赖：

T-01 → T-02 → T-03 → T-04 → T-05 → T-06 → T-07 → T-09 → T-10 → T-08 → T-11 → T-12 → T-13 → T-18 → T-14 → T-15 → T-16 → T-17 → T-19 → T-20 → T-21 → T-22 → T-23 → T-28 → T-31 → T-24 → T-25 → T-26 → T-27 → T-29 → T-30

T-15不依赖T-14；T-31只依赖T-22，不要求callback receipt先完成。T-30汇合其余30票。T-03阻塞闭包为T-03/T-30；T-23为T-23/T-30，其他票局部可规划，但全局执行Gate仍关闭。按边数最长链为T-06→T-07→T-12→T-13→T-21→T-30（不是工时估计）；current策略使全部执行串行。

## 4. 合同覆盖矩阵

| Contract ID | Ticket | 验证接缝 | 状态 | Evidence（未来） |
|---|---|---|---|---|
| AC-001 | T-01 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-01.md</Path> |
| AC-002 | T-02 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-02.md</Path> |
| AC-003 | T-03 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-03.md</Path> |
| AC-004 | T-04 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-04.md</Path> |
| AC-005 | T-05 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-05.md</Path> |
| AC-006 | T-06 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-06.md</Path> |
| AC-007 | T-07 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-07.md</Path> |
| AC-008 | T-08 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-08.md</Path> |
| AC-009 | T-09 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-09.md</Path> |
| AC-010 | T-10 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-10.md</Path> |
| AC-011 | T-11 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-11.md</Path> |
| AC-012 | T-12 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-12.md</Path> |
| AC-013 | T-13 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-13.md</Path> |
| AC-014 | T-14 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-14.md</Path> |
| AC-015 | T-15 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-15.md</Path> |
| AC-016 | T-16 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-16.md</Path> |
| AC-017 | T-17 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-17.md</Path> |
| AC-018 | T-18 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-18.md</Path> |
| AC-019 | T-19 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-19.md</Path> |
| AC-020 | T-20 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-20.md</Path> |
| AC-021 | T-21 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-21.md</Path> |
| AC-022 | T-22 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-22.md</Path> |
| AC-023 | T-23 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-23.md</Path> |
| AC-024 | T-24 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-24.md</Path> |
| AC-025 | T-25 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-25.md</Path> |
| AC-026 | T-26 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-26.md</Path> |
| AC-027 | T-27 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-27.md</Path> |
| AC-028 | T-28 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-28.md</Path> |
| AC-029 | T-29 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-29.md</Path> |
| AC-030 | T-30 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-30.md</Path> |
| AC-031 | T-31 | 本票第8节正常/失败/回归矩阵 | covered；尚未实现 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-31.md</Path> |

## 5. 并行与路径所有权

当前只有single-agent，最大实际并发1、子代理0。各票声明所有与其他票相交的shared_paths及唯一owner；由同一Lead在当前票轮次内写入，下一票回读前一结果。没有并行消费者，不为顺序互斥新增虚假blocked_by。目录级写集仅覆盖本票必需修改；不能借宽路径重写模块。

共享热点包括common-web/json、SSO、Profile、Notify、全局DDL、App路由、前端manifest/lock/OpenAPI、release与测试。精确交集由各票frontmatter和规划检查证据保存。所有票声明workspace:current-exclusive语义资源；跨票改公共合同须同步消费者与验证，不能以不同文件名声称无冲突。

## 6. Gate、Wave 与集成点

<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>拥有G-plan/G-authorize/G-ticket/G-integration/G-release；Map不复制Gate状态。每票一个串行slot，阶段分组只服务阅读，不允许并发。

## 7. 横切契约与风险

基座直接切换，无双路由、旧桥、存量兼容等待；所有仓内调用与生成物同批更新。日志副本不得改变签名原文。Provider I/O不进入数据库长事务。SQL编辑源仅六文件基座，新增DDL归10、DML归50。产物指针原子性不等于多容器运行时原子性。

## 8. 同步规则

状态/依赖/路径/绑定由Ticket拥有；Map和plan-data.json仅投影。变化递增plan_revision，复核依赖闭包和共享路径。Goal拥有调度/授权，不覆盖行为合同。永久ADR/context本轮只读。

## 9. 总控与恢复

先回读Goal、Map、当前Ticket、change状态和最新Evidence。运行ticket-control的--map只读检查；没有ready frontier是当前Gate的正确输出，不应绕过。恢复所需源基线见<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/planning-baseline.json</Path>。T/P本轮只完成计划文档，产品票不得Done。下一步先补两项决策证据及核对授权/干净工作区，再进入I-implement；本轮不自动开始。
