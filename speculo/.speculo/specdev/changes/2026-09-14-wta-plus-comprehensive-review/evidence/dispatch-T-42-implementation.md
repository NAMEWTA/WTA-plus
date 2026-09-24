# T-42 Dispatch01B — 生产邮件附件完整闭环

前置：revision220已激活；红灯固定源码和计数见red01/manifest，旧失败保留。此派单只授权Ticket当前登记写集的本地实现，不是AC通过结论。cors_audit唯一产品writer；Lead独占治理、提交、构建/服务、OpenAPI正式生成和最终验收。其他agent仅只读或/tmp驱动。不得为通过测试放宽既有权限、失败分类、事务/租约或六SQL门禁。

## 公开命令与正文邮件

NotificationCommand仅加typed List<String> attachmentOssIds（十进制正ID，内部严格转Long），HTTP可选缺省空；按正整数、有界数量、去重保序校验。Java canonical构造所有仓内消费者同批直切，无兼容重载或从templateParams偷读旧字段。12个生产构造点及测试按真实rg清单迁移；无需附件的系统调用传空列表。服务器从受信LoginUser捕获userId+Client PK，非空附件缺任一失败关闭；不接收HTTP actor或信任metadata/create_by/worker线程。Intent保存不可变actor。

Demo新增独立demo-mail场景，title/content由中心可配置包装模板渲染、不要求path；notice-published/workflow-task仍要求path。此Demo正文包装是明确例外，通知规范同步说明，不能取消业务模板权威。六SQL新增停用/未绑定实际账号的安全种子，禁止写SMTP秘密。现有红灯的旧Map夹具在绿色实现中迁移为typed字段和真实持久关系断言，不能为了使旧夹具绿而保留不安全Map后门；原失败已冻结。

## 所有权、授权、事务

Notify拥有notify_intent_attachment真实关系，System拥有sys_oss/sys_oss_ref及对象操作；common只定义SPI，System不依赖Notify Mapper/Entity。Notify实现生产Snapshot SPI，通过OssService受限API协调System；Notify Service→DAO→Mapper，System classic。新表唯一关系PK intent_attachment_id、position/source/target/reservation状态、不可变物理身份和必要版本/owner，七审计字段/中文注释/索引全齐，不恢复sys_notify_log。旧NotifyLogIdGenerator及事件虚构owner直切真实Intent/关系并清零全消费者。

提交事务先写Intent与真实关系行，再System在源对象锁内比较可信actor user+Client、ACTIVE/非PENDING、元数据/预期访问类型/原service配置，然后绑定真实关系PK来源引用；所有来源按OSS ID稳定锁序。检查和绑定不能拆成objectMetadata+reconcileReferences的TOCTOU，也不能复活PENDING。任一失败Intent/关系/引用/Outbox同事务回滚；无附件在提交/worker两处直接短路，零System OSS操作。两处幂等返回（前置查询、唯一键冲突回读）都比对持久actor与规范化有序IDs，冲突拒绝，后者用当前读取避免RR旧快照。不能借无附件请求拿到已有有附件Intent回执。

## 私有快照与不确定副作用

同一Intent多delivery复用同一组快照，不能每次生成日志ID复制。短事务预约真实关系/固定key/复制权，提交已确认后才I/O；copy前持久记录确保外部结果始终有owner。来源引用在排队及COPYING/UNKNOWN时保护来源。PRIVATE源优先其DB核实的同PRIVATE配置独立key；PUBLIC_READ源使用当前合法PRIVATE默认，缺目标明确失败，不能将PUBLIC默认当私有。固化source和target实际service/key/必要身份，target reservation也必须在现有sys_oss/引用保护下，避免预约到副本注册间配置改向。不能绕过T46配置身份互斥。

跨配置不假设同bucket copy API可用；使用既有受控下载/上传入口、明确数量/大小/等待和临时文件界限，校验实际字节、长度、文件名与摘要。复制与SMTP都在数据库短事务外；新外部I/O能力若需越界修改先报Lead登记。只在当前owner/版本下提交关系READY、快照sys_oss元数据及实际ref，全部齐备才物化交给真实Mail adapter。未完成快照不可被当成普通可用对象。不要声称MySQL事务能回滚MinIO。

COPY成功而关系/ref写入失败或commit ACK未知：保留稳定reservation/key与源引用，后续核真实目标身份/bytes收敛，不盲重拷/删/换key。迟到copy不能覆盖新owner；无法证明旧I/O结束就保守保留COPY_UNKNOWN，不起另一写入。多附件第二项失败、物化失败：SMTP=0，原源不删除，已创建目标都有持久可查恢复事实或已确认安全补偿；单纯warn/数据库回滚不是无孤儿证据。安全补偿失败也必须持久可核对。

common单delivery cleanup不得删除共享资源。生产owner协调安全释放，全部相关delivery可证明未发送、无活租约才解除真实来源/快照ref并交既有生命周期回收；明确取消/到期路径需有实际可调用/可运行的受控收敛入口。ACCEPTED、Provider UNKNOWN、在途或不确定提交持续保留，不改T37/T39/T40幂等/截止/撤回fence，不盲重发或自动释放。安全重试复用已确认快照，数量不增长。权限授权事实与common审计NotifyContext分开。

## 真实验收与交付

新增opt-in org.namewta.test.notify.NotifyMailAttachmentIntegrationTest（notify.mail.attachment.integration=true），完整NamewtaApplication Spring上下文，生产Snapshot SPI/System/Notify DAO/NotifyClient/Mail adapter/worker全部实际装配，只有最底层MailNotificationSender替换为捕获真实物化bytes的假发送器。窄故障hook必须delegate实际实现，其余路径不能mock。使用owned全新MySQL8.4六SQL+Redis+MinIO，不读/srv/ops、不发SMTP。与ops确认精确property名、方法清单和计数后回交；Lead实际运行。

真实断言至少包括：无链接正文到sender且零OSS；notice/workflow缺path仍拒绝；1/多授权附件bytes/name/size/order和独立私有对象/真实owner refs；User/Client/缺Client/缺失/PENDING/失效service及排队后源状态改变拒绝；队列中临时源受引用保护；两条提交重复出口及换User/Client/IDs/顺序；多delivery竞争与共享快照；第N项复制/关系/ref写入/提交确认/物化故障SMTP0和有主副本；UNKNOWN不重发不删除；安全取消/到期/release只影响自有引用，来源不丢失。数据库锁/竞争用真实双连接和屏障，fresh XML正数且零skip，资源清理成功。

仅新增一表时初始化器103→104，同时更新ai-retirement/release-config/release-integration及Notify基座断言；六文件精确集合保持。同步backendREADME/SystemAGENTS与Notify规范/模块事实，明确私有目标、保留/安全回收和未知处置，不能宣称已部署或已迁移旧库。frontend/api-contracts生成路径只由Lead在同源full JAR捕获后正式fetch→generate→check；子代理不得手写生成JSON/TS。

回交所有生产/测试/文档修改及精确路径、源消费者清单、未解决问题；不运行构建、服务、commit或治理脚本。Lead固定后执行受影响单元/合同、required realE2E、默认后端/full/core、正式OpenAPI/前端typecheck和适用静态门禁，双轴审查通过才勾AC。三个完整候选失败先四项复盘，不覆盖旧失败或重置attempts。无需再次询问已授权的本地实施/提交；超出写集先修订登记。

当前交接门槛：red01的附件例未到send，先修复缺少账号额度的夹具并固定red02；Lead明确发出Dispatch01B后才取得产品写锁。

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
