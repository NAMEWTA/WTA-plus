以下按 DISPATCH.md 的 Return format 输出。本轮可以完成外部工件/证据审计，但不能满足附件中 t155u 所要求的 GPT-6-Pro 外脑门禁：本会话实际模型是 GPT-5.6 Sol，附件里的 Provider 字段不能改变实际模型身份。因此下面结论应作为 advisory / non-gate review，不能据此把 C-code-review 标记为合规完成。

2026-09-10-notify-channel-config — C-code-review 外部审计
Summary

结论：保持 C-code-review = BLOCKED；不得归档，不得把 CR-001 finalize 为通过。

当前 reviews/CR-001.md 将状态保持为 blocked 是正确方向，但现有阻塞不只是 GPT-6-Pro 外脑门禁。即使后续换成满足 t155u 的模型重跑，当前 change 仍至少存在若干需要先 reconcile 的证据与治理问题。

本次审查最重要的结论如下：

级别	结论
BLOCKER	当前会话实际为 GPT-5.6 Sol，不满足 .status.json / CR-001 中 GPT-6-Pro 的 CTO 门禁。本回复不能解除 t155u-await-gpt6-pro-rerun。
P0	CR-001.fixed_point = 8680fe6...，而 Ticket Evidence 又把 8680fe6... 描述为 historical implementation commit / 实现祖先。若 feature 实现确实落在该 commit，则以它作为 fixed point 会把该实现 commit 本身排除在 git diff fixed...head 之外。必须先澄清 commit provenance。
P0	外部包硬排除了 implementation source tree，同时没有携带实际 patch/diff 与 commit-log 输出。因此外脑无法真正复核 NotifySendPlanner 等代码 finding，只能审 Evidence/治理。
P0	Goal Plan 的 G0 明确要求每张 Ticket 在 Evidence 记录 “Map → Skill → Ticket” 读取；但 T-01.md～T-06.md 的 Skill Execution Records 全部是 []。按 Goal Plan 自己的 false-completion 定义，这不是可忽略缺口。
P0	六份 Ticket Evidence 均使用 8680fe6... .. 8680fe6... 作为所谓标准轴 fixed input，同时标记 pass。这是空 review input，与 C-code-review / code-review Skill 的“diff 非空，否则不得启动 reviewer”合同冲突。
P0	Goal Plan 要求 T-01～T-06 每票形成独立非空 implementation commit、严格串行，下一票从上一票 result 开始；但 .status.json 与六份 Evidence 全部复用 3a86dfe...、相同 parent_before_sha=b06d161...，并把实现描述为 historical 8680fe6...。现状更像 retroactive/batched close-out，而不是计划声称的六票逐票执行。
P0	Goal Plan 与 LOG-031 明确 push/PR = not-authorized；但 T-01.md 又写 PACKET / OBJECTIVE / Lead 可“覆盖”为 ff push。SpecDev 总合同不允许 Lead/packet 自行提升用户授权。必须查明是否存在后续真实 USER-DECISION。
P1	当前 Spec/Ticket 的路径所有权合同仍大量使用旧 ruoyi-vue-plus-namewta/ruoyi-*、org/dromara 路径，而当前项目 Skill 已切换为 backend/wta-*、org.namewta。这不是只存在于 Spec §9 的一个低级文档问题，Ticket frontmatter 的 writable_paths / read_only_paths 也受影响。
P1	Goal Plan 仍写 status: ready、“尚未开始实现”“无 Ticket implementation SHA”、AC 状态 planned；而 tickets-map 已 completed、Ticket 已 done、.status.current_work=C-code-review。恢复语义已经漂移。
P1	T-04/T-05/T-06 将若干 AC 标成 pass，同时明确对应 Java/Skill 检查 not-run this round；T-01/T-06 required E2E 也没有在 Evidence 中证明完整的无权限 UI/API 403 负路径。历史测试可以复用，但必须绑定到确切 SHA、command、cwd、exit status，而不能把“存在旧测试”直接投影成当前 HEAD 的 pass。

因此，本 change 当前最准确的状态应是：

active / current_work=C-code-review / blocked，且至少需要 “review-input provenance + implementation provenance + evidence reconciliation + authorization reconciliation” 后，才值得重新执行正式双轴 C review。

本次没有发现足够证据支持把 change 改为 completed，也没有足够源码证据支持确认 NotifySendPlanner 的 provisional finding 已被证实或已经修复。

Findings / recommendations (evidence-backed)
P0-1 — C review 的 fixed point 可能直接排除了真正的实现提交

reviews/CR-001.md 当前记录：

fixed_point = 8680fe6b7225950fbe9cdce23d6e38b7e141b189
head        = d1ce37271c865c9b8846aa139e514daa85ef26f5

但是六份 Ticket Evidence 均把 8680fe6... 描述为 historical implementation；AC-AUDIT.md 又把它称作“实现祖先”。

Git 三点 diff：

git diff 8680fe6...d1ce372

不会包含 8680fe6 这个 commit 自身相对于其 parent 引入的修改。

因此首先必须确定：

8680fe6 到底是：
A. feature 实现前 baseline；
B. feature implementation commit；
C. 已包含大部分 feature 的历史 checkpoint。

如果是 B 或 C，那么当前 fixed_point 不足以覆盖真正实现，正式 C review 的 base 应移到该 implementation 之前的 merge-base/parent，或建立一个明确的 change-owned commit set。

建议：在继续 code finding 之前先做 Git provenance reconciliation。不要直接把 CR 的 fixed point 当作已经正确。

P0-2 — 当前 external-brain pack 无法支持真正的代码审查

MANIFEST.md 明确硬排除：

I-implement source trees (implementation stays local)

而本包也没有提供：

git diff 8680...d1ce 的实际输出
git log 8680..d1ce --oneline 的实际输出
sanitized patch
changed-path inventory

CR-001 只记录了应执行的命令以及“diff 非空”的本地声明。

这意味着外部 reviewer 无法独立核实：

NotifySendPlanner.finish()
RedisNotifyQuotaAdapter.tryAcquire
recipient hash key
NotifyConfigService.addAccount/changeStatus

这些具体代码行为。

因此 CR-001 中三个本地 code finding 可以继续保留为 provisional local finding，但本次外脑不能把它们升级为“已外部复核”。

建议：若 GPT-6-Pro 外脑是 C 的硬门禁，下一包至少增加经过脱敏的 immutable patch、commit log、changed-path list。否则应正式定义：external brain 只审 Evidence/process，不负责 code axis。现在两种合同混在一起。

P0-3 — Mandatory Project Skill Gate 没有 Evidence

goal-plan.md §0 明确要求每张 Ticket、每次派单固定读取：

Goal Plan
→ tickets-map Skill matrix
→ applicable .agents/skills/**/SKILL.md
→ ticket references
→ Ticket

goal-plan.md §3 G0 又明确：

关闭证据：Map + Skill + Ticket 已读记录在 Evidence
未读则不准写代码

并且 Success / False Completion 明确把“跳过 Skill 读取”列为 false completion。

但是：

evidence/T-01.md → Skill Execution Records = []
evidence/T-02.md → []
evidence/T-03.md → []
evidence/T-04.md → []
evidence/T-05.md → []
evidence/T-06.md → []

所以目前不存在 G0 的可恢复证据。

这里不应事后伪造“当时已读”。如果历史实现实际上早于 Goal Plan/Ticket，则应承认这是 retroactive reconciliation，并通过 deviation / reconciliation artifact 说明为什么旧实现不可能满足后来新增的 G0 运行记录。

建议：在 Goal Plan 与 Evidence 中显式区分 historical implementation 和 post-hoc close-out verification。不要用空 Skill records 同时维持 Ticket done 和“严格满足 G0”的叙述。

P0-4 — 六份 Ticket Evidence 的“双轴 review pass”输入无效

T-01～T-06 的标准轴都记录了：

8680fe6... .. 8680fe6...
result: pass

这是同一个 SHA。

而 speculo/workflows/specdev/C-code-review/C-code-review.md 和公共 code-review/SKILL.md 均规定：

三点 diff 必须非空；
为空时失败；
不得启动 reviewer / 不得创建通过结论。

因此这些 Ticket Evidence 中的历史：

标准轴 pass
规范轴 pass
Findings 无

不能作为有效代码审查证据。

这与当前 CR-001 使用 8680...d1ce 是两件事。正式 CR-001 可以重新审，但 Ticket Evidence 中这些空输入 pass 应被降级为：

invalid-review-input
或
unverified

而不是继续充当 Ticket Done 的“双轴审查”证明。

P0-5 — Goal Plan 的串行 implementation-commit 合同与现实投影不一致

Goal Plan 明确要求：

T-01 至 T-06 各有非空 implementation commit
current 模式严格串行
result_sha = implementation commit
下一 Ticket 从上一 Ticket result 开始

但当前 .status.json 六张 Ticket 均表现为：

base_sha          = 8680fe6...
source_checkpoint = 3a86dfe...
parent_before_sha = b06d161...
source_sha        = 3a86dfe...
result_sha        = 3a86dfe...

而六份 Evidence 又称：

Implementation owner = historical 8680fe6
本轮 Lead 关闭

AC-AUDIT.md 则明确把 3a86... 描述为：

batched parent HEAD

所以当前实际叙述更接近：

historical implementation
        ↓
一次 batched close-out / tests / Evidence
        ↓
全部 Ticket retroactively marked done

而不是：

T-01 commit
↓
T-02 commit
↓
T-03 commit
↓
T-04 commit
↓
T-05 commit
↓
T-06 commit

这两种执行模式必须选一种真实描述。

建议：不要为了符合原 Plan 补造六个 commit。

如果事实是 retroactive close-out，则修改 Goal Plan/Deviation/Evidence，让它承认 batched historical implementation；如果 Git 中确实存在六段实现提交，则恢复真实 ancestry、每 Ticket implementation SHA 与 parent/result 链。

P0-6 — ff push 的授权来源存在治理冲突

权威计划和决策明确写：

goal-plan.md Authorization Matrix：

Push / PR / remote merge = not-authorized

LOG-031：

不授权 push/PR/部署

.status.json.execution_authorization.implementation_commit.scope：

no push/PR

但是 evidence/T-01.md §8 写：

Goal Plan Push/PR not-authorized 被 PACKET-NCC-CLOSE / OBJECTIVE 覆盖为 ff push
批准来源：... Lead 2026-09-12 明示 ff push

Lead、PACKET、OBJECTIVE 都不能自行增加用户没有授予的外部副作用权限。

本包中没有看到一条晚于 LOG-031 的 USER-DECISION 明确授权 push。

因此这里必须二选一：

存在后续真实用户授权
→ 将 USER-DECISION provenance 补进 LOG / status / Evidence

不存在
→ 删除“被 packet/Lead 覆盖为合法授权”的表述，
  并把实际发生的行为（若发生）记录为 governance deviation

本次外部分析无法确认是否真的执行了 push；我只确认现有文档中的“授权解释”不能成立。

P1-1 — 路径所有权合同已大范围过期，不只是 Spec §9

当前工程 Skill 已明确：

monorepo:
  backend/
  frontend/

notify:
  backend/wta-modules/wta-notify

packages:
  org.namewta

但 change 中仍存在大量：

ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify
ruoyi-common-*
org/dromara

旧引用。

这些不仅出现在历史 LOG/ADR，还存在于当前执行合同，例如：

ticket/01-channel-account-admin.md writable_paths
ticket/02-mail-scene-binding.md writable_paths/read_only_paths
ticket/03-sms-template-binding.md
ticket/04-send-quotas.md
ticket/05-caller-variable-contract.md
ticket/06-test-send-and-skills.md
spec.md verification paths

因此 CR-001 当前把该问题只描述成：

Spec §9 验证路径低级文档问题

严重性偏低。

当 writable_paths 已经不指向当前真实 source tree 时，“未超出 writable_paths”无法可靠验收，路径 owner/gateway 也会漂移。

建议只迁移仍具执行语义的路径合同；历史 LOG.md 中记录当时仓库事实的路径可以保留并注明 historical，不要为了统一名称改写历史证据。

P1-2 — Goal Plan 没有从计划态收敛到真实执行态

goal-plan.md 仍然写：

status: ready
ready_for_execution: true

并在 Current Status 写：

尚未开始实现
下一动作 G0 Skill 后 T-01
无 Ticket implementation SHA

Contract coverage 也仍全部是：

planned

但其他工件已经是：

tickets-map.status = completed
T-01..T-06.status = done
.status.current_work = specdev/C-code-review
works_run 包含 I-implement

这会使恢复流程读取到互相冲突的权威状态。

建议做一次真正的 Goal Plan close-out reconciliation，记录：

planned model
actual model
historical/batched implementation provenance
verification actually rerun
verification only inherited
deviations
current C-review blocker
actual latest checkpoint

在该 reconciliation 完成前不建议修改 change 为 completed。

P1-3 — “pass” 与 “not-run this round” 没有清晰区分

典型证据：

T-04.md：

限额 Java 测试 = not-run this round
Maven notify test = 未运行
AC-008/009/010/015 = pass

T-05.md：

caller Java tests = not-run this round
AC-011/012/013 = pass

T-06.md：

validate-skill-facts.mjs = not-run this round
AC-018 = pass

“历史某个 SHA 上运行过”可以是有效 evidence，但必须能够回答：

运行在哪个 exact SHA？
command 是什么？
cwd 是什么？
exit code 是什么？
测试是否 skipped？
从该 SHA 到 CR head，该验证接缝有没有发生变化？

当前包没有建立这条 immutable provenance 链。

推荐统一状态为：

verified-at-current-head
historical-evidence-at-<sha>
not-run
not-applicable

不要用一个 pass 同时代表这四种语义。

P1-4 — Required E2E 的负路径证据不完整

T-01 Ticket 定义 required E2E：

配置页可见账号列表
无权限失败关闭

但 T-01 Evidence 主要证明：

有权限页面
新增账号 dialog
登录后 GET -> 200

没有在本包 Evidence 中看到对应无权限 API 403 / UI fail-close。

T-06 Ticket 的 required E2E 更明确：

有权限用户能触发试发
无权限按钮不可用
API 403

但 T-06 Evidence 的 E2E 结论是：

passed（入口可见，未提交供应商）

“未真实向供应商发信”本身不一定阻塞权限 E2E；真正缺的是 ticket 自己要求的 unauthorized UI/API 场景。

因此至少 T-01 与 T-06 的 E2E 状态应细分，不宜继续用单个 passed 覆盖全部 required scenes。

P1-5 — CR-001 finalize 前还缺 C-work 自身的过程证据

source-discovery.md 要求记录各候选规范/标准来源的：

found / not-found / not-applicable

reviewer-contracts.md 又要求标准轴与规范轴隔离，不共享 finding。

目前 CR-001 有 source list，但没有完整 source-discovery trail，也没有说明本地标准轴/规范轴如何隔离执行。

由于当前 CR 本来就是 blocked，这不是要求现在伪造结果；但在以后真正 finalize 时，应补齐：

source discovery results
reviewer isolation method
actual diff/log digest
validator result

否则“两个表都写了 finding”不等同于完成双轴隔离审查。

P2 — 当前 quota finding 还需要先澄清规范语义

CR-001 的 medium provisional finding 指出：

account quota tryAcquire 后，template/recipient 后续失败不回滚，会提前耗尽账号额度。

这个问题可能是真实 bug，也可能是保守的“attempt reservation”限流设计。

现有 ADR/Spec 明确规定：

供应商调用前检查三层配额
任一超限立即失败关闭
不得改选账号
测试发送使用相同限额

但并没有明确规定：

如果账号 quota 已预留，
随后 template quota / recipient quota / template validation 失败，
账号 quota 是否必须释放。

因此在没有源码、没有精确 test scenario 的情况下，本次外部审查不能把“必须 rollback”直接提升为已证实的 Spec violation。

建议补充一个明确合同：

quota accounting semantics:
  successful-provider-attempt only
或
  conservative reservation / attempted-send

并为“账号通过 → 模板失败”“账号+模板通过 → recipient 失败”分别固定测试。这样才能准确判断本地 medium finding 是 bug、设计选择还是 Spec gap。

Proposed artifacts to promote into SpecDev (paths relative to change/)
Path	建议内容	处置
external-brain/2026-09-12-gpt56-sol-c-review-audit.md	保存本回复，明确 actual model = GPT-5.6 Sol、NON-GATE / ADVISORY	可新增；不要把它改写成 GPT-6-Pro 已通过
external-brain/notes.md	增加本次 rerun 事实：新 pack 已含 CR-001，但模型仍不满足 t155u；新增发现包括 fixed-point provenance、G0 empty records、commit-chain/authorization/path drift	可更新
evidence/C-INPUT-RECONCILIATION.md	建议新增。统一记录 8680 / b06d / 3a86 / d1ce 的 ancestry 与角色、真实 change-owned commits、历史测试 SHA、actual verification、push authorization provenance	最高优先级
reviews/CR-001.md	在 provenance reconciliation 后修订 fixed point/head；加入真实 diff/log digest、source-discovery、reviewer isolation；Ticket 空输入 pass 不再作为有效依据	暂不 finalize
goal-plan.md	把“尚未开始 / planned”改成真实 execution/retroactive-close-out 状态；记录 accepted deviations；同步当前 checkpoint 与 C blocker	必须 reconcile
ticket/*.md	将仍具执行/ownership 语义的旧 ruoyi-* / org.dromara 路径迁到 backend/wta-* / org.namewta；历史事实不要机械改写	必须修 operational paths
tickets-map.md	在 Map 中反映 actual execution model、路径迁移和 Evidence 状态，而不是只写 done	与 Goal Plan 同步
evidence/T-01.md … evidence/T-06.md	将空 diff 的双轴 pass 改为 invalid/unverified；Skill records 不得伪造；验证结果区分 current-head / historical / not-run	必须 reconcile
evidence/AC-AUDIT.md	为每个 pass 增加 exact SHA + command/test locator + current-head applicability；不能仅列测试方法名	建议升级为可追溯矩阵

不要覆盖当前 external-brain/reply.md 后假装它已经变成合规 GPT-6-Pro 输出；它当前明确是 rejected artifact，这个 provenance 应保留。

Risks / open questions
问题	为什么必须回答
8680fe6... 究竟是 feature 前 baseline、implementation commit 还是已经包含 feature 的 checkpoint？	决定 CR-001.fixed_point 是否把真正实现排除在审查范围外。
3a86dfe... 是一次 batched close-out commit，还是六张 Ticket 实际共享的 implementation commit？	决定 Goal Plan 是否需要从 sequential implementation 改成 retroactive reconciliation。
d1ce372... 相对于 3a86dfe... 新增了什么？是否主要是 rename/docs/review noise？	决定正式 C 的 head 与 scope 是否正确。
LOG-031 之后是否存在真实用户明确授权 ff push？	没有则 PACKET/Lead 无权覆盖 not-authorized。
AC-AUDIT 所引用的 Java tests / skill validator 实际在哪个 SHA、什么 command/cwd/exit code 下运行？	决定能否作为最终 head 的验收证据。
quota 是“成功供应商尝试计数”还是“预留/尝试即计数”？	决定 NotifySendPlanner 的 medium finding 是实现 bug 还是规范缺口。
T-01/T-06 的无权限 UI / API 403 是否存在未打包的 Lead Evidence？	决定 required E2E 是否真的完成，而不是只验证正向页面可见。
CTO 是否仍坚持 t155u 的 GPT-6-Pro 精确型号门禁？	若仍坚持，本回复和任何非该型号输出都只能作为旁证，不能解除 blocker。
Explicit "not verified locally" notes

Not verified locally.

本次只读取了用户上传的 thin zip，并严格按 DISPATCH → MANIFEST → AGENTS → SpecDev C entry → project Skills → current change 的范围进行审查。

我没有访问 /workspace/vp-dev/WTA-plus 的真实 Git worktree，因此没有验证：

commit 对象是否存在
8680 / b06d / 3a86 / d1ce 的真实 ancestry
git diff / git log 的真实输出
changed files
NotifySendPlanner 源码行为
Maven / pnpm tests
Redis / MySQL
SMTP / SMS provider
浏览器截图
任何真实 E2E
push / PR / remote state

我尝试仅在解压后的外部包副本运行：

validate-specdev.mjs --stage review
validate-specdev.mjs --stage goal-plan
validate-specdev.mjs --stage tickets

三者均因为该 thin export 不是 Git worktree 而退出，错误为：

--repo is not a Git worktree

这不等于工件 schema 已失败，也不能被描述成项目本地 validator 已通过；它只说明该验证无法在本次外部包环境完成。

本次没有修改项目文件、没有创建项目 commit、没有执行或声称 E2E 通过、没有执行 push/PR/部署，也没有索取任何 secret、API key、SMTP/SMS 凭据或本地认证文件。

最终门禁结论仍是：C-code-review BLOCKED / 不可归档。即使暂时忽略 GPT-6-Pro 型号门禁，也应先解决 fixed-point provenance、G0 Skill Evidence、Ticket commit-chain、push authorization 与 stale path ownership 这几项，再重跑正式 C review。

其中最优先的是 8680fe6 的身份确认：如果它确实是 feature implementation commit，那么当前 CR-001 的 review range 本身就需要重建；这比直接讨论 NotifySendPlanner 的某一条实现 finding 更先决。