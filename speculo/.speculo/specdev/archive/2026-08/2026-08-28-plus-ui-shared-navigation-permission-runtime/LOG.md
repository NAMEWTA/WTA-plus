# Plus UI 共享导航与权限运行时设计日志

## LOG-001 — 2026-08-28T10:10:35+08:00 — 抽象准入原则
- **设计树节点：** D-001
- **轮次与依赖：** round 1 / 无
- **状态：** confirmed
- **问题：** 继续改造前端 monorepo 时，怎样避免为了目录解耦而制造没有真实价值的共享层。
- **事实与来源：** 用户要求不为了解耦而解耦，但符合规范、属于必备通用能力且便利后续开发时可以抽象；`USER-DECISION:2026-08-28`。
- **选项：** 尽可能把 App 文件搬入 packages；完全冻结当前边界；以稳定语义、依赖纯度和真实消费者证据逐项判断。
- **推荐：** 采用逐项准入；只有抽出后拥有清晰输入输出、脱离 Admin 单例并减少未来终端重复实现的能力进入共享包。
- **结论：** 本 change 使用价值驱动的选择性抽象，不以 App 文件数量或 packages 数量作为成功指标。
- **原因：** 过度抽象会把 Admin 策略伪装成公共合同，抽象不足又会让新 App 复制权限和菜单算法。
- **影响工件：** CONTEXT / Spec / Ticket
- **约束或不变量：** App 继续拥有 Client、Router、Store 实例、布局、白名单、品牌和部署策略。
- **后续：** 对每个候选能力分别验证共享语义、依赖边界、消费者和测试接缝。
- **替代/被替代：** 无

## LOG-002 — 2026-08-28T10:10:35+08:00 — 零兼容基座策略
- **设计树节点：** D-002
- **轮次与依赖：** round 1 / 无
- **状态：** confirmed
- **问题：** 无真实消费者的旧动态路由入口是否需要保留兼容门面。
- **事实与来源：** 用户此前明确当前前端是新基座，无需兼容；永久架构记录也确认迁移消费者后直接删除旧入口。`USER-DECISION:基座无需兼容`；`CODE:<Path>speculo/.speculo/specdev/adr/0012-compile-time-modular-monorepo.md</Path>`。
- **选项：** 保留别名与转发；标记废弃后长期保留；验证无消费者后直接删除。
- **推荐：** 直接删除并由架构检查阻止回流。
- **结论：** 本 change 不为已确认无消费者的 `dynamicRoutes`、旧 Store 名称或本地动态页面兜底建立兼容层。
- **原因：** 基座没有需要保护的外部调用方，兼容层只会继续制造双入口和错误的新 App 样例。
- **影响工件：** Spec / Ticket
- **约束或不变量：** 删除前必须用源码检索、单元测试和 E2E 证明真实流程已有正式替代入口。
- **后续：** D-007 决定具体清理范围。
- **替代/被替代：** 无

## LOG-003 — 2026-08-28T10:20:23+08:00 — 聚焦权限与导航纵向切片
- **设计树节点：** D-003
- **轮次与依赖：** round 1 / D-001
- **状态：** confirmed
- **问题：** 本 change 是否顺带重构其他 App Store、布局和 UI。
- **事实与来源：** 用户采纳聚焦建议；`USER-DECISION:Q1`。
- **选项：** 全面清理 Admin 基础设施；只移动权限文件；聚焦从身份权限到动态路由和界面可见性的完整纵向切片。
- **推荐：** 聚焦纵向切片，并将无关 Store、布局和 UI 明确排除。
- **结论：** IN 为 Vue 权限宿主、服务端菜单转换、Admin 导航 Store、路由守卫接线和遗留动态入口；其他 App 基础设施为 OUT。
- **原因：** 该范围能独立证明共享边界，又不会演变为缺少行为目标的全仓目录搬迁。
- **影响工件：** Spec / Ticket / Goal Plan
- **约束或不变量：** 登录、Client、菜单、权限、路由和页面可见性行为保持不变。
- **后续：** 下游 Spec 明确 IN/OUT 与零行为回归合同。
- **替代/被替代：** 无

## LOG-004 — 2026-08-28T10:20:23+08:00 — 建立 Vue 权限宿主合同
- **设计树节点：** D-004
- **轮次与依赖：** round 1 / D-001
- **状态：** confirmed
- **问题：** 当前只有一个激活 App 时，Vue 权限指令是否具有足够真实证据进入 Web Kit。
- **事实与来源：** system、workflow、demo、gen 多个 Web Domain 页面已经直接使用全局 `v-hasPermi`；用户确认这是真实宿主合同而非假想抽象；`CODE:<Path>plus-ui-namewta/packages/web-domains/**</Path>`；`USER-DECISION:Q2`。
- **选项：** 保留 Admin 私有实现；将指令放入跨终端 Platform；建立注入权限求值器的 Web Kit 包。
- **推荐：** 建立独立 Web Kit 权限指令包。
- **结论：** Web Kit 提供无 Admin Store 依赖的 Vue 权限指令工厂或安装入口，App 注入自己的 `AccessEvaluator`。
- **原因：** Web Domain 已经形成多个真实界面消费者，而 Vue/DOM 语义又不应进入跨终端 Platform。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 指令失败关闭，前端可见性不替代后端鉴权；移动端和小程序不依赖该包。
- **后续：** 特征测试先锁定当前指令行为和错误输入。
- **替代/被替代：** 无

## LOG-005 — 2026-08-28T10:20:23+08:00 — Permission Store 归位为 Navigation Store
- **设计树节点：** D-005
- **轮次与依赖：** round 1 / D-001
- **状态：** confirmed
- **问题：** 当前 permission Store 的名称和所有权是否与真实职责一致。
- **事实与来源：** Store 保存 routes、sidebarRouters、topbarRouters 与 defaultRoutes，并连接 Admin Router、Layout 和导航组件；用户采纳改名并留在 App 的建议；`CODE:<Path>plus-ui-namewta/apps/admin-web/src/store/modules/permission.ts</Path>`；`USER-DECISION:Q3`。
- **选项：** 整体搬入共享包；保留当前名称；改名为 navigation Store 并保持 App ownership。
- **推荐：** 改名且继续归 Admin。
- **结论：** Admin navigation Store 只拥有导航投影和 Router 注册；权限判断由 `platform-permission` 负责。
- **原因：** 名称应表达状态职责，且不同 App 不必拥有 Admin 的侧栏、顶栏或默认路由模型。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 不创建公共 Pinia Store；现有布局消费者统一迁移到新正式入口，不保留旧别名。
- **后续：** Ticket 需覆盖所有布局与守卫调用方。
- **替代/被替代：** 无

## LOG-006 — 2026-08-28T10:20:23+08:00 — 只下沉服务端菜单纯流程
- **设计树节点：** D-006
- **轮次与依赖：** round 1 / D-001
- **状态：** confirmed
- **问题：** 服务端菜单转换中哪些内容属于稳定共享能力。
- **事实与来源：** `assembleServerRoutes` 和 `restoreProtectedNavigation` 已在 `platform-app-runtime`；Store 中仍有 ParentView 展平、空 children 清理与重复名称检查；用户要求只下沉纯函数；`USER-DECISION:Q4`。
- **选项：** 不再抽取；抽公共 Store/Router；只抽无单例依赖的确定性转换与诊断。
- **推荐：** 扩展现有 `platform-app-runtime` 纯函数入口。
- **结论：** 可共享流程不得依赖 Pinia、Router 单例、Admin Layout 或 Element Plus；App 注入特殊组件、manifest resolver 和诊断呈现。
- **原因：** 同一后台的 RouterVo 语义可以复用，但导航容器和状态模型属于终端产品。
- **影响工件：** ADR / Spec / Ticket
- **约束或不变量：** 不新增大而全 common 包，不改变后端已裁剪菜单的权威性。
- **后续：** 先以输入输出测试确定具体最小公共 API。
- **替代/被替代：** 无

## LOG-007 — 2026-08-28T10:20:23+08:00 — 动态页面严格 Manifest-only
- **设计树节点：** D-007
- **轮次与依赖：** round 1 / D-002
- **状态：** confirmed
- **问题：** 是否继续允许空 local dynamicRoutes 和 App 本地 views glob 作为动态菜单兜底。
- **事实与来源：** 当前 `dynamicRoutes` 为空；Admin 自有页面均有显式静态路由或 App-owned manifest；用户确认删除兜底并遵循 manifest 边界；`USER-DECISION:Q5`。
- **选项：** 保留双解析；只删除空数组；删除两类遗留入口并保持未知键诊断。
- **推荐：** 删除遗留入口，动态页面只由特殊组件或所选 manifest 解析。
- **结论：** Admin 不再扫描本地 views 为服务端菜单补偿；未知或未选择组件键保持失败关闭和稳定诊断。
- **原因：** 隐式 glob 会绕过编译期领域选择，削弱包公开入口与 ADR-0015。
- **影响工件：** ADR / Spec / Ticket
- **约束或不变量：** 登录、错误、首页、个人中心等 App 静态路由继续显式声明。
- **后续：** 删除前复核所有服务端组件键已有 manifest 或特殊宿主注册。
- **替代/被替代：** 无

## LOG-008 — 2026-08-28T10:20:23+08:00 — 共享包公开边界
- **设计树节点：** D-008
- **轮次与依赖：** round 2 / D-004, D-006
- **状态：** confirmed
- **问题：** Vue 指令、权限算法和菜单纯流程应放在何种共享层。
- **事实与来源：** 用户在 Q2 明确选择 Web Kit 权限宿主，在 Q4 明确只将纯函数下沉；永久 ADR-0013 要求 Web 与跨终端能力分离。
- **选项：** 合并为 common；全部进入 platform；按 Web Kit、permission platform 与 app-runtime 分层。
- **推荐：** 使用现有层次，不建立聚合公共包。
- **结论：** Vue 指令进入独立 Web Kit 包；跨终端权限算法继续由 `platform-permission` 拥有；菜单纯流程扩展 `platform-app-runtime`；Admin 通过公开入口组合。
- **原因：** 三者的运行时依赖和变化原因不同，合并只会重新制造公共杂物包。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 共享包不得导入 `apps/**` 或深层入口；App 不复制共享算法。
- **后续：** D-009 决定迁移和验收门禁。
- **替代/被替代：** 无

## LOG-009 — 2026-08-28T10:23:43+08:00 — 分阶段迁移与严格验收
- **设计树节点：** D-009
- **轮次与依赖：** round 2 / D-003, D-004, D-005, D-006, D-007, D-008
- **状态：** confirmed
- **问题：** 如何迁移共享权限与导航运行时，同时避免认证、菜单和路由行为回归。
- **事实与来源：** 用户确认采用逐阶段保持绿色的五阶段策略，并再次明确当前是基座，无需兼容；`USER-DECISION:Q6`。
- **选项：** 一次性修改后全量验证；以功能开关双轨运行；行为冻结后逐阶段迁移并直接删除旧入口。
- **推荐：** 先测试、后共享、再迁移、再清理，最后执行全量门禁。
- **结论：** 顺序固定为：权限指令和菜单行为特征测试；Web Kit 与 Platform 公共能力；Admin Store/守卫/布局消费者迁移；零兼容删除；架构、静态、单元、双模式构建与 Playwright 验收。
- **原因：** 高风险链路需要紧凑反馈回路，但基座没有维持双实现或功能开关的业务价值。
- **影响工件：** Spec / Ticket / Goal Plan
- **约束或不变量：** 后端继续按 Client 裁剪；前端保持 `getInfo -> getRouters -> addRoute -> replace`；未知组件、缺失身份和权限不匹配均失败关闭；每阶段未保持绿色不得进入下一阶段。
- **后续：** 用户完成最终共识确认后进入 Spec，不直接实施。
- **替代/被替代：** 无

## LOG-010 — 2026-08-28T11:35:14+08:00 — 最终设计共识
- **设计树节点：** D-010
- **轮次与依赖：** round 3 / D-003, D-004, D-005, D-006, D-007, D-008, D-009
- **状态：** confirmed
- **问题：** 当前设计是否已经决策完备，可以结束 Grill 并进入正式 Spec。
- **事实与来源：** 用户明确确认范围、公共包边界、迁移策略、安全行为和验收标准均无开放问题；`USER-DECISION:最终共识`。
- **选项：** 继续展开函数与文件细节；直接实施；结束设计访谈并进入 Spec。
- **推荐：** 结束 Grill，由 Spec 固化外部行为、不变量和验收合同。
- **结论：** 设计树达到 consensus；具体函数名和局部文件组织由 Spec/Ticket 在既定边界内决定，不再建立设计节点。
- **原因：** 所有会改变范围、架构、安全、迁移或验收的高影响决策已经关闭。
- **影响工件：** Spec / Ticket / Goal Plan
- **约束或不变量：** 本共识不构成实现授权，不自动修改前端源码。
- **后续：** 下一 Work 为 `<Path>{roots.workflows}/specdev/S-spec/S-spec.md</Path>`。
- **替代/被替代：** 无

## LOG-011 — 2026-08-28T17:22:33+08:00 — CR-001 重新打开归档前整改
- **设计树节点：** 无：实现后评审整改，不改变既有设计树共识
- **轮次与依赖：** post-implementation review / CR-001
- **状态：** confirmed
- **问题：** T-01 至 T-05 已完成后，是否仍满足 completed 和 archive-ready。
- **事实与来源：** `CR-001` 固定评审结果为 `request-changes`：权限指令异常路径未证明 DOM 失败关闭；菜单边界仍以宽泛 Record 和双重断言穿透；两条并发 E2E locator 曾导致完整套件 37/39。用户明确要求先更新文档且不要实施；`REVIEW:CR-001`；`USER-DECISION:2026-08-28-docs-only-no-implementation`。
- **选项：** 忽略 review 并保持 completed；改写 Spec 降低验收；保留历史 Evidence 并新增整改 Ticket、重新打开状态。
- **推荐：** 新增 T-06、T-07、T-08，并以 CR-002 approve 作为恢复 completed 的必要 Gate。
- **结论：** T-01 至 T-05 保持历史 done；T-06 负责权限异常失败关闭，T-07 负责递归菜单领域边界和显式 Router adapter，T-08 负责 locator 稳定与最终全量验证。change 状态改为 blocked，等待新的实现与 candidate integration 授权。
- **原因：** 三项整改都可由现有 `AC-002`、`AC-012`、`AC-015` 和工程标准判定，不需要改变产品目标或架构所有权；但未完成前不能归档。
- **影响工件：** Source / Triage / Tickets / Tickets Map / Goal Plan / Status
- **约束或不变量：** 本轮不修改产品代码、不创建实现 worktree、不形成实现 commit、不集成、不清理未来 source、不归档；不得改写 T-01 至 T-05 的历史 Evidence。
- **后续：** 用户新授权后关闭 G-50，按 `T-06 || T-07 -> T-08 -> CR-002 -> reconcile -> archive dry-run` 执行。
- **替代/被替代：** 替代“G-40 后无 blocker”的旧进度判断；不替代 ADR 或 Spec 合同。

## LOG-012 — 2026-08-28T23:19:58+08:00 — CR-001 整改完成并恢复 completed
- **设计树节点：** 无：实现后评审整改闭环，不改变既有设计树共识
- **轮次与依赖：** post-implementation repair / T-06, T-07, T-08, CR-002
- **状态：** confirmed
- **问题：** CR-001 的三个 finding 是否已由固定实现、候选验证和独立复审充分关闭。
- **事实与来源：** T-06 真实 Vue 生命周期证明异常路径先移除 DOM 再重抛；T-07 建立递归 `ServerMenuNode` 和显式 Admin Router adapter；T-08 的两个目标 E2E 为 2/2，完整 5-worker/0-retry suite 连续三轮各 48/48，标准 suite 48/48，根 architecture/lint/typecheck/test/build:dev/prod 全部通过。前端 `main` 为 `07962c7cad9ca4db168b3c423b9e3675f312a874`；`CR-002` 为 `approved`。
- **选项：** 保持 active 等待重复验证；降低原验收要求；按固定证据关闭 finding 并恢复 completed。
- **推荐：** 保留全部原合同和 Evidence，关闭 CR-001 blocker，恢复 completed。
- **结论：** T-06 至 T-08 全部 done/integrated，AC-001 至 AC-015 全部 passed，change 恢复 completed；归档仍是独立动作。
- **原因：** 所有 satisfied-when 条件均由直接测试接缝、固定 SHA 和同一 candidate 的完整质量矩阵证明，不依赖 retry、串行化、放宽断言或产品行为变更。
- **影响工件：** T-08 / Evidence / CR-002 / Tickets Map / Goal Plan / Status
- **约束或不变量：** T-06/T-07/T-08 transient candidate worktree 与 integration branch 已清理；source cleanup 未授权，三个 source worktree 与 branch 保留；不推送、不部署、不归档。
- **后续：** 运行 SpecDev validator 和最终 Git 一致性核对；等待独立 archive/source-cleanup 授权。
- **替代/被替代：** 关闭 LOG-011 的未决 blocker；不改写 CR-001 历史结论。

## LOG-013 — 2026-08-28T23:42:03+08:00 — 已完成 worktree 全面清理
- **设计树节点：** 不适用
- **轮次与依赖：** post-completion cleanup / LOG-012
- **状态：** confirmed
- **问题：** 已完成 change 的 source/candidate worktree 与本地分支是否仍需保留。
- **事实与来源：** 用户明确要求合并已完成 worktree 并全面清理；19 个 worktree 均 clean，且其 HEAD 均已被对应子仓库 `main` 包含。
- **结论：** 无需创建空合并提交；移除 19 个 worktree 和 19 条对应本地分支，并清除空的 `specdev-worktree` 目录层级。
- **验证：** 前后端 `git worktree list` 均仅剩各自 `main`；任务/集成分支零匹配；`specdev-worktree` 路径不存在。
- **影响工件：** `.status.json` / Goal Plan / HANDOFF / T-06 至 T-08 Evidence
- **约束或不变量：** 保留所有 source/candidate/result SHA 与验证 Evidence；不修改主检出的既有 dirty 内容，不推送、不部署、不归档。
