# 工作记录

revision254：T13关闭。注册与验证码 Chrome 10 项加服务端拒绝 7 项、0 skip。25done/8cancelled/17ready。Goal active，未归档。

## Goal

执行已激活的goal-plan，完成50票的实现与当前候选验收，以实际代码和真实验证证明完成。已授权本地修改/测试/提交/direct-parent；归档仍须全部归档前置闭合并取得对应授权。

## Current status

revision254：T13关闭。注册与验证码 Chrome 10 项加服务端拒绝 7 项、0 skip。25done/8cancelled/17ready。Goal active，未归档。

## 规划阶段历史记录（revision138，不代表当前执行授权）

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

## revision175 — T41分段检查点

Revision175: T41 backend checkpoints A1 b08105f failed (fixture MPJ result mapping), A2 662b351 passed3, A3 9d748aa passed4 after atomic first-time preservation. Full JAR/live OpenAPI fetched and generated in42f07eb. Frontend sole writer cors_audit active;12done/2cancelled/T41in_progress/35ready. This is partial implementation feedback, not complete-ticket integration; default full suite/frontend/browser still pending.

## revision176 — T41测试夹具修复

Revision176: complete T41 candidate B 05d0f34 failed integration attempt1 at frontend tests: missing Notice SFC SSR context and existing manifest registry test reaches browser Router through new user Store import. Architecture/lint/typecheck passed; later stages not run. Preserve raw failed evidence; add exact registry test path before fixture repair.24 declared entries;12done/2cancelled/T41in_progress/35ready. No product assertion weakened; Goal active.

## revision177 — T41三次复盘

Revision177: T41 current batch reached3 complete-candidate attempts: B frontend fixture failure; C frontend750/buildpassed but missing declared actual-host coverage; D frontend751/default870+201skip/full/realHTTP4/shared135 passed but owned browser seed SQL exec failed before JVM/Chrome. D sourceclean/cleanup[] preserved; ticket/workspace blocked, result null. Stop automatic resend. Lead diagnosis and materially changed dispatch required before resetting a recovery batch.12done/2cancelled/T41blocked/35ready; Goal remains active.

## revision178 — T41恢复派单

Revision178: clean d58fc3d owned SQL diagnostic proves four1267/HY000 failures and collated controls pass;501/2 fixture and cleanup pass, acceptance=false. Separate login40>30 fixture defect confirmed. Four-part Lead review and materially changed Dispatch02 recorded before recovery attempts reset0; old B/C/D three attempts immutable.12done/2cancelled/T41in_progress/35ready; Goal active.

## revision179 — 恢复E检查点

Revision179: recovery E a0dcbac8 attempt1 failed real Chrome at spec90 close locator after SQL501/2, real login, top10/detail body passed; source/JAR clean and cleanup[]. Five API-controlled Chrome cases passed5/0/0/0 on exactE using independently verified C production328 artifacts. Retain E failed result; two exact-name close locator fixes authorized within existing e2e scope; no runtime or assertion relaxation.12done/2cancelled/T41in_progress/35ready; oldbatch3 retained.

## revision180 — 恢复F检查点

Revision180: recovery F cb8063b6 attempt2 failed Chrome at B shared-title locator112 after Apage26/oldest/foreign-negative and Blogin/unread2/two rows. Seed title=summary renders twice; authorize two unique table-row/title-cell assertions, retaining counts and all negative/readAll checks. Ffull/core/static/OpenAPI pass, source/JAR stable cleanup[]. Oldbatch3 and recoveryE/F retained; nextG attempt3.12done/2cancelled/T41in_progress/35ready.

## revision181 — T41完成

Revision181: T41 accepted at c21de75f, recovery attempt3; G real501 Chrome1/0/0/0 and SQL A501unread0/B2 unchanged, original timestamps preserved, exactclean source/JAR and cleanup[]. Prior3 plus recoveryE/F failures immutable. Explicit D/C/E/F same-input test/build/contract reuse; both reviews pass.13done/2cancelled/35ready; nextT40; Goal active, no archive.

## revision182 — T40实施开始

revision182：T40开始，base601b9273；13done/2cancelled/1in_progress/34ready。T41已完成，新增为前置。cors_audit唯一产品writer；Lead治理/提交/隔离验收。先现API真实撤回红灯，再版本栅栏、三处链接一致、本人深链与竞争/回滚/浏览器验收。

## Dispatch01A真实红灯与01B实施

固定test-only源码 `9077bb8d22251f2f44f5e22ad3d734fe5cb7da21`，tree `76a985b74bed05eecdcb8c9b72cd91fafaf79836`，run `f41d49a19908ed48`。六SQL103表/MySQL8.4.9/Redis8.6.3，实际选择唯一方法 `retractBeforeWorkerClaimStopsCurrentVersionAfterPositiveDeliveryControl`；先真实发布/领取/站内落库/提交后push成功，再发布并撤回后恢复Worker。fresh XML 1test/1failure/0error/0skip，在第142行消息数expected0/actual1，证实业务缺陷，不以鉴权/编译或夹具失败代替红灯。命令退出1是预期红灯，acceptance仍false。before/after同clean源码，cleanup.errors=[]，两容器/匿名卷/两个loopback端口及Maven进程组无残留。

原始字节、命令、时刻、精确计数和来源哈希见 `T-40-current-2026-09-23/red/manifest.json`。XML及日志均0必要脱敏，保留完整业务断言。驱动11项离线检查属于安全辅助，不计业务验收。本次为预定test-only反馈，完整候选attempts仍0。

Lead Dispatch01B：在Ticket22写集内实施版本metadata栅栏、exact-key撤回事务/窄基座例外、Worker/retry边界、三处path回填和本人query端口/旧路径导向；补真实竞争/回滚/版本/模板/权限测试及SFC状态测试，更新对应两条Skill事实reference。cors_audit唯一产品writer，Lead拥有治理/提交/服务/构建；不自行运行构建或启动服务。先交完整可编译候选和精确验证选择器，所有required验收由Lead串行运行；新增路径必须先登记。

## revision183 — 后端分段反馈

revision183：T40后端分段A2在a638ef48通过16定向单测、6真实MySQL/Redis，0fail/error/skip，源码前后clean、cleanup[]。A769e5d17元数据类型编译失败保留；公共Map<String,String>不变，内部noticeVersion用严格JSON字符串。完整候选attempts0；剩余竞争/重试/旧来源矩阵、前端和浏览器待实施，13done/2cancelled/T40in_progress/34ready。

真实run `ee99f3fbd678653d`，源码 `a638ef485c3819bfc8d67553877fd7c106ef9579`、tree `ceb4a5354641b7327bc65728bfaeb0f6678c52d8`；六SQL103表，2容器/1匿名卷/2端口及Maven进程组已回收。通过原红灯、两静态seed/普通missingIntent拒绝、late Snapshot SQL trigger完整回滚、V1撤回/V2送达、真实MAIL planner外部-only与混合三处path，未使用真实供应商。定向5类16测试均通过。原始失败与当前fresh XML/命令/哈希见 `T-40-current-2026-09-23/backend-a2/manifest.json`。这些是预先声明的后端反馈，未冒充整票验收。

Dispatch01C：cors_audit在原22写集内继续补充真正双连接gate先后/已获发送权真实ACCEPTED与UNKNOWN落库、旧owner、duplicate/retry零唤醒、legacy UNVERIFIED、本次Notice行数/关联冲突回滚等遗漏；保留现有6项绿灯。由Lead固定下一后端候选并串行运行，再移交前端/浏览器。若路径不足先登记；产品writer不自行构建、启动服务或提交。

## revision184 — 后端竞争矩阵与前端派单

revision184：T40后端A4在5fc319ed通过17真实MySQL/Redis测试（含双JDBC连接行锁）；A3共享8类135项通过，均0fail/error/skip、源码前后clean、cleanup[]。A2单测16沿用原始坐标且生产输入等价。13done/2cancelled/T40in_progress/34ready；完整候选attempts0，前端与真实浏览器尚待验收。

原始结果分别为A3 `270bd94e3f59a784929448e2cfa84ae913b686b7` / run `5102c97369fbecac`（16），共享 run `b1d860dd4901770e`（135），A4 `5fc319ed58d9586a92166459b3296618e7dc47dc` / run `526ecc242d191177`（17）。A4 tree `445d0a24596b32360a3b54de94c272599ad866bd`。17项包含真实双连接锁阻塞与提交后栅栏、真实provider第二gate后的三类结果保留、旧lease owner、duplicate/retry、legacy来源、版本隔离及两类晚期SQL回滚。外部provider为测试替身，不声称真实供应商发送。三个隔离run容器/卷/端口/进程组均回收。原始XML、日志、哈希及明确复用说明见 `T-40-current-2026-09-23/backend-a4/manifest.json`；有界A2安全审查不替代最终整票审查。

Dispatch01E：cors_audit作为唯一产品writer，在原22写集内完成宿主懒加载query订阅、本人分页外detail、query/session代际隔离、历史管理链接按本人messageId转换、撤回提示和两条Skill事实reference；补领域/SFC/宿主测试及独立T40真实浏览器驱动。浏览器验证真实HTTP发布→Worker送达→撤回后本人快照，A/B权限隔离与同页query；不得SQL伪造发布证据。Lead独占治理、提交、构建及服务，writer不运行构建/服务或提交。新增路径超范围须先登记。候选交回后冻结源码，再串行运行定向/全量门禁及真实Chrome；本checkpoint不是整票通过。

## revision185 — 前端定向通过，浏览器实施派单

revision185：T40前端分段F4在461b0a43通过112项定向测试及3包typecheck；F1 eb8cefe0真实后端18项全部通过、源码前后clean/cleanup[]，后端自F1未变。F1/F2/F3失败及诊断已保留。13done/2cancelled/T40in_progress/34ready，完整候选attempts0；真实Chrome、完整门禁及两条Skill事实尚待完成。

F1 `eb8cefe0f67895e75832b2a15d8f955b7b01e447` / tree `33ef52d0f1250c6221cb4ad3f586563d84332c67` 的真实run `c7fac20c13a862b2` 为18/0/0/0，新增首次READY MAIL未授权前撤回零外呼和provider ID保留断言。前端首次工具PATH错误127未启动测试；修正环境后111pass/1fail，3包typecheck通过。F2 snapshot正向通过而subscribe失败；F3临时诊断揭示Vitest并发懒导入加载真实App HTTP导致ClientContext缺失。F4仅在测试中等待已取消导入结束，再验证有效订阅，保留全部正负断言；生产未放宽，临时console未提交并已删除。当前 domain7 + webdomain25 + Admin80 =112全部通过，3包typecheck通过。结果/原始失败见 frontend-f1 与 frontend-f4 manifest。SFC缓存页检查调用实际组件注册的生命周期hook，不能冒称真实浏览器KeepAlive交互。

Dispatch01F：cors_audit唯一产品writer，在原22写集内继续独立T40真实Chrome两案例、owned runner及离线安全测试，更新两条已声明Skill reference。依据冻结设计与 `/tmp/wta-t40-browser-design.md`，真实HTTP发布→实际Worker送达→撤回后普通A快照可读，B foreign/absent拒绝、历史管理链接按本人消息导向、同页query与会话迟到隔离。控制HTTP允许，禁止伪造发布/详情响应；SQL fillers明确标识。Lead独占治理/提交/构建/服务；writer不得运行构建、测试服务或提交，超范围先登记。交回完整源码后冻结并串行执行剩余必需门禁。

## revision186 — C1门禁反馈与定向补修写集

revision186：T40完整候选C1 2c8047a2首次验收失败；前端652 Vitest＋108工具测试、全量检查与三App构建通过，浏览器离线15通过；后端默认测试中两项旧notice-published邮件夹具未触发send，待核查修复。完整candidate attempts1；full/core打包及真实Chrome尚未执行，13done/2cancelled/T40in_progress/34ready。

C1前端每步前后均同一clean HEAD/tree，补齐此前F4 clean:false来源限制。两份固定C1静态审查未发现生产合同阻断，但不能代替失败的默认测试与未运行浏览器。全部已执行记录、fresh XML及审查见complete-candidate-c1/manifest.json；token仅按精确JWT模式脱敏并保留原始哈希/替换数，其他字节保留。默认失败为DispatchNotificationServiceTest的recipientMinuteCapIsIsolatedByScene与noticePublishedMailRendersWrapperNotCallerSnapshot，均send未调用。Lead登记该测试文件作为第23写集，先定位并按新noticeVersion合同补齐夹具，保留两项原目的断言，必要时增加缺事实失败关闭负例；不得放宽生产栅栏。cors_audit唯一产品writer，Lead治理与全部服务/构建。

## revision187 — C2默认/构建通过，浏览器前置失败

revision187：T40完整C2 851a6d6e仍未验收；两旧邮件夹具修复＋缺marker零发送负例通过。默认首次OOM137失败保留，同源限定1536MiB重跑877执行通过/216环境skip；full/core打包及全部静态门禁0。真实browser前置探针或登录阶段失败，Chrome未启动，source/JAR一致且cleanup[]。attempts2；下一仅补安全阶段诊断，13done/2cancelled/T40in_progress/34ready。

C2树a6d6fe1e3c5c372372ceb34bae8a191d44c46d47。默认初次未设堆上限，kernel OOM杀死Java PID1142623，fresh237类1009项0fail/error/198skip只属未完成证据；随后同源码JAVA_TOOL_OPTIONS=-Xms128m -Xmx1536m重跑默认260类1093项0fail/error、216skip，877实际执行通过；不修改规则/排除测试，全部fresh XML均在clean前保存。C1前端与浏览器离线按输入完全相同且329产物hash一致复用；F1真实18/A3共享135保留原SHA和明确差异清单。C2 full JAR SHA a6cece4cdbe349f3529870ee39d2397c85477513da1d603c2d40d1eda2c369a1；run19a061800c7e3c0f真实隔离六SQL103表、3容器/2卷/5端口和backend进程全部回收，但固定RuntimeError尚不能区分auth/code探针或WTA/A/B登录失败，不猜根因、不计浏览器通过。core随后完成，target现为core，下一browser必须重新full打包。

Dispatch01G：cors_audit只改已授权frontend/e2e/run-notice-retraction-real.py及test_run_notice_retraction_real.py，补固定阶段白名单、HTTP/R.code受限整数、backend退出码及本脚本帧行号，不持久化任意异常串/HTTP正文/凭据。保留严格真实控制链路、两Chrome案例/0skip/0retry、owned清理与固定source/JAR。加入401/R401/缺token/探针退出/canary/伪阶段与数字边界离线检查。Lead独占所有服务/构建与提交；未定位事实前不改产品行为或放宽验收。

## revision188 — 三次尝试停止与复盘入口

revision188：T40完整候选C1/C2/C3三次均未验收，停止本批集成并先复盘。C3 d5b4f5f8 full打包/17离线通过，真实run630336ccabc8052b确认backend探针200、控制账号login HTTP200/R500、未开始Chrome，source/JAR一致、cleanup[]。已定位缺User-Agent解析为null的登录调用链，待同版依赖复现与独立审查后登记窄修恢复包。13done/2cancelled/T40in_progress/34ready；Goal active。

C1为旧notice fixture失配；C2生产及默认门禁已通过但browser前置失败，另保留OOM和有界重跑；C3只加安全诊断并复现login_control业务500。不能把探针200或离线通过当公告/Chrome通过。三次记录不删除、不静默清零；完整C3记录见complete-candidate-c3/manifest.json。Lead暂停进一步集成尝试，先记录根因/影响边界、独立审查、下一次实质改变、恢复入口与验证策略。当前静态事实：精确full JAR hutool-http5.8.47对空UA返回null；LoginHelper、同步UserLoginSuccessListener、异步SysLoginInfoServiceImpl均需核查。拟保留无UA真实HTTP为回归，不靠添加UA掩盖生产空指针。未批准新路径前不修改这三个文件。

## revision189 — 四项复盘与Dispatch02

revision189：T40前批3次失败保留；同版UA探针及独立审查完成，Dispatch02四项复盘已落盘，事前扩3生产＋1精确单测路径并绑定common Skill。恢复批attempts0，先真实消费者单测红灯再三处Unknown窄修/真实无UA登录与审计回归；不补请求头绕过。13done/2cancelled/T40in_progress/34ready，Goal active。

权威恢复包：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/dispatch-T-40-recovery.md</Path>；精确依赖探针源/实际命令输出、分析与独立审查见recovery-diagnosis/manifest.json。恢复包先于产品修改落盘；不得删除旧三次记录或将旧完整候选称为passed。

## revision190 — 隔离红灯及生产补修派单

revision190：T40隔离UA真实消费者红灯已确认，clean828ad32f：10测试/6NPE失败/4正向通过/0error/skip。两次编译和一次隔离初始化失败单独保留，不当业务红灯；默认静态单例污染已通过子JVM隔离消除，真实Spring转换工厂及生成mapper参与断言。Dispatch02B三处Unknown窄修与真实登录/在线/审计取证已派；恢复完整候选attempts0，前批3失败保留。13done/2cancelled/T40in_progress/34ready，Goal active。

证据见`T-40-current-2026-09-23/recovery-ua-isolated/manifest.json`。原6eb5783c非隔离red仍保留；ee8e6d83编译失败、b7a12bc1十项HARNESS初始化失败、de891cc4单例诊断均不冒称业务NPE。828ad32f才为有效隔离red。每个子JVM限45秒、384MB，finally回收leader/后代并删除临时文件；父默认Surefire不初始化RedisUtils/MapstructUtils。生产签名、HTTP schema、认证/权限不变。

后续真实18/共享135原driver安全环境会过滤外部JAVA_TOOL_OPTIONS，故各复制一个新版本，仅加入固定本轮-Xms128m/-Xmx1536m；原版本保留，精确diff/hash/新driver随证据留存。没有扩大测试范围或降低断言。新候选须重新执行默认、真实18/135、fresh full/core、静态、两Chrome；前端未变输入按既有等价证据复用。

## revision191 — R1完整应用发布失败与JSON合同核查

revision191：T40恢复R1 c8604809未验收，UA10/离线21/默认887pass216skip/full/core/静态、真实18及共享135均通过；run0c8f89704398d48a已实证三次无UA与Chrome登录/在线/异步审计全通过，但notice_publish HTTP200/R500，Chrome尚未开始，source/JAR一致cleanup[]。发现生产大整数JSON字符串与版本栅栏只收数字不兼容，下一仅先真实Jackson回归红灯。恢复attempts1、前批3失败保留；13done/2cancelled/T40in_progress/34ready，Goal active。

完整证据：`T-40-current-2026-09-23/complete-candidate-r1/manifest.json`。默认261类1103项中887实际通过、216环境skip；真实18 run da0dee6e6a6ce32f及共享135 run46fb407df36ab094均0fail/error/skip并清理完成。full JAR SHA59dda7ff6e9b8e94f903310bc71bbbab439b6ff50f087d825b2e23d4f2d56978，target随后core覆盖，下一browser必须重新full打包。前端760测试/三App329产物按与C1输入等价复用；Python21单独通过。

Dispatch02C限定既有NotifyNoticeVersionFenceTest路径先复现：使用真实JacksonConfig module和超过JS安全范围的ID，经initial/requirePublishedIdentity/state/retractedMetadata往返；用scoped JsonUtils.getJsonMapper替身绑定真实mapper而不修改其全局缓存/Spring状态，保留旧3例与负向校验。Lead记录test-only诊断红灯后才派既有NotifyNoticeVersionFence路径窄修：接受生产规范ASCII正整数字符串及旧数字形式，严格正值/Long范围，继续拒绝空白、符号、前导零、小数指数、溢出、boolean及身份不一致；不换全局序列化器、不取消版本栅栏、不新增API/SQL。必要字段表示说明仅在既有Skill refs中同步。

服务器异常处理仅记录异常类而非完整栈，本次不冒称已采集唯一服务器异常根因。源码序列化差异须由上述红灯及修复后的真实publish/retract/Chrome来确认；安全观测提案保留为备用，不因已有静态线索而重复无修改完整运行。下一完整R2必须新default/real18/shared135/freshfull/browser2/core/static，不能将R1业务失败追认为passed。

## revision192 — 生产Jackson红灯与Dispatch02D

revision192：T40生产Jackson大ID回归已实证：5项/1真实ServiceException失败/4通过/0error/skip；新发布版本元数据在真实序列化配置下无法自验证。Dispatch02D只修既有Fence私有数字解析与Python消费者相同表示、负向测试及必要既有Skill说明；不改全局mapper/身份/权限/SQL。恢复完整attempts仍1，R1原失败保留；13done/2cancelled/T40in_progress/34ready，Goal active。

诊断源码`9faeab8740f211ecfbc3644a4ffcad51076df17e` clean，raw XML/命令/独立意见见`T-40-current-2026-09-23/recovery-json-red/manifest.json`。当前成功序列化断言先证明noticeId/snapshotId确为字符串，失败落在requirePublishedIdentity，排除了仅夹具启动失败。仍待修复后的真实publish/worker/retract/Chrome闭环，不冒称R1服务器栈已观测。

02D唯一writer cors_audit，仅既有已授权路径：NotifyNoticeVersionFence.java、NotifyNoticeVersionFenceTest.java、run-notice-retraction-real.py、test_run_notice_retraction_real.py；必要事实说明可在既有两个Notify Skill refs同步。Java String须ASCII `[1-9][0-9]*`、≤19字符且longValueExact，旧数值表示保留。Python notice_version_fact也须同等严格接收int（禁止bool）或规范正int64字符串，对notice/snapshot/version继续精确比较；不能让字符串兼容放过浮点、符号、空白、前导零、Unicode数字、溢出或身份错配。保留所有旧负例、真实UA证明和两Chrome断言。Lead独占新候选所有测试/服务/提交。

## revision193 — R2真实发布闭环通过，seed排序夹具待修

revision193：T40恢复R2 8a387192仍未整票验收；Fence5/离线22/default889pass216skip/full/core/静态/真实18/共享135均通过。真实run318e1b1d8873fa80已完成UA3+Chrome、公告发布→Worker送达→撤回、2普通用户管理GET403；仅seed分页位置合并断言失败，Chrome尚未开始，cleanup[]。下一仅修runner合成数据时间锚点，恢复attempts2、前批3失败保留；13done/2cancelled/T40in_progress/34ready，Goal active。

完整证据见`T-40-current-2026-09-23/complete-candidate-r2/manifest.json`；新default261类1105项、真实18 run9a2dc8c991096685和共享135 run3e74344a4b3fd209已在clean前保留fresh XML。R2 full JAR SHA c2b023231d0713b1c17ca9940b94a6937af53b627205d574151ad589126d79a9。随后core覆盖target，下一次须fresh full构建。R2发布成功实证大ID表示修复已穿过真实生产调用链；seed失败未留具体计数/时间，不能把时区推断写成已观测唯一原因。

Dispatch02E限定现有两个Python文件（run-notice-retraction-real.py/test_run_notice_retraction_real.py）；必要README可同步。合成22行时间须取本次V1/A精确且唯一、create_time非null的持久关系时间+1秒，禁止用独立NOW/修改V1/改业务时区掩盖；写入前条件不成立则失败。保留22/1/22/1/0数量、20条首屏、V1离页、legacy前10及全部Chrome/owner断言；失败观测仅计数/布尔和固定阶段，不存ID/正文/token。离线覆盖缺失/歧义/空时间、时钟错位和数量/排序失败。

若产品/TS/依赖/SQL/Skill完全不变，R3可依据精确diff复用R2默认/真实18/135/core/静态和C1前端证据；仍必须新Python离线、新exact-source full JAR及两真实Chrome零skip/零retry，source/JAR一致cleanup[]。第三完整恢复候选若再失败，保留三次并执行既定四项复盘，不盲重试。Lead独占服务/提交，writer不并行构建。

## revision194 — 恢复批第三次失败已停止

revision194：T40恢复R3 378f8481未通过；离线25/fresh full及bundle通过，真实发布送达撤回/seed通过，Chrome 1pass1timedOut、0skip/0retry，cleanup[]。B越权矩阵通过；A超时仅保留finally unroute:175位置，不据此断言根因。恢复批3次已停止，保留前批3次与R1/R2/R3失败，进入只读四项复盘；13done/2cancelled/T40in_progress/34ready，Goal active。

证据 `T-40-current-2026-09-23/complete-candidate-r3/manifest.json`；run f3ff781212c46752，HEAD 378f8481deae56bf7b6971ef1b1b2e66c47ca8fe，tree 37a8e633d9f78d6285bf0dba51c51b053e3b981b，full JAR SHA f8e7bdb0723e7f5543f674f59c11b92121e87ff46b401b49341c8fe275ccf50e。source/JAR前后同值，3进程组/3容器/2卷/5端口回收。准确diff仅2 Python＋README，R2后端default889pass216skip、真实18/共享135/core/静态与C1前端760及3App构建按输入等价复用；未伪称本次重跑。真实seed 22/1/22、V1离页、legacy前10通过；Chrome A timedOut/B passed，各1attempt。post-browser验收未完成。

下一步骤：保持产品冻结与服务停止，Lead保留失败；cors_audit与ops_audit独立只读审查跨会话挂起请求、注销/登录、response事件和finally清理。先记录根因证据/不确定性、独立审查、实质变更和恢复入口四项，再派遣窄修。不得加timeout、吞取消、删断言或将cleanup位置冒称原失败位置；当前不启动第4次完整候选。

## revision195 — 跨会话浏览器四项复盘与受限恢复

revision195：T40两批各3次失败保留；跨会话测试取消/同URL误认的四项复盘与独立审查已落盘，Dispatch03只修现有E2E及必要安全观测，不改产品、不放宽断言。新恢复批attempts0，需新全前端/full JAR/Chrome2；13done/2cancelled/T40in_progress/34ready，Goal active。

权威派单 `evidence/dispatch-T-40-browser-recovery.md`，原始报告 `evidence/T-40-current-2026-09-23/recovery-browser/manifest.json`。原超时具体挂点仍未观测；下一版按精确Request验证真实注销取消、保留同页迟到成功与同context B本人正控制。Lead唯一治理/提交/测试owner，cors_audit唯一E2E writer；源码交回前不运行服务。

## revision196 — Dispatch03三次失败停止，交互可操作性复盘

revision196：T40 Dispatch03三候选均失败并停止；S1/S2 lint失败及隔离诊断保留，S3 ddc0b51d已过strictE2Etypes/全前端760/3App329产物/fresh full，但Chrome A timedOut、B passed、0skip/retry，source/JAR同值cleanup[]。正在只读核模态遮罩挡住logout操作与安全阶段诊断；13done/2cancelled/T40in_progress/34ready，Goal active。

证据 `T-40-current-2026-09-23/complete-candidates-s1-s3/manifest.json`；S1 70bb5996、S2 c4e0d597均在lint失败，后续门禁未执行。隔离3case证明直接throw AggregateError的cause第三参识别问题；命名构造保留primary/cleanup/cause，未改lint规则。S3 ddc0b51d6c3278f9e55e9c0aa21bde614958e6cb tree 67eeb3947790d010933d43e566d40ec6491a950a；Chrome run434baf70aa25766e，full JAR SHA 5a7db069f3f9fd27d32f457b0c0c773d78df3303260646415491b3b8e6f4379a。前端652Vitest+108Node、额外101架构、全lint/typecheck/OpenAPI/三Appbuild实际通过，329产物哈希保留。后端R2与PythonR3按精确输入等价复用，不冒称重跑。

A安全位置22是Aggregate清理现场，原始report已按安全合同删除，不能声称已证最初卡点。Lead静态发现openDetail在请求完成前打开el-dialog，而测试挂起响应后直接点击被模态覆盖的头像菜单；独立审查确认后才派下一窄修。候选批已停，保留前两批各3次及本批3次，不直接启动第4次；下一须先四项复盘、真实用户可操作路径和安全阶段诊断，禁止force click、加总timeout、忽略取消/错误或删除断言。

## revision197 — 模态可操作性四项复盘与Dispatch04

revision197：T40前三批各3次失败保留；模态关闭/有界动作/安全阶段定位四项复盘已完成，Dispatch04只修E2E与现有Python诊断，不改业务或验收目标。新批attempts0；13done/2cancelled/T40in_progress/34ready，Goal active。

权威派单 `evidence/dispatch-T-40-modal-recovery.md`；两独立报告 `evidence/T-40-current-2026-09-23/recovery-modal/manifest.json`。新的静态原因是pending detail已打开modal，测试却直接点底层头像；先真实关闭并证明旧Request仍pending，再完成原注销取消/同context B旅程。安全annotation仅记录last_started阶段枚举，不把cleanup位置或阶段开始冒称原始根因/动作成功。Lead独占状态/提交/所有服务，writer不并行测试。

## revision198 — T-40当前候选验收完成

revision198：T40已在26f04b94当前候选验收完成；真实Chrome2零skip/retry、source/JAR同值cleanup[]；新Python26/strictE2Etypes/全前端760/3App329产物/fresh full通过，后端R2按精确输入等价复用。14done/2cancelled/34ready，无in_progress；下一T44，Goal active，尚未完成或归档change。

完整证据 `evidence/T-40-current-2026-09-23/complete-candidate-u1/manifest.json`。结果SHA 26f04b94db68701ade80038a763eca0ffde83918，tree ad6f9ed73ff4c437741372149670d824327ec98a；真实run dd86442542876baf，两Chrome各1attempt、0skip/flaky，真实发布/Worker送达/撤回、A离页快照/同页迟到/旧链接/注销取消与B本人正控、B外人和不存在同形拒绝及畸形零请求全部通过。seed前后22/1/22及离页/top10保持；3进程组、3容器、2卷、5端口全部回收。先前三批各3次失败原样保留，新批第1次通过。

U1全前端652Vitest+108Node、strict E2E tsc、architecture/OpenAPI/lint/typecheck及3App构建实际通过，329产物哈希保留；fresh full JAR SHA efafa17218f8b4ccdf964a21751179cbde0505e2162d1d52b0e6080cc58087e1。R2 backend default889pass216envskip、real18 run9a2dc8c991096685/shared135 run3e74344a4b3fd209（均零skip）、core/静态明确按字节相同输入复用，不冒称U1重跑。外部Provider结果是类型化测试替身，真实浏览器完整Spring链路仅IN_APP；没有推送/部署/生产修复/归档。

## revision199 — T-44启动与精确写集

revision199：T44已激活，19条写集及5项Skill绑定已登记；先补诊断阻断的红灯测试，再实施业务解耦、容错启动、单配置诊断和健康组分离。14done/2cancelled/T44 in_progress/33ready；Goal active，未完成或归档change。

基线 8db922e1971b4781b2b53f8db837c06f7b60c4e7。用户已批准替代System AGENTS旧规则“readiness未达到可服务状态时不得签发访问URL”；新规则保留业务owner/Client、ACTIVE、对象自身service、当前配置和预期访问类型失败关闭。新增单配置POST `/resource/oss/config/diagnose/{ossConfigId}` 使用既有 `system:ossConfig:list`，安全@Log关闭请求/响应记录，输出VO仅status/reason/checkedAt。OpenAPI由真实full JAR捕获后正式生成，revisions只新增不可变source/provenance；不手改生成物。

启动只初始化DB配置，不做远端诊断；无/重复/坏默认清理陈旧Redis默认指针，管理唯一PRIVATE默认约束保留。可选诊断Duration错误不能阻断核心启动；管理员调用有有限超时。无条件调度移至NamewtaApplication，真实Notify Redis wake丢失后的定时兜底必验。Docker现有TCP8080探针不能冒称HTTP readiness。T45供应商策略解释/T46清理锁/T49配置去重均留给各责任票。

## revision200 — 红灯与Dispatch01B

revision200：T44定向红灯已证实，固定11960110的53例为49pass/1failure/3error/0skip；三条诊断前置阻断及空service仍进入Provider的缺陷均可重现。Dispatch01B开始产品实现，完整候选attempts0；14done/2cancelled/T44 in_progress/33ready，Goal active。

`red01` Maven exit1、编译成功，源码前后clean同值。生命周期/直传/迁移各因旧readiness门禁抛错；空service负例在objectStore.accessPolicy被调用后失败，确认须补本地路由校验。其余49项通过；未把预期红灯算成验收或完整候选失败。XML/命令/源码/两份独立审查已保存red01/manifest.json。

01B在原19条写集内完成：业务诊断解耦而授权/ACTIVE/service/policy不放松；DB成功读取后清SYS_OSS_CONFIG专用缓存和默认指针，再填合法当前行，DB故障仍核心报错；单配置管理员诊断、配置变更只失效、无启动远端探测、常驻应用调度、core和ossdiagnostics健康组及规范同步。可选诊断timeout冻结为每网络步骤100ms–3s，最多5个顺序步骤，网络等待预算最多15s，不称整个请求3s；配置无效返回固定诊断配置错误，核心仍启动。不得起未回收后台任务制造表面超时。若需严格单个总deadline或common路径先回Lead登记，T45策略解释未提前改动。

## revision201 — 原始诊断属性的测试消费者同步

为避免Spring Binder在可选Duration词法错误时阻断核心，三项诊断配置改同型String JavaBean并在诊断时解析。仓内唯一超出现有admin OSS测试根的消费者为 `backend/wta-admin/src/test/java/org/namewta/test/profile/material/ProfileSelfMaterialsBrowserIntegrationTest.java`，已事前登记为第20条精确写集，仅同步setMaxSnapshotAge的配置字面值/必要编译消费，不改Profile业务。writer尚未改此文件；红灯与AC/候选attempts不变。

## revision202 — A1真实验收与日志缺陷补修

revision202：T44 A1源码e11c1b6f定向148全过，默认1115中899pass/216环境skip，full包通过；七组核心启动/Notify轮询真实场景通过。MinIO功能链已通过但日志检测发现access key与secret key，A1安全验收失败，整票仍in_progress。26条精确写集登记后补日志红灯与修复；14done/2cancelled/1in_progress/33ready，Goal active。

源码e12df121为01B产品实现；green01在编译期发现新增测试4处泛型断言歧义，保留exit1/零执行证据，e11c1b6f修复。green02为33类148例零skip；默认261类1115例899pass/216环境skip、零失败；full JAR SHA256 `835b4de84b911ff77ba20ba003218f30af015559dccfadcdbc30fdf1d8d069a8`，源码tree `92781356146df461ab9815a13aa2884effc0740e`。这些不是环境跳过项的通过证明。

七组真实核心验收均绑定同一clean源码/JAR：empty `de1c3f734c322a79`、bad-nondefault `7704e6179563c466`、duplicate-default `003f22307c765c71`、bad-default `37e0ba0f8a622025`、invalid-diagnostic `e3e150d8206beeb2`、minio-offline `c8511c8ff56072a3`、Notify fallback `637717597daa1c8a`。前六验证核心UP、真实登录/菜单及适用的缓存/诊断失败关闭；counted端点均零调用，offline为不可连接回环端口、未伪造计数。最后一项用Redis ACL selector仅拒绝notify:outbox:wake的PUBLISH并允许缓存失效通道，真实公告发布形成READY，唤醒失败后POLL领取，Outbox DONE/Delivery DELIVERED/Attempt/Message/用户关系均1，后续两次tick不重复；无外部SMS/MAIL调用，所有资源cleanup零错误。v2全局禁PUBLISH破坏缓存登录和v3忽略channel型ACL拒绝的夹具失败均保留，不能当成产品兜底失败或通过。

MinIO驱动v1/v2因空hex SQL失败，v4到诊断审计因SQL排序规则失败；均保留并在v5前复盘。v5/v6完整走通最小权限A/B上传下载、公开无canary GET、权限/状态/坏配置负例、单桶诊断、3个非法ID零远端、诊断STALE后下载、默认B与旧A、实际OpenAPI436路径；但postcheck未通过。v6 `4ca003409fc458ce` 的安全摘要记录app access key 7次、app secret key 1次，其余8类0；不保留原值/上下文。前置logger归属可能沿用此前HTTP行，不能据此断定SQL stderr也是HTTP logger。源码证实HTTP JSON字符串url未检查签名query，且dev启用SqlLogInterceptor，后者插值实际配置参数并打印原异常消息。A1作为一次真实安全验收失败记录，整票AC未勾；已捕获OpenAPI仅属该源码事实，不是完整通过。所有MinIO运行cleanup零错误。

事前新增6个精确路径：common-json的LogSanitizer及其测试、common-mybatis的SqlLogInterceptor/SqlLogProperties、common-web的SysLogFilterTest，以及application-dev.yml的SQL日志说明。加上原20条共26条；不改数据库、业务返回、权限、URL有效性或执行SQL，不新增公共API/依赖。共用日志副本须隐藏带签名/凭据的URL与上传令牌路径，同时保留普通公开URL；SQL保留结构、mapper、耗时和异常类型，禁止写入绑定值、原错误消息或秘密。先用真实日志入口补红灯，再实现；既有敏感HTTP和操作日志合同保持。实现细节不得通过关闭整套日志或绕过安全扫描过门禁。

证据见candidate-a1/manifest.json；仅精确测试JWT模式脱敏，保留源/留存hash及替换数，失败/skip不改写。真实运行私有raw日志不纳入仓库。OpenAPI正式生成、前端/core/静态及修复后真实验收仍待完成，不归档。

## revision203 — 日志红灯与Dispatch02B

revision203：T44日志回归红灯固定于d4a15669，6例/6失败/零error与skip；A1安全失败仍保留。02B按签名URL、uploadToken和SQL固定元数据合同补修。14done/2cancelled/T44 in_progress/33ready，完整候选attempts仍1，Goal active。

日志红灯见 evidence/T-44-current-2026-09-23/logging-red01/manifest.json；这是缺陷复现，不计为新增完整候选失败，也不勾选AC。

SQL补充源码事实：BoundSql.getSql已经展开动态片段（含现有Mapper的${ew.customSqlSegment}），即便不插值绑定参数，也可能含字面量或注释中的secret。因此revision202的“SQL结构”具体落实为SqlCommandType固定枚举、经字符/长度限制的Mapper ID、耗时和异常类名；不输出原SQL文本、参数、异常message/stack。不新增SQL解析器或依赖，不关闭日志。原Statement执行、结果及异常传播保持，console/log两种配置均适用。

HTTP日志只改副本：所有嵌套/数组文本值中的签名或凭据URL隐藏；uploadToken属性、OSS上传路由令牌及签名查询字段隐藏；普通公开URL和业务code维持现有合同。真实响应和路由不改，操作日志既有服务端路由模板继续有效。写集仍26条，无公共API/数据库变更。

Dispatch02B：cors_audit为唯一产品writer，可在已登记LogSanitizer、SqlLogInterceptor、SqlLogProperties、application-dev.yml的SQL注释以及02A测试路径落实修复；不运行Maven/服务/提交，不改治理。Lead审查并冻结源码后执行定向绿灯、default/full、真实MinIO与Notify兜底；实际OpenAPI正式生成、前端/core/静态门禁仍待完成。

## revision204 — T-44当前候选验收完成

revision204：T44在fc50c1e完成当前候选验收；日志191全过、backend905pass/216环境skip、8真实应用场景及默认SSE补充捕获通过，18类canary零命中；既有OSS真实JUnit7及其内部Chrome10零skip；前端760/3App329、full/core/静态通过。15done/2cancelled/33ready，无in_progress；下一T45，Goal active，change尚未完成或归档。

结果`fc50c1e1227d42a46f8e25b3e19949baeccb89e0`，tree `c39bb13afcbf07459b2fb07c1c2179f3198fc807`；父链基线`8db922e1971b4781b2b53f8db837c06f7b60c4e7`。完整记录见 `evidence/T-44-current-2026-09-23/complete-candidate-a2/manifest.json`。后端及真实full JAR源码为`958aad6174c880fbf8c7afbfb8f657d37c968979`，最终只新增4个OpenAPI生成文件，精确输入等价证明明确区分两个SHA；未冒称后端在最终SHA重跑。A1泄密失败、6例日志红灯及夹具失败保持原时点；A2是第二个完整候选。无推送、部署、生产修复或归档。

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

revision213 同步 Map 的16张已完成票 AC 追踪说明为实际 Evidence，替换规划期 not-run 占位；不提前宣称 T-30 整体集成或 change 归档完成。

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

checkpoint216保留的Maven原始日志含工具输出尾空格，全量git diff --check因该原始字节退出2；不为格式改写不可变运行日志。排除原始Evidence目录后检查活动文档，产品源码diff此前已检查通过。

## revision217 — real01失败检查点

revision217：T46 c30391d0定向175全过零skip；真实real01两例1pass/1failure/0skip，ACK失联场景未读到UNKNOWN，精确失败保留且cleanup[]。正在定位故障注入时点，尚不定性生产或夹具问题；完整候选attempts1，16done/2cancelled/1in_progress/31ready，Goal active。

真实run `60ecc38cb9c20055`，源码前后clean c30391d0；`restoreAndCleanupSerializeOnRealObjectRowAndPreserveCurrentSource`通过，`migratesWithProductionStoreAndDualBucketsThenCleansUpOrRollsBack`在第191行期望CLEANUP_OUTCOME_UNKNOWN但实际null。不能将未到达的后续CAS/配置断言写成通过。两个owned容器、两卷、两端口及Maven进程均清理成功，无真实环境副作用。默认/full/core尚未执行。全局ACK开关是否被预约前普通SqlSession提交提前消耗仅为待证假设；先查installed DS/MyBatis和调用顺序，再给最小修复派单，不放宽状态断言。

## revision218 — 精确预约提交故障注入

revision218：real01原失败保留；installed MyBatis/DS机制支持ACK开关被预约前预读提交消耗的高置信解释，但旧异常阶段未捕获。Dispatch03仅按UNKNOWN成功更新后的XID精确注入提交回执丢失；attempts1，16done/2cancelled/1in_progress/31ready，Goal active。

权威窄修派单见 `evidence/dispatch-T-46-03.md`，诊断见 `evidence/T-46-current-2026-09-23/real01/real01-ack-injection-diagnosis.md`。无产品业务修复裁决，不能将机制推论写成旧运行已捕获的调用栈。

## revision219 — T46当前候选验收完成

revision219：T46在4ecd45d7完成当前候选验收；175定向、默认1145（928执行/217环境skip）、真实MySQL/MinIO两例零skip、full/core与五静态门禁通过，real02 cleanup[]。17done/2cancelled/31ready，无in_progress；下一T42，Goal active，change未完成或归档。

结果 `4ecd45d7207429e78d767b281851eafb1ba0f955`，tree `38f4411a53af1096388dbe4afae610f72eda8365`，base `4a8fea8c19392972ee5cfef4263962ff6b0e3bfb`。完整证据为 `evidence/T-46.md` 与 `evidence/T-46-current-2026-09-23/complete-a2/manifest.json`。red01、green01与real01的原失败保留；完整候选attempts2。前端无变更，未声称新跑浏览器；没有部署、推送或归档。

T46 close219初次文档validator缺Workspace Verification节的current-workspace字面标记而失败；补齐后0errors/180共享路径warnings，串行owner策略继续适用，两次结果均保留。complete-a2共311记录，默认XML中2处测试JWT启发式替换保留source/sanitized摘要，不代表新增凭据泄漏。

## revision220 — T42启动

revision220：T42附件生产闭环启动，基线5417c257；先固定无链接邮件与附件传递红灯，再按真实Notify关系和原user/Client授权实现。17done/2cancelled/1in_progress/30ready，完整候选attempts0，Goal active。

基线 `5417c257130216e2b283ca933d9496d76c10f46a`，main/current-workspace/direct-parent。T35/T37/T44已完成；用户已授权全部票实施、本地提交和子代理，取代旧票中的仅计划/禁止子代理措辞。保持一个产品writer，Lead独占治理、构建、隔离服务与提交。T32外部撤销确认已记录，无新增环境轮换。

Dispatch01A限既有Notify测试写集：复现Demo无链接正文被notice-published的path校验拒绝及附件仅埋Map、未映射进入真实NotifyRequest。红灯不能以mock接受submit替代行为证明；产品暂不修改。完整实施派单将在写集和事务/恢复设计核定后单独登记。验收要求真实fresh六SQL MySQL/Redis/MinIO、完整应用生产Bean装配、只替换物理MailNotificationSender，禁止真实SMTP。附件原提交者与Client从受信登录态捕获并持久化，不信任HTTP actor字段；无附件零OSS，UNKNOWN不盲重发，部分副本须可追踪恢复。此处为计划，未勾AC或宣称测试通过。

T42激活文档校验前两次因integration Evidence需精确T-42.md命名失败；改正后0errors/180共享路径warnings，继续单writer串行，不放宽校验。三次日志均保留activation/manifest。

## revision221 — T42红灯与完整实施派单

revision221：T42已固定可执行行为红灯并登记附件闭环完整写集，Dispatch01B实现真实Notify关系、服务端actor授权与可恢复私有快照。17done/2cancelled/1in_progress/30ready，完整候选attempts0，Goal active。

红灯源码 `add76d4f36a60b5a2d26182ea361ce64287b0267`，实际计数 `{"tests": 2, "failures": 2, "errors": 0, "skipped": 0}`；这是缺陷复现，不是验收通过。权威实施合同见 `evidence/dispatch-T-42-implementation.md`，全部写集在Ticket frontmatter。Java record仓内直接迁移；HTTP attachmentOssIds可选、缺省空，不接受客户端actor。全应用/真实对象/故障矩阵仍未运行。

T42 red01精度记录：2例均失败且零error/skip；正文例确证MISSING_VARIABLE，附件例停在Mockito“send未调用”，尚未到达附件列表比较，不能宣称已复现[]丢失。Lead检查发现测试账号minuteMax为空导致planner额度unboxing可能抛错，下一步只修夹具并重测，产品未改。Dispatch01B暂不交写锁。

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

## revision239 — T43基线测量启动

revision239：T43启动，先用真实公告链和MySQL测100/1000/10000基线，生产尚未优化；7写根内仅新增测量测试，common batch事务独立核查。18done/2cancelled/1in_progress/29ready；Goal active，未归档。

基线 `dd250b947576505425b67ebac0a559c56616952c`；首轮精确产品写路径 `backend/wta-admin/src/test/java/org/namewta/test/notify/NotifyFanoutMeasurementIntegrationTest.java`。Lead治理提交后cors_audit单writer；legacy_audit仓库只读审计，ops_audit仅/tmp驱动准备，Lead独占服务/命令/提交。固定三档各三次fresh同输入A/B、真实发布事务/固定10次结果/SQL行与执行数/锁等待/内存，故障回滚和唯一性为硬门禁；生产优化必须有A测量依据，聚合不新增状态机或虚构SLA。全部AC仍未勾，未运行不报通过。

## revision240 — A基线入证，批量写入尚未验收

revision240：T43 A基线21次已抄入evidence，AC-043仍未勾；正在同一动态事务内改为500行多值插入，聚合状态机不改。18done/2cancelled/1in_progress/29ready；Goal active，未归档。

A基线源码 `7933bdff61a5dbd620b869be7171b61b83fed845`，证据 `evidence/T-43-current-2026-09-24/a-baseline.md`。10k发布约30005次单行写、executeBatch为0；三次失败回滚后三张关系表为0。同探针的批量后测量尚未运行，因此不写性能提升，不勾AC。

## revision241 — T43当前候选关闭

revision241：T43在9267ecb5关闭。同探针发布写执行100/1000/10000由305/3005/30005降到8/11/65；10k回滚后三张关系表为0。聚合未改、无新SLA。19done/2cancelled/29ready；下一T48。Goal active，未归档。

证据 `evidence/T-43-replan-2026-09-23.md`。实现提交 `9267ecb52cd8ec8c663a41f06b10eb7e0b3fe6ca`。未推送、未归档。

## revision242 — T48当前候选关闭

revision242：T48关闭。日常 start 不 clean、不覆盖端口；repair 仍单独可调用。Windows 实机为 user-waived/not-run。20done/2cancelled/28ready；下一T49。Goal active，未归档。

证据 `evidence/T-48-replan-2026-09-23.md`。夹具命令退出码 0，node 测试 3 通过 0 跳过。未推送、未归档。

## revision243 — T49尚未关闭

revision243：T49进行中。avatar 的 SINGLE 策略已去掉无用分片参数，属性测试 4 项 0 跳过。MinIO 默认存储切换未跑，AC-049 未勾。20done/2cancelled/1in_progress/27ready。Goal active，未归档。

## revision244 — T49当前候选关闭

revision244：T49关闭。真实 MinIO 默认存储切换 1 项 0 skip，属性测试 4 项 0 skip，无附件提交不调用 OSS。21done/2cancelled/27ready。Goal active，未归档。

证据 `evidence/T-49-replan-2026-09-23.md`。未推送、未归档。

## revision245 — T04当前复验取消重复施工

revision245：T04按当前复验取消重复施工。真实单/双 Nginx 与直连 IPv4/IPv6 为 1+11 项、0 skip。无新实现。21done/3cancelled/26ready。Goal active，未归档。

证据 `evidence/T-04-replan-2026-09-23.md`。HEAD `e134d29bbc573653d430077e88bb0b7f9d29908a`。未推送、未归档。

## revision246 — T05当前复验取消重复施工

revision246：T05按当前复验取消重复施工。真实 Redis 防重租约 7 项、0 skip。无新实现。21done/4cancelled/25ready。Goal active，未归档。

证据 `evidence/T-05-replan-2026-09-23.md`。HEAD `671bfc86`。未推送、未归档。

## revision247 — T06当前复验取消重复施工

revision247：T06按当前复验取消重复施工。真实 HTTPS/Chrome SSO 1 项加单测 16 项、0 skip。无新实现。21done/5cancelled/24ready。Goal active，未归档。

证据 `evidence/T-06-replan-2026-09-23.md`。HEAD `56eda485`。未推送、未归档。

## revision248 — T07当前候选关闭

revision248：T07关闭。真实 Chrome 回调旅程 12 项 0 skip，外域 returnTo 单测含在 13 项 0 skip 内。22done/5cancelled/23ready。Goal active，未归档。

证据 `evidence/T-07-replan-2026-09-23.md`。HEAD `450c9d11549d16d9da4e809d57f19f6fb9741a01`。未推送、未归档。

## revision249 — T09当前候选关闭

revision249：T09关闭。三端 dev/prod 模式、full/core 包和镜像合同通过。默认套件 944 实际执行、247 服务门控 skip 不计通过。23done/5cancelled/22ready。Goal active，未归档。

证据 `evidence/T-09-replan-2026-09-23.md`。HEAD `a139dbc5`。未推送、未归档。

## revision250 — T10当前复验取消重复施工

revision250：T10按当前复验取消重复施工。发布合同 128 项、0 skip。无新实现。23done/6cancelled/21ready。Goal active，未归档。

证据 `evidence/T-10-replan-2026-09-23.md`。HEAD `c426ec98`。未推送、未归档。

## revision251 — T08当前复验取消重复施工

revision251：T08按当前复验取消重复施工。三 Origin Chrome 5 项加上下文单测 3 项、0 skip。无新实现。23done/7cancelled/20ready。Goal active，未归档。

证据 `evidence/T-08-replan-2026-09-23.md`。HEAD `ecc163ed`。未推送、未归档。

## revision252 — T11当前复验取消重复施工

revision252：T11按当前复验取消重复施工。两 App HTTPS 旅程 6 项、0 skip，生产包无 ECB/私钥。无新实现。23done/8cancelled/19ready。Goal active，未归档。

证据 `evidence/T-11-replan-2026-09-23.md`。HEAD `408f12d1`。未推送、未归档。

## revision253 — T12当前候选关闭

revision253：T12关闭。会话与导航 Chrome 20 项、0 skip。24done/8cancelled/18ready。Goal active，未归档。

证据 `evidence/T-12-replan-2026-09-23.md`。HEAD `89f94a9b`。未推送、未归档。

## revision254 — T13当前候选关闭

revision254：T13关闭。注册与验证码 Chrome 10 项加服务端拒绝 7 项、0 skip。25done/8cancelled/17ready。Goal active，未归档。

证据 `evidence/T-13-replan-2026-09-23.md`。HEAD `f2f166c9`。未推送、未归档。
