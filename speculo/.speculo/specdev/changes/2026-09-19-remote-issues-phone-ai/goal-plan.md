---
schema_version: 6
artifact: goal-plan
change: 2026-09-19-remote-issues-phone-ai
status: in_progress
modes: ["migration", "high-assurance", "release-coordination"]
orchestration: lead-directed
lead: codex-root
implementation_agent_limit: 1
integration_attempt_limit: 3
ticket_workspace_policy: current
integration_gate: direct-parent
ready_for_execution: true
---

# Goal Plan: 手机号必填与 Snail AI 退出

- **Goal Plan：** <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/goal-plan.md</Path>
- **Spec：** <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/spec.md</Path>
- **Tickets Map：** <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/tickets-map.md</Path>
- **Ticket / Evidence：** <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/ticket/</Path>；<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/evidence/</Path>

用户已通过 LOG-009 激活 I，并在 LOG-011 明确批准具体基线处理与三票 main 本地提交。T-01 本地实现已验证，当前建立不可变提交并审查；随后 T-02→T-03 严格串行。current/direct-parent 策略不变。

## 1. Outcome and Authority

### Outcome

交付两个可观察结果：新增账号必须有有效手机号，存量账号在受影响资料写入时补齐且继续保留登录；Snail AI 从当前产品业务面、构建和发布接入退出，旧数据保留且不迁移。

实际产物数量：保留 wta-ai、wta-common-ai 两个 Maven 占位模块；MySQL 初始化基座恰好六份，40-cde-ai.sql 是不创建 vendor 表的合法占位。三张 Ticket 是施工切片数，不替代业务交付数量。12 项 AC 全部纳入本 Goal。

### Success and False Completion

成功需要全部 AC 的真实行为证据、三个非空实现 commit 与 direct-parent result、同一最终源码的构建/当前 OpenAPI/真实本地 release、隔离新库与旧数据保留验证。只删菜单、只改前端校验、只让 fixture 通过、仅填写 Evidence、只把 Ticket 标为 done，均不能关闭 Goal。

### Non-goals

Go/Python 新平台归独立暂缓 change；不删除/迁移真实旧数据，不给空手机号强制登录补录，不新增手机号唯一索引，不改邮箱/短信认证，不删除 SnailJob、MCP 或 NAMEWTA OpenAPI。不部署、推送或关闭 GitHub Issue；本轮已获本地实施与提交授权（LOG-011）。

### Authoritative Inputs

| 优先级 | 来源 | 负责内容与冲突处理 |
|---|---|---|
| 1 | 用户已确认决定，记录于 <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/LOG.md</Path> LOG-003–007 | 手机号、暂缓、数据保留、最终范围和 current；新决定先更新真正 owner |
| 2 | 本 change 的 <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/CONTEXT.md</Path> 与 <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/ADR.md</Path> | 领域与架构边界；无额外接受的 ADR，不补造决定 |
| 3 | Spec 与三张 Ticket / Map revision 8 | 外部合同与局部施工；Goal 只编排，不改写 AC |
| 4 | <Path>AGENTS.md</Path>、匹配的项目 Skill、当前源码和 <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/grounding.md</Path> | 约束与实际调用/路径；摘要冲突以真实代码为准并修正父事实 |
| 5 | 独立来源快照与 <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/issue-index.md</Path> | #1/#2 当前归属，#3 已转移；冻结来源不改写 |

适用模式：migration 是消费者先退出再收缩源码/生成合同；high-assurance 针对账号、权限与数据保留；release-coordination 针对同源前后端/SQL/manifest。没有真实环境升级窗口或外部标准符合性声明。

## 2. Execution Graph

### DAG and Critical Path

```text
G-plan ── T-01 ── G-phone ──┐
        └ T-02 ── G-ai ────┴── T-03 ── G-contract ── G-final
```

真实依赖是 T-03 依赖 T-01、T-02；前两票没有业务依赖。current 策略固定实际顺序 T-01 → T-02 → T-03，不把调度顺序伪造为 blocked_by。每票集成与 required E2E 通过后才开始下一票。T-03 是共享合同汇合点。

### Waves and Ownership

| Wave | Ticket | 前置条件 | 项目写路径与 shared owner | 集成序号 / Gate |
|---|---|---|---|---|
| W1 | T-01 | G-plan 通过 | 手机号 DTO/写入/表单/导入，公开注册映射由 T-01 独占 | 1 / G-phone |
| W1 | T-02 | G-plan 通过；当前 writer 空闲且 T-01 已验收 | AI 业务模块、Admin 组合/导航与 lock；受管 local 配置仅 Snail AI 段，owner T-02 | 2 / G-ai |
| W2 | T-03 | T-01、T-02 的 result 与证据通过 | 根 POM、独立服务、release、40/50 SQL、当前 API 生成物及父 Skill 事实，owner T-03 | 3 / G-contract、G-final |

准确 writable/read-only/shared 列表由各票 frontmatter 掌握。T-01/T-02 只交回 API/SQL 差异，不修改 T-03 所有文件。W1 只是候选集合，不授权同时写入。

### Ticket Quick Reference

| ID | 可观察产出 | Dependencies | Workspace | Implementation owner | E2E disposition | Evidence |
|---|---|---|---|---|---|---|
| T-01 | 手机号写入有效、旧空号可登录 | — | current | codex-root；执行时可动态委派单一 writer | required：注册/资料/权限及存量登录 | <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/evidence/T-01.md</Path> |
| T-02 | AI 页面/桥退出、两个 Java 占位 | — | current | codex-root；同上 | required：旧菜单、未知菜单诊断、无旧请求/iframe | <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/evidence/T-02.md</Path> |
| T-03 | 最终无 AI 服务/产物，新库无 vendor 表且旧数据不变 | T-01, T-02 | current | codex-root；同上 | required：最终浏览器、真实隔离 MySQL 与保留服务启动 | <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/evidence/T-03.md</Path> |

上述 Evidence 路径是未来执行输出，当前未创建空文件冒充结果。

## 3. Gates and Completion Evidence

### Overall Definition of Done

全部 AC-001–012 通过；每个非 cancelled 票有非空 implementation commit、相应 source SHA、parent_before、Lead 验收与 result_sha；三个 result 包含关系可回读。最终两占位/六 SQL 数量准确，当前 OpenAPI 有真实后端来源，full/core 保留原选择，实际 prod/full/all release manifest 经现有 verify 校验。最终 required 测试实际执行、零未经批准 skip，数据哨兵摘要不变。

状态、Map、Goal、Evidence 与 Git 一致，无未验收 checkpoint 或高影响偏差。无改动票应 cancelled 并记录事实，不做 empty commit。关闭本地 Goal 后远程 reconcile 仍需独立明确授权，不自动关闭 #1/#2，更不关闭暂缓 #3。

### Gates

| Gate | 开启条件 | 关闭证据 | 阻塞范围 | Lead/批准人 | 失败恢复 |
|---|---|---|---|---|---|
| G-plan | Ready Spec、三票与 Map，用户要求开始 run | 真实 run/Deep 票批准、commit/direct-parent 授权；基线/owner/Skill 摘要与命令可用性复核，明确保护用户原改动 | 全部实施 | codex-root；权限来自用户 | 保留计划和阻塞原因，不自动派单 |
| G-phone | T-01 非 E2E 通过且有授权 commit | AC-001–005 的真实 validator、HTTP/导入、浏览器与旧号登录；权限/开关回归；direct-parent result | T-01及后续串行调度 | codex-root | 当前票修复，重新验证受影响合同 |
| G-ai | T-02 非 E2E 通过且有授权 commit | AC-006–008：两个退役键过滤、无关诊断保留、旧桥失效、两个占位、受管配置和 App 构建 | T-02及 T-03 | codex-root | 保留失败 checkpoint，不能仅删除测试 |
| G-contract | 两个前置 result 已验收，T-03 最终来源固定 | 正式 fetch/generate/check、SQL 六槽位、真实新库无 sai_*、旧库哨兵摘要一致、父事实更新 | T-03 / G-final | codex-root | 回退到具体合同 owner，不碰真实库 |
| G-final | T-03 最终 commit、G-contract 通过、全仓可归档源干净 | 全部受影响门禁；同一源码实际 release ID/manifest/source/tree/archive 摘要与 verify；三票证据聚合和数量核验 | Goal 完成 | codex-root | 不 stage/部署；保留产物与错误，重建新候选 |

### Contract and Reference Coverage

| 合同 | 覆盖 Ticket | 验证接缝 | Evidence | 当前状态 |
|---|---|---|---|---|
| AC-001–005 | T-01 | S1/S2，真实校验、写入无副作用、页面与旧登录 | T-01 执行 Evidence / acceptance JSON | passed，result ccd9d98 |
| AC-006–008 | T-02 | S1–S4，退役导航/桥、包依赖、占位构建 | T-02 执行 Evidence / acceptance JSON | passed，result b394c60 |
| AC-009–012 | T-03 | S1–S5，生成合同、双 bundle、真实发布与隔离数据 | T-03 执行 Evidence及 G-final 聚合 | planned，未运行 |
| 项目硬约束与明确交付数量 | 全部，T-03 最终聚合 | 每票真实 Skill 调用、owner/回归、2/6 实物核验 | 各票 phase/operation/hash 与最终清单 | 绑定已核验，实施调用未运行 |

具体命令采用 Ticket 的验证矩阵；前端统一 frontend cwd 的 corepack pnpm 10.34.5，release 裸 pnpm 通过任务专用 shim 固定。release 合同 fixture 不能代替 T-03 §8 的真实 build + verify 路线。

## 4. Execution and Integration Protocol

### Lead Orchestration

| 项目 | 决定 | 事实依据 |
|---|---|---|
| Lead | codex-root | 当前会话；唯一 SpecDev 状态/Evidence/父分支验收 owner |
| Implementation subagents | 上限 1，Lead 不计入；Lead 实施时不再有其他 writer | config 上限 3、宿主 4 个槽位；用户 current 严格串行选择进一步收紧 |
| Integration attempts | 每票 3 | 创建时 config 的 max_integration_attempts=3 快照 |
| Read-only agents | 无 SpecDev 数字上限，受宿主能力约束且不竞争可变测试资源 | review/research/test-observation 只读 |
| Dispatch | execution-time dynamic | provider/模型/具体实现者在执行期按 Ticket 选择，不在本计划预分配 |

已按 <Path>{roots.workflows}/specdev/common/skills/subagent-delivery/SKILL.md</Path> 的 operation=plan 形成合同，记录于 <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/evidence/planning-review.md</Path>；未派遣实现。

未来 Dispatch Packet 必含 Map→Skill→Ticket 读取顺序、真实 base/result、授权范围、单 writer 租约、writable/shared owner、Skill 实际 hash/phase/operation、非 E2E 命令、停止条件和返回格式。子代理不写 SpecDev 或授予权限，不直接运行 Lead 所有的 E2E。Lead 验收后才写正式 Evidence/状态。

### Ticket Workspace and Integration

| Ticket | Parent/base | Workspace/branch | Source checks | Implementation commit | Integration checks/E2E | Parent result |
|---|---|---|---|---|---|---|
| T-01 | main / dea1754 | 当前仓库/main | 通过，见 T-01 Evidence | ccd9d98（初始241a96a，审查修复后固定） | G-phone passed，28服务/10浏览器最终复核 | ccd9d98 |
| T-02 | main / 2853e45 | 当前仓库/main | 通过，见 T-02 Evidence | b394c60（初始51a9ad6） | G-ai passed，MVC2/浏览器9 | b394c60 |
| T-03 | T-02 result，且包含 T-01 | 当前仓库/main | 本票及最终生成/构建合同 | 尚无；必要的后端来源与最终生成提交均记录 | Lead 当前工作区 G-contract/G-final | 最终已验收 implementation commit |

严格单 writer，不创建 source/candidate worktree。每票实现者先运行非 E2E，取得具体提交授权后形成 commit，Lead 在相同 main/current-workspace 执行集成和 required E2E。授权提交本身会移动当前 HEAD；失败时不标 accepted、不开始下一票、不擅自 reset，保留该未验收 checkpoint并修复。不得照搬 candidate 模式“父 HEAD 始终未动”的表述。

Lead 比对验收前后 HEAD 和工作区内容，result 必须对应实际受测 commit。验收导致源码变化则原证据失效，需重新提交并重跑受影响检查；不得把未提交修复附着在旧 result。纯 Evidence 更新记录其实际状态，不更改受测产品源码。

### Authorization Matrix

| 动作 | 状态 | 目标与条件 |
|---|---|---|
| 本次 S/T/P 文档、只读调查与规划校验 | allowed | 用户最终共识和当前工作区选择；仅规划工件 |
| Current workspace Ticket changes | allowed | LOG-009：用户明确要求 I 执行并完成 change，覆盖三张既定 Deep 票；Lead 当前唯一writer |
| Implementation commit | authorized | LOG-011：本 change 基线与三票具体本地提交 |
| Local direct-parent verification and parent update | authorized | LOG-011：三票在 main 本地提交并由 Lead 验收 |
| Push / PR / remote merge / close Issue | not-authorized | 没有远程写入授权，remote #1/#2/#3 保持原状态 |
| Branch/worktree cleanup | not-authorized / 本策略无需 | 不创建工作树，也不清理其他 change |
| Deploy / migration / production actions | not-authorized | 不属于本 Goal 的本地实现验收；另行固定目标和具体批准 |

本地实现、测试、main 提交和 direct-parent 验收依据 LOG-009/011 已获授权；远程/生产边界保持独立。

### Evidence Return

返回 base/source/implementation commit、实际 dirty/文件范围、调用过的 Skill 及摘要、每条命令 cwd/工具版本/退出码/实际测试与 skip 数、失败类别、未验证项和可恢复 checkpoint。Lead 独立核对 Git 与关键行为，补集成/E2E、parent_before、result_sha 与包含关系；current 模式 candidate 字段不适用，不伪造值。

## 5. Constraints, Risk and Recovery

### Non-negotiable Constraints

用户原有 AGENTS/config/validator 改动及已删除的 validator test 不属于本 change；保护两个其他活动 change、冻结来源、历史 OpenAPI revisions、历史 release、真实数据库和私有环境配置。T-02 对受管 application-local.yml 只可删除 Snail AI 段，不能以“local”文件名漏掉源码残留，也不能改其其他内容。

手机号更新先合并再验证，不用全局必填注解破坏省略/null兼容；显式空白拒绝；旧空号可登录。仅过滤两个明确退役菜单键，保留无关未知菜单诊断。六文件槽位和来源验证不可削弱，历史数据不删除也不迁移。

### Verification Integrity

不通过移除失败测试、空 validator、降低 fixture/安全断言、零用例、未经批准 skip 或借旧 change 结果获得绿色。每票中间点也必须本票可构建、行为可验证；若生成合同滞后使其失败，先 replan owner/切片，不等 T-03 悄悄补救。

规划只核验脚本/来源/工具链，未运行产品门禁。当前工作树 validator 在 stage goal-plan 无条件要求多 change 的 implementation-map/plan，实际 exit 1（2 errors）。已定位为本轮开始前的用户修改；保持原样。当前 validator 的无 stage 工件检查与 Git HEAD 原版的 stage goal-plan 均 exit 0，独立 review pass；不能把它们写成当前 stage 命令已通过。当前 config 的 gate_fixtures 引用了用户已删除的 validator test；执行前必须让该改动所属 owner 提供有效门禁或恢复依据，不擅自还原、不删命令宣称通过。完整 release 的 clean_source 还要求受管修改和非忽略未跟踪文件均为空，包含待处理的用户改动与规划文件；未解决时阻塞真实构建，不偷偷 stash 或打包脏树。

### Migration or Release Sequence

T-01 固定手机号合同；T-02 退出 AI 消费者并保留两个 Java 占位；T-03 收缩独立服务与发布接入、统一 SQL/当前 OpenAPI、更新事实，再由 Lead 验收。前两票旧 OpenAPI 快照仅代表原捕获，不声称最终合同；最终由包含真实后端修改的授权 commit 正式生成，禁止手改历史版本。

新库按 10→20→30→40→50→60 初始化；已有库不重放，隔离旧库场景以哨兵摘要前后相等验证。实际 release 固定 prod/full/all、本地隔离 env、pnpm shim，保存真实版本/manifest并调用现有 verify；不 stage、不切换 current、不运行生产容器。未来真正升级需另设源/目标 Tag、备份、演练与批准。

### Risks, Monitoring and Recovery

| 风险 | 观测与停止 | 恢复 |
|---|---|---|
| 省略更新或导入语义回归 | 有效/旧空号码 × 省略/null/空白矩阵；失败不得落库 | 修复 T-01 写入合同并重跑定向/集成 |
| AI 残留或误删保留能力 | 精确符号/路径、运行请求、JAR/manifest 与 SnailJob/MCP 回归 | 按真正 owner 修复；历史产物与数据保留 |
| 来源、生成物或工具链漂移 | HEAD/Skill hash/OpenAPI 后端commit、pnpm版本、真实manifest校验 | 标记受影响证据 stale，刷新后重验，不改历史证据 |
| 父 HEAD 或用户文件并发变化 | 每票前后核对真实 Git 与写集 | 停止相交资源，协同 owner，重新固定 base；禁止覆盖/reset |
| direct-parent 验收连续失败 | 保存失败命令/退出码/未验收 commit；每票最多3次 | Lead 记录根因、新证据、下一轮变化与 owner 后才可重置 attempts |

只有 Lead 可验收或恢复状态；无有意义变化不重复派单。current 串行顺序下，受影响票未验收前不开始下一票，可继续只读调查。未授权修改/提交/外部副作用仍停止在具体可审查结果处。

### Deviation Control

按 <Path>{roots.workflows}/specdev/common/rules/deviation-control.md</Path> 回到真正 owner：产品决定回 G/Spec；局部写集、测试接缝与 Skill 变化回 Ticket/Map；调度/授权回 Goal。记录偏差、影响 AC 与依赖闭包，递增 Map revision 并重跑控制/校验。不得以实现便利扩展到 Go/Python 或真实数据迁移。

## 6. Progress and Decisions

### Current Status

S Ready，T-01 done / G-phone passed，固定 result ccd9d98；T-02 done / G-ai passed，result b394c60；T-03 Ready。Map revision 8；T-03 正在串行实施。G-contract/G-final 尚未完成。

规划质量与实际 validator/controller 结果归 <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/evidence/planning-review.md</Path>；它不能代替三张执行 Evidence。Go/Python 仍在 <Path>{roots.state}/specdev/changes/2026-09-19-go-python-ai-platform/</Path> 暂缓。

### Pending Decisions and Blockers

产品范围无待决定项。本地实施与提交出口授权已由 LOG-009/011 确认，受保护原改动按用户方案保留；validator 四处路由与缺失的 11 场景测试已恢复并通过。剩余工作是逐票不可变检查点、双轴审查和正式产品/发布验收。

### Resume Protocol

以后从 Map 进入 P 的 run/resume：回读本计划、Spec、当前 frontier Ticket、状态和最新 Evidence；只读运行 ticket-control，核对 config/Skill摘要、main HEAD、工作区与授权来源。从真实已验收 result 或未完成 checkpoint 恢复；当前首次 frontier 为 T-01，满足 G-plan 前不派单。用户已答过 current，不重复询问，不自动打开新工作树，也不接管另外两个 change。

## Assumptions

仅沿用已核验的 wta 命名、现有大陆手机号格式、错误壳与六文件初始化顺序。环境版本与脚本在执行前重新探测；无未确认产品假设；本地执行授权见 LOG-011，部署与远程写入仍不属于授权范围。

### 已验收检查点

T-01 result `ccd9d98fe288fc90a16de25d1145a60c7222827f`，前后 HEAD/tree/clean 和最终28+10测试见 acceptance JSON。按 LOG-012，后续收据提交与下一票推进只要求历史 result 仍为父分支祖先，不更改旧 result；最终 completed/release 继续全仓 clean。
