# 工作记录

当前revision174：12done/2cancelled/T41 in_progress/35ready；base d7d534cb，cors_audit阶段1测试writer，Lead治理/实测；以下记录按原时点保留。

## Goal

依用户2026-09-23请求，按G→S→T→P(plan)全面调整当前change所有活动文档，为审查和后续目标执行提供完整票据、验证和可归档完成条件。只规划，不执行产品。

## Current status

revision138；50票全部Ready（31历史票重开＋19新票），0done、0在途writer。用户已确认全部设计选择与G整体共识，S/T/P(plan)定稿；Goal执行未激活。文档结构校验与行为验收分别记录。

## Decisions

保留用户既有单人/current/无需旧接口兼容决定；本轮最新指令限定计划。新高影响决定由用户逐项答复并在LOG-018明确确认共识。源码基线`1264980c74e594bc594e88561bb292fbe5d968a1`，60来源逐一比对，10差异仅元信息；报告所有事项保留正确置信度。

## Files changed

当前change的全部14份既有顶层Markdown、31旧票、plan-data及状态重构；新增source/design-tree/context-map、19票、源码/旧票审计、校验与计划审查。历史47源工件快照与旧Evidence原文保留。产品/全局配置/永久知识/相邻change不改。

## Remaining work

用户之后自行激活目标、核对新的执行与commit授权；按50票与Goal实施/复验、真实提交验收、本地发布候选、安全外部门，最后批准A归档。尚未运行任何产品测试/服务联调。

## Verification

见<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/verification.md</Path>和<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/replan-validation.json</Path>。原tickets/control exit1，11摘要漂移；现绑定已按当前入口更新，原证据不改。最终G/S/T/P校验、ticket-control与git diff --check均exit0；443共享写路径警告按唯一Lead串行策略处理。schema pass不证明业务完成；实际业务矩阵本轮未运行。完整性校验47快照/原报告字节一致、旧31验收条目全部保留、无change外修改。

## 历史执行日志

原完整worklog按字节保存：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/replan-2026-09-23-before/worklog.md</Path>，历史执行Evidence仍在<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/</Path>。新旧授权与结果不相互覆盖。

2026-09-23执行更新：用户已激活Goal；恢复本地实施/逐票提交验收，覆盖前述计划时点的未激活说明。当前先T-32，全部50票目标保持。

## revision139 — T-32完成与用户新增授权

T-32固定产品提交 `bafd5d512a5d17c4848db278f2350fcc6631fbd7`验收通过，证据见 evidence/T-32.md；49票待完成，下一票T-33。用户允许gpt-6-sol/xhigh子代理，当前派遣只读部署核查和CORS审查；current仍单writer。用户提供三个/srv/ops目录授权按现场调整，环境风险门尚未关闭。

## revision141 — T-33完成

T-32/T-33 done，48待办。T-33最终result `da48f850cf34d8d23c09f1ad9c92ed433d5617b0`；默认932项809通过/123其他环境skip，专用24项+2Chrome零skip，独立review通过。组合运行超时失败记录保留，分离默认与专用环境后同一clean提交通过。环境实际轮换见G-security-external-2026-09-23，额外退役AI/qcloud处置等待用户来源信息。下一票T-34，子代理已只读调查，无writer已派遣。

## 当前可恢复状态 — T-34

HEAD 6fcbfeb50747b67bcaf4bf9af1d964cc7d760c04，T34已in_progress。原生cors_audit(gpt-6-sol/xhigh)拥有唯一frontend产品写锁，Packet为evidence/dispatch-T-34.md；Lead只写治理、编译backend core并拥有required E2E。legacy_audit只在/tmp/wta-t34-harness准备真实Admin/ownedDB Redis/六SQL运行器，不执行E2E或写产品。Lead core build日志/tmp/wta-t34/core-package.log，首次 -DskipTests 在core profile的full模块测试编译失败；按仓库已声明命令改为-Pdev,bundle-core -Dmaven.test.skip=true clean package，core-package-v2 exit0；打包不冒充测试，后端上游T33测试已通过。产品后端在T34不变。

历史只读审查已确认：T01当前facts7/7、mode26/26、CI shell等通过，可在clean当前时点复验后依法cancelled并用AC001证据重算下游；T09尚缺当前全部dev/prod三App/full-core/真实服务；T10发布128通过但缺当前完整stage/容器恢复E2E；T23回调核心paths未变但当前真实MySQL/Redis/签名矩阵未跑。T29确需修：工程profile仍引用不存在的release-state.py，实际mjs；票里50POM/247测试数过期，当前49POM（测试数随新票增加，必须最终重算）。不要把旧共享result或旧测试绿补造为Done。

已完成环境凭据轮换原DB/Redis和关联MinIO，私有恢复state与报告位于temp/relase，旧值均拒绝；额外已退役AI、禁用非seed qcloud管理来源/撤销记录已通过async问用户，不阻止独立代码。不要重新运行轮换脚本或打印私有报告/旧Compose healthcheck内容。Goal仍active，48票未完成，未归档/push。

## 外部凭据门关闭 — 用户处置确认

2026-09-23用户确认追加退役AI token/模型凭据、禁用qcloud密钥“已经撤销停用，没有其他进行系统进行使用”。记录为负责人处置确认，非供应商接口复核；与实际DB/Redis/MinIO轮换证据共同关闭G-security-external。其余票据验收和归档授权要求不变。

T-34前置类型修复已声明OssPage.vue精确写集：只补两表列OssVO泛型，实际列表竞态由T-47继续负责。另，附加skill-facts检查发现部署Skill固定temp/relase与工程事实检查禁止该目录冲突；现场维护依据该Skill生成了私有报告，当前报告须保全。该仓库治理矛盾登记T-29修复，不把本次附加检查写成passed；T-34不改变该检查器或删除私密恢复数据。

T34独立规范轴发现打开盒子与初始查询重叠时未保证补查。已在修改前追加Navbar.vue精确写集；仅打开动作fresh=true，登录被动查询仍合并。前两轮证据保留，下一轮补对应UI/Promise回归。

T34已按I流程在第3轮停止并完成Lead复盘；旧3次不删除，新Packet见evidence/dispatch-T-34-recovery.md。Lead唯一writer，真实E2E独占测试资源，不与编译/类型门禁并行。未改变超时或AC，全部票目标保持。

T34恢复候选e78d886真实场景1/1且clean，但双轴发现新驱动失败路径资源清理/选集来源问题，未验收Done。新Packet T-34-20260923-03由cors_audit唯一writer修.py及无Docker回归；产品消息行为/超时不变，Lead独占后续E2E。

T34恢复周期第2次：7c98383真实浏览器1/1，资源全回收；Docker短/全ID比较误报失败，Lead统一完整ID并测试captured/uncaptured两路径。所有失败保留，仍in_progress。公开原始日志保留原样空白，产品diff-check无空白问题。

## revision143 — T34完成

T34result 177eb5bd889afd2ab54f4a8e162dc8358d80f140；3done/47ready。714前端测试、5合成浏览器及1真实Admin登录通过；两轴审查通过。第3轮资源超时后有强制Lead复盘/独占恢复；旧失败未覆盖。G-security-external用户追加撤销确认已关闭。下票T47，预研/tmp/wta-t47-audit.md；T29预研/tmp/wta-t29-audit.md，必须保全私有恢复资料。未推送/归档。

## revision144 — T47启动

base 97e1ee9e1ad40de75deffd025379a6a5488c4882；唯一产品writer cors_audit，范围OSS列表请求所有权与受控组件回归。Lead状态/提交/验收。3done/1in_progress/46ready。

T47首候选2d60e1a全前端728/静态/构建及clean通过，但规范轴发现可选预览阻塞列表。Packet02保持写集，列表结果先提交、预览同代渐进，不改授权/旧响应边界；仍in_progress。

## revision145 — T47完成

result 7cd6fb22b28b7d464b9648ba77f2a2308e5b2016；731前端/17新增SFC/包73通过，完整lint/typecheck/OpenAPI/三App构建与双轴通过，clean两端一致。4done/46ready。下一T29规范/私有目录修复→T01当前复验→T35。依据/tmp/wta-t29-replan-audit.md先更新DAG/精确写集及可验证备份合同；不假取消T01。

## revision146 — T29当前修复启动

当前部署Skill要求旧私有目录但facts拒绝该目录，T01当前复验须等T29修复；历史T01实现早于T29且仍在父链，旧施工边已履行。本轮T29无需等重复验收，改为T29→T01，不取消合同或伪造Done。 base 7a1810288d3292ceeb4987f488b13353af1a1286，4done/1in_progress/45ready。cors_audit仅产品文档/checker；Lead私有备份与迁移独占。两份预研/tmp/wta-t29-audit.md、/tmp/wta-t29-replan-audit.md；私有工具/tmp/wta-t29-private-move.py须审查后执行，不重新轮换。

## revision147 — T29完成

result 9f055ba15d9d5a828fb08cdfb0b24efa32642889；当前37历史原文/28规则/48模块事实和真实私有33项备份/恢复/迁移通过，facts9+handbook8+deploy17+release128全部零skip。5done/45ready；下一T01完整当前复验。私有备份及首次权限拒绝记录保留，未重轮换/重启/部署。

## revision148 — T01完整复验

T29已完成；T01暂不裁决done/cancelled，按/tmp/wta-t01-current-audit.md执行完整合同，无产品写者。5done/1in_progress/44ready。

## revision149 — T01完整复验后无需新增改动

当前源码已满足AC-001且无需新增产品实现；历史非空3aa047b及其父链保留，T29已完成当前路径冲突修复。按Goal历史票无改动出口，取消的是本轮重复施工，不取消AC-001，不把缺失历史clean单独作为理由，不生成空commit或新result。AC-001最终组合复验继续由T30承担。 当前证据<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-01-replan-2026-09-23.md</Path>；83checker+3Notify零skip、五layered、CI/buildguard通过，历史结果不改写。5done/1cancelled/44ready；下一T35。

## revision150 — T35短信跨层实现

base 8b758ea8074a63835659b9731e3a4c74c5c029c5；writer cors_audit；Ticket新增结果Service/SMSadapter与测试根，真实Provider替身且真实隔离持久化验收；不读生产secret，不重发UNKNOWN。5done/1cancelled/1in_progress/43ready。

## revision151预声明安全扩写集

- backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/core/NotifyDispatcher.java
- backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/event/NotifyDeliveryEvent.java
- backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/model/NotifyAuditPolicy.java
- backend/wta-common/wta-common-json/src/main/java/org/namewta/common/json/utils/LogSanitizer.java
- backend/wta-common/wta-common-json/src/test/java/org/namewta/common/json/utils/LogSanitizerTest.java

原admin notify测试根保持；java-api-compatibility实施/验证绑定，见Ticket新增安全契约。不改Controller/Filter生产路径；若需补对应真实调用测试先登记路径。

## revision152当前

T35新增6个精确写集先于实施登记；REDACT_SENSITIVE 通知的供应商消息标识仅保留内部持久化用于回执关联；query、重复提交 receipt 和 monitor 公开投影隐藏该值，FULL 原行为保持。监控查询按本次有界结果批量读取 Intent 审计策略，不引入逐行查询；空ID集合不扫描全表。新增 NotifyAuditSupport 可统一策略与公开投影判断，NotificationReceipt 仅补公开字段的安全语义说明，不改签名。测试覆盖真实供应商返回手机号/验证码作为ID、内部值保留与公开值隐藏、FULL、重复提交及回执关联。

## T35候选1失败与有界返工

T35第1候选41882b54a2b644f7ae661b84205cfcc9d1b72886未通过：真实隔离5例均在夹具插入已有auth-captcha/SMS种子时失败；独立安全审查发现发送前Redis配额异常仍归UNKNOWN/WAITING_RECEIPT。产品writer cors_audit在原写集内修复夹具与SMS发送阶段分类，已知校验终态与可能发送后的UNKNOWN保持，发送前可恢复暂态使用现有有界重试，不变更IN_APP/MAIL。单测实际37类163通过/0skip，缺一个文件名与类名不同的选择器已修正，未宣称完整38类通过。运行bdea78c404fd83db的两个自有容器和进程组已退出，两端口关闭；clean后HEAD/tree与验收前相同。证据在/tmp/wta-t35，正式关闭时保留本次失败。当前仍5done/1cancelled/1in_progress/43ready。

## T35候选2验证与类型化幂等返工

候选2 0b14311c9652b4d765c4db8c8898d28662eb5adb的38类168项单测与6项真实MySQL/Redis用例全部0失败/0skip；run b7ae5911512726de两个容器/进程组已清理、32790/32791关闭、clean前后HEAD/tree不变。安全增量审查通过已审范围，但功能轴指出Typed NotifyIdempotencyUnavailableException.phase=ACQUIRE仍在供应商调用前错误进入UNKNOWN/WAITING，整体验收不通过，attempts=2。返工只区分SMS明确ACQUIRE准备错误的有界重试和COMPLETE/未知phase的不确定结果；已有原请求的InProgress/Conflict不推断未发送，仍保守处理并保留T37/T38责任。不自动删除生产幂等key，故障恢复用例只清本轮受控注入的隔离key。第三候选由原writer在原20条写集内修复。

## revision153 — T35完成

Revision153: T35 accepted at fd8c346;38classes170unit+8real MySQL/Redis zero skips, both reviews pass, clean exact HEAD/tree before/after and owned cleanup verified. All three candidates and failed governance missing-link evidence retained; concrete evidence link repaired after source verification, no product change or attempt reset. 6done/1cancelled/43ready; next T36.

## revision154 — T36启动

Revision154: T36 active at 5118051403de0648540646d98536d8c4f3f9f26d; cors_audit sole product writer, Lead owns governance/commit/isolated real acceptance. 6done/1cancelled/1in_progress/42ready; IN_APP atomic persistence plus result, fence and AFTER_COMMIT; no external channel I/O in transaction or production repair.

## revision155 IN_APP有界尝试预算

代码事实确认：原claim不计次数，结果事务回滚也回滚attempt_count，不能宣称现有重领机制有限。采用原有Outbox字段、既有代理端口和统一锁序：确定参数预检后，beginInAppAttempt独立短DSTransactional按Intent→Outbox→Delivery锁及数据库fence消耗一次预算；成功返回才进入消息/关系/结果的原子事务。IN_APP outbox.attempt_count表示已开始尝试（含随后回滚/崩溃），Delivery与Attempt只记录原子提交的结果；其他渠道保持原含义。消息事务不再次消耗预算。

预算耗尽则在锁内将IN_APP Delivery FAILED、Outbox DEAD_LETTER并刷新聚合，零persist；预算事务失败或提交结果不确定时停止，不能猜测已获得预算。消息事务提交ACK丢失时依已持久DONE/DELIVERED抑制重投，DB暂不可读则等待安全恢复。完全不可写期间不能保证提交终态，但不得在未取得持久预算时调用persist；恢复后仍在预算上限内收束。允许SQL失败后保留独立预算，不允许消息/关系/投递结果部分提交。

验证补充：预算先提交后消息回滚、max边界/耗尽零persist、预算提交ACK丢失及失效lease、同lease重入不能越过最大物理次数。既有写集覆盖port/usecase/runtime/DAO/Mapper/XML；不改worker/claim，不新增表/状态机，不转移给T38，外部渠道未知合同不变。

### revision155 预留去重与开发红灯环境补充

同有效lease重复begin仅第一项获准：采用固定内部码IN_APP_ATTEMPT_RESERVED，不把lease token放入错误码/监控。claim SQL成功领取新token时仅清此固定预留码，其他历史错误保留；预算仍在begin事务消耗，worker不改。此处细化前段“不改claim”为不改变领取策略，仅清理上次预留标志。旧owner/newowner及同lease重入必须验证。

首个开发红灯run c8ae375d4258fb06在创建故障trigger时报MySQL1419，未到业务断言；Maven1、1error/0failure/0skip，非行为红灯。隔离MySQL仅调整trust_function_creators启动参数供故障注入，不给应用全局SUPER、不改部署。旧驱动把error归开发red的记录保留并由Lead assessment明确否决；新版要求精确目标方法的1failure/0error。两个owned容器/进程组已清理，32794/32795已关；该开发运行不计正式候选attempt。

## revision156 预算字段语义文档写集

编辑前增加两条精确路径：NotifyOutbox.java字段Javadoc，以及唯一六SQL中的10-cde-base-ddl.sql，仅notify_outbox.attempt_count/last_error_code中文注释。已有DDL“领取次数”不符合旧结果计数也不符合新预算语义，须同步为IN_APP已开始尝试预算、外部渠道已提交结果次数，固定IN_APP_ATTEMPT_RESERVED内部标记。无列/类型/索引/结构变化、不重放存量基座；全新隔离六SQL装载复核仍必需。预算上限指一次自动调度周期；合法人工重试新周期由T38精确API合同负责。

- backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/domain/entity/NotifyOutbox.java
- release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql

### revision156 持久化摘要长度核查

当前InAppNotificationService把完整content同时写入varchar(1000)的message摘要和longtext正文；公告内容只NotBlank，合法长文会因摘要列溢出失败。现有写集内修正字段映射：message至多1000个Unicode code point、不截断代理对，content保持完整，幂等快照比较仍比较完整content。真实MySQL覆盖长文/emoji边界，不通过扩列、放宽SQL模式或截断正文规避。

## T36 第一候选验收未通过

固定54cf715c2829ff95cf994401ff52f02668c1e171，39类175单元/Wake1/SMS8零失败零skip，但Atomic61有3断言失败（两项提交后推送计数0、一项取消收尾PROCESSING）；run 0e830cfa64189d7f与d5a41784604fd16a全部owned清理完成，clean前后源一致。功能审查还发现多渠道JSON文本比较与确定字段长度预检缺口。正式attempt=1，保留原始记录/tmp/wta-t36-c1；不标done，不重置次数。writer在原授权写集内按根因修复第二候选，禁止放宽断言。

### T36 依赖事件清理的限定范围

dynamic-datasource 4.5.0 的提交异常可能跳过同步清理，依赖源码与实际class由ops专项复核。本票仅在自己的IN_APP代理结果调用失败、调用前无XID且同步集合为空、退出后无XID时清理，保留原异常；不能在入口按“无XID+有sync”泛清，因为正常AFTER_COMMIT回调阶段也有该形态。本地修复不代表所有UseCase的依赖问题已消失；T22/T30当前候选复验应核查其他提交失败后线程复用与事件传播，不能把本票局部证明扩成全局保证。

## T36 第二候选验收未通过

固定a2749a7dcc0bbc5c0643e07eba938446611c995a，39类178单元/Wake1/SMS8通过零skip；Atomic66零failure/1error，AFTER参数分支未清duringProvider故障钩子，下一正常投递再次被注入lost ACK。C1三断言及多渠道/列边界已通过。run909ae637d59b2ebe与0abb8a5220460930全部清理、clean源前后一致；正式attempt2保留。Lead接管唯一测试写锁，仅在两分支汇合后重置钩子，不改生产代码、不删弱断言。第三候选须完整Atomic/Wake真实复验；178单元与SMS8输入未变可严格等价复用，明确原实际运行SHA。

## revision157 — T36完成

Revision157: T36 accepted at64d67d5; C3 Atomic66+Wake1 zero skips; exact unchanged-input reuse of C2 unit178/SMS8 and gates, original run sources explicit. Both reviews pass; clean exact HEAD/tree and owned cleanup. Three attempts preserved. 7done/1cancelled/42ready; next T37; goal active, no archive.

## revision158 — T37启动

Revision158: T37 active at 38032d24335c52cafea855b19d51fb36295162ef;7done/1cancelled/1in_progress/41ready;cors_audit sole product writer, Lead governance/commit/isolated E2E. Typed retryable unsent facts and owner-CAS RETRYABLE preserving digest/TTL; unknown remains closed; T38 owns precise manual retry.

## revision159 单次SDK请求的实例事实闭环

编辑前新增精确写集：<Path>backend/wta-common/wta-common-sms/src/main/java/org/namewta/common/sms/config/SmsAutoConfiguration.java</Path>。在现有Sms4jBlendRegistry维护自身以maxRetries=0创建的实际代理实例identity；使用已验证的BaseProviderFactory.createSms→SmsProxyFactory.getProxySmsBlend→SmsFactory.register同一引用，保留原SDK初始化所需钩子，不将void create后再get的可覆盖对象盲认证。remove先撤认证；注册/更新失败不保留认证，按账号并发更新需一致，不暴露配置对象。common-sms notify目录内小型SmsSingleAttemptBlendVerifier SPI由Registry实现，Resolver经AutoConfiguration ObjectProvider注入，缺失/identity不匹配即不把拒绝标成可重试；严格腾讯结构allowlist只有该证明成立才启用。已有Registry拥有这一事实，不新增第二套全局注册平台，不反射SDK，不让common反向依赖业务。固定对象捕获到send，避免查验A发送B。公共SPI/构造/配置方法调用者与测试同步。

### revision159 Redis序列化与剩余TTL实证

生产RedisConfig默认CompositeCodec(StringCodec,TypedJsonJackson3Codec)，旧Store bucket采用client默认codec；SMS真实fixture默认codec，Redis专项fixture显式StringCodec。新单键脚本必须沿用旧值的codec及NameMapper，不能改成StringCodec后让旧key不可读/比较失败。Redisson4.6.1源码的CompareAndSetArgs不指定TTL会SET并清TTL，不能作为保留期限实现；使用与bucket一致编码的原子CAS+PTTL/KEEPTTL，在同脚本内验证key存在且有正剩余TTL。真实回归需涵盖项目CompositeCodec及名称前缀、旧四字段StoredState，不只StringCodec绿色。

### T22/T28事件残余范围复核（只读预研，未关闭票据）

ops固定38032d2复核显示：Callback UseCase/Service自身不发布DsTx事件，干净线程的callback提交异常不会自身登记同步；不能把依赖缺陷直接说成T22新原子性漏洞。真正登记wake的是submit/retry，提交异常后线程复用的即时wake风险应由T28/T30结合周期poll兜底边界验证。T22的关键原子/租约/双worker已由T36当前66+1覆盖，但T37触及结果与重试，须重验组合后才合法处理历史票。详见/tmp/wta-t22-current-audit.md，非Done证据。

### 新事实：T02真实在线token路径，下一独立安全修复候选

legacy只读核查确认UserLoginSuccessListener将真实tokenValue赋UserOnlineDTO.tokenId，系统在线设备/myself接口和前端常规强退使用该tokenId路径变量；SysLogFilter记录原始servletPath，异常处理URI也需核验。区别于任意客户端元数据猜测，这是AC002的真实业务凭据副本。保持T37单writer当前实施；完成后优先串行复核/修复T02（与T38无依赖冲突），再推进Notify后续。统一现有path sanitizer及HTTP/OperLog/错误sink，不以改API或删正常观测规避。X-Request-Id泛canary不列阻断，仅可选格式有界加固。报告/tmp/wta-t02-t03-current-audit.md生成后作后续Packet输入，当前不关闭T02。

## T37 C1 当前失败与修正

固定 `e2907c41f4b1bef442c68b95a0fc09aa9c3b273a`，39类187项单元零skip、静态门禁通过；真实run `b0d200e1eaf3766e` 中SMS10项1error（模板映射夹具）和Redis3项1error（生产Composite codec完成CAS）。formal attempts=1，不能验收。另驱动匿名卷不存在检查异常，容器/进程组均无残留、32808/32809已关闭；ops核精确卷清理证据。cors恢复唯一产品writer修正，Lead不并行Maven。失败记录保留，T36真实回归尚未启动。

## revision160 — T37完成

Revision160: T37 accepted at3a87bf7, attempts2; current187units+13real retry/Redis+67Atomic/Wake zeroSkip, both reviews pass, clean source and owned cleanup. C1 two errors and driver cleanup-evidence limitation retained.8done/1cancelled/41ready; next T02 real token-path log fix, thenT38; Goal active, no archive.

## revision161 — T02启动

Revision161: T02 implementation active atc16966167526f9b6ab6eb213265034b3bbe53e46;8done/1cancelled/1in_progress/40ready. Real login token in online device URL leaks to HTTP/error logs; minimal shared LogSanitizer path fix with current HTTP/MySQL canary acceptance. cors_audit sole product writer; Lead services/commits/governance. T37 closed, no active services.

### revision161 同一在线会话响应字段闭环

补充当前源码：SysUserOnline.tokenId未经JSON忽略直接随GET /monitor/online/list及GET /monitor/online返回，LogSanitizer敏感名当前不含tokenid，且两条GET不省略响应日志，因此同一个有效token还会经在线列表HTTP响应正文复制。既有LogSanitizer字段策略需补tokenId，仅改变日志副本，业务响应仍保留可用于正常设备操作的tokenId；canary验收覆盖列表响应→正常操作URL，而非仅路径静态替换。未扩大写集或改业务API。

## T02 真实行为红灯（正式验收0次）

测试专用checkpoint `8a88965363ca295122efadabf0bc97e99c6704c4`，未改生产。冻结driver cff4e66c3f108c71a212623fdab88f88783415f977d33803ed2bf2180024f452，run4e81e6d59c798f07，七类35项仅新canary一failure、零error/skip；有效token及真实controller强退已执行，HTTP审计仍含token断言失败。后续error/OperLog/DB断言因首断言失败尚未执行。隔离MySQL、匿名卷、进程组、32814端口全清，source前后clean。cors恢复唯一产品writer作最小修复，Lead当前无服务/Maven；原red及两次编译开发记录保留。

## revision162 — T02完成

Revision162: T02 accepted at617a369; current real39 zeroSkip, consumers70+HTTPS1/Chrome6 zeroSkip, default1000 includes150 environment skips explicitly excluded; full/core packages verified. Initial consumer skip and pnpm environment failure retained.9done/1cancelled/40ready; T03 current no-new-work adjudication then T38. Goal active; no archive.

## revision163 — T03当前复验

Revision163: T03 cancelled as no new product work after current617a369 real bounded HTTP/signature/heap and consumers/full-core proof. Historical214de538 remains; AC003 retained by T30 and only redundant implementation edge removed.9done/2cancelled/39ready; nextT38; Goal active.

## revision164 — T38启动

Revision164: T38 started fromb47ff8b after acceptedT02 and no-new-workT03 closure.9done/2cancelled/1in_progress/38ready. cors_audit sole product writer, Lead governance/commit/isolated acceptance; exact ID and safe actual requeue contracts; no new schema or Client/owner model.

T38 revision164方案复核：保留原Ticket的IN_APP安全重做条款，澄清仅外部UNKNOWN/WAITING不可重发；定向只作用其所属一项，批量含外部未知写前拒绝。尚无产品实现/真实运行，HTTP红灯测试由writer编写。

### T38已取得行为红灯

测试专用checkpoint `9f8fa2b1e36c851be8cee6763a84ebf6e2072928`；`red-http-identity`实际exit1，1test/1failure/0error/0skip。Controller.retry:48把URL101/body202传给Service，测试29行verifyNoInteractions失败；后续异常断言未执行。代码测试在standalone MockMvc，不冒充完整ACL/DB。formal attempts仍0；当前单writer进入完整实现。原记录/tmp/wta-t38/red-http-identity.json/log与red-behavior-assessment.json。准备的ownedMySQL/Redis驱动SHA d068610f75a9c65cabf77d69d230884b33edd9c9f3d64c73c73296655066c31e，12项合成保护通过，尚未启动真实服务。

## revision165 T38字典合同补齐

revision165：NotifyDelivery已有真实PENDING状态，但notify_delivery_status基座字典缺该值，重试后监控会显示未知。提前扩50-cde-base-dml.sql写集，只补唯一PENDING=待投递字典项，保留同六SQL基座与其他初始化；前端Delivery类型/字典渲染同步，不新增后端状态。不得执行生产DML或重放基座；当前新隔离库验收，存量Tag差异由T30持有。

### T38 开发检查与冻结副本预审（非正式验收）

writer 的 selected-dev-v2 退出0，13项通过、8项真实数据库用例因opt-in关闭而skip；前端domain4项和web-domain13项通过，两个包typecheck/lint退出0。原首次编译检查、web-domain-test-v1夹具SSR context失败保留于/tmp/wta-t38，不将后续绿色覆盖原日志。当前代码尚未冻结、未提交实施候选，formal attempts仍0。

legacy对/tmp/wta-t38/preliminary-source的21文件哈希冻结副本预审未发现已证新增生产安全阻断，提出混合外部UNKNOWN整批零写、IN_APP缺关系幂等和确定性锁竞争验证缺口。writer已补部分场景，继续在原测试白名单补真实Planner失败后修配置重试、剩余预算、orphan与claim交错，并补真实SaInterceptor权限负例；standalone无拦截器HTTP红灯不冒充权限实证。唯一产品writer/开发构建窗仍属cors_audit；Lead不并行Maven/前端构建/服务。

ops在/tmp/wta-t38准备完整JAR的owned MySQL/Redis/MinIO live OpenAPI捕获驱动，仅静态和合成自检；旧target JAR不属于T38。待后端固定clean SHA并独立检查、clean full package后，才运行该JAR并直接捕获/v3/api-docs；生成合同回写后另固定最终候选，不混淆backend source SHA与最终前端生成物SHA。当前尚未启动上述真实服务。

## revision166 — T38完成

Revision166: T38 accepted at a3b289e, backend sourceA7a6f75 explicit; final real14 and frontend736 zeroSkip, defaultA854 executed/163 environment skips, fullA/coreB and complete frontend gates passed. Live Redis config failure preserved; correct raw HTTP provenance and clean owned cleanup.10done/2cancelled/38ready; nextT39; Goal active, no archive.

## revision167 — T39启动

Revision167: T39 in_progress from bec94ae after T38 closure; 10done/2cancelled/1in_progress/37ready. cors_audit sole product writer; Lead integration/governance. Absolute Captcha/Redis deadline, safe expiry and reclaimed-provider uncertainty; no production data repair.

## revision168 — T39完成

Revision168: T39 accepted at6e1d7f8/tree602161; C1 real117/3failure retained, C2 real118 zeroSkip; default861 executed/181 environment skips, full/core and applicable static gates passed; dual reviews and exact owned cleanup complete.11done/2cancelled/37ready; nextT50. Goal active, no production deployment/repair/archive.

## revision169 — T50启动

Revision169: T50 active fromd544f02 after T39 closure;11done/2cancelled/T50in_progress/36ready. ALL+ASYNC+0 early rejection, all12 production callers, fenced historical unsupported disposition and real OpenAPI regeneration. cors_audit sole writer; Lead build/services/commit/governance. Goal active, no production operations.

### T50阶段2实施检查点

红灯d1c3548424f696022292cf1a2fb6ec9b432c011f为clean测试专用提交，NotificationSupportedModeRuntimeTest四项均因未抛业务拒绝失败，0error/skip；/tmp/wta-t50/red-supported-counts.json与fresh XML已保存。Lead已授权cors_audit阶段2在原15写集完整实现，未授权build/服务/提交；Lead负责固定候选验收和真实full OpenAPI捕获，ops仅准备/tmp owned驱动。Goal active，未归档。

### revision170 T50 Profile消费者测试

已预登记PersonRebindNotificationTest和EnterpriseTransferServiceTest两个精确测试路径，原15项变17项。cors_audit单writer，其他边界不变。

## revision171 — T50恢复

Revision171: T50 prior batch3 attempts retained; B real135 assertions passed but exact-clean gate failed during overlapping Vite build. Lead review and new Dispatch02 saved before reset0; serial-only recovery, no relaxed checks. API README provenance correction is within existing directory scope. 11done/2cancelled/T50in_progress/36ready; Goal active.

## revision172 — T50完成

Revision172: T50 accepted at84ce0a9/treec118348; prior batch3 retained and Lead-reviewed recovery batch1 real135 zeroSkip/source-clean/owned cleanup passed. A3 default870 executed/197skip/full/live and B frontend736/core reused only for identical inputs; C static+OpenAPI check passed.37 paths within17 scope entries;12done/2cancelled/36ready. NextT41; Goal active, no production queue action/deployment/archive.

## revision173 — T41启动

Revision173: T41 active from d7d534cb after T50 closure;12done/2cancelled/T41in_progress/35ready. Bounded personal JOIN pagination/detail, global unread/read-all, session fencing and real 501 evidence. cors_audit sole writer; Lead serial build/services/commit/governance. Goal active, no production operations.

## revision174 — 读投影路径纠正

Revision174: before production edits, correct T41 Mapper read projection to domain/model/read/NotifyInboxRow.java per FILES-002/005; no dto exception. 23 declared entries, same red-only phase;12done/2cancelled/T41in_progress/35ready.
