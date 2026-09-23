# 工作记录

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
