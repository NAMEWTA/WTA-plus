---
schema_version: 6
artifact: "goal-plan"
change: "2026-09-14-wta-plus-comprehensive-review"
status: "in_progress"
modes: ["high-assurance", "release-coordination", "migration"]
orchestration: "lead-directed"
lead: "single-agent"
implementation_agent_limit: 1
integration_attempt_limit: 3
ticket_workspace_policy: "current"
integration_gate: "direct-parent"
ready_for_execution: true
---

# Goal Plan：完成本review change并满足归档前置

Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>；Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Tickets：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/</Path>；Evidence：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/</Path>。

**run已激活，revision157，ready_for_execution=true。** 用户要求执行全部50票，以代码及真实验收为完成依据；单人串行/current-direct-parent。

## 1. Outcome and Authority

### Outcome

在当前代码上保留已交付基座合同，闭合报告全部18项及19新增切片，完成50票合法处置和AC-001—050当前候选验收，交付完整同源本地发布候选、恢复与安全处置记录；最后满足change-completion，另行批准A归档/知识提升。

### Success and False Completion

成功由用户行为与真实证据证明，不由票数、文件数、代码行数判断。旧测试、mock Provider成功、跳过环境测试、test list、统一旧result、only-evidence/empty commit、schema通过均不等于完成。

### Non-goals

本Goal完成本地实现与逐票提交验收；远程推送/部署/真实数据修复/轮换/归档仍须独立批准。OIDC实现归相邻change；不为归档强行完成它，也不撤销它的既有决定。普通review change的归档不自动部署生产；本地完整candidate验证仍必需。

### Authoritative Inputs

用户本轮请求→已接受ADR/领域合同→Spec→Ticket→Goal；proposed ADR不冒充已接受决定。源报告与源码比对见<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>、<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/current-review.md</Path>，旧证据仅证明其原输入。高影响冲突回G，局部施工由Ticket。

## 2. Execution Graph

### DAG and Critical Path

Notify主链：T-36/T-37→T-38→T-39→T-50，并分支到T-40/T-43；附件需T-35/T-37/T-44→T-42；开发配置T-32→T-48与T-44汇合T-49。T-30等待其余49票合法关闭。完整依赖由Map及frontmatter推导。

### Waves and Ownership

| Wave | Ticket | 开始条件 | Owner/资源 | Gate |
|---|---|---|---|---|
| W-legacy | 原T-01—31，除T-30 | G-plan、G-authorize；先核对历史提交/证据 | Lead逐票当前合同重验，不重复旧实现 | G-legacy |
| W-security | T-32/T-33 | G-plan、G-authorize | local配置/CORS独占 | G-safe-default |
| W-visible | T-34/T-47 | 已授权且各票Ready | 消息/OSS页面独占 | G-visible |
| W-notify | T-35/36/37/38/39/50 | 真实DAG前置闭合 | runtime/common/API/事务独占 | G-notify |
| W-journey | T-41/T-40 | T-34和Notify所需前置 | REST/页面/取消独占 | G-user-flow |
| W-oss | T-44/45/46 | D-003/D-008已接受 | 存储配置/诊断/指针独占 | G-oss |
| W-close | T-42/43/48/49 | 各自前置闭合，附件D-009已接受 | 附件/schema/脚本/配置独占 | G-feature-complete |
| W-final | T-30 | 其余49票闭合 | 同一候选/测试环境/产物独占 | G-integrated/G-complete |

current模式Wave仍串行。推荐顺序：T-32 → T-33 → T-34 → T-47 → T-29 → T-01 → T-35 → T-36 → T-37 → T-38 → T-39 → T-50 → T-41 → T-40 → T-44 → T-45 → T-46 → T-42 → T-43 → T-48 → T-49 → T-30；可以在DAG允许时调整独立票顺序，必须记录资源交接和新HEAD，不能借调整绕过安全或外部处置Gate。

### Ticket Quick Reference

| ID | 可观察产出 | Dependencies | Workspace | Implementation owner | E2E disposition | Evidence |
|---|---|---|---|---|---|---|
| T-01 | 干净clone不创建temp/release也通过事实检查 | T-29 | current | single-agent | not-required: 仓库静态/脚本合同由正负夹具及本地workflow检查覆盖，无在线业务边界 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-01-replan-2026-09-23.md</Path> |
| T-02 | canary不出现在HTTP sink、OperLogEvent、数据库或错误日志 | — | current | single-agent | required: 用唯一凭据canary调用签发/失败接口，检查HTTP sink、OperLogEvent与数据库均不含明文 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-02-replan-2026-09-23.md</Path> |
| T-03 | 大小边界前/等于/超限一字节结果可判定 | T-02 | current | single-agent | required: 通过真实HTTP发送定长/chunked边界请求、伪签名大正文及正常签名正文 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-03-replan-2026-09-23.md</Path> |
| T-04 | 任意外来XFF不改变直连或正常入口的授权结果 | — | current | single-agent | required: 隔离单/双Nginx链发IPv4/IPv6和伪造XFF请求，比较白名单、限流、审计来源 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-04-replan-2026-09-23.md</Path> |
| T-05 | A失败不得删除B的键 | — | current | single-agent | required: 真实Redis以屏障复现A过期/B接管/A失败/C被拒及正常失败重试 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-05-replan-2026-09-23.md</Path> |
| T-06 | 生产实现不含ThreadLocalRandom/雪花ID作为bearer | — | current | single-agent | required: HTTPS隔离SSO验证Cookie、CORS、PKCE、过期与单次兑换；显式dev HTTP例外 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-06-replan-2026-09-23.md</Path> |
| T-07 | 复杂state往返相等且无重复code/state参数 | T-06 | current | single-agent | required: 专用SSO Playwright跑/admin与/home base、复杂state、过期与重新登录 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-07-replan-2026-09-23.md</Path> |
| T-08 | 每个shipped App均有且仅有完整配套，缺项在promotion前失败 | T-07, T-09, T-10 | current | single-agent | required: 本地三Origin HTTPS部署候选，登录、刷新、过期恢复与health可用 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-08-replan-2026-09-23.md</Path> |
| T-09 | build:dev最终三个App均development，build:prod均production | — | current | single-agent | not-required: 构建产物扫描与脚本负向夹具覆盖；线上组合由T-08/T-30验收 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-09-replan-2026-09-23.md</Path> |
| T-10 | manifest每个artifact的digest和source可追溯 | T-09 | current | single-agent | required: 隔离文件系统完整stage、缺件/中断/混源注入、固定版本消费者与容器重建恢复 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-10-replan-2026-09-23.md</Path> |
| T-11 | 生产bundle不再携带该共享响应私钥或ECB路径 | — | current | single-agent | required: 两App登录注册/下载/错误回归及生产bundle密钥/ECB扫描 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-11-replan-2026-09-23.md</Path> |
| T-12 | logout超时/401/离线时本地token和动态路由仍清空 | T-07 | current | single-agent | required: 离线/超时logout、并发401、空角色恢复和切Client重登 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-12-replan-2026-09-23.md</Path> |
| T-13 | 服务端关闭注册时入口与页面均准确，后端仍拒绝直接调用 | T-12 | current | single-agent | required: 禁用注册直接访问、验证码错误后刷新重试、慢旧响应及键盘提交 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-13-replan-2026-09-23.md</Path> |
| T-14 | 新个人CN_RESIDENT_ID上传正反面后完成提交 | T-18 | current | single-agent | required: 新个人身份证双面、企业条件材料经真实MySQL/OSS提交，刷新/失败/越权覆盖 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-14-replan-2026-09-23.md</Path> |
| T-15 | 旧入口引用为零且替代能力覆盖完整 | — | current | single-agent | not-required: 方法级迁移表、全Profile/消费者编译和原行为测试覆盖，无独立新交互 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-15-replan-2026-09-23.md</Path> |
| T-16 | B失败绝不发出A的审批请求 | — | current | single-agent | required: 真引擎授权矩阵及快速A/B切换、B失败、确认期间切任务、重复办理 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-16-replan-2026-09-23.md</Path> |
| T-17 | 错误origin、错误source、未知payload不能关闭标签 | — | current | single-agent | required: 真实iframe发合法close及外部window伪造消息，刷新卸载不残留 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-17-replan-2026-09-23.md</Path> |
| T-18 | 下载URL失败不生成无人回收的Blob URL，也不把已完成上传误报失败 | — | current | single-agent | required: 真实上传后URL失败、导入网络/业务/401/取消、重试与资源回收 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-18-replan-2026-09-23.md</Path> |
| T-19 | 快速筛选时旧响应不能覆盖新列表，失败/loading可恢复 | T-18 | current | single-agent | not-required: 受控Promise组件测试覆盖乱序和取消，既有App回归由T-30执行 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-19-replan-2026-09-23.md</Path> |
| T-20 | 受影响边界类型检查通过，nullable与非法transport样本有明确处理 | T-12, T-14, T-19 | current | single-agent | not-required: 受影响包诊断、typecheck和边界单测直接覆盖；相关业务E2E属于原票 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-20-replan-2026-09-23.md</Path> |
| T-21 | 只用键盘可完成公开流程，焦点可见且错误能被读屏发现 | T-13 | current | single-agent | required: 320/768/1440、200%缩放、键盘/读屏语义和渲染对比检查 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-21-replan-2026-09-23.md</Path> |
| T-22 | 任意一条SQL失败不会留下Delivery/Attempt/Outbox不一致 | — | current | single-agent | required: 真实MySQL/Redis双worker、过期/reclaim、Attempt失败与finish冲突注入 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-22-replan-2026-09-23.md</Path> |
| T-23 | 回滚后相同事件可重试成功 | T-22 | current | single-agent | required: 两实例重启/并发重复/跨provider同eventId/提交失败/早到回调；真实签名适配 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-23-replan-2026-09-23.md</Path> |
| T-24 | 崩溃后permit在规定上限内恢复，不依赖人工删key | — | current | single-agent | required: 隔离Redis kill进程、嵌套申请失败、降低并发在途收束、rate窗口更新 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-24-replan-2026-09-23.md</Path> |
| T-25 | 自父/后代父/并发互移均不能形成环 | — | current | single-agent | required: 真MySQL并发同树/跨根移动及注入后代更新失败，树不变量成立 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-25-replan-2026-09-23.md</Path> |
| T-26 | 每个候选有迁移或保留理由，不遗漏调用者 | T-02, T-16, T-25 | current | single-agent | required: 各受影响资源代表读/写/批量删除与越权请求，旧CRUD方法拒绝、生成合同一致 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-26-replan-2026-09-23.md</Path> |
| T-27 | 保存/删除路径不再有虚假校验TODO | T-26 | current | single-agent | required: 真实树新建、移动、越权/非法父/后代父、带子节点删除和合法叶删除 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-27-replan-2026-09-23.md</Path> |
| T-28 | 慢provider不阻塞业务提交线程 | T-22 | current | single-agent | required: 慢provider下提交耗时、Redis失败/丢wake后poll及双worker fence | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-28-replan-2026-09-23.md</Path> |
| T-29 | 每个删除文件有owner、内容迁移落点和无丢失硬约束证据 | — | current | single-agent | not-required: 逐文件hash/规则去向/路径引用和事实检查直接覆盖文档交付 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-29-replan-2026-09-23.md</Path> |
| T-30 | 全部已接受AC均有实际命令/退出码/环境/源码checkpoint | T-02, T-03, T-04, T-05, T-06, T-07, T-08, T-09, T-10, T-11, T-12, T-13, T-14, T-15, T-16, T-17, T-18, T-19, T-20, T-21, T-22, T-23, T-24, T-25, T-26, T-27, T-28, T-29, T-31, T-32, T-33, T-34, T-35, T-36, T-37, T-38, T-39, T-40, T-41, T-42, T-43, T-44, T-45, T-46, T-47, T-48, T-49, T-50 | current | single-agent | required: 同一候选完整运行SSO/Profile/workflow/Notify/Third/树/三App发布及失败恢复 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-30-replan-2026-09-23.md</Path> |
| T-31 | 真实Notify返回QUEUED时send不抛DELIVERY_FAILED，transfer与通知同事务提交 | T-22 | current | single-agent | required: 真Profile→Notify QUEUED、worker受理→确认及DB/Redis部分失败、重复确认/错用户/绑定变更 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-31-replan-2026-09-23.md</Path> |
| T-32 | 新 clone 的当前检出与发布产物不含真实凭据；历史对象仍可能保留披露值，必须另外轮换。部署者从未跟踪的本地文件或环境变量注入。公开模板可启动到明确的缺配置错误，日志不回显 secret。 | — | current | single-agent | required: Git 跟踪清单、合成配置加载、日志脱敏；实际轮换是 G-security-external 的外部动作 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-32-replan-2026-09-23.md</Path> |
| T-33 | 默认同源访问正常；跨域仅接受显式受信 Origin。生产带凭证通配配置拒绝启动；本地开发复用 Vite 同源代理。 | — | current | single-agent | required: 真实 Servlet/CorsFilter 测试与 Spring profile 绑定 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-33-replan-2026-09-23.md</Path> |
| T-34 | 有效会话始终可经 REST 读取；开关只控制实时连接。盒子展示单份摘要与明确加载/失败/空状态，切身份不串数据。 | — | current | single-agent | required: 现有 push.test.ts、notice 组件、真实 Admin 登录→打开盒子 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-34-replan-2026-09-23.md</Path> |
| T-35 | 合法短信由真实 runtime→dispatcher→适配器发送一次；逻辑快照可解释且不泄露验证码，本地可判定校验失败不等回执。 | — | current | single-agent | required: DispatchNotificationServiceTest、NotifyDispatcherUnitTest、CaptchaNotifyCallerUnitTest 的真实跨层组合 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-35-replan-2026-09-23.md</Path> |
| T-36 | 同 intent 的不同收件人并发投递均可读取且关系唯一；本地消息、关系与投递结果原子提交，实时事件仅提交后发送。 | — | current | single-agent | required: 扩展 NotifyAtomicResultIntegrationTest；真实 DSTransactional 代理、MySQL 双连接、提交故障与 AFTER_COMMIT | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-36-replan-2026-09-23.md</Path> |
| T-37 | Outbox 独占重试次数与节奏；明确未发送的可重试失败允许新一次物理发送，ACCEPTED/DELIVERED/UNKNOWN 保留防重。 | — | current | single-agent | required: NotifyIdempotencyDispatcherUnitTest、RedisNotifyIdempotencyStoreIntegrationTest、真实 runtime/Dispatcher 组合 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-37-replan-2026-09-23.md</Path> |
| T-38 | URL 唯一定位 intent；指定 delivery 只重试其所属一项，无可重试任务返回真实状态及零计数；外部 UNKNOWN 明确拒绝自动重发。 | T-36, T-37 | current | single-agent | required: HTTP ID 冲突、真实 DB requeue 并发、API/领域合同测试 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-38-replan-2026-09-23.md</Path> |
| T-39 | 统一截止时刻驱动验证码 TTL 与 command；提交/重试拒绝到期，Worker 发请求前检查，过期任务结束且 Provider 调用为零。 | T-38 | current | single-agent | required: 可控时钟 runtime/worker、CaptchaNotifyCallerUnitTest、真实 DB 过期任务终结 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-39-replan-2026-09-23.md</Path> |
| T-40 | 撤回只停止该发布版本未开始发送的任务，保留已送达内容与审计；本人从收件箱读快照，无需公告管理权限。 | T-38, T-39 | current | single-agent | required: 真实发布→撤回→Worker→inbox；普通用户浏览器详情与越权 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-40-replan-2026-09-23.md</Path> |
| T-41 | 完整收件箱使用项目 PageQuery/PageResult 分页，稳定 create_time/message_id 排序；本人第501条可取，顶部只取最近摘要。全部已读仍作用本人全部消息。 | T-34 | current | single-agent | required: 真实 MySQL分页＋HTTP 登录身份过滤＋前端分页组件/浏览器 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-41-replan-2026-09-23.md</Path> |
| T-42 | 正文邮件无需伪造链接；专用 demo-mail 场景以主题/正文包装模板发送。显式附件字段经授权/冻结/持久化传递，零附件不访问 OSS。 | T-35, T-37, T-44 | current | single-agent | required: DemoNotifyCallerUnitTest、NotifyAttachmentDispatcherUnitTest、真实 owner/快照引用＋假邮件物理适配器 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-42-replan-2026-09-23.md</Path> |
| T-43 | 代表性规模有可重复 SQL/时延/锁等待基线；保持现有总量上限与持久聚合语义，优先减少插入往返，测量不足不引入新计数状态机。 | T-36, T-38, T-39 | current | single-agent | required: 真实 MySQL代表规模、SQL计数、现有原子结果/fence回归 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-43-replan-2026-09-23.md</Path> |
| T-44 | 业务仅校验当前对象/配置/权限/预期访问类型，远端操作按实际结果反馈；管理员诊断独立，坏的可选存储不阻断核心就绪。 | — | current | single-agent | required: OssStorageReadiness*、OssLifecycle*、OssUpload*；核心启动＋最小权限MinIO与health组 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-44-replan-2026-09-23.md</Path> |
| T-45 | 诊断仅报告观察事实与范围：读403为未知，单对象匿名读取只证明该对象，PRIVATE未知不能宣称全桶安全。 | T-44 | current | single-agent | required: OssAccessDiagnosticUnitTest、受限MinIO读权限与诊断展示 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-45-replan-2026-09-23.md</Path> |
| T-46 | 所有切指针/删来源入口共用对象→工单锁序。恢复先成功则清理不得删来源；清理已获得合法执行权则恢复明确拒绝。 | — | current | single-agent | required: OssStorageMigrationIntegrationTest：真实MySQL两连接＋阻塞替身＋MinIO对象存在验收 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-46-replan-2026-09-23.md</Path> |
| T-47 | A慢B快最终行、total、preview、loading、error均属于B；卸载或旧失败不污染当前页面。 | — | current | single-agent | not-required: 受控Promise组件与类型检查能直接判定全部竞态；T-30执行页面组合回归 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-47-replan-2026-09-23.md</Path> |
| T-48 | 一个脚本提供显式start/build/doctor/repair子命令（菜单仅薄包装），普通再次启动不深度修复且尊重Spring/Vite环境优先级。 | T-32 | current | single-agent | required: shell fake命令夹具＋真实Linux初次/二次启动；Windows由支持环境实际验收 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-48-replan-2026-09-23.md</Path> |
| T-49 | 默认存储来自DB，历史对象按service；静态上传策略有安全代码默认值，只有部署差异/必要额度可覆盖，SINGLE无需MULTIPART参数。 | T-44, T-48 | current | single-agent | required: OssUploadPropertiesUnitTest、配置绑定测试、默认存储切换真实MinIO | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-49-replan-2026-09-23.md</Path> |
| T-50 | 统一路径只承诺ALL/ASYNC/default priority=0；非支持值在写意图前明确拒绝，所有仓内生产调用同批迁移，不增加高级编排引擎。 | T-39 | current | single-agent | required: 公共入口负向合同、每类生产调用方、历史WAIT样本终结 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-50-replan-2026-09-23.md</Path> |

## 3. Gates and Completion Evidence

### Overall Definition of Done

AC-001—050有当前候选证据；全部计划票done或因真实无需新改动而合法cancelled，取消保留合同映射。required Skill/E2E通过，无未批准偏差。逐票实际非空产品commit、真实clean exact HEAD/tree验收及result/父链可重建，最终治理记录提交后tracked与未忽略untracked均clean。完整本地release candidate及manifest/恢复测试通过；所有适用外部风险Gate关闭或经用户明确不适用/豁免。最后运行complete校验，不用Archive补造完成。

### Gates

| Gate | 开启条件 | 关闭证据 | 阻塞范围 | owner/批准 | 失败恢复 |
|---|---|---|---|---|---|
| G-plan | 本轮计划已定稿 | G完整frontier回答及明确共识；S Ready；50票DoR；T/P校验和控制无错误 | 相关Ready发布/全部执行 | 用户设计决定＋Lead校验 | 回具体D节点，保留草案 |
| G-authorize | G-plan | 用户实际激活目标/明确实施范围；本轮commit/direct-parent授权与workspace归属明确 | 产品写入与提交 | 用户，Lead核对 | 保持plan，不继承旧批次push授权 |
| G-legacy | 当前代码和旧31票 | 逐票提交/测试/源差异/完成证据处置；旧AC在新候选覆盖 | 相应依赖票、最终完成 | Lead；取消需事实和范围核对 | 缺clean记录不补造，按下文分支 |
| G-safe-default | T-32/33 | repo无本地真实密钥、CORS正负向、示例可用 | 后续候选发布 | Lead | 不回滚到泄漏secret/通配 |
| G-security-external | 凭据操作清单 | owner轮换/连接验证/旧凭据失效证据，或用户明确该项不适用/风险处置决定 | 归档与任何真实发布 | 环境owner/用户 | 停受影响外部动作，本地可继续 |
| G-visible | T-34/47 | 实时关闭本人REST可读、A/B乱序不污染 | 用户路径验收 | Lead | 回责任票 |
| G-notify | T-35—39/50 | 真实dispatcher、DB/Redis、截止/重试/UNKNOWN矩阵；现有调用无退化 | 取消/附件/最终集成 | Lead | 不全量清缓存或重发 |
| G-user-flow | T-40/41 | 发布→撤回、本人详情、501条历史和全部已读 | 完整用户路径 | Lead | 保留快照/关系 |
| G-oss | T-44—46 | 最小权限访问/三态诊断/双连接指针安全，核心health真实配置 | 附件/配置/最终集成 | Lead | 不扩大桶权限换绿 |
| G-feature-complete | T-42/43/48/49 | 附件生产装配及真实owner、成本测量、首次/二次启动与配置单源 | T-30 | Lead | 新设计需求回G |
| G-integrated | 其余49票合法关闭 | 同一clean候选全门禁/真实服务/专用E2E/full-core/三App/完整manifest及恢复 | 本地产品完成 | Lead | 失败返责任票，重跑受影响闭包 |
| G-complete | 上述适用Gate均关闭 | completion记录、无活动事务、最终clean/complete校验、数量核对 | completed | Lead | 未提交治理文件不宣称完成 |
| G-archive | completed | reconcile/publish门不适用或关闭、永久知识候选评估及用户明确归档批准 | Archive移动/知识写入 | 用户＋A网关 | 保持completed，不自动移动 |

### Contract and Reference Coverage

Map完整50行AC矩阵及<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/coverage.md</Path>覆盖18个报告项和所有旧票。每项Evidence须绑定源码、命令、退出码/数量/skip与不变量。T-30不能仅收集票的done标志。

## 4. Execution and Integration Protocol

### Lead Orchestration

Implementation subagents：上限1、当前1；Read-only agents：当前2。execution-time dynamic由Lead分配有界任务；用户已明确允许gpt-6-sol/xhigh子代理，撤销此前禁止。current产品保持单writer，不创建新worktree。

Lead=single-agent，负责状态与direct-parent集成。implementation_agent_limit=1；只读研究/审查可并行且不得写产品。实际派遣使用gpt-6-sol/xhigh，按任务固定输入及路径返回Lead验收。integration_attempt_limit=3，来自config上限。subagent-delivery operation=plan仅核对空task_kind集合、唯一Lead/状态/父分支/E2E owner，不dispatch、不创建新workspace。

### Ticket Workspace and Integration

所有票current/direct-parent；父分支执行前重新确认，不写死旧SHA。规划基线 `1264980c74e594bc594e88561bb292fbe5d968a1`，启动时clean；本轮仅文档diff。每票非空实现commit后，Lead在同一当前工作区、精确HEAD/tree且全仓clean的时点验收并捕获证据，再记录result与父链；旧result保持不可变，不随以后治理提交更新。下一票在该出口通过后开始。

**历史31票恢复分支（必须在首次run审查）：** 原commit全部存在且在父链，旧数据却把31票result统一记为整批6c8764c，worktrees为空。先回读原日志确认是否存在真实当时逐票clean验收；有则按原事实修正索引，不能从现在的clean反推过去。若没有而当前代码已满足旧合同，无需新增实现，保存当前复验证据后按“无需新改动”合法cancelled并保留AC由T-30覆盖；这不把取消说成新实现成功。发现缺陷则在责任票做非空修复并走新出口。不得创建空commit或重复旧修改填字段。该分支直接沿用 change-completion 的“因权威事实无需改动而记录为 cancelled”与 I-implement 的无改动出口；仅缺历史证据不能成为取消依据，必须先证明当前合同已满足且确实无需新实现。D-004已确认；本轮仍不执行状态转换，首次run按事实办理。

T-30不需要为验证伪造产品改动：可引用其真实历史测试实现提交及当前完整集成证据；若旧票无法合法done且无需新实现，按同一取消规则记录，整体验收仍由Goal持有且不可取消。新的测试基座/验收代码确需修改时才形成非空实现commit。


### 历史票关闭后的依赖重算

`cancelled`不自动满足`blocked_by`，控制器只把合法done作为成功前置。只有当前证据证明原合同已满足、无需新实现时，才可按无改动出口取消本轮重复施工；保留历史实现事实及AC责任。随后由T/P按证据逐条裁决其下游：取消的是“待实现”前置时移除已无意义的施工边，并在下游第2/6节记录替代证据和仍须复验的合同；合同仍缺失则转交明确责任票、建立新依赖，禁止直接删边放行。同步Ticket、Map、plan-data、Goal revision并重新运行控制器，尤其T-30的49条汇合边必须逐项处理，不能让cancelled成为永久假阻塞或自动成功。

本轮原31票统一重开并发布计划Ready，旧review仅为历史记录；恢复执行时Lead先读历史证据并裁决一票，再只允许一张票进入in_progress/review，避免把整批历史review当成30个在途writer。新修复若影响已复验旧合同，登记受影响AC由对应票/T-30在最终候选重验。

### Authorization Matrix

Implementation commit：同change既有全部提交授权＋本次明确Goal执行；Local direct-parent verification and parent update：同一授权内逐票串行，执行前后捕获clean HEAD/tree。

| 动作 | 本轮状态 | 依据/条件 |
|---|---|---|
| 当前change文档、只读源码/校验 | allowed | 用户完整重规划请求 |
| current workspace产品实施 | authorized | 用户2026-09-23已激活本Goal，执行全部Ticket |
| implementation commit / direct-parent父分支更新 | authorized | 本change既有全部commit授权持续有效；本次目标激活明确要求按含逐票提交验收的Goal执行 |
| source/candidate worktree | not-applicable | current策略，原用户禁止新worktree |
| push/PR/merge/分支清理 | not-authorized | 不从计划或旧批次继承 |
| 部署/凭据轮换/真实数据修复 | scoped-authorized / completed | 用户指定三个/srv/ops目录并要求据部署调整；仅WTA MySQL/Redis/MinIO凭据及必要消费者已按备份执行。额外AI/qcloud用户确认已撤销；整套产品部署和其他真实数据修复未授权 |
| 归档/永久知识写入 | not-authorized | completed后走A与原网关 |

### Evidence Return

每票写新日期Evidence，不改旧执行原文；Skill调用记录包含实际摘要、输入输出、动作和结果；无宿主调用不写passed。Lead自己核对，无子代理自报。默认skip须找实际系统属性/标签启用，不能以Maven总绿替代。

## 5. Constraints, Risk and Recovery

### Non-negotiable Constraints

权限/Client/日志、事务/lease、对象owner、唯一SQL基座、公共边界与生成物流程保持；不删测试/放宽编译/扩例外。System readiness硬约束先经D-008替代，不能静默越过；永久ADR-0007/0009的候选替代只在change-local，正式提升走A。公开secret不可复述或使用。

### Verification Integrity

规划阶段仅完成文档/静态证据；运行阶段必须按票实际执行Maven/浏览器/真实服务验证并记录。旧测试必须与输入hash/源码等价性关联，发生行为变化的闭包重跑；无关测试不用反复重跑。具体命令、服务属性及required零skip见<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/verification.md</Path>。

### Migration or Release Sequence

先安全配置与发送/访问正确性，再用户闭环/配置收缩，最后同源完整候选。新环境六SQL按序；既有环境Tag差异、备份、隔离演练、受控处置清单，真实执行另批。先验full JAR并保存再core clean；三个App和Compose/Nginx/SQL一并校验。运行容器不因文件指针切换自动更新，生产发布不是本地candidate验证。

### Risks, Monitoring and Recovery

真实轮换、生产Origin/代理CIDR、TLS/容量、平台支持与既有数据不能从代码假定；它们只阻塞相关外部门/声明，不应阻止独立已授权本地修复。未来归档时不得把仍适用的风险伪写不适用。外部UNKNOWN先核对、IN_APP按ID幂等恢复，OSS删成功但DB失败按相同对象事实前向恢复。

### Deviation Control

接口/数据/安全/验收变化回G/S，局部路径变更先Ticket/Map。owner冲突只停相关闭包；不得接管另一个SSO change或任何未闭合记忆事务。HEAD/依赖摘要漂移使未验证candidate失效；重复失败无新证据或3次尝试后记录根因、下一次改变与恢复入口，不盲重试。

## 6. Progress and Decisions

### Current Status

revision157；T-29/T-32/T-33/T-34/T-35/T-36/T-47 done，T-01取消重复施工，其余42票ready。T-36固定result64d67d5通过第三候选真实66+1，178单元/SMS8严格输入等价复用；下一T-37，目前无产品writer。G-security-external已关闭；T-01的AC-001仍由T-30复验。整个change尚未完成，归档未获授权。

### Pending Decisions and Blockers

<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/design-tree.json</Path>D-002—009全部answered，含OSS硬约束替代与生产附件能力；LOG-018为真实整体共识。设计未决为零；用户已激活目标，本地commit与direct-parent授权已核对，G-authorize关闭；外部环境动作另批。

### Resume Protocol

G→S→T→P(plan)已完成。用户主动激活目标/授权时，先重读已确认决定及当前Skill摘要，复核HEAD/owner/授权并重跑计划控制，再设置run所需状态；先G-legacy与必要安全前置，再从Map控制器选择符合DAG的单票。读取实际HEAD、当前票、状态与Evidence，不重复已完成副作用。

## Assumptions

沿用既有current/单人模式与源码命名；测试样本合成且可回收；不假设生产账号/Origin/TLS/容量已知。设计已确认，当前本地执行已授权；生产环境参数不作猜测。

## 2026-09-23 run恢复

本节覆盖上文规划时点的“本轮不实施/未激活”表述。执行已授权，当前先T-32；历史closeout与全量集成仍必须完成，不能把新19票代替全部50票。远程写入、真实凭据轮换和归档继续独立批准。

## 2026-09-23 revision142执行更新

T-32 product/result `bafd5d512a5d17c4848db278f2350fcc6631fbd7`，clean exact tree两端取证完成；适用发布128项与Spring3项0skip。用户修订目标允许gpt-6-sol/xhigh子代理；撤销之前零子代理限制，其余current单writer/无新worktree继续。部署目录已由用户指明并要求按现场替换调整；先核对精确身份、影响面、备份和可逆方案，再执行授权范围内动作。凭据轮换未完成，不关闭G-security-external。

## T-33派单

base `dd8179e1cd394b092bfb36b8b91c0e04386563ce`；依赖为空、T-32已关闭；产品writer cors_audit，Lead治理/E2E/提交验收；Packet见 evidence/dispatch-T-33.md。

## revision142验收与环境处置

T-33 source/result `da48f850cf34d8d23c09f1ad9c92ed433d5617b0`正式验收通过，独立双轴审查通过；失败/恢复及真实clean前后证据见evidence/T-33.md。下一票T-34。用户授权的WTA MySQL/Redis/MinIO凭据已实际轮换、消费者同步、旧值拒绝；共享root不变。G-security-external原披露项已处置，额外退役AI及禁用qcloud非种子凭据正在等待用户提供归属/外部撤销记录，不阻止独立代码票。未获push/归档授权，不发布整套应用。

## revision142当前

T-34 in_progress，cors_audit唯一产品writer；2done/1in_progress/47ready。Lead准备隔离真实Admin登录与消息盒子E2E，其他agent无写锁。base `6fcbfeb50747b67bcaf4bf9af1d964cc7d760c04`。

## 外部凭据门关闭 — 用户处置确认

2026-09-23用户确认追加退役AI token/模型凭据、禁用qcloud密钥“已经撤销停用，没有其他进行系统进行使用”。记录为负责人处置确认，非供应商接口复核；与实际DB/Redis/MinIO轮换证据共同关闭G-security-external。其余票据验收和归档授权要求不变。

T34已按I流程在第3轮停止并完成Lead复盘；旧3次不删除，新Packet见evidence/dispatch-T-34-recovery.md。Lead唯一writer，真实E2E独占测试资源，不与编译/类型门禁并行。未改变超时或AC，全部票目标保持。

## revision143当前验收

T32/T33/T34 done，47ready；T34固定result 177eb5bd889afd2ab54f4a8e162dc8358d80f140，714/5/1与clean证据齐备，当前无产品writer。下一T47。G-security-external关闭；T29私有目录合同矛盾待处理，不宣称整个change已完成。

## revision144当前执行

T47取得唯一产品writer，base 97e1ee9e1ad40de75deffd025379a6a5488c4882，3done/1in_progress/46ready；下一Notify T35，T29事实检查路径矛盾仍待闭合。

## revision145当前验收

4done/46ready，T47result 7cd6fb22b28b7d464b9648ba77f2a2308e5b2016；731前端测试/完整静态与三App生产构建通过。下一T29→T01→T35，正式DAG重排先于派单。

## revision146当前

当前部署Skill要求旧私有目录但facts拒绝该目录，T01当前复验须等T29修复；历史T01实现早于T29且仍在父链，旧施工边已履行。本轮T29无需等重复验收，改为T29→T01，不取消合同或伪造Done。 队列T29→T01→T35；4done/1in_progress/45ready。T29 base 7a1810288d3292ceeb4987f488b13353af1a1286；12条精确新增写集与私有备份/恢复/无覆盖迁移边界见Ticket及Packet。历史证据不改写。

## revision147当前

5done/45ready；T29 result 9f055ba15d9d5a828fb08cdfb0b24efa32642889。当前facts及文档审计通过，私有资料已完整备份/恢复演练后迁移，维护脚本和唯一部署报告路径一致且0600。下一T01完整可信门禁复验，不以T29绿色直接取消旧票；随后T35。

## revision148当前

T01完整当前复验启动，无产品写者；5done/1in_progress/44ready。只在全部合同证明且无需新增实现后，才裁决取消历史重复施工并保持AC001的T30最终责任。

## revision149当前

当前源码已满足AC-001且无需新增产品实现；历史非空3aa047b及其父链保留，T29已完成当前路径冲突修复。按Goal历史票无改动出口，取消的是本轮重复施工，不取消AC-001，不把缺失历史clean单独作为理由，不生成空commit或新result。AC-001最终组合复验继续由T30承担。 当前5done/1cancelled/44ready，无产品writer；下一T35。T09与T30重复施工边已逐项移除，替代当前Evidence <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-01-replan-2026-09-23.md</Path>；T09自身合同及T30全部AC最终复验仍保留。

## revision150当前

T35短信真实跨层修复启动，base 8b758ea8074a63835659b9731e3a4c74c5c029c5，cors_audit单产品writer；新增3路径先登记于Ticket，Lead隔离环境与治理独占。5done/1cancelled/1in_progress/43ready。

## revision151当前

T35新增5个安全实现/测试/语义文档路径及java-api-compatibility绑定已在编辑前登记；修复REDACT事件原文与Captcha/通知普通日志泄露接缝，不改供应商原始参数、HTTP传输、FULL或幂等摘要。

## revision152当前

T35新增6个精确写集先于实施登记；REDACT_SENSITIVE 通知的供应商消息标识仅保留内部持久化用于回执关联；query、重复提交 receipt 和 monitor 公开投影隐藏该值，FULL 原行为保持。监控查询按本次有界结果批量读取 Intent 审计策略，不引入逐行查询；空ID集合不扫描全表。新增 NotifyAuditSupport 可统一策略与公开投影判断，NotificationReceipt 仅补公开字段的安全语义说明，不改签名。测试覆盖真实供应商返回手机号/验证码作为ID、内部值保留与公开值隐藏、FULL、重复提交及回执关联。

## revision153 — T35完成

Revision153: T35 accepted at fd8c346;38classes170unit+8real MySQL/Redis zero skips, both reviews pass, clean exact HEAD/tree before/after and owned cleanup verified. All three candidates and failed governance missing-link evidence retained; concrete evidence link repaired after source verification, no product change or attempt reset. 6done/1cancelled/43ready; next T36.

## revision154 — T36启动

Revision154: T36 active at 5118051403de0648540646d98536d8c4f3f9f26d; cors_audit sole product writer, Lead owns governance/commit/isolated real acceptance. 6done/1cancelled/1in_progress/42ready; IN_APP atomic persistence plus result, fence and AFTER_COMMIT; no external channel I/O in transaction or production repair.

## revision155 IN_APP有界尝试预算

代码事实确认：原claim不计次数，结果事务回滚也回滚attempt_count，不能宣称现有重领机制有限。采用原有Outbox字段、既有代理端口和统一锁序：确定参数预检后，beginInAppAttempt独立短DSTransactional按Intent→Outbox→Delivery锁及数据库fence消耗一次预算；成功返回才进入消息/关系/结果的原子事务。IN_APP outbox.attempt_count表示已开始尝试（含随后回滚/崩溃），Delivery与Attempt只记录原子提交的结果；其他渠道保持原含义。消息事务不再次消耗预算。

预算耗尽则在锁内将IN_APP Delivery FAILED、Outbox DEAD_LETTER并刷新聚合，零persist；预算事务失败或提交结果不确定时停止，不能猜测已获得预算。消息事务提交ACK丢失时依已持久DONE/DELIVERED抑制重投，DB暂不可读则等待安全恢复。完全不可写期间不能保证提交终态，但不得在未取得持久预算时调用persist；恢复后仍在预算上限内收束。允许SQL失败后保留独立预算，不允许消息/关系/投递结果部分提交。

验证补充：预算先提交后消息回滚、max边界/耗尽零persist、预算提交ACK丢失及失效lease、同lease重入不能越过最大物理次数。既有写集覆盖port/usecase/runtime/DAO/Mapper/XML；不改worker/claim，不新增表/状态机，不转移给T38，外部渠道未知合同不变。

## revision156 预算字段语义文档写集

编辑前增加两条精确路径：NotifyOutbox.java字段Javadoc，以及唯一六SQL中的10-cde-base-ddl.sql，仅notify_outbox.attempt_count/last_error_code中文注释。已有DDL“领取次数”不符合旧结果计数也不符合新预算语义，须同步为IN_APP已开始尝试预算、外部渠道已提交结果次数，固定IN_APP_ATTEMPT_RESERVED内部标记。无列/类型/索引/结构变化、不重放存量基座；全新隔离六SQL装载复核仍必需。预算上限指一次自动调度周期；合法人工重试新周期由T38精确API合同负责。

## revision157 — T36完成

Revision157: T36 accepted at64d67d5; C3 Atomic66+Wake1 zero skips; exact unchanged-input reuse of C2 unit178/SMS8 and gates, original run sources explicit. Both reviews pass; clean exact HEAD/tree and owned cleanup. Three attempts preserved. 7done/1cancelled/42ready; next T37; goal active, no archive.
