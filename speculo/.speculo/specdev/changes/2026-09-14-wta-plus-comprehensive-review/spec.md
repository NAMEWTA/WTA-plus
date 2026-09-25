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
| AC-004 | 当前源码已满足，无新实现；证据 `evidence/T-04-replan-2026-09-23.md`；T-30 仍负责组合复验 | 建立可信代理来源IP合同 | 任意外来XFF不改变直连或正常入口的授权结果；可信双代理解析一致，非法值失败关闭；白名单与限流使用同一个规范来源；未获得环境资料时不得把likely改成已复现越权 | required: 隔离单/双Nginx链发IPv4/IPv6和伪造XFF请求，比较白名单、限流、审计来源；T-04 |
| AC-005 | 当前源码已满足，无新实现；证据 `evidence/T-05-replan-2026-09-23.md`；T-30 仍负责组合复验 | 修复防重键过期后的所有权竞态 | A失败不得删除B的键；正常失败可重试，正常成功TTL内被拒；异常/线程复用无ThreadLocal遗留；DB唯一约束和通知业务幂等不被此注解替代 | required: 真实Redis以屏障复现A过期/B接管/A失败/C被拒及正常失败重试；T-05 |
| AC-006 | 当前源码已满足，无新实现；证据 `evidence/T-06-replan-2026-09-23.md`；T-30 仍负责组合复验 | 强化SSO令牌随机性与Cookie安全 | 生产实现不含ThreadLocalRandom/雪花ID作为bearer；已接受的PKCE/client/version/expiry/单次兑换负向测试全部保留；HTTPS生产Cookie属性成立，开发HTTP例外不可进入prod；CORS拒绝未知origin，登录不会因Cookie错误进入循环 | required: HTTPS隔离SSO验证Cookie、CORS、PKCE、过期与单次兑换；显式dev HTTP例外；T-06 |
| AC-007 | 当前候选 `450c9d11`；证据 `evidence/T-07-replan-2026-09-23.md`；T-30 仍负责组合复验 | 修复SSO回调编码与可恢复登录旅程 | 复杂state往返相等且无重复code/state参数；过期、错state、错误verifier均失败关闭且用户可重新授权；成功回到原App内路径，带外域returnTo被拒；日志与UI不暴露code/verifier/token | required: 专用SSO Playwright跑/admin与/home base、复杂state、过期与重新登录；T-07 |
| AC-008 | 当前源码已满足，无新实现；证据 `evidence/T-08-replan-2026-09-23.md`；T-30 仍负责组合复验 | 补齐SSO独立Origin发布合同 | 每个shipped App均有且仅有完整配套，缺项在promotion前失败；SSO三端跳转、刷新、过期、跨OriginCookie可运行；已发布current在配置失败时不改变；发布清单与Compose、Nginx、文档一致 | required: 本地三Origin HTTPS部署候选，登录、刷新、过期恢复与health可用；T-08 |
| AC-009 | 当前候选 `a139dbc5`；证据 `evidence/T-09-replan-2026-09-23.md`；默认套件 247 个服务门控 skip 不计通过；T-30 仍负责组合复验 | 统一App模式、bundle与依赖服务验证矩阵 | build:dev最终三个App均development，build:prod均production；core/full产物必需与禁用列表逐项断言；测试证据与打包候选匹配，不以skip冒充测试通过；CI与部署镜像版本相同或有明确差异测试 | not-required: 构建产物扫描与脚本负向夹具覆盖；线上组合由T-08/T-30验收；T-09 |
| AC-010 | 当前源码已满足，无新实现；证据 `evidence/T-10-replan-2026-09-23.md`；T-30 仍负责组合复验 | 删除混源局部发布并实现原子stage | manifest每个artifact的digest和source可追溯；缺模板/坏SQL/坏JAR/中断时current/context SHA256完全不变；单目标构建不能stage为完整release；恢复只操作本次stage，不触及他人文件或历史发布 | required: 隔离文件系统完整stage、缺件/中断/混源注入、固定版本消费者与容器重建恢复；T-10 |
| AC-011 | 当前源码已满足，无新实现；证据 `evidence/T-11-replan-2026-09-23.md`；T-30 仍负责组合复验 | 删除浏览器共享私钥与ECB传输包装 | 生产bundle不再携带该共享响应私钥或ECB路径；正常登录、注册、错误响应及下载可运行；机器调用HMAC与OSS签名不被删除；传输切换有前后端同批发布、流量隔离与整体恢复方案（不承诺多容器原子热更新） | required: 两App登录注册/下载/错误回归及生产bundle密钥/ECB扫描；T-11 |
| AC-012 | 当前候选 `89f94a9b`；证据 `evidence/T-12-replan-2026-09-23.md`；T-30 仍负责组合复验 | 统一幂等会话清理与导航恢复状态 | logout超时/401/离线时本地token和动态路由仍清空；空角色账户每次恢复最多一次，不循环replace；切Client无旧菜单/权限残留，服务端授权仍为最终门禁；并发401只有一次恢复流程并可终止 | required: 离线/超时logout、并发401、空角色恢复和切Client重登；T-12 |
| AC-013 | 当前候选 `f2f166c9`；证据 `evidence/T-13-replan-2026-09-23.md`；T-30 仍负责组合复验 | 完成Home注册开关与验证码重试交互 | 服务端关闭注册时入口与页面均准确，后端仍拒绝直接调用；验证码错误后无需整页刷新即可再次成功；慢旧captcha不能覆盖新uuid；提交/取消/失败后按钮状态恢复 | required: 禁用注册直接访问、验证码错误后刷新重试、慢旧响应及键盘提交；T-13 |
| AC-014 | 当前源码已满足，无新实现；证据 `evidence/T-14-replan-2026-09-23.md`；T-30 仍负责组合复验 | 补齐个人与企业自助认证材料闭环 | 新个人CN_RESIDENT_ID上传正反面后完成提交；企业必填及条件材料齐备时完成提交，缺项定位准确；取消/失败/过期OSS/刷新不会伪造完成或越owner访问；后端必填校验不被关闭，真实浏览器+MySQL+OSS验收通过 | required: 新个人身份证双面、企业条件材料经真实MySQL/OSS提交，刷新/失败/越权覆盖；T-14 |
| AC-015 | 当前源码已满足，无新实现；证据 `evidence/T-15-replan-2026-09-23.md`；T-30 仍负责组合复验 | 移除Profile过渡接口与测试构造桥 | 旧入口引用为零且替代能力覆盖完整；全Profile及主要消费者编译/测试通过；当前接口不再让缺owner上下文的调用编译通过；删除清单逐项有替代入口/调用方/验证证据 | not-required: 方法级迁移表、全Profile/消费者编译和原行为测试覆盖，无独立新交互；T-15 |
| AC-016 | 当前候选 `b5cd3db5`；证据 `evidence/T-16-replan-2026-09-23.md`；T-30 仍负责组合复验 | 保证流程任务读取和办理对象一致 | B失败绝不发出A的审批请求；陌生用户读取任务节点/变量被拒，合法办理人/发起人按既有规则可读；双击/过期任务不重复推进流程；快速切换、关闭、网络乱序与服务端失败可恢复 | required: 真引擎授权矩阵及快速A/B切换、B失败、确认期间切任务、重复办理；T-16 |
| AC-017 | 当前候选 `90c80ca6`；证据 `evidence/T-17-replan-2026-09-23.md`；T-30 仍负责组合复验 | 收紧流程设计器消息来源 | 错误origin、错误source、未知payload不能关闭标签；合法设计器close仍工作；设计器刷新/卸载无重复listener；不把未来save/publish消息自动加入允许列表 | required: 真实iframe发合法close及外部window伪造消息，刷新卸载不残留；T-17 |
| AC-018 | 当前源码已满足，无新实现；证据 `evidence/T-18-replan-2026-09-23.md`；T-30 仍负责组合复验 | 明确上传完成、引用移除和导入失败生命周期 | 下载URL失败不生成无人回收的Blob URL，也不把已完成上传误报失败；导入网络失败、业务错误、401和取消均复位且可重试；业务提交等待上传与材料归属登记完成；现有OSS删除权限/引用保护不退化；若保留本地URL则replace/remove/unmount释放 | required: 真实上传后URL失败、导入网络/业务/401/取消、重试与资源回收；T-18 |
| AC-019 | 当前源码已满足，无新实现；证据 `evidence/T-19-replan-2026-09-23.md`；T-30 仍负责组合复验 | 收敛System大页面中的异步状态与重复封装 | 快速筛选时旧响应不能覆盖新列表，失败/loading可恢复；权限树与用户编辑状态相互独立；提取前后功能/权限/排序/分页行为相同；每个超过1k行文件有职责删除或保留理由，无同义薄wrapper堆叠 | not-required: 受控Promise组件测试覆盖乱序和取消，既有App回归由T-30执行；T-19 |
| AC-020 | 既有提交待当前候选复验 | 收紧已触及的TypeScript合同边界 | 受影响边界类型检查通过，nullable与非法transport样本有明确处理；不增加双cast、ignore或检查排除来消除诊断；全量现有typecheck仍通过；严格化范围与诊断记录一致；未触及存量严格债明确列出，不冒充全仓strict完成 | not-required: 受影响包诊断、typecheck和边界单测直接覆盖；相关业务E2E属于原票；T-20 |
| AC-021 | 当前候选 `cfb57baa`；证据 `evidence/T-21-replan-2026-09-23.md`；T-30 仍负责组合复验 | 统一公开页面的可访问交互基线 | 只用键盘可完成公开流程，焦点可见且错误能被读屏发现；移动/放大页面无遮挡必要动作和横向不可达内容；不同App品牌差异不被当成bug强制同化；报告记录真实截图/可访问性结果，静态检查不冒充视觉通过 | required: 320/768/1440、200%缩放、键盘/读屏语义和渲染对比检查；T-21 |
| AC-022 | 当前源码已满足，无新实现；证据 `evidence/T-22-replan-2026-09-23.md`；T-30 仍负责组合复验 | 原子提交通知投递结果与lease fence | 任意一条SQL失败不会留下Delivery/Attempt/Outbox不一致；旧lease不能覆盖新owner结果；双worker只产生符合合同的结果记录；provider去重能力不足时风险明确且不宣称exactly-once | required: 真实MySQL/Redis双worker、过期/reclaim、Attempt失败与finish冲突注入；T-22 |
| AC-023 | 当前源码已满足，无新实现；证据 `evidence/T-23-replan-2026-09-23.md`；T-30 仍负责组合复验 | 将供应商回调幂等纳入持久事务 | 回滚后相同事件可重试成功；跨实例相同事件不重复生效，不同provider同ID互不影响；乱序/伪签名/迟到回调按现有规则失败关闭或no-op；重启不丢幂等记录，聚合与delivery一致 | required: 两实例重启/并发重复/跨provider同eventId/提交失败/早到回调；真实签名适配；T-23 |
| AC-024 | 当前源码已满足，无新实现；证据 `evidence/T-24-replan-2026-09-23.md`；T-30 仍负责组合复验 | 恢复Third并发租约并明确限额热更新 | 崩溃后permit在规定上限内恢复，不依赖人工删key；正常/超时/异常释放正确，旧请求不伤害新租约；管理阈值变更在声明时机跨实例一致生效；Redis不可用时按既有失败合同拒绝，日志不回显凭据 | required: 隔离Redis kill进程、嵌套申请失败、降低并发在途收束、rate窗口更新；T-24 |
| AC-025 | 当前源码已满足，无新实现；证据 `evidence/T-25-replan-2026-09-23.md`；T-30 仍负责组合复验 | 保证部门移动无环且并发一致 | 自父/后代父/并发互移均不能形成环；合法跨支移动及根移动保持正确ancestors；权限数据范围不因错误树扩大；失败时整个子树维持原状态 | required: 真MySQL并发同树/跨根移动及注入后代更新失败，树不变量成立；T-25 |
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
| AC-043 | 当前候选 `9267ecb5`；证据 `evidence/T-43-replan-2026-09-23.md` | 测量公告批量写入并控制聚合成本 | 报告含同环境同样本前后SQL/时延/锁等待和正确性，未测量不写性能提升；无漏收件人/重复关系/错误聚合，回滚与租约测试不退化；单次上限、超限失败和压力证据可定位；不强设未经用户确认的SLA | 真实 MySQL代表规模、SQL计数、现有原子结果/fence回归；T-43 |
| AC-044 | 已确认设计、合法用户与隔离数据 | 上传下载与核心启动脱离 OSS 诊断前置条件 | 缺canary/诊断过期不阻断合法签名/上传；DB元数据仍零OSS调用；空/非默认坏存储不阻断核心启动，真实文件失败仍准确报错；无权用户/错误访问类型仍拒绝；启动不遍历存储作远端巡检 | OssStorageReadiness*、OssLifecycle*、OssUpload*；核心启动＋最小权限MinIO与health组；T-44 |
| AC-045 | 已确认设计、合法用户与隔离数据 | 存储诊断区分允许、拒绝和未知事实 | PUBLIC_READ策略不可读+对象可读不误报确定POLICY_MISMATCH；PRIVATE未知不宣称匿名写已禁止；复杂策略结果有明确边界；全程无破坏性探测及高权限自动申请 | OssAccessDiagnosticUnitTest、受限MinIO读权限与诊断展示；T-45 |
| AC-046 | 已确认设计、合法用户与隔离数据 | 恢复与清理共享对象锁以保护当前来源 | 两种竞争顺序均不删当前来源，恢复/清理结果明确；指针/工单更新冲突回滚；删除确认丢失能安全幂等重试；单对象超时/失败不锁住整个批次，真实MinIO目标对象仍可读取 | OssStorageMigrationIntegrationTest：真实MySQL两连接＋阻塞替身＋MinIO对象存在验收；T-46 |
| AC-047 | 已确认设计、合法用户与隔离数据 | OSS列表在回填前验证请求代次 | A最后返回不覆盖B的行/总数/预览；旧失败不覆盖B成功；组件卸载无回填，loading属于最新请求；权限、分页排序及预览生命周期不退化 | OssPage受控Promise组件测试，已有presentation.test.ts回归；T-47 |
| AC-048 | 当前候选证据 `evidence/T-48-replan-2026-09-23.md`；Windows `user-waived/not-run` | 将日常启动与构建修复诊断分开 | 普通二次启动无深度哨兵/依赖重装/清缓存，显式repair仍可调用；SERVER_PORT不被脚本覆盖；缺工具/secret单一错误不泄密；安全删除拒绝越界、端口不乱杀、reactor/单模块启动正确 | shell fake命令夹具＋真实Linux初次/二次启动；Windows实机运行用户明确豁免（user-waived/not-run，不计测试通过）；T-48 |
| AC-049 | 当前候选证据 `evidence/T-49-replan-2026-09-23.md`；MinIO 路由 1 项与属性 4 项均 0 skip | 配置保留真实差异并收敛无效重复项 | 每个删除配置有真实无消费者/代码默认证据，SINGLE与MULTIPART各可用；切默认存储不重路由历史对象，权限/类型/大小仍失败关闭；无canary可接入；无附件通知不创建OSS客户端；OIDC信任项不删 | OssUploadPropertiesUnitTest、配置绑定测试、默认存储切换真实MinIO；T-49 |
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

## revision202 — AC-044验收暴露的日志边界

签发URL及上传令牌只交给已授权业务调用者；HTTP/操作日志副本不得保留带凭据或签名的URL、上传令牌路径。SQL诊断保留结构/mapper/耗时/异常类别，不能通过绑定值或错误消息输出OSS secret。业务响应与实际SQL保持原行为。此为AC-002安全合同在AC-044真实路径的落实，责任仍T44/T30，不新增票。

## revision203 — SQL日志安全摘要

AC-002/AC-044中的SQL结构诊断限定为命令类型枚举、受限Mapper ID、耗时和异常类型。BoundSql模板可包含已展开的动态值，禁止记录其原文本；也禁止参数值及异常原文。保留执行语义与现有日志输出配置，具体回归与真实日志扫描由T44承担。

## revision205 — T-45启动与范围登记

revision205：T44已完成（结果fc50c1e，治理4c93a2f）；T45激活并先复现403/未知事实红灯。15done/2cancelled/1in_progress/32ready；Goal active，未完成或归档。

基线 `4c93a2fb9c7d2c1d68a0c8194197e2af1908b4d2`，main/current/direct-parent。T44 已提供单配置管理员 POST 与安全三字段 VO；本票将该 VO 扩展为有来源/范围/时间的安全事实投影，正式重新生成 OpenAPI 与前端映射。方法、权限、正ID和审计禁正文保持。策略/ACL 403不能伪装空策略；单对象匿名 HEAD/GET 仅陈述该对象，404/超时/网络异常/重定向不得推断匿名拒绝。PRIVATE未知不能宣称全桶安全或匿名写已禁止；PUBLIC_READ对象可读而策略不可读不能确定POLICY_MISMATCH。

仅解释明确、无条件且匹配目标资源的策略子集，尊重 Deny，Condition/Not*/不明Principal/Action/Resource及坏JSON保留UNKNOWN；可识别危险写以有界风险警告表达，不称实际PUT成功。保留每个独立读取的部分事实；不匿名PUT/DELETE、不提权、不影响T44业务/核心就绪解耦。每网络步骤100ms–3s/最多5步的已接受预算保持，无未回收后台任务。

公开 Java 结果按已确认仓内同步切换决定迁移全部实际调用者，java-api-compatibility用于调用清单/语义/编译核查，不新增已被用户排除的兼容桥。写集从6扩为17条：模型/Javadoc、管理VO与对应HTTP测试、domain transport/测试/出口、正式OpenAPI生成、运行文档及system模块事实。目录授权仅限本票行为，原快照不可覆盖。新增其他文件先回Lead登记。

Dispatch01A仅可改 `backend/wta-admin/src/test/java/org/namewta/test/oss/readiness/OssAccessDiagnosticUnitTest.java`，用旧公开合同可编译断言复现PUBLIC_READ+policy/ACL403误报和PRIVATE未知误称writeDenied；生产不改。Lead固定红测试提交并实际运行后再给01B写锁。legacy_audit独立只读合同审查；ops_audit仅准备隔离驱动。Lead独占治理/提交/Maven/pnpm/服务与生成。最终需受限MinIO真实零skip、HTTP权限/事实/无写调用、真实管理页面与OpenAPI同源、默认测试/full-core/前端适用门禁和cleanup。当前尚无T45实施/验收结果。

## revision206 — 红灯与Dispatch01B

revision206：T45红灯在73edcbff复现（8例/2预期failure/0error/0skip，源码前后clean）；登记三态事实投影和Dispatch01B。15done/2cancelled/1in_progress/32ready，完整候选attempts0，Goal active。

两条失败是PUBLIC_READ+策略/ACL403被误判MISMATCH，以及PRIVATE+403被误判VERIFIED；其余6项旧合同测试通过。保留red01/manifest.json，不计完整候选失败，不勾AC。

实现合同：公开OssAccessDiagnostic同批改为verification/reason/expectedAccessPolicy/checkedAt及不可变facts；每条事实包含subject、observation(ALLOWED/DENIED/UNKNOWN)、source、scope(OBJECT/BUCKET)、observedAt和固定basis。basis区分文档缺失、不可读、复杂/坏格式、超时、HTTP观察类别；不返回原始policy、bucket/key、URI、凭据、错误文本。subjects表达POLICY_READ/POLICY_WRITE限定文档声明、ACL_LIST桶列表授权、ACL_WRITE_RISK桶写入/ACL修改危险声明、OBJECT_HEAD/OBJECT_GET单对象实测。文档里的Allow不等于实际操作允许；NoSuchBucketPolicy只证明文档缺失，普通404不是同义结论。

bucket ACL READ只表示列对象而非GetObject，WRITE/WRITE_ACP/FULL_CONTROL作为危险声明警告；不因缺Allow或读文档403推断DENIED。明确支持资源/Principal/Action且无Condition/Not*的策略子集才可作限定声明，考虑Deny优先及重叠未知，不能以忽略条件的Allow宣称有效授权。PRIVATE仍有未知时不得声称全桶安全或匿名写已禁止。

匿名HEAD/GET的401/403只证明该对象该次DENIED；404/3xx/5xx/timeout/网络异常UNKNOWN，禁止自动重定向。两步独立保留部分事实，中断保留线程状态并停止后续网络操作；请求超时释放自身资源，不起遗留后台探测。总网络步骤至多5及每步100ms–3s预算保持。

管理POST/权限/正ID/审计禁正文不变；VO在status/reason/checkedAt外增加安全facts，Service私有Evaluation(entry+facts)，registry保留既有summary合同与revision fence。前端通过既有ossConfigs transport解析unknown为域模型，页面按需执行、明确事实来源范围/时间，处理失败、切换和卸载，不用旧响应覆盖当前上下文。正式OpenAPI由Lead捕获生成。

Dispatch01B：cors_audit唯一产品writer，使用Ticket17条写集，迁移全体Java构造/调用方、HTTP合同测试、domain transport与页面、文档；不写治理、不跑构建/服务/提交、不手改生成物。新增写集先回Lead。Lead固定源码后定向green/default/full、真实受限MinIO+HTTP/UI/安全与cleanup、正式OpenAPI、前端/core/静态验收；legacy_audit只读，ops_audit仅私有驱动。

协议依据：[AWS S3 ACL权限表](https://docs.aws.amazon.com/AmazonS3/latest/userguide/acl-overview.html)、[AWS策略显式Deny评估](https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_evaluation-logic_policy-eval-denyallow.html)。只用其约束解释范围，不宣称实现完整云端IAM计算。

## revision207 — 会话隔离与验证检查点

revision207：T45首轮OSS定向164例/1failure/0error/0skip；11c6e9e已修正500断言与HTTP事实依据，尚未重测。登记同用户新会话隔离修复写集，15done/2cancelled/1in_progress/32ready，完整候选attempts0，Goal active。

独立固定8dc775b审查发现：仅userId/权限相同不足以识别重新登录；Admin既有sessionGeneration和identityLoaded应以只读runtime合同投影，禁止web-domain读取token或App Store。新增写集为 `frontend/packages/web-domains/system/src/runtime.ts`, `frontend/packages/web-domains/system/src/index.test.ts`, `frontend/apps/admin-web/src/router/adminManifestRegistry.ts`, `frontend/apps/admin-web/src/router/adminManifestRegistry.test.ts`。仅扩展SystemWebRuntime、Admin装配及合同测试，不改认证Store或路由业务。页面发起时要求身份已加载；响应完成比较会话代次、用户、权限和局部请求版本；代次变化/identityLoaded变false时取消请求并清除已呈现结果。测试覆盖同一userId、同权限而会话变化，以及未加载期间拒绝发起。

Dispatch01C：cors_audit唯一产品writer，限上述4文件及原oss-config页面/测试；Lead负责提交与串行测试。green01与只读审查原始证据保留于checkpoint207/manifest.json；164例只有163通过，不记完整候选通过，不勾AC。11c6e9e同时将供应商HTTP错误按状态投影固定basis，避免误称网络错误。后续必须定向重测、真实受限MinIO+HTTP+UI、正式OpenAPI及适用质量门禁。

## revision208 — 真实公开域名回归

revision208：T45候选A1真实HTTP失败（公开域名重复桶路径，匿名HEAD/GET404，signedHEAD200，Policy/ACL403）；cleanup0。定向166全通过、前端110通过、默认1135/216skip/full通过，不能替代真实验收。15done/2cancelled/1in_progress/32ready，完整候选attempts1，Goal active。

固定候选081c75ff、完整JAR的真实受限MinIO结果e9853018a23b0d59：恰好5个只读请求，Policy/ACL403、signedHEAD200、匿名HEAD/GET404。根因anonymousReadFacts使用getBucketUrl(bucket)，path-style与已绑定桶的domainUrl一起重复拼桶；DefaultOssObjectStore.publicUrl及现有OssAccessUrlProviderUnitTest明确自定义域名已绑定桶。产品诊断应遵循相同访问地址语义，保留结构化对象键编码、尾斜杠规范化，拒绝userinfo/query/fragment；不改上传旧getBucketUrl全局合同、不放宽真实HTTP断言。

Dispatch01D：cors_audit唯一产品writer，仅原写集AbstractOssClientImpl.java及OssAccessDiagnosticUnitTest.java，修正诊断URL选择并以真实本地HTTP精确路径补测：path-style + bucket-bound domain + slash/空格/加号/片段字符键只编码一次、尾斜杠、无自定义域名回退，以及无效域名不发匿名请求。已有真实失败即回归红灯。Lead固定后重新跑受影响门禁和真实HTTP，最多3次完整候选的复盘门槛保持。

A1完整证据在candidate-a1/manifest.json。green02 34类166/0/0/0；fronttarget02 domain12+web17+Admin81全pass且前后clean。fronttarget01三个命令也pass，但Admin子集自动导入生成器删4声明导致包装源校验失败，保留差异且只恢复自身生成文件，改跑Admin全套后clean。默认reactor262类1135例，919执行/216环境skip，0failure/error；full clean package与bundle通过。独立081审查已关闭same-user generation finding，SSO callback候选反例经顶层路由卸载证据撤回。真实HTTP A1未通过，未生成新OpenAPI、未跑真实UI、未勾AC。

## revision209 — A2结果与第三候选边界

revision209：T45候选A2真实受限HTTP通过/cleanup0，168定向与默认1137（216环境skip）/full通过，正式OpenAPI仅加Fact及VO facts；前端全量typecheck发现诊断handler要求完整VO导致DefaultRow不兼容，A2尚未验收。15done/2cancelled/1in_progress/32ready，attempts2，Goal active。

backend d1fd57fb，生成候选9c567ba0。真实run6e5d5c24c38c793d确认公开匿名HEAD200/GET206、两私有桶HEAD/GET403、三个配置Policy/ACL403皆UNKNOWN，每次5个只读请求，旧A/新B对象路由及权限/审计/日志检查通过。18类凭据canary零命中、cleanup[]，默认SSE schema438路径/450 schemas已正式fetch/generate/check/typecheck，无现有路径删改。

全量前端architecture(33包)/architecture tests101/openapi/lint通过，Admin vue-tsc对OssConfigPage.vue:127报DefaultRow不能传给完整OssConfigVO。Dispatch01E由Lead唯一writer，仅原oss-config页面/测试：输入收窄Partial<OssConfigVO>并先拒绝缺失ID，保留后端权限和既有session fence，补缺失ID不发请求测试。不放宽编译器、不用断言强转绕过类型。

第三完整候选继续；若仍失败，先执行三次尝试复盘，不自动无限重试。后端输入未变时以精确tree等价复用A2默认/定向/HTTP证据，明确两个SHA；最终源码重新full打包与前端完整门禁、真实UI、core/static及两类真实JUnit（ReadinessMinio与AccessUrlMinio）必须实际通过。最后两类使用owned bootstrap兼容回归，不替代受限身份HTTP证据。A1和A2原始证据保持，不勾AC。

## revision210 — 三次复盘与Dispatch02

revision210：T45前批3候选停止并完成四项复盘；C3前端771/三App329/full通过，但Chrome1pass1fail、0skip/flaky、cleanup0，脚本旧文案与页面不匹配，首失败行未保留故不猜测。Dispatch02仅修私有验收脚本；恢复批attempts0，原3次永久保留。15done/2cancelled/1in_progress/32ready，Goal active。

权威恢复入口：`evidence/dispatch-T-45-recovery.md`。C3证据与安全独立意见在candidate-c3/manifest.json；C3 full JAR与311文件Admin dist绑定同源，浏览器失败不能被其它门禁替代。新恢复批先冻结私有UIv3、再严格同源验证；产品保持C3，前批3次不可追认为通过。

## revision211 — 实测失败与Principal解析修复

revision211：T45恢复R1 Chrome行数失败、R2 owned清理SQL排序规则失败均保留；真实2JUnit有1项POLICY_READ失败。独占MinIO与Jackson复核定位对象Principal的asText异常，Dispatch03仅修解析类型守卫与回归测试；恢复attempts2不重置。15done/2cancelled/1in_progress/32ready，Goal active。

权威补充派单：`evidence/dispatch-T-45-parser.md`；完整失败与探针证据：`evidence/T-45-current-2026-09-23/recovery-r1-r2/manifest.json`。前次Dispatch02冻结产品决定被此实测缺陷的窄修取代，非扩大合同；既有失败不追认、attempts不静默清零。

## revision212 — T-45当前候选验收完成

revision212：T45在09be6db完成当前候选验收；170定向、默认1139（923执行/216环境skip）、两真实MinIO JUnit和Chrome2零skip、受限HTTP/18类canary、full/core通过；前端771门禁按树等价复用并同SHA重建329产物。16done/2cancelled/32ready，无in_progress；下一T46，Goal active，change未完成或归档。

结果 `09be6db6c3bace8594f7db0fe49a221e3cc5f140`，tree `2d5459e7dc77ec4120516d97a63de17eee390be4`，base `4c93a2fb9c7d2c1d68a0c8194197e2af1908b4d2`。完整Evidence为 `evidence/T-45.md` 与 `evidence/T-45-current-2026-09-23/complete-recovery-r3/manifest.json`。前批3次、恢复R1/R2及旧两JUnit失败永久保留；恢复R3是真实通过候选，无push/deploy/生产操作/归档。

## revision213 — T-46启动与共同互斥合同

revision213：T45已验收完成（产品09be6db、治理4a8fea8c）；T46激活，以恢复/清理竞争红灯起步。16done/2cancelled/1in_progress/31ready，Goal active，未完成或归档。

基线`4a8fea8c19392972ee5cfef4263962ff6b0e3bfb`，main/current/direct-parent。实现仍复用sys_oss对象锁及现有工单，不新增schema、队列/分布式状态机。所有unpublish/rollback/process切指针及cleanup统一Object→Item锁序，在被Spring代理的public DSTransactional边界重读对象/工单，核对ACTIVE、当前service、source/target/key、最新工单、版本/状态和安全窗口；指针与工单条件更新均须恰1行且同事务，不能吞CAS失败。批次逐对象处理，不持整批锁。

严格超时不能证明供应商DELETE立即取消。为满足“清理获得合法执行权后恢复拒绝”和“删除已成功但DB提交失败可安全重试”，先在对象锁事务中用既有FAILED + lastErrorStage=COMPLETED + 固定CLEANUP_OUTCOME_UNKNOWN持久化执行/未知栅栏，提交已知后才发有界DELETE；不确定提交不发DELETE。完成再按Object→Item与同版本收敛COMPLETED。超时、响应/提交确认丢失保留栅栏，所有恢复/切指针路径拒绝绕过；后续显式cleanup可用有界HEAD确认来源缺失、目标有效后仅finalize，来源仍在或读取未知不盲重发DELETE或恢复来源。这是已有工单字段的保守执行权，不引入新的分布式状态平台；SDK cancel不等于撤销远端副作用。

来源存在与核对HEAD、DELETE均采用明确Duration预算，复用OssClient/Abstract最小重载及迁移ObjectStore端口；普通OSS调用语义保持。读写I/O不跨整批持锁，副作用前后只锁单对象；copy/verify不借此扩张重写。写集从5扩到10：两common API/实现、client测试目录、backend运行文档和system事实。java-api-compatibility用于新增有界操作及仓内消费者核对，不新建旧API兼容桥。HTTP形状/权限/公开副本提示原则上保持；实际合同变化须先报Lead。

Dispatch01A仅修改既有OssStorageMigrationServiceUnitTest.java，使用可阻塞对象删除和两个调用者固定旧时序，证明旧cleanup已进入删除时unpublish仍能恢复来源而后被删；锁获胜顺序和条件更新失败后续用真实MySQL双连接/真实DSTransactional代理及MinIO补验。当前生产代码不动，不宣称内存替身能证明DB锁。Lead固定红灯后派01B完整实现。cors_audit唯一writer；legacy_audit只读合同审查；ops_audit仅私有隔离驱动准备。Lead独占治理、提交、构建、真实服务。

必需门禁：受影响OSS单元/合同、默认后端、full/core；真实MySQL两物理连接证明共同锁、两竞争顺序、指针/工单CAS0与回滚、旧工单/未知栅栏、超时及晚到DELETE、提交确认丢失和安全finalize；第二对象仍能推进，MinIO目标字节可读。只有fresh XML零skip/ownedcleanup及双轴审查完成才勾AC；最多3次完整候选失败先四项复盘。当前attempts0。

## revision214 — 红灯与Dispatch01B

revision214：T46红灯在0f8729d复现，15例/1预期failure/0error/0skip，源码前后clean；DELETE已进入后旧unpublish返回成功。Dispatch01B开始共同锁、持久UNKNOWN和有界I/O实现。16done/2cancelled/1in_progress/31ready，完整候选attempts0，Goal active。

权威实施包为 `evidence/dispatch-T-46-implementation.md`；红灯和写入口清单见 `evidence/T-46-current-2026-09-23/red01/manifest.json`。没有真实服务或完整候选通过记录，AC未勾。

## revision215 — 配置身份与红灯证据边界

revision215：T46补充配置物理身份保护，写集10扩14；未结束迁移保护source/target配置，关闭编辑/删除与新工单创建竞态。实现仍由cors_audit独占，尚未完整候选验证；16done/2cancelled/1in_progress/31ready，attempts0，Goal active。

配置引用不能只计算当前sys_oss.service：迁移切换后仍须保护未终结工单的source/target，FAILED（含CLEANUP_OUTCOME_UNKNOWN）继续持有引用，不能改名、删除或改向另一物理存储后把404误判为原来源已删除。已有对象引用保护同步覆盖影响物理寻址的endpoint/isHttps/region及原configKey/bucket/accessPolicy；凭据按同一存储身份轮换仍允许，不能把普通密钥轮换等同身份迁移。域名等字段按实际调用面审查，避免无关冻结。

创建工单须与配置编辑/删除形成真实互斥，配置锁统一稳定顺序且先于Object→Item；其它既有工单事务保持Object→Item，不引入逆序Config锁。配置批删须避免首项普通读建立RR快照后、后续配置锁等待期间新增引用漏检；先取得全部配置锁再读引用，或采用有证据的等价当前读方案。真实MySQL验证创建与编辑/删除竞争、切换后源配置引用、UNKNOWN及正常凭据更新；不以Mapper字符串或mock断言替代竞争证据。

产品写集新增SysOssConfigMapper.java、SysOssConfigServiceImpl.java、oss/config测试目录、System AGENTS.md，共14根；只补本票安全边界与事实，不扩展配置架构。既有common/事务/Java API技能绑定仍适用。

证据精度更正：revision213“恢复来源而后被删”是风险描述，red01 FakeObjects.delete仅计数与等待，没有物理删除字节。该红灯只实证DELETE进入后旧unpublish成功；不能当作来源已不存在证据。真实MinIO HEAD/GET将在最终集成补验，旧记录原字节保留。

## revision216 — 有界缺失证明与Dispatch02

revision216：T46候选4f8c4b4a定向174例1error（Region空值误拒轮换）；窄修f6f8dd8后174例全通过零skip。独立审查发现源桶404可误归对象缺失，Dispatch02补有界桶存在核对。完整候选attempts0，16done/2cancelled/1in_progress/31ready，Goal active。

只强化新Duration版headObject：对象HEAD404之后在同一剩余总预算内HEAD Bucket；桶可达才将对象404作为OBJECT_NOT_FOUND。桶不存在、拒绝、超时或未知均保守报告PROVIDER_ERROR，迁移保留UNKNOWN且不finalize/重DELETE。SDK两请求各以剩余预算限制总/attempt timeout并有界await；普通旧OSS调用语义保持。不增加接口/schema/路径，14根写集不变。补受控404/403/timeout单元负例和owned真实缺桶负例；更新运行限制说明。cors_audit仅获上述窄修产品写锁；Lead继续独占构建/服务/提交。原失败与后续绿灯分开保留，均不代表真实集成验收。

## revision217 — real01失败检查点

revision217：T46 c30391d0定向175全过零skip；真实real01两例1pass/1failure/0skip，ACK失联场景未读到UNKNOWN，精确失败保留且cleanup[]。正在定位故障注入时点，尚不定性生产或夹具问题；完整候选attempts1，16done/2cancelled/1in_progress/31ready，Goal active。

真实run `60ecc38cb9c20055`，源码前后clean c30391d0；`restoreAndCleanupSerializeOnRealObjectRowAndPreserveCurrentSource`通过，`migratesWithProductionStoreAndDualBucketsThenCleansUpOrRollsBack`在第191行期望CLEANUP_OUTCOME_UNKNOWN但实际null。不能将未到达的后续CAS/配置断言写成通过。两个owned容器、两卷、两端口及Maven进程均清理成功，无真实环境副作用。默认/full/core尚未执行。全局ACK开关是否被预约前普通SqlSession提交提前消耗仅为待证假设；先查installed DS/MyBatis和调用顺序，再给最小修复派单，不放宽状态断言。

## revision219 — T46当前候选验收完成

revision219：T46在4ecd45d7完成当前候选验收；175定向、默认1145（928执行/217环境skip）、真实MySQL/MinIO两例零skip、full/core与五静态门禁通过，real02 cleanup[]。17done/2cancelled/31ready，无in_progress；下一T42，Goal active，change未完成或归档。

结果 `4ecd45d7207429e78d767b281851eafb1ba0f955`，tree `38f4411a53af1096388dbe4afae610f72eda8365`，base `4a8fea8c19392972ee5cfef4263962ff6b0e3bfb`。完整证据为 `evidence/T-46.md` 与 `evidence/T-46-current-2026-09-23/complete-a2/manifest.json`。red01、green01与real01的原失败保留；完整候选attempts2。前端无变更，未声称新跑浏览器；没有部署、推送或归档。

## revision220 — T42启动

revision220：T42附件生产闭环启动，基线5417c257；先固定无链接邮件与附件传递红灯，再按真实Notify关系和原user/Client授权实现。17done/2cancelled/1in_progress/30ready，完整候选attempts0，Goal active。

基线 `5417c257130216e2b283ca933d9496d76c10f46a`，main/current-workspace/direct-parent。T35/T37/T44已完成；用户已授权全部票实施、本地提交和子代理，取代旧票中的仅计划/禁止子代理措辞。保持一个产品writer，Lead独占治理、构建、隔离服务与提交。T32外部撤销确认已记录，无新增环境轮换。

Dispatch01A限既有Notify测试写集：复现Demo无链接正文被notice-published的path校验拒绝及附件仅埋Map、未映射进入真实NotifyRequest。红灯不能以mock接受submit替代行为证明；产品暂不修改。完整实施派单将在写集和事务/恢复设计核定后单独登记。验收要求真实fresh六SQL MySQL/Redis/MinIO、完整应用生产Bean装配、只替换物理MailNotificationSender，禁止真实SMTP。附件原提交者与Client从受信登录态捕获并持久化，不信任HTTP actor字段；无附件零OSS，UNKNOWN不盲重发，部分副本须可追踪恢复。此处为计划，未勾AC或宣称测试通过。

## revision221 — T42红灯与完整实施派单

revision221：T42已固定可执行行为红灯并登记附件闭环完整写集，Dispatch01B实现真实Notify关系、服务端actor授权与可恢复私有快照。17done/2cancelled/1in_progress/30ready，完整候选attempts0，Goal active。

红灯源码 `add76d4f36a60b5a2d26182ea361ce64287b0267`，实际计数 `{"tests": 2, "failures": 2, "errors": 0, "skipped": 0}`；这是缺陷复现，不是验收通过。权威实施合同见 `evidence/dispatch-T-42-implementation.md`，全部写集在Ticket frontmatter。Java record仓内直接迁移；HTTP attachmentOssIds可选、缺省空，不接受客户端actor。全应用/真实对象/故障矩阵仍未运行。

## revision222 — T42红灯校准与写锁交接

revision222：T42在59b30b3c稳定复现两项行为红灯（MISSING_VARIABLE与附件[77,88]变空，2fail/0error/0skip），Dispatch01B开始生产闭环实现。17done/2cancelled/1in_progress/30ready，完整候选attempts0，Goal active。

red01附件例因测试账号minuteMax缺失未进入send，原失败保留；Lead仅补测试额度后red02真正到达NotifyRequest附件列表比较。无生产修复混入红灯。源码前后clean `59b30b3c1b4b920c7c82643d0350c83f391218ba`。唯一产品writer为cors_audit，完整合同与52登记写集见Ticket及dispatch-T-42-implementation；禁止子代理构建/服务/提交，Lead继续独占。

## revision223 — 新附件ID传输精度

revision223：T42实施中新附件HTTP字段明确为十进制字符串ID数组，内部严格转Long，避免生成number[]损失雪花ID精度；52写集不变。17done/2cancelled/1in_progress/30ready，完整候选attempts0，Goal active。

NotificationCommand.attachmentOssIds使用List<String>，HTTP可选、缺省空；Demo输入Long映射十进制字符串，服务端在任何持久化前验证正整数及Long范围，再规范化去重保序为内部Long。旧recipientIds等公共ID已采用字符串；此裁决仅约束本票新增字段，不做全仓ID迁移或添加Swagger依赖。BigNumberSerializer仅按数值范围切换序列化，不能单凭它保证OpenAPI/TS客户端精度。真实HTTP/序列化覆盖quoted 9007199254740993及零/负/小数/溢出拒绝，live schema必须items.type=string且非required；标准工具生成，不手写快照。原只读List<Long>设计稿作为历史保留，以本修订为准。

## revision224 — 附件有界传输写集

revision224：T42事前扩展3条common OSS有界传输精确写集，共55根；复制按整体deadline、读取字节上限与目标digest验证，测试2MiB限制不进入产品。17done/2cancelled/1in_progress/30ready，完整候选attempts0，Goal active。

现有OssClient仅HEAD/DELETE有Duration重载，新增受控下载/上传入口及OssBoundedTransferTest，保持其他调用语义。System源GET、目标PUT和目标GET共用绝对deadline，读流阶段硬限制字节、核对目标长度与SHA256；超时/中断取消不能证明远端PUT已停止，仍保留COPY_UNKNOWN预约和引用。邮件附件产品默认限制为去重后20件、单件10MiB、总量25MiB，提交前预检并同步可配置合同与文档；隔离测试代理2MiB仅为小型fixture限制。READY复用须核验原actor仍有效、目标仍PRIVATE且摘要一致；重复ID保序去重，唯一键冲突后的附件关系与返回投递使用当前读。新增Mapper遵循BaseMapperPlus硬约束。以上为实施约束，尚未验收通过。

## revision225 — 附件物化后的发信前核验

revision225：T42事前增加Mail适配器发信前期限/租约核验写集，共56根；附件复制物化后重新核验既有数据库gate，保留T39合同。17done/2cancelled/1in_progress/30ready，完整候选attempts0，Goal active。

新增精确写路径为common-mail/notify/MailNotifyChannelAdapter.java。NotifyRequest只由内部builder携带JSON忽略的beforeProviderSend回调，不扩HTTP NotificationCommand；审计事件FULL/REDACT均去除回调。Dispatch绑定本次真实outbox/token，Mail在全部物化后、物理sender调用前执行已有DB deadlineGate。false为已关闭，不再调用供应商；SQL/提交异常原样外溢。Mail和Common Dispatcher两层都不能把尚未发信的gate失败包装为provider UNKNOWN，当前幂等owner按未发送边界释放。补复制/物化跨截止或丢lease时MailSender零调用，以及异常和事件序列化断言。此处为事前实施合同，尚未验收。

## revision226 — 未就绪快照状态合同

revision226：T42事前补充SysOss实体/VO状态说明写集，共58根；NOT_READY不可作为普通可用附件，元数据与URL均仅接受ACTIVE。17done/2cancelled/1in_progress/30ready，完整候选attempts0，Goal active。

新增2条精确路径仅用于补全SysOss与SysOssVo的NOT_READY语义说明；六SQL同列注释同步。NOT_READY表示通知私有快照预约未确认，不是可下载对象；普通objectMetadata与访问URL入口仅接受ACTIVE。复制结果不确定时保留NOT_READY、真实关系引用与稳定目标键，不能将其包装为可用附件。配套真实故障用例核对metadata及URL拒绝。尚未验收通过，产品writer仍为cors_audit，Lead不并发构建。

## revision227 — 首候选失败与附件发送预约

revision227：T42首候选0595e2cc定向编译失败（实际0测试），attempts1且未验收；保留双轴审查并事前登记邮件发送预约事务端口2路径，共60根。17done/2cancelled/1in_progress/30ready，Goal active。

A1不可判定完成：三个OSS测试close受检异常声明已由a20c977f修复，尚未重跑；原始日志、clean source与两轴审查冻结在evidence/T-42-current-2026-09-24/a1-precheck。候选尝试计数保守记1（编译前置失败，full-suite/E2E仍pending），不因后续修复重置。静态审查发现取消＋旧SMTP仍在途＋租约到期重领后，CANCELLED/无回执/无租约不能证明未发；无邮箱USER的UNDELIVERABLE无Outbox却未释放来源引用。

事前增加NotifyDispatchResultPort与NotifyDispatchResultUseCase精确写集：最终Mail适配器的PreSendGate改调用专用beginMailProviderSend短事务，复用Intent→Outbox→Delivery锁及deadline/lease规则，再锁附件关系、确认READY未RELEASED，单向持久置send_reserved=true；事务提交确认后才允许物理sender。普通早期deadlineGate不置预约，无附件不访问OSS。任一关系send_reserved不是明确false时，自动回收持续保留所有共享引用；标记表示可能已发送，不能以取消/重领/失败/无receipt清零。新增字段进入唯一DDL和真实实体，初始false；不另建状态机或修改通用取消合同。

新增关系补齐@Version/@TableLogic，去掉四处手工version+1，由真实Mapper更新验证乐观冲突和逻辑删除；不能沿用旧实体偏差。UNDELIVERABLE仅在其他全部安全条件成立时解除引用。真实反例须让底层sender阻塞，另连接取消与过期重领，确认CLOSE后仍不释放源/目标refs；保留真正未发送取消与无邮箱正例。补齐多附件第N项失败仍有主、DB提交不确定与幂等唯一键竞争/授权负例、实际HTTP字符串ID与正式OpenAPI等尚缺验收，不将静态检查或单元JSON替代真实证据。

当前fixture受控线程/UNDELIVERABLE两文件尚未提交；cors_audit将继续唯一产品writer，Lead不并发构建或启动服务。所有新代码、default/full/core、真实E2E及生成合同待固定新源码后执行。

## revision228 — 前置失败复盘与分段恢复

revision228：T42前三候选失败全部保留；A3编译通过、105测试中104通过/1error/0skip。四项Lead复盘与Dispatch02A已落盘，恢复批attempts0、累计失败3；17done/2cancelled/1in_progress/30ready，Goal active。

证据与四项复盘：evidence/T-42-current-2026-09-24/a2-a3-prechecks/lead-retrospective-dispatch02a.md。A1受检异常、A2缺import、A3旧metadata正向夹具缺ACTIVE状态；代码保守检查不放宽。Lead接管Dispatch02A，仅修已有OSS metadata测试夹具并补PENDING/NOT_READY/null反例，固定源码重跑16类门禁；通过后才派发JDBC ACK/强制幂等竞争/身份负例。53505153已实现持久发送预约、真实未发UNDELIVERABLE释放、新实体乐观锁/逻辑删除及20项full-context用例，均尚未真实验收。完整应用测试关闭owned Redis自动唤醒，手动驱动真实Worker，独立八类回归覆盖唤醒。HTTP/OpenAPI v3由ops在/tmp准备，未运行。60写集不变，当前唯一产品owner Lead；无构建/服务在跑。恢复批计数仅在本次四项复盘和新派单后按既有流程开始，前三次历史永久保留，不变成通过。

## revision229 — 真实应用装配前置修正

revision229：T42恢复R1定向106/16套件零skip通过；真实MAIL完成104表初始化但非Web上下文缺MVC Bean启动失败，20方法体执行0，owned资源已清理。恢复attempts1/前批失败3保留；17done/2cancelled/1in_progress/30ready。

证据evidence/T-42-current-2026-09-24/recovery-r1/manifest.json，clean源码7070e1d7。JUnit类初始化1error不等同20方法验收；S3到达0不是无附件正例。Lead下一仅将NotifyMailAttachmentIntegrationTest从NONE改MOCK，以完整MVC/安全生产Bean启动而不监听HTTP端口；不mock AllUrlHandler或替换生产SPI，不修改产品安全配置。源码固定后重跑20方法。独立真实HTTP/OpenAPI由full-JAR v3待验证；JDBC ACK、强制唯一碰撞、身份负例需后续02B整合。60写集不变、owner Lead，所有AC未勾、Goal active，不归档。

## revision230 — 测试认证上下文修正

revision230：T42恢复R2已启动完整MVC应用并执行20方法，因测试Sa-Token线程context缺失统一20error/0skip，资源全部清理。恢复attempts2/前批失败3保留，Lead只修测试上下文；17done/2cancelled/1in_progress/30ready。

证据evidence/T-42-current-2026-09-24/recovery-r2/manifest.json，clean4f30529b。本机Sa-Token1.45源码证明手工服务测试需同时设置Servlet wrappers的modelBox与Spring RequestContextHolder；原夹具只设置后者。Lead在当前60根内修测试login与3个finally范围，恢复原上下文，不替换全局SaManager或生产认证。106定向通过沿用原始7070e1d7坐标，不宣称本轮重跑；下轮固定新SHA运行真实Mail20。私人HTTP工具的numeric JSON是观察项：公开合同仍可选string[]，未另增必须拒绝所有数字token的产品需求；字符串精度/合法性及权限负例和正式schema门槛不变，接受数字时仍须核实际ID精确和持久归属。其他ACK/唯一碰撞/身份负例待02B，AC不勾，Goal active。

## revision231 — 无请求 Worker 上下文与引用历史验收修正

revision231：T42恢复R3真实20方法4通过/16failure/0error/0skip，owned资源清理完成；前三恢复失败保留，Lead四项复盘后Dispatch03A新批attempts0，累计失败6。17done/2cancelled/1in_progress/30ready，Goal active。

真实证据在evidence/T-42-current-2026-09-24/recovery-r3/manifest.json，clean源码14140907。R3已排除MVC/登录夹具前置问题，Worker确有claim而未进入复制；生产RequestNotifyContextResolver调用LoginHelper，在没有Sa-Token上下文的后台线程会抛上下文异常。下一固定候选先用真实配置Bean无上下文测试复现，再在NotifyContextConfiguration中通过官方SaTokenContext.isValid()仅对无请求线程返回空审计身份，有效请求仍按真实登录态；附件授权继续使用Intent持久actor，不能伪造Worker登录或吞任意认证异常。事前登记该配置精确写路径，合计61根。两项引用释放实际已RELEASED，sys_oss_ref采用逻辑删除；验收应核active=0、历史行del_flag=1仍保留，不修改生产删除语义。正例增加安全状态诊断，不输出地址/token/密钥。

四项Lead复盘和Dispatch03A见同目录lead-retrospective-dispatch03a.md；前三次原始候选与R1/R2/R3永久保留，共6次失败。Lead为新批唯一产品writer，先完成无请求背景线程红绿测试及现20项真实门禁，再派Dispatch02B JDBC ACK/强制碰撞/身份反例。HTTP/OpenAPI v4已独立静态审查，无新阻断但未运行。106定向旧证据不改写，全部AC、full/core、E2E完成状态仍未通过，不归档。

## revision232 — 生产幂等存储装配诊断

revision232：T42已以真实配置Bean红绿证明后台审计修复，当前110单元零skip通过；Mail A1为20项6pass/14fail，A2确证生产Redis幂等Store未装配，20方法体0。当前批attempts2/前批失败6，17done/2cancelled/1in_progress/30ready。

证据evidence/T-42-current-2026-09-24/dispatch03-prechecks/manifest.json。context-red固定7d50c43b，4项3pass/1error，实际SaTokenContextException；修复797de347固定17套件110/0fail/0error/0skip。独立静态审核确认无权限绕过，后台审计为空不替代持久actor授权；91232702进一步添加实际登录后resolve的userId/clientPk断言，尚未执行到该分支。797de347完整Mail20仍14fail，但取消/UNDELIVERABLE逻辑解除及历史保留两项已通过；91232702的BeforeAll明确NotifyIdempotencyStore为null，fresh104表和资源清理均通过。1项生命周期失败不能算20项业务通过。

事前增加common-notify/config/NotifyAutoConfiguration.java精确写集，合计62根；现@ConditionalOnBean(RedissonClient)未声明生产Redisson自动配置顺序。先核当前4.6.1/Boot4的RedissonAutoConfigurationV4并用实际自动配置装配测试验证，再通过afterName固定顺序；不新增手工Redis实例、无内存降级，不在测试中补Store Bean冒充产品装配。真实完整应用的生产Store非空断言保留。当前Lead唯一产品writer，无服务在跑；下一次完整候选为本批第三次，若失败按既有四项复盘流程处理。02B及其余门禁尚未开始，不勾AC、不归档。

## revision233 — 真实附件20项通过与剩余故障矩阵派单

revision233：T42固定764820dd的113项单元与20项真实MAIL均零skip通过，104表fresh与owned清理通过；整票仍缺ACK/强制碰撞/身份/HTTP等验收。Lead复盘后Dispatch02B补验批attempts0，历史8失败保留；17done/2cancelled/1in_progress/30ready。

证据evidence/T-42-current-2026-09-24/dispatch03-a3-pass/manifest.json。store-order-red固定2534405f为3项2pass/1fail；afterName修复764820dd固定18套件113/0/0/0，真实MAIL run dbc2abb3de3a852b为20/0/0/0，源码前后clean同HEAD/tree。真实生产Redis Store已装配，实际请求审计userId/Client正确；仅底层物理邮件sender替换，完整Notify/System/OSS/Redis/事务链均为生产Bean。20项覆盖零附件零OSS、真实私有字节、多收件人共享与顺序、owner/client撤权、COPY_UNKNOWN/迟到PUT、deadline/lease、send_reserved在取消及二次领取后保护引用、N项失败保留引用、乐观锁/逻辑删除。三个容器、两匿名卷、五端口、Maven/proxy进程全部清理。此为局部完整门禁通过，不是T42 Done；全默认、full/core、八类回归和真实HTTP/生成合同仍待完成。

本批达到3次candidate尝试，前两失败与第三通过均保留；按Lead四项复盘进入有不同交付物的Dispatch02B，而非继续盲重跑。新owner cors_audit唯一产品writer，只可修改现有admin notify测试根下NotifyMailAttachmentIntegrationTest及两份OwnedAttachment JDBC helper，负责7项真实用例（提交BEFORE/AFTER、最终发送预约BEFORE/AFTER、强制唯一竞争、更换User、更换Client）；不得改生产、构建、服务、提交或治理。完整Packet和四项复盘见同目录lead-retrospective-dispatch02b.md。Lead负责回读、固定source、runner准确27方法清单、实际执行与Evidence。新补验批attempts0，前两批6加本批2共8失败保留，62写集不变，所有整票AC不勾，Goal active。

## revision234 — 104表事实同步写集

revision234：T42当前113单元及20真实MAIL通过证据保持，02B三测试文件由cors_audit独占补验；事前登记3处104表事实文档，实际唯一写集64根。整票未完成，17done/2cancelled/1in_progress/30ready，补验批attempts0。

只读来源见evidence/T-42-current-2026-09-24/scope234/stale-103-scope-audit.md。当前六SQL已由实际fresh安装确认104表；事前登记00-project-profile、fullstack backend/mapper-and-sql、release-artifacts/README三精确路径，Lead仅在cors交还产品写锁后改103→104，保留AI退役、旧数据留存、已有库不重放硬约束，不改旧Evidence历史数值。frontmatter实际原为61条唯一路径；revision232/233口述62把原已登记的NotifyAutoConfiguration重复计入，owner条目也重复，本轮按Path去重后新增三项，四数组均64项。原历史描述保留，以当前解析清单为准。02B writer范围仍只有3个测试文件，不由本文扩其写锁；无构建/服务在跑。后续T48真实Windows验收环境已通过异步问题向用户询问，答复未到，不影响本票和其他独立工作。

## revision235 — 附件故障矩阵与通知回归通过

revision235：T42补强后的真实Mail27与八类135回归全部零skip通过；原BEFORE证据缺口及预置绑定10→11断言失败均保留。当前17done/2cancelled/1in_progress/30ready，补验批attempts3；用户已豁免T48真实Windows运行，其余验收不变。

Mail27固定43e5c2be2c0bea045a902271ff32a71a4c8166bb，run a36d6d66aa5e06bd，27/0/0/0且104表和全部owned清理通过。独立JDBC审查确认提交前断连必须同时证明KILL成功及真实commit已尝试，原6f5的XML虽27绿但有BEFORE注入证据缺口，不能追认为通过；AFTER、强制唯一竞争和有效User/Client换身份反例均经真实应用执行。43e5八类回归run fe74fb5e20ee9b0f为134pass/1fail/0error/0skip，唯一失败为fresh预置绑定数量仍断言10而新增demo-mail后11。cd78c39588e16ca4165c2c604f9986a131ef2144只改这一行测试，保持默认禁用账号/无密钥/未绑定账号及删除namespace不可复用断言；run 0c1f52d8c3de3c81真实135/0/0/0及owned清理通过。Mail27测试及全部生产/SQL与43e5逐字节不变，明确复用其源坐标，不宣称在cd78重跑。

私有驱动有一次Lead误抄expected-head，run 63404705414ab19c在source_preflight拒绝，0服务/0构建/0测试；拒绝记录保留，不作为第四个产品候选或业务测试失败。补验批A1有审查缺口，A2真实回归有一项fixture失败，A3修复后真实回归通过；当前计数3，前批8次历史失败不改。下一阶段继续此已通过候选的默认/full/core、真实HTTP/OpenAPI生成及前端/静态门禁；如出现新的候选失败，先按既有四项复盘决定新派单，不自动清零。全部整票AC仍不勾，T42保持in_progress。证据见evidence/T-42-current-2026-09-24/dispatch02b/manifest.json。

## 2026-09-24 用户验收豁免：仅 T-48 Windows 实机

用户对真实 Windows 环境问题答复：“这一块不用验证了，就当是验证通过”。据此接受该项验收豁免，记录为 `user-waived/not-run`，不计为已执行通过或零skip测试。决定与范围见 <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-48-windows-user-waiver-2026-09-24.md</Path>。Shell/fake夹具、真实Linux首次/二次启动、端口/路径空格、doctor/repair及安全负向仍需实测；Windows既有代码合同不删除。T48状态及T30依赖不因豁免提前关闭，T30矩阵仅以本决定替代该单项运行证据，其他全部required验收照常。

## revision236 — 默认测试合同同步与 Dispatch04

revision236：T42真实Mail27/135回归通过保持；全默认clean恢复排除旧问题字节码后1183项检出2fail/1error（244环境skip），三项测试/载体合同待同步，未完成。事前新增2写路径共66根；17done/2cancelled/1in_progress/30ready。

原默认在common-openapi发现ClassFormatError；保留javap与摘要证明旧class含未解析LoginUser及重复create/find问题桩，源码没有重复且本票未改该模块。独立审查支持保留原失败后清理target再按同clean3efc5862完整重跑，不把JDT进程观察当确定归因。恢复1默认265套1183项，936通过/2failure/1error/244环境skip，full尚未运行。两次失败及报告见evidence/T-42-current-2026-09-24/default-prechecks/manifest.json。

三项具体缺口：BusinessOssOwnerArchitectureUnitTest发现新notify_intent_attachment的source_oss_id、snapshot_oss_id未登记；OssNotifyMigrationUnitTest把attachment_actor_client_pk误判成不允许的通用client_pk；OssStorageReadinessArchitectureUnitTest在整个common类禁止putObject，命中诊断方法之外的新有界上传。前者必须补真实owner清单与生命周期、调用者和测试证据，不能塞allowlist；后两必须保留原硬约束，精确禁止独立client_pk列，诊断实际调用闭包仍不得写桶/对象，不得仅删putObject反例或绕过扫描。

Dispatch04先登记migration测试与test/resources/oss/business-oss-owners.json两精确路径，原oss测试根已授权，共66根。cors_audit只获上述清单及三相关架构测试的产品写锁；只修测试/清单，无生产/DDL/依赖变更，禁止构建/服务/提交/生成物/治理。Lead回读后固定源码先定向验证这三类，再完整默认/full及后续HTTP/OpenAPI/core/frontend/static；真实Mail27和135仅在生产/各自测试输入相同证据下沿用原坐标。若发现生产缺陷或需要新增路径，先回报，不越界修改。

四项复盘：共同模式为局部用例通过但全仓合同未同步；根因分别是旧构建输出与新表/字段/方法未进入原架构验收范围；下一实质变化为精确维护硬约束而非修改业务语义；owner为唯一测试writer cors_audit，Lead独占命令/服务，Dispatch04新合同同步批attempts0。此前02B三候选及默认构建恢复失败均永久保留，不追认为成功。Windows用户豁免已正式记录，其他T48检查不减。

## revision237 — 分层门禁阻断与 Dispatch05

revision237：T42真实Mail27/135及HTTP、默认940执行/244环境skip、full/core、前端771和三App构建通过；Notify layered静态门禁8错误阻止关闭。事前新增3路径共69根，Dispatch05修正分层事务入口；17done/2cancelled/1in_progress/30ready。

所有本批证据见evidence/T-42-current-2026-09-24/dispatch04-gates/manifest.json。c980默认265套1184总项、940执行通过/244环境skip，full与core打包清单通过；初次默认9项LoginUserAgent失败及构建产物异常证据保留。临时暂停拥有当前项目target的编辑器JDT后clean默认成功，结束恢复原进程；现有证据不足以证明此前每次异常的精确写入者，不把关联说成确定归因。HTTPv4 run90043be9be226f04在登录验证码前置失败；v5仅改隔离captcha配置并GET核其关闭，同c980同full JAR的run2a1672a910adffea真实通过并cleanup[]。无token/普通用户的401/403及非法值500为项目R.code，HTTP状态实际200，不伪称HTTP4xx。数字JSON本次accepted_exact；公开附件schema仍可选string[]。

正式工具从已接受438路径/450schema原文生成73c28e75，只改4个生成物；该clean来源frontend663Vitest+108Node零失败（101架构例已含在108中），lint/typecheck/三App329产物通过。工程facts、fullstackfacts、FM、handbooks、release128及Compose检查通过。Notify分层真实失败8项，旧功能绿不替代分层门禁。全部T42 AC保持未勾，full_suite及E2E整票出口仍pending。

四项Lead复盘已保留：缺陷是新增generic adapters越过Port直接访问DAO/Service、ActorPort错放runtime、Transactions类命名及事务位置违反既有规则；Lead发现分层门禁顺序过晚。下一实质变化仅校正依赖和事务层次，不改业务状态、安全、SQL或HTTP合同，不放宽校验规则。ActorPort移port；新增SnapshotPort与SnapshotUseCase；旧Transactions改为runtime的SnapshotService并移除事务注解；UseCase实现Port且四个public方法逐个经Spring代理进入原短事务；复制I/O仍在事务外。adapter只持Port及现有OssService；候选读取也经Port→UseCase→Service→DAO。确认参数通过端口自有合同传递，不以全限定类名隐藏违规。owner清单同步真实Service，测试改为注入端口，原断言不削弱。

三个新精确路径事前登记在frontmatter，合计69根；其余改名删除、adapter、runtime与两测试及owner清单已在原66根。Lead负责固定源码、构建、服务及提交；治理提交后cors_audit才取得本派单唯一产品写锁。先跑Notify分层，再定向/真实Mail27与Notify135/default/full/core/HTTP；后端事务图变化后不得自动复用旧真实验收。前端/schema如确实无变更，可通过明确输入等价证据保留本轮原坐标。Dispatch05新批attempts0，此前所有候选失败、JDT恢复及v4失败永久保留，无盲重跑、不归档。

## revision238 — T42当前候选验收完成

revision238：T42在d95b464e完成当前候选验收；真实Mail27/通知135零skip及HTTP通过，默认940执行/244环境skip、full/core、分层及静态门禁通过；前端771与329产物按精确输入等价复用原73c28证据。18done/2cancelled/30ready，无in_progress；下一T43，Goal active，未归档。

base `5417c257130216e2b283ca933d9496d76c10f46a`，source/result `d95b464e46ec73d7a913809f4c84332a7f2eeffb`，tree `39f18706226fed11ed19b5cdc1f822d60492350c`，current-workspace/direct-parent。完整证据见 `evidence/T-42.md` 与 `evidence/T-42-current-2026-09-24/complete-dispatch05/manifest.json`。Dispatch05修复8项真实分层错误后已复验新事务代理链；原失败、旧源验收与Windows单项用户豁免均保留原记录，不追认改写。当前批attempts1，未部署、推送或归档。
