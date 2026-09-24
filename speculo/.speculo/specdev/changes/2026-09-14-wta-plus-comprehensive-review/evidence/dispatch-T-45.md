# T-45 Dispatch01 — 有界诊断事实

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
