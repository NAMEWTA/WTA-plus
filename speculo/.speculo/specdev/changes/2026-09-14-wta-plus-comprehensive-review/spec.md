---
schema_version: 3
artifact: "spec"
change: "2026-09-14-wta-plus-comprehensive-review"
status: "ready"
ready_for_tickets: true
sources: ["USER-DECISION:2026-09-23 complete replan; user review before goal execution", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/WTA-plus-review-64b4ea7.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/current-source-audit.json</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>"]
---

# Spec：既有基座复验与通知、存储、配置闭环

Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；ADR：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>；领域词汇：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/CONTEXT.md</Path>。

**Spec Ready。** 用户已确认D-002—009及整体G共识（LOG-010—018）；本Spec据此定稿。Ready代表计划合同完整，目标执行仍由用户之后自行激活。

## 1. 问题与目标

报告的10项Notify、4项OSS、2项安全问题及2项复杂度建议覆盖用户能否读取本人消息、短信/邮件能否真正发出、状态是否最终可解释、文件访问是否被可选诊断阻断、日常启动是否必须排障等行为。旧31票已有提交与历史测试，当前不能笼统写成“未实现”，也不能用旧全绿宣称本次完成。

目标用户：普通登录收件人、公告发布者、通知业务调用方、OSS管理员、开发者和环境负责人。成功状态是当前基座已有合同保持，报告全部事项有明确处置与当前候选证据，可交付完整本地发布候选并具备真实归档条件。

非目标：新通知平台、Kafka/Saga、通用请求仲裁/锁框架、自研IAM解释器、全仓类型重写；相邻OIDC迁移不在本change实施。本轮仅规划，不执行真实服务变更。

## 2. 解决方案与外部行为

### 主要流程

1. 登录用户即使关闭实时连接仍经REST读本人消息；顶部展示近期摘要，完整历史分页；详情读不可变快照。
2. 发布保存快照与接收人/Outbox；IN_APP在本地结果事务内可靠落库，外部渠道单次I/O后短事务记录事实；实时仅提交后提示。
3. 模板/变量/绑定错误在可判断时明确拒绝，合法SMS真实通过dispatcher；可重试的明确未发送失败可以重发，UNKNOWN不盲重试。
4. 发送前遵守截止和取消，重试准确定位delivery；无任务不伪装QUEUED。撤回保留已送达与不可撤销外部边界（D-002已接受）。
5. 普通邮件无需业务链接，附件经授权与快照实际发送；无附件零OSS调用。生产快照适配及归属按已接受D-009实施，不能只复用测试替身。
6. OSS访问依据对象/配置/授权/访问类型，管理诊断报告有界事实；并发恢复/清理不得删除当前来源。readiness规则替代与锁取舍按已接受D-003/D-008实施。
7. 公开仓库无真实本地凭据，CORS精确受信；日常start/build/repair职责清楚，默认存储与历史对象各用自己的权威事实。

### 状态与失败不变量

- 草稿、发布、QUEUED、ACCEPTED、DELIVERED及用户已读不是同一状态，不互相冒充。
- IN_APP本地回滚不进入供应商WAITING_RECEIPT；临时失败有限重试，确定失败终结。
- 外部已接受/UNKNOWN、回执重放、lease失效和提交失败保持防重/单调状态与安全审计。
- 提交/重试/最终发送检查截止；now>=expiresAt不发。过期/取消Outbox须收敛，不只是从claim查询排除。
- 页面数据、loading/error、预览只属于当前会话/请求代次；REST失败与真实空列表分开。
- secret不进入日志或浏览器；权限、Client、对象owner与PRIVATE预期不因简化放宽。

### 稳定错误行为

沿用现有项目ServiceException/HTTP/R合同并测试code与status；URL/body不一致、越权、未支持模式、过期、无权附件明确拒绝。报告中的NOTIFICATION_EXPIRED仅为建议名，不声明仓库已存在该码；具体新增机器码须由责任票在公共合同中登记并同步消费者，不隐式以任意字符串改变协议。

## 3. 用户故事

- **US-001** 普通用户关闭实时连接也能读取、翻阅并标记自己的消息，不串其他身份（AC-034/040/041）。
- **US-002** 发布者能追踪投递、停止尚未发送的版本，接收者能读实际快照（AC-036/038/040/043）。
- **US-003** 业务调用方能发送短信、时效验证码和有/无附件邮件，重试不吞实际发送且不重复未知请求（AC-035—039/042/050）。
- **US-004** 管理员正常访问文件无需诊断高权限，并发恢复与清理保护当前来源，列表保持最近查询结果（AC-044—047/049）。
- **US-005** 开发者用无密钥示例及清晰启动入口运行，环境负责人有真实凭据/CORS处置合同（AC-032/033/048/049）。
- **US-006** 维护者可复核旧合同、交付完整同源候选并按真实证据关闭/归档（AC-001—031及全体新AC）。

## 4. 验收合同

AC-001—031保留稳定编号和既有行为，不把新问题塞进旧finding。AC-032—050为本轮补充。所有行均是目标合同，当前未重跑产品测试。

| ID | 前置条件 | 动作或事件 | 可观察结果 | 验证接缝 |
|---|---|---|---|---|
| AC-001 | 当前合同已复验；重复施工取消，T30最终重验 | 恢复可信的仓库门禁与治理入口 | 干净clone不创建temp/release也通过事实检查；注释中DSTransactional不触发越层误报，真实非法import必须失败；不存在CI文件时文档不得宣称active；远程required状态有独立证据；每个声明命令可在对应cwd解析，Maven/前端测试与package分开记录 | not-required: 仓库静态/脚本合同由正负夹具及本地workflow检查覆盖，无在线业务边界；T-01 |
| AC-002 | 既有提交待当前候选复验 | 消除HTTP与操作日志中的凭据副本 | canary不出现在HTTP sink、OperLogEvent、数据库或错误日志；普通字段/操作者/耗时/失败状态仍可观测；签名、加解密、SSE与正常token响应保持正确；旧日志处置与凭据轮换另有批准记录，本票不自动删历史数据 | required: 用唯一凭据canary调用签发/失败接口，检查HTTP sink、OperLogEvent与数据库均不含明文；T-02 |
| AC-003 | 既有提交待当前候选复验 | 统一有界请求体采集与验签缓存 | 大小边界前/等于/超限一字节结果可判定；未知长度无法绕过限制，413时业务尚未执行；OpenAPI签名测试逐字节通过；同一正文不会因观察者线性增加完整副本，SSE/上传可取消 | required: 通过真实HTTP发送定长/chunked边界请求、伪签名大正文及正常签名正文；T-03 |
| AC-004 | 既有提交待当前候选复验 | 建立可信代理来源IP合同 | 任意外来XFF不改变直连或正常入口的授权结果；可信双代理解析一致，非法值失败关闭；白名单与限流使用同一个规范来源；未获得环境资料时不得把likely改成已复现越权 | required: 隔离单/双Nginx链发IPv4/IPv6和伪造XFF请求，比较白名单、限流、审计来源；T-04 |
| AC-005 | 既有提交待当前候选复验 | 修复防重键过期后的所有权竞态 | A失败不得删除B的键；正常失败可重试，正常成功TTL内被拒；异常/线程复用无ThreadLocal遗留；DB唯一约束和通知业务幂等不被此注解替代 | required: 真实Redis以屏障复现A过期/B接管/A失败/C被拒及正常失败重试；T-05 |
| AC-006 | 既有提交待当前候选复验 | 强化SSO令牌随机性与Cookie安全 | 生产实现不含ThreadLocalRandom/雪花ID作为bearer；已接受的PKCE/client/version/expiry/单次兑换负向测试全部保留；HTTPS生产Cookie属性成立，开发HTTP例外不可进入prod；CORS拒绝未知origin，登录不会因Cookie错误进入循环 | required: HTTPS隔离SSO验证Cookie、CORS、PKCE、过期与单次兑换；显式dev HTTP例外；T-06 |
| AC-007 | 既有提交待当前候选复验 | 修复SSO回调编码与可恢复登录旅程 | 复杂state往返相等且无重复code/state参数；过期、错state、错误verifier均失败关闭且用户可重新授权；成功回到原App内路径，带外域returnTo被拒；日志与UI不暴露code/verifier/token | required: 专用SSO Playwright跑/admin与/home base、复杂state、过期与重新登录；T-07 |
| AC-008 | 既有提交待当前候选复验 | 补齐SSO独立Origin发布合同 | 每个shipped App均有且仅有完整配套，缺项在promotion前失败；SSO三端跳转、刷新、过期、跨OriginCookie可运行；已发布current在配置失败时不改变；发布清单与Compose、Nginx、文档一致 | required: 本地三Origin HTTPS部署候选，登录、刷新、过期恢复与health可用；T-08 |
| AC-009 | 既有提交待当前候选复验 | 统一App模式、bundle与依赖服务验证矩阵 | build:dev最终三个App均development，build:prod均production；core/full产物必需与禁用列表逐项断言；测试证据与打包候选匹配，不以skip冒充测试通过；CI与部署镜像版本相同或有明确差异测试 | not-required: 构建产物扫描与脚本负向夹具覆盖；线上组合由T-08/T-30验收；T-09 |
| AC-010 | 既有提交待当前候选复验 | 删除混源局部发布并实现原子stage | manifest每个artifact的digest和source可追溯；缺模板/坏SQL/坏JAR/中断时current/context SHA256完全不变；单目标构建不能stage为完整release；恢复只操作本次stage，不触及他人文件或历史发布 | required: 隔离文件系统完整stage、缺件/中断/混源注入、固定版本消费者与容器重建恢复；T-10 |
| AC-011 | 既有提交待当前候选复验 | 删除浏览器共享私钥与ECB传输包装 | 生产bundle不再携带该共享响应私钥或ECB路径；正常登录、注册、错误响应及下载可运行；机器调用HMAC与OSS签名不被删除；传输切换有前后端同批发布、流量隔离与整体恢复方案（不承诺多容器原子热更新） | required: 两App登录注册/下载/错误回归及生产bundle密钥/ECB扫描；T-11 |
| AC-012 | 既有提交待当前候选复验 | 统一幂等会话清理与导航恢复状态 | logout超时/401/离线时本地token和动态路由仍清空；空角色账户每次恢复最多一次，不循环replace；切Client无旧菜单/权限残留，服务端授权仍为最终门禁；并发401只有一次恢复流程并可终止 | required: 离线/超时logout、并发401、空角色恢复和切Client重登；T-12 |
| AC-013 | 既有提交待当前候选复验 | 完成Home注册开关与验证码重试交互 | 服务端关闭注册时入口与页面均准确，后端仍拒绝直接调用；验证码错误后无需整页刷新即可再次成功；慢旧captcha不能覆盖新uuid；提交/取消/失败后按钮状态恢复 | required: 禁用注册直接访问、验证码错误后刷新重试、慢旧响应及键盘提交；T-13 |
| AC-014 | 既有提交待当前候选复验 | 补齐个人与企业自助认证材料闭环 | 新个人CN_RESIDENT_ID上传正反面后完成提交；企业必填及条件材料齐备时完成提交，缺项定位准确；取消/失败/过期OSS/刷新不会伪造完成或越owner访问；后端必填校验不被关闭，真实浏览器+MySQL+OSS验收通过 | required: 新个人身份证双面、企业条件材料经真实MySQL/OSS提交，刷新/失败/越权覆盖；T-14 |
| AC-015 | 既有提交待当前候选复验 | 移除Profile过渡接口与测试构造桥 | 旧入口引用为零且替代能力覆盖完整；全Profile及主要消费者编译/测试通过；当前接口不再让缺owner上下文的调用编译通过；删除清单逐项有替代入口/调用方/验证证据 | not-required: 方法级迁移表、全Profile/消费者编译和原行为测试覆盖，无独立新交互；T-15 |
| AC-016 | 既有提交待当前候选复验 | 保证流程任务读取和办理对象一致 | B失败绝不发出A的审批请求；陌生用户读取任务节点/变量被拒，合法办理人/发起人按既有规则可读；双击/过期任务不重复推进流程；快速切换、关闭、网络乱序与服务端失败可恢复 | required: 真引擎授权矩阵及快速A/B切换、B失败、确认期间切任务、重复办理；T-16 |
| AC-017 | 既有提交待当前候选复验 | 收紧流程设计器消息来源 | 错误origin、错误source、未知payload不能关闭标签；合法设计器close仍工作；设计器刷新/卸载无重复listener；不把未来save/publish消息自动加入允许列表 | required: 真实iframe发合法close及外部window伪造消息，刷新卸载不残留；T-17 |
| AC-018 | 既有提交待当前候选复验 | 明确上传完成、引用移除和导入失败生命周期 | 下载URL失败不生成无人回收的Blob URL，也不把已完成上传误报失败；导入网络失败、业务错误、401和取消均复位且可重试；业务提交等待上传与材料归属登记完成；现有OSS删除权限/引用保护不退化；若保留本地URL则replace/remove/unmount释放 | required: 真实上传后URL失败、导入网络/业务/401/取消、重试与资源回收；T-18 |
| AC-019 | 既有提交待当前候选复验 | 收敛System大页面中的异步状态与重复封装 | 快速筛选时旧响应不能覆盖新列表，失败/loading可恢复；权限树与用户编辑状态相互独立；提取前后功能/权限/排序/分页行为相同；每个超过1k行文件有职责删除或保留理由，无同义薄wrapper堆叠 | not-required: 受控Promise组件测试覆盖乱序和取消，既有App回归由T-30执行；T-19 |
| AC-020 | 既有提交待当前候选复验 | 收紧已触及的TypeScript合同边界 | 受影响边界类型检查通过，nullable与非法transport样本有明确处理；不增加双cast、ignore或检查排除来消除诊断；全量现有typecheck仍通过；严格化范围与诊断记录一致；未触及存量严格债明确列出，不冒充全仓strict完成 | not-required: 受影响包诊断、typecheck和边界单测直接覆盖；相关业务E2E属于原票；T-20 |
| AC-021 | 既有提交待当前候选复验 | 统一公开页面的可访问交互基线 | 只用键盘可完成公开流程，焦点可见且错误能被读屏发现；移动/放大页面无遮挡必要动作和横向不可达内容；不同App品牌差异不被当成bug强制同化；报告记录真实截图/可访问性结果，静态检查不冒充视觉通过 | required: 320/768/1440、200%缩放、键盘/读屏语义和渲染对比检查；T-21 |
| AC-022 | 既有提交待当前候选复验 | 原子提交通知投递结果与lease fence | 任意一条SQL失败不会留下Delivery/Attempt/Outbox不一致；旧lease不能覆盖新owner结果；双worker只产生符合合同的结果记录；provider去重能力不足时风险明确且不宣称exactly-once | required: 真实MySQL/Redis双worker、过期/reclaim、Attempt失败与finish冲突注入；T-22 |
| AC-023 | 既有提交待当前候选复验 | 将供应商回调幂等纳入持久事务 | 回滚后相同事件可重试成功；跨实例相同事件不重复生效，不同provider同ID互不影响；乱序/伪签名/迟到回调按现有规则失败关闭或no-op；重启不丢幂等记录，聚合与delivery一致 | required: 两实例重启/并发重复/跨provider同eventId/提交失败/早到回调；真实签名适配；T-23 |
| AC-024 | 既有提交待当前候选复验 | 恢复Third并发租约并明确限额热更新 | 崩溃后permit在规定上限内恢复，不依赖人工删key；正常/超时/异常释放正确，旧请求不伤害新租约；管理阈值变更在声明时机跨实例一致生效；Redis不可用时按既有失败合同拒绝，日志不回显凭据 | required: 隔离Redis kill进程、嵌套申请失败、降低并发在途收束、rate窗口更新；T-24 |
| AC-025 | 既有提交待当前候选复验 | 保证部门移动无环且并发一致 | 自父/后代父/并发互移均不能形成环；合法跨支移动及根移动保持正确ancestors；权限数据范围不因错误树扩大；失败时整个子树维持原状态 | required: 真MySQL并发同树/跨根移动及注入后代更新失败，树不变量成立；T-25 |
| AC-026 | 既有提交待当前候选复验 | 按资源合同清除旧CRUD方法并同步客户端 | 每个候选有迁移或保留理由，不遗漏调用者；已迁移CRUD无旧PUT/PATCH/DELETE可达入口；前后端method/path/权限/日志与OpenAPI快照一致；生成/源码门禁和代表资源E2E通过 | required: 各受影响资源代表读/写/批量删除与越权请求，旧CRUD方法拒绝、生成合同一致；T-26 |
| AC-027 | 既有提交待当前候选复验 | 修复Demo树样例并删除误导占位实现 | 保存/删除路径不再有虚假校验TODO；非法parent、环、带子节点删除按明确合同处理；模板代表输出可编译且与前后端树语义一致；保留演示所需权限/日志，不把示例缺陷推广到System | required: 真实树新建、移动、越权/非法父/后代父、带子节点删除和合法叶删除；T-27 |
| AC-028 | 既有提交待当前候选复验 | 让通知提交后唤醒保持短路径 | 慢provider不阻塞业务提交线程；Redis wake丢失时慢poll仍收敛；峰值不会创建无限线程/队列；重复wake不会绕过lease生成重复任务 | required: 慢provider下提交耗时、Redis失败/丢wake后poll及双worker fence；T-28 |
| AC-029 | 既有提交待当前候选复验 | 清理过时文档与重复AGENTS权威 | 每个删除文件有owner、内容迁移落点和无丢失硬约束证据；当前POM/App/Java测试源码动态库存与最终源码一致（源码数量不冒充JUnit运行数）；所有当前引用和cwd命令可解析；文档不把候选CI、未跑服务或未批准设计写成已完成 | not-required: 逐文件hash/规则去向/路径引用和事实检查直接覆盖文档交付；T-29 |
| AC-030 | 既有提交待当前候选复验 | 完成升级整体验收与可审查交付 | AC-001—AC-050全部有当前候选通过证据，报告18项全部有关闭结论；历史共同result及worktrees空缺已按事实处置，未伪造验收时点或新空提交；真实MySQL/Redis/MinIO及浏览器所需场景无required skip，full/core/三App同源候选完整；G-security-external和所有归档必需条件关闭，未批准风险不消失；部署、永久知识、归档移动按授权执行 | required: 同一候选完整运行SSO/Profile/workflow/Notify/Third/树/三App发布及失败恢复；T-30 |
| AC-031 | 既有提交待当前候选复验 | 修复企业转移发码的同步/排队合同阻断 | 真实Notify返回QUEUED时send不抛DELIVERY_FAILED，transfer与通知同事务提交；只有匹配用户、已提交transfer、有效验证码及ACCEPTED/DELIVERED通知可以确认；QUEUED/失败/过期不可确认；发送或确认事务失败后无错误绑定，Redis残留不可越权且可重新发码恢复；真实跨模块测试不固定mock ACCEPTED；重复确认不重复转移，PersonRebind/Captcha/TestSend状态各自准确 | required: 真Profile→Notify QUEUED、worker受理→确认及DB/Redis部分失败、重复确认/错用户/绑定变更；T-31 |
| AC-032 | 已确认设计、合法用户与隔离数据 | 移除受跟踪本地凭据并交付轮换清单 | git ls-files 不再包含本地真实配置；示例与公开产物无真实凭据；使用合成凭据覆盖配置加载与脱敏，缺 secret 不被弱默认值替代；轮换清单逐项记录环境、账号、依赖连接、负责人和证据状态；未执行保持未验证 | Git 跟踪清单、合成配置加载、日志脱敏；实际轮换是 G-security-external 的外部动作；T-32 |
| AC-033 | 已确认设计、合法用户与隔离数据 | 恢复精确来源的 CORS 默认边界 | 恶意 Origin 无许可响应头，受信来源与同源开发可用；生产通配+credentials 明确配置失败，无按请求 Origin 自动加入白名单；SSO Cookie/预检回归通过，桶权限不因后端 CORS 改动而扩大 | 真实 Servlet/CorsFilter 测试与 Spring profile 绑定；T-33 |
| AC-034 | 已确认设计、合法用户与隔离数据 | 关闭实时连接时仍加载本人消息盒子 | flag=false 仍有 GET inbox，后端实时服务关闭仍可读；退出 A 登录 B，A 延迟结果及错误不能污染 B；无 token 才清空；正文不重复，加载失败可重试且不冒充空列表 | 现有 push.test.ts、notice 组件、真实 Admin 登录→打开盒子；T-34 |
| AC-035 | 已确认设计、合法用户与隔离数据 | 打通短信内容快照与真实分发器合同 | 有效 SMS 实际适配器调用一次，模板参数正确且快照非空；空快照仍被拒绝；本地错误无 WAITING_RECEIPT；敏感值不进入普通日志、管理快照或 Evidence | DispatchNotificationServiceTest、NotifyDispatcherUnitTest、CaptchaNotifyCallerUnitTest 的真实跨层组合；T-35 |
| AC-036 | 已确认设计、合法用户与隔离数据 | 站内信落库与结果同事务且可安全重试 | 并发两个用户一条共享消息、每人一条关系；重复任务不增加关系；任一 SQL/提交失败不留下消息与结果部分提交，失效租约零写入；实时发送失败不回滚或重复已落库消息；本地暂时失败能重试收敛 | 扩展 NotifyAtomicResultIntegrationTest；真实 DSTransactional 代理、MySQL 双连接、提交故障与 AFTER_COMMIT；T-36 |
| AC-037 | 已确认设计、合法用户与隔离数据 | 让 Outbox 重试真正重新调用可重试供应商 | 拒绝且可重试→接受产生两次实际发送，非两次缓存异常；已接受重复及响应丢失 UNKNOWN 不盲目重发；Redis complete 故障、过期 owner 和不同 digest 保持可解释且安全 | NotifyIdempotencyDispatcherUnitTest、RedisNotifyIdempotencyStoreIntegrationTest、真实 runtime/Dispatcher 组合；T-37 |
| AC-038 | 已确认设计、合法用户与隔离数据 | 重试与取消接口准确定位资源和任务 | A/B 失败仅指定 A 排队；外部 UNKNOWN 拒绝，归属错误/ID 冲突拒绝；全部完成时状态不变且 queuedCount=0；重复请求无多份活跃 outbox；取消返回的对象与 URL 一致；鉴权和日志保持 | HTTP ID 冲突、真实 DB requeue 并发、API/领域合同测试；T-38 |
| AC-039 | 已确认设计、合法用户与隔离数据 | 让验证码与通知截止时间在发送前生效 | 未来消息不早发；now>=expiresAt 不调用供应商；验证码 TTL 与 command 截止一致；重试不延长有效期；过期 Outbox 可终結，不永久 READY/WAIT；转移/绑定确认仍拒绝旧码 | 可控时钟 runtime/worker、CaptchaNotifyCallerUnitTest、真实 DB 过期任务终结；T-39 |
| AC-040 | 已确认设计、合法用户与隔离数据 | 公告撤回停止待发且收件人读到正确快照 | 撤回先于发送执行权时无新请求，旧快照不变；后续重发布不受前版本取消影响；已进入外部 I/O 或已接受提示不可追回，已送达站内信仍可读；普通收件人只读本人消息，管理权限不扩大 | 真实发布→撤回→Worker→inbox；普通用户浏览器详情与越权；T-40 |
| AC-041 | 已确认设计、合法用户与隔离数据 | 本人收件箱提供完整分页与准确读取状态 | 501条全部可分页访问、顺序稳定、他人关系不可见不可改；unreadTotal 为本人真实未读总数；全部已读不限当前页且按钮说明清晰；顶部摘要与完整页已读结果同步；API/domain/typecheck 和生成合同一致 | 真实 MySQL分页＋HTTP 登录身份过滤＋前端分页组件/浏览器；T-41 |
| AC-042 | 已确认设计、合法用户与隔离数据 | 无链接邮件与授权附件完整送入适配器 | 无链接正文邮件发送；notice/workflow 必填 path 校验仍有效；一个/多个授权附件实际交给适配器；无附件 OSS 调用零次；越权/丢失/待删除附件拒绝；部分快照失败不发送且无孤儿资源；异步 worker 不能绕过原提交者身份，重试与去重不复制无限快照；全应用装配证明生产快照SPI存在；不能只用mock通过 | DemoNotifyCallerUnitTest、NotifyAttachmentDispatcherUnitTest、真实 owner/快照引用＋假邮件物理适配器；T-42 |
| AC-043 | 已确认设计、合法用户与隔离数据 | 测量公告批量写入并控制聚合成本 | 报告含同环境同样本前后SQL/时延/锁等待和正确性，未测量不写性能提升；无漏收件人/重复关系/错误聚合，回滚与租约测试不退化；单次上限、超限失败和压力证据可定位；不强设未经用户确认的SLA | 真实 MySQL代表规模、SQL计数、现有原子结果/fence回归；T-43 |
| AC-044 | 已确认设计、合法用户与隔离数据 | 上传下载与核心启动脱离 OSS 诊断前置条件 | 缺canary/诊断过期不阻断合法签名/上传；DB元数据仍零OSS调用；空/非默认坏存储不阻断核心启动，真实文件失败仍准确报错；无权用户/错误访问类型仍拒绝；启动不遍历存储作远端巡检 | OssStorageReadiness*、OssLifecycle*、OssUpload*；核心启动＋最小权限MinIO与health组；T-44 |
| AC-045 | 已确认设计、合法用户与隔离数据 | 存储诊断区分允许、拒绝和未知事实 | PUBLIC_READ策略不可读+对象可读不误报确定POLICY_MISMATCH；PRIVATE未知不宣称匿名写已禁止；复杂策略结果有明确边界；全程无破坏性探测及高权限自动申请 | OssAccessDiagnosticUnitTest、受限MinIO读权限与诊断展示；T-45 |
| AC-046 | 已确认设计、合法用户与隔离数据 | 恢复与清理共享对象锁以保护当前来源 | 两种竞争顺序均不删当前来源，恢复/清理结果明确；指针/工单更新冲突回滚；删除确认丢失能安全幂等重试；单对象超时/失败不锁住整个批次，真实MinIO目标对象仍可读取 | OssStorageMigrationIntegrationTest：真实MySQL两连接＋阻塞替身＋MinIO对象存在验收；T-46 |
| AC-047 | 已确认设计、合法用户与隔离数据 | OSS列表在回填前验证请求代次 | A最后返回不覆盖B的行/总数/预览；旧失败不覆盖B成功；组件卸载无回填，loading属于最新请求；权限、分页排序及预览生命周期不退化 | OssPage受控Promise组件测试，已有presentation.test.ts回归；T-47 |
| AC-048 | 已确认设计、合法用户与隔离数据 | 将日常启动与构建修复诊断分开 | 普通二次启动无深度哨兵/依赖重装/清缓存，显式repair仍可调用；SERVER_PORT不被脚本覆盖；缺工具/secret单一错误不泄密；安全删除拒绝越界、端口不乱杀、reactor/单模块启动正确 | shell fake命令夹具＋真实Linux初次/二次启动；Windows由支持环境实际验收；T-48 |
| AC-049 | 已确认设计、合法用户与隔离数据 | 配置保留真实差异并收敛无效重复项 | 每个删除配置有真实无消费者/代码默认证据，SINGLE与MULTIPART各可用；切默认存储不重路由历史对象，权限/类型/大小仍失败关闭；无canary可接入；无附件通知不创建OSS客户端；OIDC信任项不删 | OssUploadPropertiesUnitTest、配置绑定测试、默认存储切换真实MinIO；T-49 |
| AC-050 | 已确认设计、合法用户与隔离数据 | 收缩未兑现通知模式并迁移现有调用方 | 所有仓内合法业务提交仍成功；非支持策略/模式/优先级在持久化前拒绝；生产调用无非零优先级，时效字段与业务幂等不丢失；存量未支持任务有只读清单与受控处置，无永久WAIT/未知外部重发 | 公共入口负向合同、每类生产调用方、历史WAIT样本终结；T-50 |

## 5. 范围

### IN

既有31票重新核对并合法闭合，19新增垂直切片，报告18项全部处置，当前change所有活动文档同步，历史证据保留，最终集成与归档准备。凭据外部处置有单独Gate，不静默删除该风险。

### REUSE

既有App→web-domain→domain→platform、layered/classic、wta-api、BaseMapperPlus、DSTransactional、六SQL基座、Outbox/fence、回执HMAC、OSS引用/上传票据、真实服务测试。

### OUT

- **OOS-001** 本轮不实现、不提交/推送、不部署、不轮换或修复真实数据；未来执行逐动作核对授权。
- **OOS-002** OIDC升级只引用<Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/ADR.md</Path>。移交合同：可选内置issuer，业务App标准OIDC客户端，关内置可用外部IdP；用户注册不伪造统一OIDC私有注册API。这里只记录用户报告要求，不宣称协议实现或验收通过。
- **OOS-003** 不自动清Git历史、删除历史快照、清Redis幂等缓存、批量重发外部UNKNOWN或重放既有库基座。
- **OOS-004** 不以N-10风险为由重写架构；没有实测不得声称吞吐/延迟事故或容量保证。

AC-030同时保留原票要求：全部已接受AC均有实际命令/退出码/环境/源码checkpoint；required E2E与失败注入全部完成，not-run不能被标通过；未增加安全/类型豁免或删除测试制造绿色；用户能据artifact digest批准明确候选，未自动发布。

## 6. 已锁定实现约束

- **DEC-001** 最新用户只授权全面规划；Goal ready_for_execution=false。
- **DEC-002** 继承ADR-CR-008仓内直接切换，同步所有调用方/生成合同，不新增旧版兼容等待。真实数据处理仍单独批准。
- **DEC-003** 保留Client/用户关系授权、安全日志、对象owner及第三方协议；不通过关闭规则/删测试取绿。
- **DEC-004** 六份MySQL基座是唯一初始化源，新环境完整构建；既有环境只按源/目标Tag差异备份演练。
- **DEC-005** D-002—009已获用户明确答复并确认G共识；ADR-CR-010—019作为当前change计划合同。新发现高影响偏差回G，不授权实现者自行改变。

## 7. 数据、接口与兼容

分页变更：GET inbox响应改为项目分页结构，保留本人关系过滤、稳定排序和全局本人未读；前后端与生成物同版切换。retry资源以path为准，显式delivery归属、queuedCount和无操作真实状态。notification command新增附件字段及截止校验，未支持模式明确拒绝但同步现有非零priority消费者。

附件持久化与生产快照适配按D-009已确认的数据合同，以Notify意图/真实关系为owner，不复活sys_notify_log。消息关系唯一键已存在，本地原子写入优先复用，不重复添加索引。结果/回执/租约与既有状态保持真实单调性。数据结构变化只落唯一DDL/DML基座，在线数据修复另有清单及批准。

## 8. 非功能要求

- **NFR-001 安全隐私**：精确Origin、后端授权、脱敏Evidence；绝不验证已披露真实密钥的可用性来“证明风险”。
- **NFR-002 性能容量**：N-10按固定合成样本测量SQL/时延/锁等待，既有上限保留；不发明生产SLO。单对象OSS I/O有严格预算并测超时收敛。
- **NFR-003 可用性可靠性**：可选实时/存储不成为未使用它的业务前置；有限重试、提交故障、幂等与并发负向验证必需。
- **NFR-004 可观测性**：元数据/错误分类足以解释状态；计划验证与产品通过、模拟与真实服务、历史与当前候选分别记录。

## 9. 验证策略

| 接缝 | 层级 | 覆盖 | 现有先例/入口 | Evidence |
|---|---|---|---|---|
| 真实runtime→common→假供应商 | 跨模块 | AC-035/037/039/042/050 | <Path>backend/wta-admin/src/test/java/org/namewta/test/notify/core/NotifyDispatcherUnitTest.java</Path> | 调用次数、结果/失败分类及脱敏 |
| 真实事务/双连接/Redis | 集成 | AC-036/038/040/043 | <Path>backend/wta-admin/src/test/java/org/namewta/test/notify/NotifyAtomicResultIntegrationTest.java</Path> | SQL故障/提交故障、租约、关系与聚合 |
| 真实对象/最小权限/并发 | 集成 | AC-044—046/049 | <Path>backend/wta-admin/src/test/java/org/namewta/test/oss/migration/OssStorageMigrationIntegrationTest.java</Path> | 对象存在、指针、403/未知与超时 |
| 组件受控Promise＋浏览器 | 用户行为 | AC-034/040/041/047 | <Path>frontend/apps/admin-web/src/utils/push.test.ts</Path> | 本人会话/历史分页/旧响应 |
| 配置/启动/安全 | HTTP/脚本/装配 | AC-032/033/048 | <Path>scripts/ci/verify-dev-build-guard.sh</Path> | profile/CORS/二次启动/repair负向 |
| 同候选全仓与产物 | 集成/交付 | AC-001—050 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/verification.md</Path> | 新矩阵、manifest、源码、真实命令及零required skip |

## 10. 风险、假设与未决问题

### 风险

当前源码问题未动态复现，报告不证明现场唯一根因。旧31票共同result及空worktrees不能直接满足现行逐票完成门；真实轮换/目标环境未授权。不能将这些缺口隐入“归档时补”。

### 已采用的低影响假设

测试场景复用当前基座，100/1000/10000为测量样本，非生产SLO；局部类名由实现者按规范选择。

### 共识来源

<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/design-tree.json</Path>的D-002—009全部answered，status=consensus，LOG-018为用户明确共识。高影响设计未决项为零，ready_for_tickets=true；缺未来执行/commit授权是Goal运行条件，不是Spec设计缺口。

### 未决问题

无。
