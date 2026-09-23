# Change Log

- 2026-09-14：原始全面审查与31票草案；原始机器证据保留。
- 2026-09-18：单人串行重审当前工作树，修订全部票据、Spec/ADR/索引与报告；删除兼容等待和无证据的扩展设计，修正SQL路径、状态语义、依赖与验收；重跑9条现有检查并记录实际非零基线。产品实现未开始。

## 2026-09-18 T-tickets / P-goal-plan（plan）

用户明确激活两Work并授权自主完善文档，保持单人串行、禁止子代理。承接已完成的review-architecture复核检查点，依次设置current_work=tickets→goal-plan；T完成后登记works_run；P文档已形成但阶段检查受阻，保留current_work=specdev/goal-plan，不登记P成功。历史lead locator转为single-agent，保留history，不表示新建或派遣agent。

完成31票完整合同及Goal/Map；29票局部Ready，T-03/T-23因真实证据缺口blocked，Spec总体draft/Goal执行关闭。全局状态仅补当前change的active指针；未改config、其他change、永久知识或产品代码。P创建current/direct-parent策略，用户已要求单并发，此次规划沿用当前workspace，不反复询问。

调用plan-quality-review逐票/逐轴检查，结果见<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/plan-quality-review.md</Path>；subagent-delivery仅operation=plan检查禁止派遣及Lead所有权，无dispatch。两阶段校验/控制器记录见<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/planning-validation.json</Path>。规划完成不表示T-03/T-23或产品实现完成，也不构成commit/部署授权。

最终校验纠正：P阶段校验器存在单change路由缺陷：validateParentImplementation以stage==goal-plan无条件要求implementation-map.md/implementation-plan.md，而single-change-plan只要求goal-plan.md。未创建虚假父级工件，也未越范围修改workflow工具。 P未登记works_run，current_work保留specdev/goal-plan；T已完成。两张blocked票与授权Gate照实保留。


## 2026-09-23 G 重规划（当前轮）

原日志保留为历史事实；以下编号是当前设计树唯一恢复指针。没有用户回复的选项仍是推荐，不伪造共识。

## LOG-001 — 2026-09-23 — 本轮范围与执行边界
- **设计树节点：** D-001
- **轮次与依赖：** round 1 / 无
- **状态：** confirmed
- **问题：** 是否只规划并保留现有change？
- **事实与来源：** 用户本轮请求、<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/current-source-audit.json</Path>、<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/WTA-plus-review-64b4ea7.md</Path>。
- **推荐：** 全面修订原change，G→S→T→P计划；用户审查后自行激活目标。
- **结论：** 用户2026-09-23明确请求；本轮禁止产品实现、commit/push、部署、真实数据操作。
- **影响工件：** ADR / Spec / Ticket / Goal Plan
- **后续：** Lead收到明确答复后更新本节点，重算frontier，再校验G。

## LOG-002 — 2026-09-23 — 撤回语义
- **设计树节点：** D-002
- **轮次与依赖：** round 1 / 无
- **状态：** deferred
- **问题：** 撤回是否保留已送达内容？
- **事实与来源：** 用户本轮请求、<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/current-source-audit.json</Path>、<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/WTA-plus-review-64b4ea7.md</Path>。
- **推荐：** 只停止本发布版本未发送任务；保留已送达站内信/快照/审计，外部已发送不可追回。
- **结论：** 等待用户确认，未获得回答不等于采纳；阻止受影响合同Ready。
- **影响工件：** ADR / Spec / Ticket / Goal Plan
- **后续：** Lead收到明确答复后更新本节点，重算frontier，再校验G。

## LOG-003 — 2026-09-23 — OSS互斥取舍
- **设计树节点：** D-003
- **轮次与依赖：** round 1 / 无
- **状态：** deferred
- **问题：** 是否采用单对象有界I/O事务锁？
- **事实与来源：** 用户本轮请求、<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/current-source-audit.json</Path>、<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/WTA-plus-review-64b4ea7.md</Path>。
- **推荐：** 统一sys_oss→工单锁序，单对象短事务内幂等删除，有界超时；不锁整批。
- **结论：** 等待用户确认，未获得回答不等于采纳；阻止受影响合同Ready。
- **影响工件：** ADR / Spec / Ticket / Goal Plan
- **后续：** Lead收到明确答复后更新本节点，重算frontier，再校验G。

## LOG-004 — 2026-09-23 — SSO与历史票边界
- **设计树节点：** D-004
- **轮次与依赖：** round 1 / 无
- **状态：** deferred
- **问题：** 是否保持SSO迁移由相邻change承接？
- **事实与来源：** 用户本轮请求、<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/current-source-audit.json</Path>、<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/WTA-plus-review-64b4ea7.md</Path>。
- **推荐：** 报告N/O/S/D全纳入；原31票保留复核，OIDC迁移引用相邻change且不跨写。
- **结论：** 等待用户确认，未获得回答不等于采纳；阻止受影响合同Ready。
- **影响工件：** ADR / Spec / Ticket / Goal Plan
- **后续：** Lead收到明确答复后更新本节点，重算frontier，再校验G。

## LOG-005 — 2026-09-23 — 收件箱分页与全部已读
- **设计树节点：** D-005
- **轮次与依赖：** round 1 / 无
- **状态：** deferred
- **问题：** 是否保留完整历史及本人全部已读？
- **事实与来源：** 用户本轮请求、<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/current-source-audit.json</Path>、<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/WTA-plus-review-64b4ea7.md</Path>。
- **推荐：** 项目分页合同、稳定排序、本人总未读；顶部最近摘要；全部已读保持全量本人语义。
- **结论：** 等待用户确认，未获得回答不等于采纳；阻止受影响合同Ready。
- **影响工件：** ADR / Spec / Ticket / Goal Plan
- **后续：** Lead收到明确答复后更新本节点，重算frontier，再校验G。

## LOG-006 — 2026-09-23 — 未支持通知合同
- **设计树节点：** D-006
- **轮次与依赖：** round 1 / 无
- **状态：** deferred
- **问题：** 是否收缩为ALL/ASYNC/priority=0？
- **事实与来源：** 用户本轮请求、<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/current-source-audit.json</Path>、<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/WTA-plus-review-64b4ea7.md</Path>。
- **推荐：** 拒绝其他取值，同批迁移所有现有非零priority调用，保留截止时间与幂等。
- **结论：** 等待用户确认，未获得回答不等于采纳；阻止受影响合同Ready。
- **影响工件：** ADR / Spec / Ticket / Goal Plan
- **后续：** Lead收到明确答复后更新本节点，重算frontier，再校验G。

## LOG-007 — 2026-09-23 — 凭据轮换与归档边界
- **设计树节点：** D-007
- **轮次与依赖：** round 1 / 无
- **状态：** deferred
- **问题：** 真实凭据处置如何关闭？
- **事实与来源：** 用户本轮请求、<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/current-source-audit.json</Path>、<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/WTA-plus-review-64b4ea7.md</Path>。
- **推荐：** 本地整改及轮换操作清单先做；真实轮换需环境owner批准及证据，未处置/未明确豁免阻止归档。
- **结论：** 等待用户确认，未获得回答不等于采纳；阻止受影响合同Ready。
- **影响工件：** ADR / Spec / Ticket / Goal Plan
- **后续：** Lead收到明确答复后更新本节点，重算frontier，再校验G。

## LOG-008 — 2026-09-23 — 替代OSS硬门禁
- **设计树节点：** D-008
- **轮次与依赖：** round 1 / 无
- **状态：** deferred
- **问题：** 是否替代System AGENTS的readiness访问硬前置？
- **事实与来源：** 用户本轮请求、<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/current-source-audit.json</Path>、<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/WTA-plus-review-64b4ea7.md</Path>。
- **推荐：** 按报告取消诊断门禁但保留用户授权/状态/PRIVATE类型；规范和测试同票修改。
- **结论：** 等待用户确认，未获得回答不等于采纳；阻止受影响合同Ready。
- **影响工件：** ADR / Spec / Ticket / Goal Plan
- **后续：** Lead收到明确答复后更新本节点，重算frontier，再校验G。

## LOG-009 — 2026-09-23 — 附件持久归属
- **设计树节点：** D-009
- **轮次与依赖：** round 1 / 无
- **状态：** deferred
- **问题：** 报告假设的快照能力仅有SPI及测试替身，是否补齐生产适配和真实owner？
- **事实与来源：** 用户本轮请求、<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/current-source-audit.json</Path>、<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/WTA-plus-review-64b4ea7.md</Path>。
- **推荐：** 附件归Notify意图的持久关系；系统OSS实现快照/物化端口，绑定真实表PK；不恢复sys_notify_log。
- **结论：** 等待用户确认，未获得回答不等于采纳；阻止受影响合同Ready。
- **影响工件：** ADR / Spec / Ticket / Goal Plan
- **后续：** Lead收到明确答复后更新本节点，重算frontier，再校验G。

## LOG-010 — 2026-09-23 — 撤回语义已确认
- **设计树节点：** D-002
- **轮次与依赖：** round 1 / 无
- **状态：** answered
- **问题：** 撤回是否保留已送达内容？
- **事实与来源：** 本轮异步问题的真实用户答复；原调查见LOG-002。
- **推荐：** 只停止本发布版本未发送任务；保留已送达站内信/快照/审计，外部已发送不可追回。
- **结论：** 用户回复“采用以上三项（推荐）”，本节点推荐已接受。
- **影响工件：** ADR / CONTEXT / Spec / Tickets / Goal
- **后续：** 所有设计选择已答，等待一次整体共识确认；不授权实施。

## LOG-011 — 2026-09-23 — OSS互斥取舍已确认
- **设计树节点：** D-003
- **轮次与依赖：** round 1 / 无
- **状态：** answered
- **问题：** 是否采用单对象有界I/O事务锁？
- **事实与来源：** 本轮异步问题的真实用户答复；原调查见LOG-003。
- **推荐：** 统一sys_oss→工单锁序，单对象短事务内幂等删除，有界超时；不锁整批。
- **结论：** 用户回复“采用以上三项（推荐）”，本节点推荐已接受。
- **影响工件：** ADR / CONTEXT / Spec / Tickets / Goal
- **后续：** 所有设计选择已答，等待一次整体共识确认；不授权实施。

## LOG-012 — 2026-09-23 — SSO与历史票边界已确认
- **设计树节点：** D-004
- **轮次与依赖：** round 1 / 无
- **状态：** answered
- **问题：** 是否保持SSO迁移由相邻change承接？
- **事实与来源：** 本轮异步问题的真实用户答复；原调查见LOG-004。
- **推荐：** 报告N/O/S/D全纳入；原31票保留复核，OIDC迁移引用相邻change且不跨写。
- **结论：** 用户回复“采用以上三项（推荐）”，本节点推荐已接受。
- **影响工件：** ADR / CONTEXT / Spec / Tickets / Goal
- **后续：** 所有设计选择已答，等待一次整体共识确认；不授权实施。

## LOG-013 — 2026-09-23 — 收件箱分页与全部已读已确认
- **设计树节点：** D-005
- **轮次与依赖：** round 1 / 无
- **状态：** answered
- **问题：** 是否保留完整历史及本人全部已读？
- **事实与来源：** 本轮异步问题的真实用户答复；原调查见LOG-005。
- **推荐：** 项目分页合同、稳定排序、本人总未读；顶部最近摘要；全部已读保持全量本人语义。
- **结论：** 用户回复“采用这些边界（推荐）”，本节点推荐已接受。
- **影响工件：** ADR / CONTEXT / Spec / Tickets / Goal
- **后续：** 所有设计选择已答，等待一次整体共识确认；不授权实施。

## LOG-014 — 2026-09-23 — 未支持通知合同已确认
- **设计树节点：** D-006
- **轮次与依赖：** round 1 / 无
- **状态：** answered
- **问题：** 是否收缩为ALL/ASYNC/priority=0？
- **事实与来源：** 本轮异步问题的真实用户答复；原调查见LOG-006。
- **推荐：** 拒绝其他取值，同批迁移所有现有非零priority调用，保留截止时间与幂等。
- **结论：** 用户回复“采用这些边界（推荐）”，本节点推荐已接受。
- **影响工件：** ADR / CONTEXT / Spec / Tickets / Goal
- **后续：** 所有设计选择已答，等待一次整体共识确认；不授权实施。

## LOG-015 — 2026-09-23 — 凭据轮换与归档边界已确认
- **设计树节点：** D-007
- **轮次与依赖：** round 1 / 无
- **状态：** answered
- **问题：** 真实凭据处置如何关闭？
- **事实与来源：** 本轮异步问题的真实用户答复；原调查见LOG-007。
- **推荐：** 本地整改及轮换操作清单先做；真实轮换需环境owner批准及证据，未处置/未明确豁免阻止归档。
- **结论：** 用户回复“采用这些边界（推荐）”，本节点推荐已接受。
- **影响工件：** ADR / CONTEXT / Spec / Tickets / Goal
- **后续：** 所有设计选择已答，等待一次整体共识确认；不授权实施。

## LOG-016 — 2026-09-23 — 替代OSS硬门禁已确认
- **设计树节点：** D-008
- **轮次与依赖：** round 1 / 无
- **状态：** answered
- **问题：** 是否替代System AGENTS的readiness访问硬前置？
- **事实与来源：** 本轮异步问题的真实用户答复；原调查见LOG-008。
- **推荐：** 按报告取消诊断门禁但保留用户授权/状态/PRIVATE类型；规范和测试同票修改。
- **结论：** 用户回复“按新报告替代（推荐）”，本节点推荐已接受。
- **影响工件：** ADR / CONTEXT / Spec / Tickets / Goal
- **后续：** 所有设计选择已答，等待一次整体共识确认；不授权实施。

## LOG-017 — 2026-09-23 — 附件持久归属已确认
- **设计树节点：** D-009
- **轮次与依赖：** round 1 / 无
- **状态：** answered
- **问题：** 报告假设的快照能力仅有SPI及测试替身，是否补齐生产适配和真实owner？
- **事实与来源：** 本轮异步问题的真实用户答复；原调查见LOG-009。
- **推荐：** 附件归Notify意图的持久关系；系统OSS实现快照/物化端口，绑定真实表PK；不恢复sys_notify_log。
- **结论：** 用户回复“采用完整附件闭环（推荐）”，本节点推荐已接受。
- **影响工件：** ADR / CONTEXT / Spec / Tickets / Goal
- **后续：** 所有设计选择已答，等待一次整体共识确认；不授权实施。

## LOG-018 — 2026-09-23 — G整体共识确认
- **设计树节点：** D-001—D-009（整体检查）
- **轮次与依赖：** round 2 / D-002—009均answered，frontier为空
- **状态：** consensus
- **问题：** 全部设计分支是否达成共识并可定稿S/T/P，目标执行留待用户自行激活？
- **事实与来源：** 本轮真实异步答复。
- **结论：** 用户明确回复“确认共识，定稿计划（推荐）”；G可完成，按已授权顺序定稿S→T→P(plan)。不启动目标、不授权产品实施。
- **影响工件：** design-tree / ADR / CONTEXT / Spec / Tickets / Goal / 状态
- **后续：** 发布计划并校验；用户之后主动激活Goal。

## LOG-019 — 2026-09-23 — S/T/P计划定稿
- **状态：** plan completed；run未启动
- **来源：** LOG-018与用户原始G→S→T→P请求。
- **结论：** revision137，Spec及50票Ready，Goal计划定稿、ready_for_execution=false。原31票当前重开Ready，历史review留在快照；高影响未决为零，未授权执行不误当设计缺口。
- **质量审查：** <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/plan-quality-review-2026-09-23.md</Path>。443共享路径提示按唯一Lead/current串行处理。
- **验证：** <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/replan-validation.json</Path>；<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/replan-integrity.json</Path>。
- **后续：** 用户自行激活目标后按Goal恢复；本轮无产品实施和外部副作用。
