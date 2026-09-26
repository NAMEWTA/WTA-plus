# Dispatch Packet T-36-20260923-01

operation=dispatch；task_kind=implementation；delivery_channel=native；provider=gpt-6-sol/xhigh；Lead=single-agent (/root)，owner=cors_audit (/root/cors_audit)。repository=/srv/WTA-plus，workspace_ref=current，branch=main，current/direct-parent单writer，无新worktree。base `5118051403de0648540646d98536d8c4f3f9f26d`。当前治理dirty属于Lead，不回滚。预计产品checkpoint由Lead提交后填写，不伪造SHA。

目标：AC-036站内信消息/关系/投递结果原子提交，实时仅提交后；并发不同收件人共享一条消息，失效租约零写入、暂态失败可有界恢复，外部UNKNOWN不受损。读取顺序：完整tickets-map.md→下列项目Skill及必要硬规则→ticket/36-atomic-in-app-delivery.md、goal-plan.md与spec/ADR的AC036。它们均位于本Packet的父级change目录。最低Skill列表：
- <Path>.agents/skills/engineering-standards/SKILL.md</Path>
- <Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>
- <Path>.agents/skills/wta-module-guide/SKILL.md</Path>
- <Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>
- <Path>.agents/skills/java-api-compatibility/SKILL.md</Path>

额外输入：T35已验收 evidence/T-35.md（170单元+8真实），/tmp/wta-t36-audit.md只读预研。必须重读当前fd8c346后产品分支，不按旧行号改代码。按I-implement TDD/codebase-design/双轴合同，先可观察红灯再最小实现。

唯一产品写集（目录仅授权本票所需文件），shared owner cors_audit本票独占：
- <Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/</Path>
- <Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/</Path>
- <Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/port/</Path>
- <Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/adapter/event/</Path>
- <Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/NotifyNotificationDao.java</Path>
- <Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/mapper/</Path>
- <Path>backend/wta-modules/wta-notify/src/main/resources/mapper/notify/</Path>
- <Path>backend/wta-modules/wta-notify/src/test/</Path>
- <Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>
- <Path>backend/wta-api/src/main/java/org/namewta/notify/api/InAppNotificationPort.java</Path>

只读：SpecDev、Skill、相邻OIDC、永久知识、其他产品路径。禁止生产服务/凭据/私有temp读取写入、Docker服务启动、commit/push/部署/归档。允许产品修改与精确非E2E Maven/分层检查，先报选择器避免并发构建。日志与返回写/tmp/wta-t36。Lead拥有所有真实服务/E2E和commit；代理不修改SpecDev状态/Evidence。

实现约束：消息/关系和结果在已代理DSTransactional同一短事务，Intent→Outbox→Delivery统一锁序/fence先校验；既有PK/unique保护，无无差别吞SQL异常；真实DsTxEventListener AFTER_COMMIT，回滚零推送，推送失败不回滚/重投。SMS/MAIL供应商I/O不得移进事务，T35安全及ACQUIRE/COMPLETE分类保持。public语义/内部构造器消费者同步；没有兼容桥要求。

测试：真实代理/双连接障栅、message/recipient/Delivery/Attempt/finish/aggregate六点SQL故障、commit前断连/commit后ACK丢失、租约过期/owner替换、重复任务、提交后push失败。先提交测试并告知Lead可运行红灯的测试类/方法/环境属性；代理不跑真实E2E。沿用六SQL隔离库，修夹具账号密码为环境注入、fresh fork，seed可能已存在须受控使用/恢复。先完成非E2E红绿，准备真实套件；不得放宽断言/扩大超时制造绿色。

OUT：T37缓存重试模型、T38截止、T42附件、真实存量恢复。提供仅针对IN_APP指定ID的只读盘点与恢复操作稿给Lead写入Evidence，不改未授权文档/SQL路径，不执行。如生产worker/新路径/API需要扩范围，停止相关编辑先报Lead；不得先改后报。无相同失败盲重试，三候选限制由Lead控制。

返回：Ticket/Packet/base、实际文件、红绿完整argv/cwd/exit/count/skip、技能操作、设计/失败边界、未运行项与真实环境准备、dirty/写锁状态。只报告真实结果，不自行Done。最终交还产品锁，Lead独立审查/提交/固定候选验收。

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

## T36 第二候选验收未通过

固定a2749a7dcc0bbc5c0643e07eba938446611c995a，39类178单元/Wake1/SMS8通过零skip；Atomic66零failure/1error，AFTER参数分支未清duringProvider故障钩子，下一正常投递再次被注入lost ACK。C1三断言及多渠道/列边界已通过。run909ae637d59b2ebe与0abb8a5220460930全部清理、clean源前后一致；正式attempt2保留。Lead接管唯一测试写锁，仅在两分支汇合后重置钩子，不改生产代码、不删弱断言。第三候选须完整Atomic/Wake真实复验；178单元与SMS8输入未变可严格等价复用，明确原实际运行SHA。
