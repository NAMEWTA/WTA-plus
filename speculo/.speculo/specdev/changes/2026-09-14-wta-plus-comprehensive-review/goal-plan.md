---
schema_version: 6
artifact: goal-plan
change: 2026-09-14-wta-plus-comprehensive-review
status: blocked
modes: [high-assurance, release-coordination]
orchestration: lead-directed
lead: single-agent
implementation_agent_limit: 1
integration_attempt_limit: 3
ticket_workspace_policy: current
integration_gate: direct-parent
ready_for_execution: false
---

# Goal Plan：WTA-plus基座修复与收敛

Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Ticket目录：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/</Path>。

## 1. Outcome and Authority

### Outcome

按31项AC修复可证实问题，以最小必要改动交付一致的基座与可追溯发布候选。本次mode=plan，只完善文档和Work状态；产品实现未开始。

### Success and False Completion

规划交付完成要求31票均有完整合同/写集/Skill/验证矩阵、真实未知显式阻塞、Map与本Goal一致、T阶段及单change/schema检查无error；P阶段单change路由缺陷单独记录。产品完成另要求31票实际验收、获授权的commit/result与整体Gate闭合；两者不能混淆。禁止用空commit、Evidence-only Done、取消代替成功、跳过required测试或修改断言制造完成。

### Non-goals

无旧版兼容工程、全仓strict重写、通用租约/事件投影框架、额外控制台或多容器热切换承诺；不提交、推送、部署、处理真实运行数据或修改永久知识。

### Authoritative Inputs

用户最新请求（自主完善、单并发、禁子代理、无需兼容）→<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>及<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/CONTEXT.md</Path>→永久ADR/context（只读）→Spec→Ticket→Map/Goal。代码事实能推翻旧事实摘要，改变行为合同时回写真正owner。证据基线为<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/re-review.md</Path>及<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/planning-baseline.json</Path>；不能只以HEAD忽略dirty工作树。

## 2. Execution Graph

### DAG and Critical Path

真实边来自Ticket frontmatter，完整投影见Map。结构最长链T-06→T-07→T-12→T-13→T-21→T-30；T-08汇合SSO与构建发布，T-20汇合会话/材料/页面，T-26汇合日志/流程/部门，T-30汇合其余30票。没有工时估计，不能把结构链当排期承诺。

### Waves and Ownership

| Wave | 串行Ticket顺序 | 入口 | 交付/退出 |
|---|---|---|---|
| W1 门禁/公共安全 | T-01,T-02,T-03,T-04,T-05 | G-plan、G-authorize；各自真实依赖 | 各票G-ticket |
| W2 SSO/发布准备 | T-06,T-07,T-09,T-10,T-08,T-11 | 各票依赖Evidence | SSO旅程与同源完整产物 |
| W3 页面/业务边界 | T-12,T-13,T-18,T-14,T-15,T-16,T-17,T-19,T-20,T-21 | 各票依赖Evidence | owner/会话/交互不变量 |
| W4 通知/一致性/树 | T-22,T-23,T-28,T-31,T-24,T-25,T-26,T-27 | 各票依赖Evidence | 有效租约、可恢复排队与无环树 |
| W5 收敛/整体验收 | T-29,T-30 | T-29仅依赖T-01；T-30等待其余全部 | G-integration，准备G-release候选 |

每个Wave仍严格一个Ticket/一个writer；阻塞票只暂停其依赖闭包，可跳过到独立票，不额外把Wave先后变成代码依赖。W1/W4中的真实未知关闭前Goal不进入run。

### Ticket Quick Reference

| ID | 产出 | Dependencies | Workspace / owner | E2E disposition | Evidence（未来） |
|---|---|---|---|---|---|
| T-01 | 干净clone不创建temp/release也通过事实检查 | — | current / single-agent | not-required: 仓库静态/脚本合同由正负夹具及本地workflow检查覆盖，无在线业务边界 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-01.md</Path> |
| T-02 | canary不出现在HTTP sink、OperLogEvent、数据库或错误日志 | — | current / single-agent | required: 用唯一凭据canary调用签发/失败接口，检查HTTP sink、OperLogEvent与数据库均不含明文 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-02.md</Path> |
| T-03 | 大小边界前/等于/超限一字节结果可判定 | T-02 | current / single-agent | required: 通过真实HTTP发送定长/chunked边界请求、伪签名大正文及正常签名正文 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-03.md</Path> |
| T-04 | 任意外来XFF不改变直连或正常入口的授权结果 | — | current / single-agent | required: 隔离单/双Nginx链发IPv4/IPv6和伪造XFF请求，比较白名单、限流、审计来源 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-04.md</Path> |
| T-05 | A失败不得删除B的键 | — | current / single-agent | required: 真实Redis以屏障复现A过期/B接管/A失败/C被拒及正常失败重试 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-05.md</Path> |
| T-06 | 生产实现不含ThreadLocalRandom/雪花ID作为bearer | — | current / single-agent | required: HTTPS隔离SSO验证Cookie、CORS、PKCE、过期与单次兑换；显式dev HTTP例外 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-06.md</Path> |
| T-07 | 复杂state往返相等且无重复code/state参数 | T-06 | current / single-agent | required: 专用SSO Playwright跑/admin与/home base、复杂state、过期与重新登录 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-07.md</Path> |
| T-09 | build:dev最终三个App均development，build:prod均production | T-01 | current / single-agent | not-required: 构建产物扫描与脚本负向夹具覆盖；线上组合由T-08/T-30验收 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-09.md</Path> |
| T-10 | manifest每个artifact的digest和source可追溯 | T-09 | current / single-agent | required: 隔离文件系统完整stage、缺件/中断/混源注入、固定版本消费者与容器重建恢复 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-10.md</Path> |
| T-08 | 每个shipped App均有且仅有完整配套，缺项在promotion前失败 | T-07, T-09, T-10 | current / single-agent | required: 本地三Origin HTTPS部署候选，登录、刷新、过期恢复与health可用 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-08.md</Path> |
| T-11 | 生产bundle不再携带该共享响应私钥或ECB路径 | — | current / single-agent | required: 两App登录注册/下载/错误回归及生产bundle密钥/ECB扫描 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-11.md</Path> |
| T-12 | logout超时/401/离线时本地token和动态路由仍清空 | T-07 | current / single-agent | required: 离线/超时logout、并发401、空角色恢复和切Client重登 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-12.md</Path> |
| T-13 | 服务端关闭注册时入口与页面均准确，后端仍拒绝直接调用 | T-12 | current / single-agent | required: 禁用注册直接访问、验证码错误后刷新重试、慢旧响应及键盘提交 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-13.md</Path> |
| T-18 | 下载URL失败不生成无人回收的Blob URL，也不把已完成上传误报失败 | — | current / single-agent | required: 真实上传后URL失败、导入网络/业务/401/取消、重试与资源回收 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-18.md</Path> |
| T-14 | 新个人CN_RESIDENT_ID上传正反面后完成提交 | T-18 | current / single-agent | required: 新个人身份证双面、企业条件材料经真实MySQL/OSS提交，刷新/失败/越权覆盖 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-14.md</Path> |
| T-15 | 旧入口引用为零且替代能力覆盖完整 | — | current / single-agent | not-required: 方法级迁移表、全Profile/消费者编译和原行为测试覆盖，无独立新交互 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-15.md</Path> |
| T-16 | B失败绝不发出A的审批请求 | — | current / single-agent | required: 真引擎授权矩阵及快速A/B切换、B失败、确认期间切任务、重复办理 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-16.md</Path> |
| T-17 | 错误origin、错误source、未知payload不能关闭标签 | — | current / single-agent | required: 真实iframe发合法close及外部window伪造消息，刷新卸载不残留 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-17.md</Path> |
| T-19 | 快速筛选时旧响应不能覆盖新列表，失败/loading可恢复 | T-18 | current / single-agent | not-required: 受控Promise组件测试覆盖乱序和取消，既有App回归由T-30执行 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-19.md</Path> |
| T-20 | 受影响边界类型检查通过，nullable与非法transport样本有明确处理 | T-12, T-14, T-19 | current / single-agent | not-required: 受影响包诊断、typecheck和边界单测直接覆盖；相关业务E2E属于原票 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-20.md</Path> |
| T-21 | 只用键盘可完成公开流程，焦点可见且错误能被读屏发现 | T-13 | current / single-agent | required: 320/768/1440、200%缩放、键盘/读屏语义和渲染对比检查 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-21.md</Path> |
| T-22 | 任意一条SQL失败不会留下Delivery/Attempt/Outbox不一致 | — | current / single-agent | required: 真实MySQL/Redis双worker、过期/reclaim、Attempt失败与finish冲突注入 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-22.md</Path> |
| T-23 | 回滚后相同事件可重试成功 | T-22 | current / single-agent | required: 两实例重启/并发重复/跨provider同eventId/提交失败/早到回调；真实签名适配 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-23.md</Path> |
| T-28 | 慢provider不阻塞业务提交线程 | T-22 | current / single-agent | required: 慢provider下提交耗时、Redis失败/丢wake后poll及双worker fence | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-28.md</Path> |
| T-31 | 真实Notify返回QUEUED时send不抛DELIVERY_FAILED，transfer与通知同事务提交 | T-22 | current / single-agent | required: 真Profile→Notify QUEUED、worker受理→确认及DB/Redis部分失败、重复确认/错用户/绑定变更 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-31.md</Path> |
| T-24 | 崩溃后permit在规定上限内恢复，不依赖人工删key | — | current / single-agent | required: 隔离Redis kill进程、嵌套申请失败、降低并发在途收束、rate窗口更新 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-24.md</Path> |
| T-25 | 自父/后代父/并发互移均不能形成环 | — | current / single-agent | required: 真MySQL并发同树/跨根移动及注入后代更新失败，树不变量成立 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-25.md</Path> |
| T-26 | 每个候选有迁移或保留理由，不遗漏调用者 | T-02, T-16, T-25 | current / single-agent | required: 各受影响资源代表读/写/批量删除与越权请求，旧CRUD方法拒绝、生成合同一致 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-26.md</Path> |
| T-27 | 保存/删除路径不再有虚假校验TODO | T-26 | current / single-agent | required: 真实树新建、移动、越权/非法父/后代父、带子节点删除和合法叶删除 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-27.md</Path> |
| T-29 | 每个删除文件有owner、内容迁移落点和无丢失硬约束证据 | T-01 | current / single-agent | not-required: 逐文件hash/规则去向/路径引用和事实检查直接覆盖文档交付 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-29.md</Path> |
| T-30 | 全部已接受AC均有实际命令/退出码/环境/源码checkpoint | T-01, T-02, T-03, T-04, T-05, T-06, T-07, T-08, T-09, T-10, T-11, T-12, T-13, T-14, T-15, T-16, T-17, T-18, T-19, T-20, T-21, T-22, T-23, T-24, T-25, T-26, T-27, T-28, T-29, T-31 | current / single-agent | required: 同一候选完整运行SSO/Profile/workflow/Notify/Third/树/三App发布及失败恢复 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-30.md</Path> |

## 3. Gates and Completion Evidence

### Overall Definition of Done

全部已纳入AC有真实通过证据；所有非cancelled票有非空implementation commit、direct-parent通过、parent包含关系及result SHA；无未决高影响偏差；本次未批准deferred项。取消必须有不再需改动的真实事实且重算AC覆盖，不自动算产品成功。

### Gates

| Gate | 开启条件 | 关闭证据 | 阻塞范围 | owner/批准 | 失败恢复 |
|---|---|---|---|---|---|
| G-plan | T/P文档已完善 | T-03预算、T-23身份/保留期固定；Spec及31票Ready；两阶段validator和ticket-control无结构错误 | 全局run | single-agent；高影响合同按真实证据/用户决定 | 回写Spec/ADR及责任票；当前未关闭 |
| G-authorize | G-plan已关闭 | 产品实施/commit/direct-parent授权及当前workspace用户改动归属处理完成；保存新基线 | 所有产品实现与提交 | 用户授权；Lead核对 | 缺授权只停执行，不重做已完成文档 |
| G-ticket | 本票Ready、依赖Done、写集/Skill摘要匹配 | 当前workspace正常/失败/回归、适用E2E、非空commit与result；真实Skill记录 | 本票及依赖闭包 | single-agent | 保留失败diff/checkpoint，不开启下一依赖票 |
| G-integration | 其余30票验收、T-30可开始 | 同一候选full测试、full/core产物、前端、真实服务/SSO专用E2E/失败恢复全通过 | 整体产品完成与发布候选 | single-agent | 回责任票修复并重跑受影响闭包 |
| G-release | 完整候选digest、G-integration关闭 | 真实CIDR/入口清洗、三Origin/TLS/端口、运行预算、发布及恢复操作获明确授权 | 真实发布；不阻止本地隔离验证 | 用户/环境owner | 保持既有版本；不自动执行部署 |

### Contract and Reference Coverage

AC-001—AC-031与单票同号，Map第4节有完整31行覆盖及Evidence目标；目前covered表示有计划，无一代表产品pass。没有隐藏deferred或以发布未知替代业务未决。

## 4. Execution and Integration Protocol

### Lead Orchestration

| 项目 | 本计划约束 |
|---|---|
| Lead | single-agent，唯一项目/SpecDev/Evidence/父分支/E2E owner |
| Implementation subagents | 禁止，实际派遣数0；用户指令优先 |
| Read-only agents | 同样禁止，review/research/test-observation均由Lead本人串行完成 |
| execution-time dynamic | 本计划不派遣；不调用spawn/followup/dispatch，不生成外部任务包 |
| Schema上限快照 | v6要求implementation_agent_limit为正整数，因此记录最小合法上限1；它不是许可，用户禁止将有效派遣数限制为0。全局配置3保持不变，不修改workflow/schema以迁就本change |
| integration attempts | 3；同一失败无新证据或达上限时先复盘，不盲目重复 |

已按subagent-delivery的operation=plan检查所有权：允许task_kind集合为空、派遣权限无、实际agent数0；输出为以上禁止派遣合同。未调用dispatch/accept，也未启动任何子代理。

### Ticket Workspace and Integration

current/direct-parent；不创建source/candidate worktree。当前父分支main，采样HEAD=e285d0800c530fcb6b90790a8aab5dcc3162d3bd；含既有staged/unstaged内容，仅为规划基线，不是可发布干净源码。用户修改不得自动stash/reset/提交。

全部票统一协议：授权及workspace归属确认→记录parent_before和实际diff→只改本票写集→非E2E及适用集成检查→获授权后形成非空implementation commit→Lead在同一current-workspace验证该commit及required E2E→HEAD未漂移时记录result_sha=implementation commit→进入下一票。实现提交后集成失败时不标Done、不继续下一票；保留失败HEAD/检查点，修复后重新提交与验证，不宣称current模式已经自动回滚父HEAD。

| Ticket范围 | Parent/base | Source checks | Implementation commit | Integration checks/E2E | Parent result |
|---|---|---|---|---|---|
| T-01—T-31，按Map排序 | 执行前main实际HEAD；不得复用当前旧规划SHA | 单票第8节，current-workspace | 每票非空；当前全部未创建 | Lead串行、current-workspace | 通过后记录；当前全部不存在 |

### Authorization Matrix

| 动作 | 当前状态 | 条件 |
|---|---|---|
| 本change计划与相关状态索引 | allowed | 2026-09-18用户明确激活T/P并完善文档 |
| Current workspace Ticket changes（产品） | not-authorized | 当前请求仅规划；后续明确实施范围 |
| Implementation commit | not-authorized | 具体可审查diff及授权后执行 |
| Local direct-parent verification and parent update | not-authorized | 未来实现授权与干净基线满足后；本轮仅文档校验已授权 |
| Local candidate integration and parent update | not-applicable | current策略不用candidate |
| Push / PR / remote merge | not-authorized | 不从规划继承 |
| Branch/worktree cleanup | not-authorized | 本轮无新建worktree |
| Deploy / migration / production actions | not-authorized | 具体目标、digest、数据动作与恢复方案明确后授权 |

### Evidence Return

Lead本人记录每条命令cwd、环境、退出码、测试数/跳过项、源码checkpoint、实际Skill调用、AC与未验证项。未来单票Evidence不得用本轮规划检查冒充；没有source/candidate/subagent回报。

## 5. Constraints, Risk and Recovery

### Non-negotiable Constraints

单人串行、禁止子代理；没有兼容窗口；不越过App/web-domain/domain/platform和后端layered/classic边界；新/实质修改事务遵守DSTransactional；不取消权限/材料/数据规则换取成功。源引用和永久知识只读，本轮不重写全局配置。

### Verification Integrity

本轮先前9条检查基线见<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/re-review-command-results.json</Path>：事实检查、SSO/Notify分层及release合同有既有失败，不能称已修。门禁误报归T-01，真实SSO/Notify边界归T-06/T-28；各票记录基线差异，不以全局豁免跳过。产品Maven/pnpm/浏览器/服务测试本轮未执行。

### Migration or Release Sequence

无兼容迁移模式。仓内接口/调用方/生成物同批切换；新环境使用唯一六份SQL基座。先验证full JAR再core clean；完整同源构建形成不可变目录，验证后切单指针；运行容器须显式重建。真实部署前G-release必须关闭，不能把产物原子性扩展成服务零停机承诺。

### Risks, Monitoring and Recovery

安全状态改变时保留安全收口，不通过恢复旧泄漏/无界读取来恢复服务；通知外部已受理而DB失败需核对及幂等重试；跨DB/Redis失败按T-31重发码恢复，不引入额外投影。观测复用现有耗时、失败、Outbox积压/lease等信号。具体恢复和停止条件由单票第9/12节拥有。

### Deviation Control

依赖、接口、写集、Skill摘要或验证事实变化先改owner工件与plan_revision，再重算受影响闭包。单票相同失败无新证据或3次集成失败：在该票Evidence记录共同模式、原因、下一次具体改变、恢复入口；没有实质变化不重试，不通过派子代理规避。

## 6. Progress and Decisions

### Current Status

T/P规划工件已完整编排；29票Ready、2票blocked，0实施/0Done，G-plan/G-authorize均未关闭，ready_for_execution=false。无implementation/source/candidate/result SHA，无产品完成百分比。规划检查结果见<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/planning-validation.json</Path>。

### Pending Decisions and Blockers

- T-03：正文各视图预算和最大合法样本；只有2 MiB候选不足以锁定对外拒绝合同。
- T-23：供应商账号/事件身份与重试/保留期证据；当前apply签名没有account，不能凭空保证唯一性。
- P-stage：当前校验器错误要求单change创建父级implementation-map/plan，原命令退出1；保留真实缺陷，未增加父级冗余工件。
- G-authorize：未获实施/commit/父分支更新授权，workspace含用户改动。规划继续不受影响。
- G-release：真实CIDR/域名/TLS/运行参数与部署批准；仅阻止真实发布。

### Resume Protocol

回读Goal→Map→适用Skill→当前Ticket→change状态/Evidence。先运行ticket-control只读检查，再核对HEAD、dirty归属、摘要和前置Evidence。补两项参数后修订Spec/ADR及票据、重验；只有授权与workspace gate满足才设置ready_for_execution=true进入I-implement。本轮plan完成不自动启动run。

## Assumptions

当前项目版本与工具入口在执行前可复核；不假定生产拓扑、供应商事件作用域或请求预算。上述高影响未知未关闭，ready_for_execution必须为 `false`。
