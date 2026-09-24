# 恢复入口

revision210：T45前批3候选停止并完成四项复盘；C3前端771/三App329/full通过，但Chrome1pass1fail、0skip/flaky、cleanup0，脚本旧文案与页面不匹配，首失败行未保留故不猜测。Dispatch02仅修私有验收脚本；恢复批attempts0，原3次永久保留。15done/2cancelled/1in_progress/32ready，Goal active。

当前revision172：12done/2cancelled/36ready；T50最终84ce0a9已验收。无产品writer，下一T41；/tmp/wta-t41-implementation-design.md及real-environment-outline.md为只读准备。以下历史保持原时点。

当前revision168：11done/2cancelled/37ready；T39已在6e1d7f8验收。无产品writer，下一T50；/tmp/wta-t50-implementation-outline.md仅只读实施输入。以下旧记录保持原时点。

当前revision166：10done/2cancelled/38ready，T38已验收a3b289e；无产品writer，下一T39。预研/tmp/wta-t39-implementation-outline.md与/tmp/wta-t50-current-audit.md仅实施输入；以下旧状态保留。

当前revision163：9done/2cancelled/39ready，T02已验收617a369，T03取消重复施工但AC003由T30保留；下一T38。以下是历史状态。

当前revision162：9done/1cancelled/40ready，T02已验收617a369；无产品writer，下一T03裁决再T38；以下旧状态按时间保留。

当前revision161：8done/1cancelled/T02 in_progress/40ready，cors_audit唯一产品writer，base c16966167526f9b6ab6eb213265034b3bbe53e46；T37已闭合。以下旧状态按时间保留，不覆盖本段。

当前revision160：8done/1cancelled/41ready，无产品writer，下一T02；T37的3a87bf7已验收，Goal仍active。以下旧状态按时间保留，不覆盖本段。

当前revision159：7done/1cancelled/T37 in_progress/41ready，cors_audit唯一产品writer；T36的64d67d5已验收，Goal仍active。以下旧状态按时间保留，不能覆盖当前段。

当前revision156：Goal仍active；6done/1cancelled/1in_progress/42ready，T-36唯一产品writer cors_audit。T-35已在fd8c346以170+8项零skip验收。使用gpt-6-sol/xhigh原生子代理、current单writer、Lead治理/E2E；不新建worktree。以下revision138起段落属于历史计划，不能覆盖本段执行状态。

先读<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/README.md</Path>→<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>→<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>→适用Skill→当前票。当前revision138，50票Ready、0done、0在途writer；G共识已确认，S/T/P(plan)顺序完成，current_work=specdev/goal-plan。Goal schema状态draft、ready_for_execution=false仅因为用户保留自行激活与新的执行授权，不能误读成待设计答复。

用户已确认D-002—009及LOG-018整体共识，不重复询问撤回、OSS共同锁、SSO归属、分页、模式收缩、凭据Gate、硬readiness规则替代、附件真实owner。之后由用户自行激活目标；Lead先核对HEAD/归属/实际产品与commit授权，设置run条件后从Map恢复。已有决定不因新会话丢失。

产品工作沿用单人、零子代理、零新worktree/current-direct-parent。历史31个primary implementation commit已核验存在/父链；原worktrees空、共同result不能补造clean验收，按G-legacy复核。确实无需新改动时按有证据的取消程序处置；cancelled不自动满足blocked_by，必须同步下游合同证据/责任票/依赖再重算。业务验收本轮not-run。

共享写集443组提示已由唯一Lead和串行模式覆盖；不能忽略owner策略另行并行。原47源工件快照、原报告及历史Evidence按字节保留。新业务结果写带日期Evidence，不改旧原文。

相邻OIDC change及永久知识只读，不接管其owner。公开凭据不使用、不回显；本地取消跟踪不清除Git历史，真实轮换由环境owner单独授权。归档须completed、全部适用Gate/数量/clean证据和A批准；不得以另一个change尚未实现为由回退既有安全合同。

2026-09-23执行更新：用户已激活Goal；恢复本地实施/逐票提交验收，覆盖前述计划时点的未激活说明。当前先T-32，全部50票目标保持。

## revision139 — T-32完成与用户新增授权

T-32固定产品提交 `bafd5d512a5d17c4848db278f2350fcc6631fbd7`验收通过，证据见 evidence/T-32.md；49票待完成，下一票T-33。用户允许gpt-6-sol/xhigh子代理，当前派遣只读部署核查和CORS审查；current仍单writer。用户提供三个/srv/ops目录授权按现场调整，环境风险门尚未关闭。

## revision141 — T-33完成

T-32/T-33 done，48待办。T-33最终result `da48f850cf34d8d23c09f1ad9c92ed433d5617b0`；默认932项809通过/123其他环境skip，专用24项+2Chrome零skip，独立review通过。组合运行超时失败记录保留，分离默认与专用环境后同一clean提交通过。环境实际轮换见G-security-external-2026-09-23，额外退役AI/qcloud处置等待用户来源信息。下一票T-34，子代理已只读调查，无writer已派遣。

## revision143 — T34完成

T34result 177eb5bd889afd2ab54f4a8e162dc8358d80f140；3done/47ready。714前端测试、5合成浏览器及1真实Admin登录通过；两轴审查通过。第3轮资源超时后有强制Lead复盘/独占恢复；旧失败未覆盖。G-security-external用户追加撤销确认已关闭。下票T47，预研/tmp/wta-t47-audit.md；T29预研/tmp/wta-t29-audit.md，必须保全私有恢复资料。未推送/归档。

## revision144 — T47启动

base 97e1ee9e1ad40de75deffd025379a6a5488c4882；唯一产品writer cors_audit，范围OSS列表请求所有权与受控组件回归。Lead状态/提交/验收。3done/1in_progress/46ready。

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

## revision157 — T36完成

Revision157: T36 accepted at64d67d5; C3 Atomic66+Wake1 zero skips; exact unchanged-input reuse of C2 unit178/SMS8 and gates, original run sources explicit. Both reviews pass; clean exact HEAD/tree and owned cleanup. Three attempts preserved. 7done/1cancelled/42ready; next T37; goal active, no archive.

## revision158 — T37启动

Revision158: T37 active at 38032d24335c52cafea855b19d51fb36295162ef;7done/1cancelled/1in_progress/41ready;cors_audit sole product writer, Lead governance/commit/isolated E2E. Typed retryable unsent facts and owner-CAS RETRYABLE preserving digest/TTL; unknown remains closed; T38 owns precise manual retry.

## revision159 单次SDK请求的实例事实闭环

编辑前新增精确写集：<Path>backend/wta-common/wta-common-sms/src/main/java/org/namewta/common/sms/config/SmsAutoConfiguration.java</Path>。在现有Sms4jBlendRegistry维护自身以maxRetries=0创建的实际代理实例identity；使用已验证的BaseProviderFactory.createSms→SmsProxyFactory.getProxySmsBlend→SmsFactory.register同一引用，保留原SDK初始化所需钩子，不将void create后再get的可覆盖对象盲认证。remove先撤认证；注册/更新失败不保留认证，按账号并发更新需一致，不暴露配置对象。common-sms notify目录内小型SmsSingleAttemptBlendVerifier SPI由Registry实现，Resolver经AutoConfiguration ObjectProvider注入，缺失/identity不匹配即不把拒绝标成可重试；严格腾讯结构allowlist只有该证明成立才启用。已有Registry拥有这一事实，不新增第二套全局注册平台，不反射SDK，不让common反向依赖业务。固定对象捕获到send，避免查验A发送B。公共SPI/构造/配置方法调用者与测试同步。

### 新事实：T02真实在线token路径，下一独立安全修复候选

legacy只读核查确认UserLoginSuccessListener将真实tokenValue赋UserOnlineDTO.tokenId，系统在线设备/myself接口和前端常规强退使用该tokenId路径变量；SysLogFilter记录原始servletPath，异常处理URI也需核验。区别于任意客户端元数据猜测，这是AC002的真实业务凭据副本。保持T37单writer当前实施；完成后优先串行复核/修复T02（与T38无依赖冲突），再推进Notify后续。统一现有path sanitizer及HTTP/OperLog/错误sink，不以改API或删正常观测规避。X-Request-Id泛canary不列阻断，仅可选格式有界加固。报告/tmp/wta-t02-t03-current-audit.md生成后作后续Packet输入，当前不关闭T02。

## T37 C1 当前失败与修正

固定 `e2907c41f4b1bef442c68b95a0fc09aa9c3b273a`，39类187项单元零skip、静态门禁通过；真实run `b0d200e1eaf3766e` 中SMS10项1error（模板映射夹具）和Redis3项1error（生产Composite codec完成CAS）。formal attempts=1，不能验收。另驱动匿名卷不存在检查异常，容器/进程组均无残留、32808/32809已关闭；ops核精确卷清理证据。cors恢复唯一产品writer修正，Lead不并行Maven。失败记录保留，T36真实回归尚未启动。

## revision160 — T37完成

Revision160: T37 accepted at3a87bf7, attempts2; current187units+13real retry/Redis+67Atomic/Wake zeroSkip, both reviews pass, clean source and owned cleanup. C1 two errors and driver cleanup-evidence limitation retained.8done/1cancelled/41ready; next T02 real token-path log fix, thenT38; Goal active, no archive.

## revision161 — T02启动

Revision161: T02 implementation active atc16966167526f9b6ab6eb213265034b3bbe53e46;8done/1cancelled/1in_progress/40ready. Real login token in online device URL leaks to HTTP/error logs; minimal shared LogSanitizer path fix with current HTTP/MySQL canary acceptance. cors_audit sole product writer; Lead services/commits/governance. T37 closed, no active services.

## T02 真实行为红灯（正式验收0次）

测试专用checkpoint `8a88965363ca295122efadabf0bc97e99c6704c4`，未改生产。冻结driver cff4e66c3f108c71a212623fdab88f88783415f977d33803ed2bf2180024f452，run4e81e6d59c798f07，七类35项仅新canary一failure、零error/skip；有效token及真实controller强退已执行，HTTP审计仍含token断言失败。后续error/OperLog/DB断言因首断言失败尚未执行。隔离MySQL、匿名卷、进程组、32814端口全清，source前后clean。cors恢复唯一产品writer作最小修复，Lead当前无服务/Maven；原red及两次编译开发记录保留。

## revision162 — T02完成

Revision162: T02 accepted at617a369; current real39 zeroSkip, consumers70+HTTPS1/Chrome6 zeroSkip, default1000 includes150 environment skips explicitly excluded; full/core packages verified. Initial consumer skip and pnpm environment failure retained.9done/1cancelled/40ready; T03 current no-new-work adjudication then T38. Goal active; no archive.

## revision163 — T03当前复验

Revision163: T03 cancelled as no new product work after current617a369 real bounded HTTP/signature/heap and consumers/full-core proof. Historical214de538 remains; AC003 retained by T30 and only redundant implementation edge removed.9done/2cancelled/39ready; nextT38; Goal active.

## revision164 — T38启动

Revision164: T38 started fromb47ff8b after acceptedT02 and no-new-workT03 closure.9done/2cancelled/1in_progress/38ready. cors_audit sole product writer, Lead governance/commit/isolated acceptance; exact ID and safe actual requeue contracts; no new schema or Client/owner model.

## revision165 T38字典合同补齐

revision165：NotifyDelivery已有真实PENDING状态，但notify_delivery_status基座字典缺该值，重试后监控会显示未知。提前扩50-cde-base-dml.sql写集，只补唯一PENDING=待投递字典项，保留同六SQL基座与其他初始化；前端Delivery类型/字典渲染同步，不新增后端状态。不得执行生产DML或重放基座；当前新隔离库验收，存量Tag差异由T30持有。

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
