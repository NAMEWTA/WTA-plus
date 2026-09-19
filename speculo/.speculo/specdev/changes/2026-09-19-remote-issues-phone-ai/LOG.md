# Change Decision Log

## LOG-001 — 2026-09-19 — 一个 change 覆盖三条来源

- **设计树节点：** 不适用；用户在启动指令中已经明确。
- **轮次与依赖：** 启动 / 无。
- **状态：** confirmed。
- **问题：** 三条来源是否拆成三个 change。
- **事实与来源：** <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/source.md</Path> 中的“都在这个change里进行解决”。
- **选项：** Triage 默认逐条 intake 各建 change；用户本次选择聚合。
- **推荐：** 保留一个 change，并逐条独立冻结来源。
- **结论：** 本 change 纳入捕获时 remote 的全部三条 open Issue。
- **原因：** 用户最新明确决定优先；无需再次确认已说明的组织方式。
- **影响工件：** Source / Triage / Spec / Ticket / Goal Plan。
- **约束或不变量：** 每条来源 locator/hash 保留；新 change 不继承旧 change 的授权。
- **后续：** 在本 change 内建立独立验收切片；不自动关闭远程 Issue。
- **替代/被替代：** 替代每条 capture 各建 change 的本次默认；不修改 workflow。

## LOG-002 — 2026-09-19 — 串联 Work 与事实探索

- **设计树节点：** 不适用；用户已指定串联。
- **轮次与依赖：** 启动 / LOG-001。
- **状态：** confirmed。
- **问题：** Triage 后的执行顺序。
- **事实与来源：** <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/source.md</Path>。
- **选项：** 用户已指定 Triage → G → S → T → P。
- **推荐：** 按指定顺序推进，高影响决定由 G 访谈，事实由 Agent 查找。
- **结论：** 无需逐 Work 询问启动许可；G 仍须真实答案与明确共识，之后才允许 Ready Spec。P 默认 plan，产品实施及提交不由规划派生。
- **原因：** 串联授权与决策完备是两个独立条件。
- **影响工件：** design tree / Spec / Ticket / Goal Plan。
- **约束或不变量：** 未决高影响问题不得默认采用推荐答案。
- **后续：** 第一轮询问完整 frontier；答案进入新 LOG 条目。
- **替代/被替代：** 无。

## Round 1 — 已达成共识

本轮开始时完整 frontier 为 D-001、D-002、D-003；均无未关闭前置依赖。三项均已获回答，见 LOG-003/004/005。用户将 Go/Python 明确分出并暂缓，相关下游管理面、模型、协议与部署问题因此不属于本 change；不在本次继续追问。用户随后明确确认共识，见 LOG-006；当前 frontier 为空，连续进入 S / T / P。

事实来源：<Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/grounding.md</Path>。根节点答案会决定哪些后续分支适用。低影响命名沿用现有 wta 惯例；没有全仓 cde 重命名要求。

## LOG-003 — 2026-09-19 — 手机号写入必填与登录兼容

- **设计树节点：** D-001。
- **轮次与依赖：** round 1 / 无。
- **状态：** confirmed。
- **问题：** 手机号必填后，已有空手机号账号何时必须补齐；更新省略字段是否保留号码。
- **事实与来源：** 用户对 Q1 明确回答“按推荐：写入时补齐，保留登录”；所引用提问明确包含省略字段保留已有号码、禁止清空。
- **选项：** 写入时补齐并保留登录；上线前全部补齐；仅约束新用户。
- **推荐：** 新增必须填，存量在相关写入时补齐，保留登录。
- **结论：** 新注册、新建用户、新增导入必须有手机号；存量空手机号用户仍可登录，在管理员编辑、个人资料保存或覆盖导入时必须补齐。更新省略手机号可沿用已有号码，禁止显式清空；省略但当前也为空时不能据此绕过必填。
- **原因：** 满足手机号必填目标，同时保留既有登录与部分更新语义。
- **影响工件：** 后续 Spec / Ticket / 验收合同；本节点属行为兼容决定，不强行创建架构 ADR。
- **约束或不变量：** 本决定不增加邮箱必填、短信验证或强制登录补录流程；不改变已确认的单 change 范围。
- **后续：** S 覆盖缺失、空白、格式错误、已有有效值省略保留、新/旧账号写入，以及存量登录不受阻；同步相关初始化与成功测试数据。
- **替代/被替代：** 无。

## LOG-006 — 2026-09-19 — 最终共识与 S/T/P 串联

- **设计树节点：** 全部适用节点 D-001/D-002/D-003。
- **轮次与依赖：** round 1 完成 / LOG-003、LOG-004、LOG-005。
- **状态：** confirmed。
- **问题：** 修订后的最终范围是否达成共识。
- **事实与来源：** 用户对最终范围核对明确选择“确认共识，继续 S → T → P（推荐）”。
- **结论：** 手机号按已确认写入策略必填、保留旧用户登录；完整移除 Snail AI 代码、页面/菜单、依赖、服务及发布接入，保留 wta-ai / wta-common-ai 的 Maven 占位；旧 AI 数据保留不迁移，六份 SQL 文件保留，AI 槽位不创建 vendor 表；Go/Python 已另建暂缓 change。
- **原因：** 所有适用高影响决定均有明确答案。
- **影响工件：** design tree 标记 consensus；G 完成，连续编写并校验 Spec / Ticket / Map / Goal Plan。
- **约束或不变量：** 本次只授权规划链；不继承其他 change 的实现提交、推送或部署授权。
- **后续：** 完成 S / T / P；P 的 workspace 选择单独记录到本 change。
- **替代/被替代：** 关闭原共识等待项。

## LOG-007 — 2026-09-19 — 当前工作区串行

- **设计树节点：** 不适用；P 的执行拓扑选择。
- **状态：** confirmed。
- **事实与来源：** 用户明确选择“不开启：当前工作区串行（推荐）”。
- **结论：** 本 Goal 使用 ticket_workspace_policy=current、integration_gate=direct-parent，严格串行、单一 implementation writer；不创建 source/candidate worktree。
- **约束或不变量：** 本选择不授予实现提交、推送、发布或数据操作权限；只写本 Goal，不修改全局配置。

## 规划校验记录 — S 首次校验

实际执行 stage spec 返回 exit 1：其一，未决问题章节必须严格为“无。”，已按真实无未决状态修正文案；其二，当前工作树校验器在 stage spec 无条件要求 Ticket 覆盖，见 <Path>{roots.workflows}/specdev/common/tools/validate-specdev.mjs</Path> 的 declaredContracts/coveredContracts 检查。此时 T 尚未写入，产生全部 AC 未覆盖错误。

处理：保留用户已有 validator 改动，不改门禁、不生成虚假覆盖。Spec 先通过人工 readiness，接着准备用户已授权的真实 Ticket 草稿；有真实覆盖后重跑 S，再完成 T Ready review。最终 S/T/P 全部须实际 exit 0，当前不提前记 S 完成。

## LOG-004 — 2026-09-19 — Go/Python 分出并暂缓

- **设计树节点：** D-002。
- **轮次与依赖：** round 1 / 无。
- **状态：** confirmed。
- **问题：** Go/Python AI 第一版在本 change 要交付什么。
- **事实与来源：** 用户回答“这个先不做，另开一个change，等我想清楚”。
- **选项：** 最小对话链路、仅骨架、完整平台；用户选择本次不做，另立暂缓 change。
- **推荐：** 已被用户修订，不再采用最小对话链路推荐。
- **结论：** #3 转入 <Path>{roots.state}/specdev/changes/2026-09-19-go-python-ai-platform/</Path>，blocked 等用户主动恢复；本 change 仅处理 #1 和 #2。
- **原因：** 用户尚未锁定构想，明确要求延期。
- **影响工件：** 两个 change 的状态、Triage、来源索引、后续 Spec / Ticket / Goal Plan。
- **约束或不变量：** 不创建 Go/Python 服务、管理界面、模型接入或相关实施票；本 change 最终 Java AI 模块为占位。
- **后续：** 独立 change 的恢复入口为 G，原来源快照只作审计。
- **替代/被替代：** 替代 LOG-001 中“三条都在本 change 开发”的范围；手机号与 Snail AI 仍在本 change。

## LOG-005 — 2026-09-19 — 旧 Snail AI 数据保留且不迁移

- **设计树节点：** D-003。
- **轮次与依赖：** round 1 / 无。
- **状态：** confirmed。
- **问题：** Snail AI 退出后的历史数据处置。
- **事实与来源：** 用户选择“保留旧数据，不迁移（推荐）”。
- **选项：** 保留不迁移、迁移到新 AI、可弃用。
- **推荐：** 保留旧数据，本次清理源码和初始化内容，不执行真实库删除。
- **结论：** 原有模型配置、会话和知识库等数据保留，不迁移。新初始化不再创建/填充 Snail AI vendor 表；已有运行库不重放初始化基座、不删除其历史数据。
- **原因：** 删除产品运行面与删除用户数据是不同合同。
- **影响工件：** 后续 Spec / Ticket / 发布兼容与验收。
- **约束或不变量：** 无运行库删除或自动迁移；现有六文件基座合同保留，AI 文件可为不含 vendor SQL 的合法占位。
- **后续：** 通过隔离新初始化和已有数据保留验证，证明新产品不再依赖旧表；真实部署仍独立授权。
- **替代/被替代：** 无。


## LOG-008 — 2026-09-19 — S/T/P 规划交付与验证限制

- **状态：** planning-complete；执行未启动。
- **结果：** Spec Ready，3 张 Deep Ticket Ready，Map revision 2，Goal 固定 current/direct-parent、codex-root、implementation_agent_limit=1、integration_attempt_limit=3。12 AC、2个Java占位、6份SQL均有计划覆盖。
- **质量评审：** 受管 local 配置与真实 release 验证两个 block 已修复并独立复审 pass；Goal 最终 review pass。
- **验证限制：** 当前工作树 stage goal-plan 因用户已有 validator 修改误要求跨change工件，exit 1/2 errors。保留该失败；当前无stage全工件校验、已提交 HEAD 原版 stage goal-plan、只读 controller 均 exit 0。具体证据在 <Path>{roots.state}/specdev/changes/2026-09-19-remote-issues-phone-ai/evidence/planning-review.md</Path>。这修正前文“最终S/T/P全部实际exit0”的预期，不伪造当前P命令通过。
- **边界：** 本轮只规划；没有 run/Deep票实施/commit/direct-parent集成授权，不写产品、不提交、不操作远程；Go/Python change 继续暂缓。Goal blocked 是执行条件而非产品未决。
- **后续：** 用户未来要求实施时从 Map 恢复，先核对真实授权、基线/用户改动与校验器/fixture有效性。


## LOG-009 — I 执行授权与可审查交付

用户明确激活 I-implement 并要求完成本 change，授权既定三张 Deep 票的本地实现、测试与修复；current 串行策略不变。实现 commit、受保护既有改动处理、远程写/部署仍单独核对。先完成已授权的可逆本地实现和检查，形成具体 diff，再在实际提交动作前确认。不能将缺 commit 授权理解为禁止准备可审查结果；不会在缺授权时提交或标 Done。

Lead codex-root 为唯一产品 writer，phone_facts 和 ai_facts 仅只读 research，派单固定当前 HEAD，返回事实由 Lead 独立验收。


## LOG-010 — T-01 本地实现完成与授权恢复点

T-01 的本地修改、定向 red/green、全量测试、真实 MySQL、full/core 打包及 34 个浏览器用例完成，详见 Evidence。独立工作区静态预检的纯空白手机号缺陷已通过独立页面 red/green 修复；401 浏览器既有夹具改为必然触发的受保护 profile 读取。所有用户基线改动保持原 hash/删除状态，HEAD 未变。

当前唯一阻塞是已经发出的 preflight-proposal 异步确认：恢复 validator 四处路由及 11 场景测试、保留原 AGENTS/config 变动形成基线，再逐票本地提交。未收到授权，不提交、不标 Done；T-02/T-03 保持 Ready 未启动。用户无需重新决定产品范围、worktree 或 Deep 票实施；后续只恢复此具体授权步骤。


## LOG-011 — 本地基线与逐票提交授权

用户明确答复“同意该方案与本地提交（推荐）”，批准 evidence/preflight-proposal.md 的具体范围：恢复 validator 四处路由和原 11 场景测试；保留 AGENTS 空行/config 键重排和本轮规划工件形成基线；随后各票在 main 本地提交并验收。授权时间 2026-09-19T10:33:11.939574+00:00。不包括 push、部署、真实数据操作或远程 Issue 写回。

已核验 29 个 T-01 源文件摘要未变；按提案应用补丁，validator 回归 11 pass / 0 skip，已恢复与原 HEAD 一致的工具字节。
