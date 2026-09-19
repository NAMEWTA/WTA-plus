# Planning Review

本文件记录规划过程与真实工具结果，不是实现 Evidence。当前未运行产品测试、未修改产品代码、未提交或发布。

## 输入与范围

- G 共识：LOG-006；工作区选择：LOG-007，位于 <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/LOG.md</Path>。
- Spec：<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/spec.md</Path>。
- 三份真实 Ticket：<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/ticket/</Path>；Map：<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/tickets-map.md</Path>。
- 起始 Git：main，86154daa4c67fa02ac13439c038a09caf5e6308a。工作树有用户先前改动，规划未覆盖。

## Plan Quality Review

按 <Path>{roots.workflows}/specdev/common/skills/plan-quality-review/SKILL.md</Path> 及其 checklist 执行，完整三票已由只读 reviewer 核对，Lead 对实际工件和命令独立复核。

| 检查轴 | 当前结果 | 证据 |
|---|---|---|
| 背景与边界 | pass | 12 AC 映射两项行为；Go/Python 明确独立暂缓，无隐含新AI功能 |
| 账号兼容 | pass | 独立只读评审 AC-001–005；新增、旧空/有效、null/省略/显式空白、登录与导入边界均明确 |
| Skill 调用 | pass（结构/来源） | 真实7入口元数据扫描，匹配的5项按票绑定ID、路径、SHA-256、phase、operation、输入输出与block-ticket；实际实现调用尚未运行 |
| 执行计划 | pass | 三票均有有序路线、正常/失败/回归、Deep恢复与停止点；独立全量review两个block均修正并复审通过 |
| 控制图/资源 | pass（Lead与独立review） | T-01/T-02独立，T-03真实依赖两者；前端组合/lock归T-02，SQL/当前API/release归T-03，current严格串行 |
| 验收与数量 | pass | 12 AC全部有真实票覆盖；Maven占位2个、SQL文件6份明确记录；T数不冒充业务产物数 |
| 权限 | pass（plan） | 只形成文档，run/commit/父分支推进/远程/部署授权不从G或旧change继承；Goal保持不可执行直到条件满足 |
| 恢复 | pass（Lead与独立review） | 真实HEAD/Skill摘要/当前owner，单writer/Lead E2E/direct-parent、停止条件与暂缓change边界明确 |

## Subagent Delivery — operation=plan

调用入口：<Path>{roots.workflows}/specdev/common/skills/subagent-delivery/SKILL.md</Path>。

- **输入：** operation=plan；Lead=codex-root；允许的任务类型 implementation/review/research/test-observation；当前策略 current/direct-parent；计划 implementation_agent_limit=1（config允许3，current实际单writer）；integration_attempt_limit=3。
- **授权边界：** 当前只规划和只读review。implementation类型只是后续可用派单类型，未派遣生产实现、未创建worktree、未授权commit。
- **规划产出：** Lead独占SpecDev、Evidence、父分支与E2E；implementation在明确run/commit授权后仅写当票writable/sharedowner路径；其他任务只读；provider/模型/具体agent由执行期动态决定。
- **派单必需输入：** 当票/Map/Goal、已验收依赖、真实base SHA、main/current locator、单writer锁、Skill绑定摘要、允许动作、路径与非E2E检查、停止条件和候选返回格式。
- **返回与验收：** 实现者返回commit/dirty/实际路径/命令退出码/未验证项；Lead回读真实Git与输出，在current-workspace执行集成/E2E，再记录direct-parent result。subagent不能自写Evidence或授予父分支推进权限。
- **恢复：** 重复失败或3次集成尝试达到上限时，Lead先记录失败模式/原因/下一轮变化/owner，才能重置计数。不得无变化反复派回。
- **结果：** plan合同已形成；没有实际implementation dispatch/accept结果。

## 实际验证记录

命令均从项目根运行，frontend工具版本探测明确使用frontend cwd。

| 命令或检查 | Exit | 结果 |
|---|---|---|
| validate-specdev --stage grill（共识后） | 0 | 0 errors / 0 warnings，G四源回读且consensus有用户依据 |
| validate-specdev --stage spec（首次） | 1 | 严格未决问题段格式与提前Ticket覆盖错误；已保留原失败，不改validator |
| validate-specdev --stage spec（真实Ticket草稿补齐后） | 0 | 0 errors / 0 warnings，12 AC有真实覆盖 |
| validate-specdev --stage tickets（draft结构） | 0 | 0 errors / 0 warnings；并非Ready/实现通过声明 |
| frontend cwd: corepack pnpm --version | 0 | 10.34.5；系统默认9.15.9不用于本计划执行 |
| Node / Java / Docker只读版本与可用性 | 0 | Node24.21.0、Java21.0.12.1、Docker29.7.2 |

最终结果如下；未重复无关产品构建。

## 完整三票复审

只读 reviewer ai_facts 发现并确认闭合两项：B1 受管 application-local.yml 的 Snail AI 段明确归 T-02，其他内容保护；B2 T-03 增加真实 prod/full/all 本地 release 构建、任务专用 pnpm shim、隔离 env、实际版本/来源与现有 verify() 校验，不通过 stage 取巧。Lead 回读源码并修正后，独立复审 pass，无剩余规划 block。另补齐 mkdir shim 目录、clean_source 的受管与非忽略未跟踪文件必须为空的实际要求。

三张票发布 Ready，Map revision=2；Ready 是计划可实施的质量状态，不构成 run/commit 授权。P 最终编排审查与只读控制在生成 Goal 后记录。

## P 最终规划质量与校验器限制

Lead 与只读 reviewer ai_facts 完整复审 Goal：pass，背景、DAG/单writer、shared owner、12 AC、2/6数量、Deep批准、current提交后失败保留checkpoint、Lead E2E、授权与恢复均闭合。当前没有规划设计 block；run 尚未授权，Goal blocked/ready_for_execution=false 符合本轮仅plan。

实际工作树命令 `node speculo/workflows/specdev/common/tools/validate-specdev.mjs --stage goal-plan --repo <project-root> <change-directory>` 返回 exit 1，2 errors / 0 warnings：要求 implementation-map.md、implementation-plan.md。定位到 validateParentImplementation 的 `required = stage === "goal-plan"` 无条件分支；Git diff 证明该变化及删除单change guard属于本轮开始前用户改动。此处是单 change，不创建虚假的跨change工件，不修改用户校验器。

补充复核，不替代或涂改上述失败：

- 当前工作树 validator 不指定 stage，回读本 change 全部存在的 Spec/Ticket/Map/Goal/状态：exit 0，0 errors / 0 warnings。
- 从实际 HEAD 86154daa4c67fa02ac13439c038a09caf5e6308a 用 git show 逐字节提取 validate-specdev.mjs 与 plan-contract.mjs 到系统临时目录，未改任何校验代码、未覆盖工作树；用该已提交版本对相同 change 运行 stage goal-plan：exit 0，0 errors / 0 warnings。
- 提取版 SHA-256：validate-specdev.mjs 为 caaa167d4ab25b0b1001fdd3463e1189745aca6531ba7ecce83325dbe36ce3a5；plan-contract.mjs 为 ab1cd29d6228a4d26ec7959741ca849d0a6b114fc419b3c16814d3470f45b1fb。提取命令为 `git show <上述40位HEAD>:speculo/workflows/specdev/common/tools/<文件名>`；运行 `node <任务临时目录>/validate-specdev.mjs --stage goal-plan --repo <project-root> <change-directory>`，临时目录不属于 Ticket worktree。
- 当前只读 ticket-control：exit 0，validation_errors/errors/warnings 为空；workspace_policy=current，implementation_limit=1。frontier 为空是 Goal 未获执行授权；T-03 另受两项依赖限制，无失效票；输出不核实授权、不执行动作。

规划发布与产品执行分开记录。当前 stage goal-plan 的工具限制仍开放，交回该用户改动的 owner；不宣称所有当前命令绿色。产品测试、真实 release、提交、部署、Issue 关闭均未执行。

## 发布前最终回读

| 检查 | Exit / 结果 |
|---|---|
| 当前 validator --stage spec | 0；0 errors / 0 warnings |
| 当前 validator --stage tickets，三票 Ready | 0；0 errors / 0 warnings |
| HEAD原版 validator --stage goal-plan，同一最终工件 | 0；0 errors / 0 warnings；当前版P失败仍保留如上 |
| 当前 validator 无stage，全工件 | 0；0 errors / 0 warnings |
| 当前 ticket-control --map ... --repo ... | 0；无errors/warnings，current，上限1，无可派单frontier |
| 暂缓 change --stage triage / capture账本 | 均0；0 errors / 0 warnings |
| 来源与 Skill SHA-256 | 5份来源、5个唯一实际Skill入口全部一致 |
| 状态回读 | 五个规划Work均已记录、current_work=null、change active、worktrees为空；3票Ready；暂缓change blocked |
| Git | diff --check exit0；HEAD未变；未编辑原有用户修改、未产生产品源码改动或实现commit |

控制器原始结构化结果：<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/evidence/planning-controller.json</Path>。恢复时重新运行，不把本次digest永久视为当前事实。
