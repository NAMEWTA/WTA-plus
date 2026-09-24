# T-44 Dispatch01 — 诊断与业务解耦

## 权威、基线与所有权

Ticket `ticket/44-optional-oss-diagnostics.md`；Goal `goal-plan.md`；已接受ADR D-008及AC-044；此前T40完成结果26f04b94，治理基线8db922e1971b4781b2b53f8db837c06f7b60c4e7。main/current，provider=git，direct-parent，无worktree。用户Goal允许实施、本地commit及委派；cors_audit唯一产品writer，Lead独占所有治理、提交、测试/构建/隔离服务，ops_audit与legacy_audit只读。writer不推送/部署/生产操作。

## 写集与实际Skill

Ticket frontmatter 19条是权威；本轮01A仅admin OSS测试根可写。生产与生成物、其他票、change治理只读。01B须Lead接收红灯后再发。engineering/fullstack/module/common按实际入口及命中references检查System classic、Service复用OssFactory/RedisUtils、HTTP POST和安全日志、默认缓存与事务失效；不得扩wta-api/common或修改六SQL。额外写路径先回Lead登记。生成物只由Lead用真实full JAR捕获与正式工具生成。

## 01A 红灯与01B预期合同

先使用现有生命周期、上传、迁移服务的真实方法编写诊断缺失/过期不阻断合法配置的测试；保留权限/非ACTIVE/错误policy/缺service负例，确保失败来自旧requireServing，而非错误夹具或编译。不要先改生产使测试变绿。返回路径、测试选择器、具体红灯预期和未验证声明；Lead串行执行并保留结果。

01B合同见Ticket revision199和activation-audit/writer-plan/http-path-registration存档。业务不依赖registry；启动/config提交后无远端refresh；管理员单配置有界诊断、去敏VO，唯一PRIVATE默认管理不放松；无配置/坏非默认核心可启动，无效默认不复活Redis指针；无效可选Duration不abort核心。健康组隔离，应用全局调度保留；真实最小权限MinIO+full app/Notify scheduled fallback必验。

## 返回、验证与停止

writer返回Ticket ID、workspace、base/HEAD、dirty和精确修改列表、未运行测试、冲突/风险。Lead先审核并提交固定候选，定向red/green、全后端测试/full与core包、Skill静态、真实MySQL8.4/Redis/最小权限MinIO、full app核心和诊断HTTP、授权/审计去敏及Notify兜底；OpenAPI正式生成/check/typecheck后再完整合同门禁。测试任务局部JVM heap 128m/1536m，不改全局环境。源码冻结期间writer停写，真实测试零skip及cleanup[]、来源/产物一致才算通过。完整候选最多3次，重复失败无新证据提前复盘；无权限/新公共决策/写集不足/资源冲突立刻回Lead。尚无implementation/result，不能勾AC或宣称完成。

## revision200 — 红灯与Dispatch01B

revision200：T44定向红灯已证实，固定11960110的53例为49pass/1failure/3error/0skip；三条诊断前置阻断及空service仍进入Provider的缺陷均可重现。Dispatch01B开始产品实现，完整候选attempts0；14done/2cancelled/T44 in_progress/33ready，Goal active。

`red01` Maven exit1、编译成功，源码前后clean同值。生命周期/直传/迁移各因旧readiness门禁抛错；空service负例在objectStore.accessPolicy被调用后失败，确认须补本地路由校验。其余49项通过；未把预期红灯算成验收或完整候选失败。XML/命令/源码/两份独立审查已保存red01/manifest.json。

01B在原19条写集内完成：业务诊断解耦而授权/ACTIVE/service/policy不放松；DB成功读取后清SYS_OSS_CONFIG专用缓存和默认指针，再填合法当前行，DB故障仍核心报错；单配置管理员诊断、配置变更只失效、无启动远端探测、常驻应用调度、core和ossdiagnostics健康组及规范同步。可选诊断timeout冻结为每网络步骤100ms–3s，最多5个顺序步骤，网络等待预算最多15s，不称整个请求3s；配置无效返回固定诊断配置错误，核心仍启动。不得起未回收后台任务制造表面超时。若需严格单个总deadline或common路径先回Lead登记，T45策略解释未提前改动。

01B source=11960110ff4f5a8c99a885dbf2866578fc8493af；main/current。cors_audit唯一产品writer，允许原Ticket19条内生产/测试/对应文档，OpenAPI生成三处仍由Lead正式工具写；不跑构建/服务/提交、不写治理。返回精确diff与选择器后停写，Lead定向绿灯及独立审查。ops_audit/legacy_audit仅准备/tmp/wta-t44私有隔离驱动，未启动或改仓库。

## revision201 — 原始诊断属性的测试消费者同步

为避免Spring Binder在可选Duration词法错误时阻断核心，三项诊断配置改同型String JavaBean并在诊断时解析。仓内唯一超出现有admin OSS测试根的消费者为 `backend/wta-admin/src/test/java/org/namewta/test/profile/material/ProfileSelfMaterialsBrowserIntegrationTest.java`，已事前登记为第20条精确写集，仅同步setMaxSnapshotAge的配置字面值/必要编译消费，不改Profile业务。writer尚未改此文件；红灯与AC/候选attempts不变。

## revision202 — A1真实验收与日志缺陷补修

revision202：T44 A1源码e11c1b6f定向148全过，默认1115中899pass/216环境skip，full包通过；七组核心启动/Notify轮询真实场景通过。MinIO功能链已通过但日志检测发现access key与secret key，A1安全验收失败，整票仍in_progress。26条精确写集登记后补日志红灯与修复；14done/2cancelled/1in_progress/33ready，Goal active。

源码e12df121为01B产品实现；green01在编译期发现新增测试4处泛型断言歧义，保留exit1/零执行证据，e11c1b6f修复。green02为33类148例零skip；默认261类1115例899pass/216环境skip、零失败；full JAR SHA256 `835b4de84b911ff77ba20ba003218f30af015559dccfadcdbc30fdf1d8d069a8`，源码tree `92781356146df461ab9815a13aa2884effc0740e`。这些不是环境跳过项的通过证明。

七组真实核心验收均绑定同一clean源码/JAR：empty `de1c3f734c322a79`、bad-nondefault `7704e6179563c466`、duplicate-default `003f22307c765c71`、bad-default `37e0ba0f8a622025`、invalid-diagnostic `e3e150d8206beeb2`、minio-offline `c8511c8ff56072a3`、Notify fallback `637717597daa1c8a`。前六验证核心UP、真实登录/菜单及适用的缓存/诊断失败关闭；counted端点均零调用，offline为不可连接回环端口、未伪造计数。最后一项用Redis ACL selector仅拒绝notify:outbox:wake的PUBLISH并允许缓存失效通道，真实公告发布形成READY，唤醒失败后POLL领取，Outbox DONE/Delivery DELIVERED/Attempt/Message/用户关系均1，后续两次tick不重复；无外部SMS/MAIL调用，所有资源cleanup零错误。v2全局禁PUBLISH破坏缓存登录和v3忽略channel型ACL拒绝的夹具失败均保留，不能当成产品兜底失败或通过。

MinIO驱动v1/v2因空hex SQL失败，v4到诊断审计因SQL排序规则失败；均保留并在v5前复盘。v5/v6完整走通最小权限A/B上传下载、公开无canary GET、权限/状态/坏配置负例、单桶诊断、3个非法ID零远端、诊断STALE后下载、默认B与旧A、实际OpenAPI436路径；但postcheck未通过。v6 `4ca003409fc458ce` 的安全摘要记录app access key 7次、app secret key 1次，其余8类0；不保留原值/上下文。前置logger归属可能沿用此前HTTP行，不能据此断定SQL stderr也是HTTP logger。源码证实HTTP JSON字符串url未检查签名query，且dev启用SqlLogInterceptor，后者插值实际配置参数并打印原异常消息。A1作为一次真实安全验收失败记录，整票AC未勾；已捕获OpenAPI仅属该源码事实，不是完整通过。所有MinIO运行cleanup零错误。

事前新增6个精确路径：common-json的LogSanitizer及其测试、common-mybatis的SqlLogInterceptor/SqlLogProperties、common-web的SysLogFilterTest，以及application-dev.yml的SQL日志说明。加上原20条共26条；不改数据库、业务返回、权限、URL有效性或执行SQL，不新增公共API/依赖。共用日志副本须隐藏带签名/凭据的URL与上传令牌路径，同时保留普通公开URL；SQL保留结构、mapper、耗时和异常类型，禁止写入绑定值、原错误消息或秘密。先用真实日志入口补红灯，再实现；既有敏感HTTP和操作日志合同保持。实现细节不得通过关闭整套日志或绕过安全扫描过门禁。

证据见candidate-a1/manifest.json；仅精确测试JWT模式脱敏，保留源/留存hash及替换数，失败/skip不改写。真实运行私有raw日志不纳入仓库。OpenAPI正式生成、前端/core/静态及修复后真实验收仍待完成，不归档。

Dispatch02A：cors_audit仅可写LogSanitizerTest、SysLogFilterTest和原admin OSS测试根，先复现签名URL及SQL绑定secret进入日志，产品不动。Lead固定测试checkpoint后跑红灯；另行交02B实现。writer不得运行构建/服务/提交、不得改治理。

## revision203 — 日志红灯与Dispatch02B

revision203：T44日志回归红灯固定于d4a15669，6例/6失败/零error与skip；A1安全失败仍保留。02B按签名URL、uploadToken和SQL固定元数据合同补修。14done/2cancelled/T44 in_progress/33ready，完整候选attempts仍1，Goal active。

日志红灯见 evidence/T-44-current-2026-09-23/logging-red01/manifest.json；这是缺陷复现，不计为新增完整候选失败，也不勾选AC。

SQL补充源码事实：BoundSql.getSql已经展开动态片段（含现有Mapper的${ew.customSqlSegment}），即便不插值绑定参数，也可能含字面量或注释中的secret。因此revision202的“SQL结构”具体落实为SqlCommandType固定枚举、经字符/长度限制的Mapper ID、耗时和异常类名；不输出原SQL文本、参数、异常message/stack。不新增SQL解析器或依赖，不关闭日志。原Statement执行、结果及异常传播保持，console/log两种配置均适用。

HTTP日志只改副本：所有嵌套/数组文本值中的签名或凭据URL隐藏；uploadToken属性、OSS上传路由令牌及签名查询字段隐藏；普通公开URL和业务code维持现有合同。真实响应和路由不改，操作日志既有服务端路由模板继续有效。写集仍26条，无公共API/数据库变更。

Dispatch02B：cors_audit为唯一产品writer，可在已登记LogSanitizer、SqlLogInterceptor、SqlLogProperties、application-dev.yml的SQL注释以及02A测试路径落实修复；不运行Maven/服务/提交，不改治理。Lead审查并冻结源码后执行定向绿灯、default/full、真实MinIO与Notify兜底；实际OpenAPI正式生成、前端/core/静态门禁仍待完成。

## revision204 — T-44当前候选验收完成

revision204：T44在fc50c1e完成当前候选验收；日志191全过、backend905pass/216环境skip、8真实应用场景及默认SSE补充捕获通过，18类canary零命中；既有OSS真实JUnit7及其内部Chrome10零skip；前端760/3App329、full/core/静态通过。15done/2cancelled/33ready，无in_progress；下一T45，Goal active，change尚未完成或归档。

结果`fc50c1e1227d42a46f8e25b3e19949baeccb89e0`，tree `c39bb13afcbf07459b2fb07c1c2179f3198fc807`；父链基线`8db922e1971b4781b2b53f8db837c06f7b60c4e7`。完整记录见 `evidence/T-44-current-2026-09-23/complete-candidate-a2/manifest.json`。后端及真实full JAR源码为`958aad6174c880fbf8c7afbfb8f657d37c968979`，最终只新增4个OpenAPI生成文件，精确输入等价证明明确区分两个SHA；未冒称后端在最终SHA重跑。A1泄密失败、6例日志红灯及夹具失败保持原时点；A2是第二个完整候选。无推送、部署、生产修复或归档。
