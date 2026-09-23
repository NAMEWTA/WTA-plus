---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-18枚举.agents/skills入口并按本票真实路径/领域绑定；Map为最低集合"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/23-durable-provider-callback.md</Path>", "<Path>backend/wta-modules/wta-notify/</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path>"], "outputs": ["T-23的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/23-durable-provider-callback.md</Path>", "<Path>backend/wta-modules/wta-notify/</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path>"], "outputs": ["T-23的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-module-guide", "path": "<Path>.agents/skills/wta-module-guide/SKILL.md</Path>", "sha256": "bb57a781314abe316f06ba9538f62043f1bd053904c0832969e575c09731a7a3", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/23-durable-provider-callback.md</Path>", "<Path>backend/wta-modules/wta-notify/</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path>"], "outputs": ["T-23的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-common-modules-guide", "path": "<Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>", "sha256": "e92775ce47af41bd33c1b3293d7f8a3185fdd739b9b588519e043c654f678d98", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>backend/wta-common/wta-common-sms/</Path>", "<Path>backend/wta-common/wta-common-json/src/main/java/org/namewta/common/json/utils/LogSanitizer.java</Path>"], "outputs": ["SMS4J回执标识、官方TC3/ACS3有界只读查询与统一回执日志脱敏"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/23-durable-provider-callback.md</Path>", "current-workspace实际diff及本票验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-23.md</Path>：命令、退出码、测试数、AC与Skill Execution Records"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "finding:B-05", "contract:AC-023"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-23
title: 将供应商回调幂等纳入持久事务
status: "review"
planning_depth: "deep"
planning_depth_reason: "安全/鉴权、公共合同、数据一致性或共享核心路径变更：将供应商回调幂等纳入持久事务"
ready: true
risk: high
blocked_by: ["T-22"]
contract_ids: [AC-023]
owner: single-agent
expected_changes: ["<Path>backend/wta-modules/wta-notify/</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/ProviderCallbackService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/ProviderCallbackUseCase.java</Path>", "<Path>backend/wta-common/wta-common-sms/</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/50-cde-base-dml.sql</Path>", "<Path>frontend/packages/web-domains/notify/</Path>", "<Path>docs/notify-providers.md</Path>", "<Path>release-artifacts/tests/notify-baseline-contract.test.mjs</Path>", "<Path>.agents/skills/engineering-standards/references/notification.md</Path>", "<Path>.agents/skills/wta-module-guide/references/modules/notify/index.md</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/migration/ossnotify/OssNotifyMigrationUnitTest.java</Path>", "<Path>backend/wta-common/wta-common-json/src/main/java/org/namewta/common/json/utils/LogSanitizer.java</Path>", "<Path>backend/wta-common/wta-common-json/src/test/java/org/namewta/common/json/utils/LogSanitizerTest.java</Path>", "<Path>release-artifacts/scripts/init-mysql-container.sh</Path>", "<Path>release-artifacts/tests/release-integration-contract.test.mjs</Path>", "<Path>release-artifacts/tests/release-config.test.mjs</Path>"]
writable_paths: ["<Path>backend/wta-modules/wta-notify/</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/ProviderCallbackService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/ProviderCallbackUseCase.java</Path>", "<Path>backend/wta-common/wta-common-sms/</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/50-cde-base-dml.sql</Path>", "<Path>frontend/packages/web-domains/notify/</Path>", "<Path>docs/notify-providers.md</Path>", "<Path>release-artifacts/tests/notify-baseline-contract.test.mjs</Path>", "<Path>.agents/skills/engineering-standards/references/notification.md</Path>", "<Path>.agents/skills/wta-module-guide/references/modules/notify/index.md</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/migration/ossnotify/OssNotifyMigrationUnitTest.java</Path>", "<Path>backend/wta-common/wta-common-json/src/main/java/org/namewta/common/json/utils/LogSanitizer.java</Path>", "<Path>backend/wta-common/wta-common-json/src/test/java/org/namewta/common/json/utils/LogSanitizerTest.java</Path>", "<Path>release-artifacts/scripts/init-mysql-container.sh</Path>", "<Path>release-artifacts/tests/release-integration-contract.test.mjs</Path>", "<Path>release-artifacts/tests/release-config.test.mjs</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>", "<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: ["<Path>backend/wta-modules/wta-notify/</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/ProviderCallbackService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/ProviderCallbackUseCase.java</Path>", "<Path>backend/wta-common/wta-common-sms/</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/50-cde-base-dml.sql</Path>", "<Path>frontend/packages/web-domains/notify/</Path>", "<Path>docs/notify-providers.md</Path>", "<Path>release-artifacts/tests/notify-baseline-contract.test.mjs</Path>", "<Path>.agents/skills/engineering-standards/references/notification.md</Path>", "<Path>.agents/skills/wta-module-guide/references/modules/notify/index.md</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/migration/ossnotify/OssNotifyMigrationUnitTest.java</Path>", "<Path>backend/wta-common/wta-common-json/src/main/java/org/namewta/common/json/utils/LogSanitizer.java</Path>", "<Path>backend/wta-common/wta-common-json/src/test/java/org/namewta/common/json/utils/LogSanitizerTest.java</Path>", "<Path>release-artifacts/scripts/init-mysql-container.sh</Path>", "<Path>release-artifacts/tests/release-integration-contract.test.mjs</Path>", "<Path>release-artifacts/tests/release-config.test.mjs</Path>"]
shared_path_owners: ["<Path>backend/wta-modules/wta-notify/</Path> => single-agent (Lead; serial T-23 turn)", "<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path> => single-agent (Lead; serial T-23 turn)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path> => single-agent (Lead; serial T-23 turn)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/ProviderCallbackService.java</Path> => single-agent (Lead; serial T-23 turn)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/ProviderCallbackUseCase.java</Path> => single-agent (Lead; serial T-23 turn)", "<Path>backend/wta-common/wta-common-sms/</Path> => single-agent (Lead; serial T-23 turn)", "<Path>release-artifacts/docker/infrastructure/mysql/init/50-cde-base-dml.sql</Path> => single-agent (Lead; serial T-23 turn)", "<Path>frontend/packages/web-domains/notify/</Path> => single-agent (Lead; serial T-23 turn)", "<Path>docs/notify-providers.md</Path> => single-agent (Lead; serial T-23 turn)", "<Path>release-artifacts/tests/notify-baseline-contract.test.mjs</Path> => single-agent (Lead; serial T-23 turn)", "<Path>.agents/skills/engineering-standards/references/notification.md</Path> => single-agent (Lead; serial T-23 turn)", "<Path>.agents/skills/wta-module-guide/references/modules/notify/index.md</Path> => single-agent (Lead; serial T-23 turn)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/migration/ossnotify/OssNotifyMigrationUnitTest.java</Path> => single-agent (Lead; serial T-23 turn)", "<Path>backend/wta-common/wta-common-json/src/main/java/org/namewta/common/json/utils/LogSanitizer.java</Path> => single-agent (Lead; serial T-23 turn)", "<Path>backend/wta-common/wta-common-json/src/test/java/org/namewta/common/json/utils/LogSanitizerTest.java</Path> => single-agent (Lead; serial T-23 turn)", "<Path>release-artifacts/scripts/init-mysql-container.sh</Path> => single-agent (Lead; serial T-23 turn)", "<Path>release-artifacts/tests/release-integration-contract.test.mjs</Path> => single-agent (Lead; serial T-23 turn)", "<Path>release-artifacts/tests/release-config.test.mjs</Path> => single-agent (Lead; serial T-23 turn)"]
---

# T-23：将供应商回调幂等纳入持久事务

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先读Map，再按Skill矩阵读取适用入口/引用，再读本票。禁止任何implementation/review/research子代理。Ready仅表示计划合同就绪，不表示已经授权实施或已验证通过。

## 1. 战略与来源

- 目标与可观察产出：回滚后相同事件可重试成功。
- 来源：B-05；AC-023；USER-DECISION: 全面完善计划、无兼容、单人串行。
- 当前事实与调用链：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/re-review.md</Path>中T-23行及对应专项报告；源码导航为frontmatter预计修改点。
- 规划深度：deep；安全/鉴权、公共合同、数据一致性或共享核心路径变更：将供应商回调幂等纳入持久事务。

## 2. 决策状态

### 已锁定决策

用户授权常见供应商预置，短信腾讯/阿里、邮件QQ/163/腾讯企业邮箱SMTP。配置预设默认关闭、无凭据，现有场景文案沿用；启用校验真实账号必要字段，短信仍必须填写已审核签名/模板。providerKey在实际代码中是渠道账号configKey。MySQL receipt与状态同事务，未经核实的重试期限不用于自动清理。

### 已采用的低影响假设

沿用当前仓库版本与既有模块命名；实施前回读实际源码，路径变化由本票修订，不猜测不存在的实现。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 已验签回调→供应商命名空间receipt→状态推进/聚合提交 | 现有callback adapter与状态单向规则；无默认inbox/事件总线 | 本票之外的模块重写、兼容桥、远程发布与重要数据操作 |

## 4. 要构建什么

已验签回调→供应商命名空间receipt→状态推进/聚合提交。调用者可观察到：回滚后相同事件可重试成功。失败时：同事务失败可重试；已提交重复幂等；未匹配消息ID不消费receipt。

## 5. 实现契约

- 入口、输入输出与数据流：已验签回调→供应商命名空间receipt→状态推进/聚合提交。
- 不变量及失败语义：同事务失败可重试；已提交重复幂等；未匹配消息ID不消费receipt。
- 公共合同：本票只按以上行为及执行路线变更；同步全部仓内调用/生成物，沿用已有权限/Client/owner校验。未列出的接口保持原语义。
- 兼容：用户明确无需旧版兼容；仓内一次切换，不加双路由/版本等待。实际供应商协议仍须遵守。
- 安全与隐私：凭据不进入日志/UI证据；越权/过期/无owner拒绝；数据库与资源约束不能为前端成功而放宽。

自定义HMAC接入持久receipt身份为channel+providerKey（账号configKey）+eventId的SHA-256；长度分界使用JSON数组。事实摘要为规范化渠道、账号、消息ID、状态和target，不含允许重签更新的传输timestamp；签名始终覆盖完整原文。同event不同事实拒绝，未知JSON字段/非字符串/过长值拒绝。可选target在多收件人消息下必填，匹配歧义拒绝；供应商消息ID索引不再错误要求跨收件人唯一。receipt、状态CAS与聚合同事务；不自动过期、不提供清理入口。Worker尚未回写providerMessageId时不能先记已处理，应给供应商可重试失败；只有供应商合同证明不会重试才另评估持久inbox，不默认创建第二套消息系统。保留HMAC原文与时间窗。

## 6. 执行路线

1. 固定事务提交失败后seenEvents仍抑制重试及跨provider事件ID碰撞红灯。
2. 按provider/account/channel与eventId明确唯一业务身份，必要时包括delivery，避免碰撞。腾讯/阿里通过既有密钥的只读签名查询取得原生终态，具体窗口/分页/预约合同见evidence/T-23-native-query-contract.md。
3. 在同事务写事件receipt与状态推进/聚合；唯一冲突按已经完成的结果返回。
4. 删除进程内seenEvents及其过期清理分支。
5. 保留签名校验、单向状态等级和旧事件忽略语义。
6. 定义receipt保留与清理期限及对重放窗口的影响，清理不改变未完成事件。

## 7. 路径访问契约

预计点、可写范围、只读上下文及共享项以frontmatter为唯一权威。共享owner固定single-agent；只有当前票轮次可写，下一票须回读前一结果。跨模块目录只允许本票行为必需的文件，目录授权不意味着重写全部模块。新增测试位于同模块测试目录；未覆盖的新路径先修订本票/Map再写。
保留当前用户未提交改动、永久ADR/context、供应商源码与运行数据；生成物通过正式工具更新。

## 8. 验证矩阵

| 行为或风险 | 验证接缝/步骤 | 预期结果 | Evidence |
|---|---|---|---|
| 正常路径 | 已验签回调→供应商命名空间receipt→状态推进/聚合提交；执行下列定向命令及对应场景 | 回滚后相同事件可重试成功 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-23.md</Path> |
| 失败路径 | 同事务失败可重试；已提交重复幂等；未匹配消息ID不消费receipt；固定时序/故障注入，记录输入与最终可观察状态 | 无越权、错误状态或部分提交；可按定义恢复 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-23.md</Path> |
| 回归 | 运行所属包原有测试及受影响调用者；逐项核对下列AC | 跨实例相同事件不重复生效，不同provider同ID互不影响；乱序/伪签名/迟到回调按现有规则失败关闭或no-op；重启不丢幂等记录，聚合与delivery一致 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-23.md</Path> |

Workspace checks：current-workspace；`frontend:`/`backend:`表示先进入该目录，其他命令cwd为仓根，逐条串行执行。新增用例实施时登记精确选择器、实际测试数与跳过项，零测试/required跳过不算通过。

- `backend: ./mvnw -pl wta-modules/wta-notify -am test`

- E2E disposition：required: 两实例重启/并发重复/跨provider同eventId/提交失败/早到回调；真实签名适配。
- E2E owner/environment：single-agent（Lead）/current-workspace；使用隔离MySQL/Redis/OSS及必要真实HTTP/浏览器，禁止连生产。场景步骤以上表、本票AC为准；需新用例时在写集内创建后记录精确命令。
- Integration evidence：记录parent before、implementation commit及direct-parent检查；result SHA等于通过验证的implementation commit，candidate不适用。本票验证以T-23-checkpoint.json和当前Evidence为准；原生送达通过鉴权只读查询适配，未使用真实凭据。全change整体门禁归T-30。

## 9. 发布、迁移与恢复

- 顺序：前置票产生已验证合同后实施本票；源码、仓内消费者、测试和生成物同批交付。涉及DDL只编辑10-cde-base-ddl.sql，新环境按六文件基座初始化；不增加存量迁移工程。
- 兼容窗口：无；不保留旧接口或数据格式桥。生产部署不是本票自动步骤。
- 监控/诊断：观察本票AC的成功/错误状态、耗时及资源/持久化结果，日志只含安全元数据；复用现有观测入口，不新建监控平台。
- 恢复：新receipt与业务状态原子提交；恢复保留幂等数据，不自动清表。
- 不可逆批准点：提交、推送、部署、运行数据删除/修复分别需授权；本轮实现和本地验证已获授权，用户暂缓全部提交。
- 收缩条件：本票替代的旧调用/配置引用归零且仓内回归通过；无被替代入口时不适用，不为凑清单扩大删除范围。

## 10. 验收标准

- [x] `AC-023`：回滚后相同事件可重试成功。
- [x] `AC-023`：跨实例相同事件不重复生效，不同provider同ID互不影响。
- [x] `AC-023`：乱序/伪签名/迟到回调按现有规则失败关闭或no-op。
- [x] `AC-023`：重启不丢幂等记录，聚合与delivery一致。
- [x] 按Map→适用Skill→本票完成读取及实际调用；所有required Skill记录passed并可回读。
- [x] 正常/失败/回归及required E2E均完成，证据写入<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-23.md</Path>，未执行不得标通过。
- [x] 修改不超出写集，共享项只有single-agent当前票轮次写入。
- [x] 获得授权后形成非空implementation commit，Lead完成direct-parent验收并记录parent result SHA；未获授权不提交、不标Done。
- [x] Ticket、Map、Goal与Evidence一致；不存在未批准偏差。

## 11. SKILL 调用计划

frontmatter绑定的项目Skill在implementation阶段接收本票路径和上游合同，产出适用分层、权限、数据/资源边界及实现diff；engineering-standards在verify阶段根据本节命令选择受影响门禁并输出AC/退出码/E2E记录。按入口scope展开引用，不以“已读”代替实际操作。
必需入口缺失或sha256漂移时阻塞本票；Lead回读差异后更新绑定及Map，不能自动接受新摘要。实际记录归<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-23.md</Path>的`## Skill Execution Records`，本次规划不伪造实现调用记录。

## 12. 停止、检查点与交付

交付本票完整可观察行为及验收证据；数量以Map为准。缺依赖/测试环境/Skill、越界或高影响事实变化时停止受影响票，保留checkpoint和失败证据，其他独立票仍可串行推进。恢复先读Goal、Map、本票、状态及最新Evidence；记录实际HEAD/dirty差异，禁止覆盖用户修改。
依赖：T-22。单票完成条件为全部AC、实际Skill证据和获授权的direct-parent出口；仅补文档不能标Done。

Revision113只读复核：当前源码证据见<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/planning-blocker-source-review-current.md</Path>及17文件哈希。保留原blocked状态，未新增参数默认值、DDL或完成声明；所需业务/供应商事实已询问，尚无答复。

Revision120：T-23开始，610上游hash已核对。用户要求的五种禁用账号预设、必要凭据/签名校验和SMS4J消息ID保留先行；已登记common-sms、DML、通知配置页面、精确父规范与供应商说明写集。官方原生回执均不同于现有HMAC；阿里重试文档互相矛盾、腾讯仅说明再试2次，保留期不猜测。回执安全接入/身份仍在内部研究，不等待用户、不标review。29review/1in_progress/1ready/0Done，全部提交暂缓。

Revision121：T-23三项真实MySQL红灯已复现（回滚重试、跨账号eventId、同事件事实冲突），3/3失败且隔离资源已回收。冻结自定义HMAC接入的渠道+账号configKey+eventId持久身份；业务事实摘要排除每次重签的传输timestamp，其他字段不可变；同一供应商消息多收件人必须明确target，歧义拒绝且不消费receipt。receipt无自动清理，与状态和聚合同事务。原生厂商接入仍单独研究；现有HMAC不宣称原生短信回调。新增common Skill绑定；29review/1in_progress/1ready/0Done，所有提交暂缓。

Revision122：T-23已完成30项真实数据库/HTTP/独立JVM回归及2项JDBC提交故障测试，均零skip；扩展测试首次夹具NotAMock错误已修复，失败记录保留。补齐账号命名空间生命周期：configKey和渠道创建后不可变，已选短信厂商不可替换；账号逻辑删除并永久保留唯一标识，禁止删除后重建同名账号混淆迟到回执。该变更在既有Notify/DDL写集内，随后验证真实Mapper与软删除。原生短信回执接入仍不宣称完成，29review/1in_progress/1ready/0Done，全部提交暂缓。

Revision123：全后端首次838项中1项静态基座断言失败（旧测试要求供应商消息号跨收件人唯一），110项环境测试默认跳过。实际33项MySQL/HTTP验收已证明共享消息号按target关联。先登记精确OssNotifyMigrationUnitTest写集并保存原文，再将过时断言更新为普通关联索引+持久receipt唯一约束；保留既有OSS与基础字段检查，不删测试放宽门禁。前端首次旧参数提示断言失败已按阿里名称/腾讯位置两种合同更新，9项测试/typecheck/lint通过。T-23仍in_progress，全部提交暂缓。

Revision124：带真实GlobalExceptionHandler的回调SQL失败已映射503并可重试，34/34隔离集成通过。复核新增target关联字段会经过HTTP日志，按通知规范的手机号/邮箱不得写日志硬约束，先登记common-json脱敏实现与测试两个精确写集；自定义回执正文/查询参数仅保留脱敏摘要，签名与业务原文不变。之前full/core和838默认测试通过为上一源码阶段；变更后重新运行受影响验证。全部提交暂缓。

Revision125：T-23阶段检查点已保存（整票仍in_progress/ready=false）。五种停用账号预设、凭据/模板校验、SMS消息ID、持久回执及账号命名空间、真实HTTP失败重试和隐私日志已实现。34/34隔离MySQL/Redis/HTTP/独立JVM及提交故障通过；最新默认后端840项=729通过+111环境skip，前端9项/typecheck/lint、full/core构建清单通过。33路径owned diff，9上游重叠/601非重叠不变，累计634；源码0eafcc88e72430f715389660b5bac34c504dca49d85dce9040ffe6b7b7967842。腾讯/阿里原生回执安全适配仍待内部闭合，不冒充厂商支持；T-30未做正式整体验收。29review/1in_progress/1ready/0Done，HEAD与空index保持，全部提交暂缓。

Revision126：冻结T-23原生短信状态核对合同：腾讯DescribeSendRecordList（72小时/最多1000条）、阿里QuerySendDetails（BizId+号码+发送日），使用相同账号密钥签名HTTPS查询，复用持久receipt事务。查询失败、截断或歧义不提升状态、不重新发送；查询记录与厂商推送事件区分命名空间，SMTP保持受理语义。实施及协议/真实数据库验证待完成，旧v5证据只对应已保存阶段。29review/1in_progress/1ready/0Done，全部提交暂缓。

Revision127：T-23完成本地review。五种禁用供应商预设、真实发送消息ID、持久回执/账号命名空间及腾讯TC3/阿里ACS3鉴权状态查询已闭合；原生推送不冒充支持。11项协议/HTTP测试、41项真实MySQL/Redis/重启/并发/提交故障零skip通过；默认后端857项=740通过+117环境skip，Notify前端9项/typecheck/lint、full/core同源构建及清单通过。47路径checkpoint、9上游重叠/601非重叠不变，累计648；源码88a5a9a25f9c3d88def978ac6aac64d60522638cfa43150e0cfb3f4924a9fee0。无真实账号试发或生产容量声明；30review/1ready/0Done，下一步T-30同一候选整体验收。HEAD/空index保持，全部提交暂缓。

Revision129：T-30真实依赖门禁发现T-23新增receipt表未同步受保护初始化器：实际126表/预期125，测试尚未开始即exit1，owned资源已恢复。回到T-23补修，先登记初始化脚本及两份发布合同测试精确写集；T-30暂停为ready，29review/1in_progress/1ready/0Done。保留旧源码88a5a9a的通过与失败证据；修正后重新冻结输入并运行受影响发布/真实服务门禁。全部提交继续暂缓。

Revision130：T-23初始化补修完成review：受保护六文件初始化实际126表，发布117项及真实MySQL/Redis/MinIO八项均零skip通过，资源恢复；仅初始化脚本和两处旧计数断言变化。T-23-checkpoint-v2共50路径，累计649路径；源码1dff1a345e1979d809bb547f3060645d86b4508f3df3aad5fd227de53ab2c504。T-30继续整体验收，30review/1in_progress/0Done；未受影响的前端/后端源树逐文件相同，旧验证证据按明确输入等价关系关联，受影响release/external已重跑。全部提交暂缓。

## Revision135 实际提交与父分支验收

用户已明确授权全部commit/push。implementation commits：`3892e0638f668417918971fc3d64a28da30b411d`, `d6aba2f3beaa503d4ff76adbf178857081d50b65`；完整实现链 result SHA：`6c8764cca97bb6057fcb90ccdfe635c7efbf502a`。每个提交均非空、实际父SHA已核对且被result包含；Git归档逐文件等于T-30已验证输入，未声称拆分过程中的中间树独立通过全部测试。精确路径/共享owner/验证见 `../evidence/commit-delivery.json`。本票保持review；正式发布候选与change最终Done独立验收。
