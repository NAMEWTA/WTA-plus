---
schema_version: 3
plan_contract_version: 1
plan_revision: 207
requested_deliverables: [{"name": "完整Tickets Map", "count": 1}, {"name": "Goal Plan", "count": 1}]
deliverable_policy: "用户要求全面重规划；保留31历史票并新增19个行为切片，共50票不是用户指定数量。完整修订所有活动文档，旧证据原字节保留。"
artifact: "tickets-map"
change: "2026-09-14-wta-plus-comprehensive-review"
status: "ready"
---

# Tickets Map：既有基座复验与review闭环

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>；Ticket目录：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/</Path>；Evidence目录：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/</Path>。

## 1. 目标与拆分策略

原31票保留稳定ID及历史实现证据；新增T-32—T-50覆盖18项新报告事项。N-06按时效与未支持模式拆两票；N-08轻量消息盒子状态纳入T-34，完整分页由T-41。T-30重新承担全部AC的最终闭合。不是重做已提交旧实现，也不遗漏旧验收。

### 总体实施背景

revision207：T45首轮OSS定向164例/1failure/0error/0skip；11c6e9e已修正500断言与HTTP事实依据，尚未重测。登记同用户新会话隔离修复写集，15done/2cancelled/1in_progress/32ready，完整候选attempts0，Goal active。

分层保持Notify layered、System classic；公开API由wta-api，SQL仅六份基座，前端依赖方向不变。外部I/O与本地消息事务分开；外部UNKNOWN不盲重试；元数据只查DB，移除诊断门禁必须先保留本地权限/访问类型校验。用户此前“无兼容窗口”不取消外部协议、安全或数据保护。

### 项目 Skill 读取矩阵

先完整读本Map，再读匹配Skill入口与必要references，再读当前Ticket。最低集合不是allowlist；新增触发项由Lead先改绑定/Map后执行。真实入口及sha256在每票frontmatter，不以“读过”替代Skill实际步骤。

| Applies To | Project Skill | Trigger / Scope | Read Timing | Purpose |
|---|---|---|---|---|
| ALL | <Path>.agents/skills/engineering-standards/SKILL.md</Path> | 架构/API/数据库/权限/质量门禁及交付 | Map后、Ticket前，verify再次按scope | 硬约束与真实验证 |
| T-29 | <Path>.agents/skills/deploy-namewta-environment/SKILL.md</Path> | 私有发布目录备份、恢复校验与路径事实修正 | 实施与验证前 | 保留权限、秘密边界和可恢复备份 |
| T-35, T-36, T-37, T-38, T-41, T-42, T-45, T-50 , T-39 | <Path>.agents/skills/java-api-compatibility/SKILL.md</Path> | 按票路径和API/模块/公共能力实际触发 | Map后Ticket前；implement/verify按绑定 | 真实入口路由及消费者/验证边界 |
| T-06, T-07, T-08, T-11, T-12, T-13, T-14, T-15, T-16, T-17, T-18, T-19, T-22, T-23, T-24, T-25, T-26, T-27, T-28, T-29, T-30, T-31, T-34, T-35, T-36, T-37, T-38, T-39, T-40, T-41, T-42, T-43, T-44, T-45, T-46, T-47, T-49, T-50 | <Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path> | 按票路径和API/模块/公共能力实际触发 | Map后Ticket前；implement/verify按绑定 | 真实入口路由及消费者/验证边界 |
| T-02, T-03, T-04, T-05, T-11, T-14, T-23, T-24, T-26, T-28, T-31, T-33, T-35, T-36, T-37, T-42, T-45, T-46, T-49 , T-39, T-40, T-44 | <Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path> | 按票路径和API/模块/公共能力实际触发 | Map后Ticket前；implement/verify按绑定 | 真实入口路由及消费者/验证边界 |
| T-14, T-15, T-16, T-22, T-23, T-24, T-25, T-26, T-28, T-29, T-30, T-31, T-34, T-35, T-36, T-37, T-38, T-39, T-40, T-41, T-42, T-43, T-44, T-45, T-46, T-47, T-49, T-50 | <Path>.agents/skills/wta-module-guide/SKILL.md</Path> | 按票路径和API/模块/公共能力实际触发 | Map后Ticket前；implement/verify按绑定 | 真实入口路由及消费者/验证边界 |

## 2. 执行清单

50票中14done、2cancelled、T44 in_progress、33ready；AC-001/003仍由T30复验。

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | Contract IDs | Wave/Gate | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| T-01 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/01-restore-trustworthy-gates.md</Path> | 干净clone不创建temp/release也通过事实检查 | T-29 | standard | medium | no | single-agent | AC-001 | W-legacy / G-legacy | cancelled |
| T-02 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/02-unify-log-redaction.md</Path> | canary不出现在HTTP sink、OperLogEvent、数据库或错误日志 | — | deep | high | yes | single-agent | AC-002 | W-legacy / G-legacy | done |
| T-03 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/03-bound-request-capture.md</Path> | 大小边界前/等于/超限一字节结果可判定 | T-02 | deep | high | no | single-agent | AC-003 | W-legacy / G-legacy | cancelled |
| T-04 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/04-trusted-client-address.md</Path> | 任意外来XFF不改变直连或正常入口的授权结果 | — | deep | high | yes | single-agent | AC-004 | W-legacy / G-legacy | ready |
| T-05 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/05-repeat-submit-lease.md</Path> | A失败不得删除B的键 | — | deep | high | yes | single-agent | AC-005 | W-legacy / G-legacy | ready |
| T-06 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/06-secure-sso-session.md</Path> | 生产实现不含ThreadLocalRandom/雪花ID作为bearer | — | deep | high | yes | single-agent | AC-006 | W-legacy / G-legacy | ready |
| T-07 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/07-sso-callback-journey.md</Path> | 复杂state往返相等且无重复code/state参数 | T-06 | deep | high | yes | single-agent | AC-007 | W-legacy / G-legacy | ready |
| T-08 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/08-complete-sso-release.md</Path> | 每个shipped App均有且仅有完整配套，缺项在promotion前失败 | T-07, T-09, T-10 | deep | high | yes | single-agent | AC-008 | W-legacy / G-legacy | ready |
| T-09 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/09-coherent-build-matrix.md</Path> | build:dev最终三个App均development，build:prod均production | — | deep | high | yes | single-agent | AC-009 | W-legacy / G-legacy | ready |
| T-10 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/10-atomic-release-provenance.md</Path> | manifest每个artifact的digest和source可追溯 | T-09 | deep | high | yes | single-agent | AC-010 | W-legacy / G-legacy | ready |
| T-11 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/11-remove-browser-shared-private-key.md</Path> | 生产bundle不再携带该共享响应私钥或ECB路径 | — | deep | high | yes | single-agent | AC-011 | W-legacy / G-legacy | ready |
| T-12 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/12-session-navigation-lifecycle.md</Path> | logout超时/401/离线时本地token和动态路由仍清空 | T-07 | standard | high | yes | single-agent | AC-012 | W-legacy / G-legacy | ready |
| T-13 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/13-recoverable-registration.md</Path> | 服务端关闭注册时入口与页面均准确，后端仍拒绝直接调用 | T-12 | standard | medium | yes | single-agent | AC-013 | W-legacy / G-legacy | ready |
| T-14 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/14-profile-self-materials.md</Path> | 新个人CN_RESIDENT_ID上传正反面后完成提交 | T-18 | deep | high | yes | single-agent | AC-014 | W-legacy / G-legacy | ready |
| T-15 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/15-contract-profile-legacy-bridges.md</Path> | 旧入口引用为零且替代能力覆盖完整 | — | deep | high | yes | single-agent | AC-015 | W-legacy / G-legacy | ready |
| T-16 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/16-workflow-task-integrity.md</Path> | B失败绝不发出A的审批请求 | — | deep | high | yes | single-agent | AC-016 | W-legacy / G-legacy | ready |
| T-17 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/17-trusted-designer-messages.md</Path> | 错误origin、错误source、未知payload不能关闭标签 | — | standard | medium | yes | single-agent | AC-017 | W-legacy / G-legacy | ready |
| T-18 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/18-upload-ownership-lifecycle.md</Path> | 下载URL失败不生成无人回收的Blob URL，也不把已完成上传误报失败 | — | deep | high | yes | single-agent | AC-018 | W-legacy / G-legacy | ready |
| T-19 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/19-system-page-state-locality.md</Path> | 快速筛选时旧响应不能覆盖新列表，失败/loading可恢复 | T-18 | standard | medium | yes | single-agent | AC-019 | W-legacy / G-legacy | ready |
| T-20 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/20-strict-contract-target.md</Path> | 受影响边界类型检查通过，nullable与非法transport样本有明确处理 | T-12, T-14, T-19 | deep | medium | yes | single-agent | AC-020 | W-legacy / G-legacy | ready |
| T-21 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/21-accessible-public-apps.md</Path> | 只用键盘可完成公开流程，焦点可见且错误能被读屏发现 | T-13 | standard | medium | yes | single-agent | AC-021 | W-legacy / G-legacy | ready |
| T-22 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/22-atomic-notify-result.md</Path> | 任意一条SQL失败不会留下Delivery/Attempt/Outbox不一致 | — | deep | high | yes | single-agent | AC-022 | W-legacy / G-legacy | ready |
| T-23 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/23-durable-provider-callback.md</Path> | 回滚后相同事件可重试成功 | T-22 | deep | high | yes | single-agent | AC-023 | W-legacy / G-legacy | ready |
| T-24 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/24-recoverable-third-resilience.md</Path> | 崩溃后permit在规定上限内恢复，不依赖人工删key | — | deep | high | yes | single-agent | AC-024 | W-legacy / G-legacy | ready |
| T-25 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/25-acyclic-department-moves.md</Path> | 自父/后代父/并发互移均不能形成环 | — | deep | high | yes | single-agent | AC-025 | W-legacy / G-legacy | ready |
| T-26 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/26-cut-over-crud-contracts.md</Path> | 每个候选有迁移或保留理由，不遗漏调用者 | T-02, T-16, T-25 | deep | high | yes | single-agent | AC-026 | W-legacy / G-legacy | ready |
| T-27 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/27-honest-demo-tree-baseline.md</Path> | 保存/删除路径不再有虚假校验TODO | T-26 | standard | medium | yes | single-agent | AC-027 | W-legacy / G-legacy | ready |
| T-28 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/28-bounded-notify-wake.md</Path> | 慢provider不阻塞业务提交线程 | T-22 | deep | high | yes | single-agent | AC-028 | W-legacy / G-legacy | ready |
| T-29 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/29-converge-current-documentation.md</Path> | 每个删除文件有owner、内容迁移落点和无丢失硬约束证据 | — | deep | medium | yes | single-agent | AC-029 | W-legacy / G-legacy | done |
| T-30 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/30-integrated-upgrade-acceptance.md</Path> | 全部已接受AC均有实际命令/退出码/环境/源码checkpoint | T-02, T-04, T-05, T-06, T-07, T-08, T-09, T-10, T-11, T-12, T-13, T-14, T-15, T-16, T-17, T-18, T-19, T-20, T-21, T-22, T-23, T-24, T-25, T-26, T-27, T-28, T-29, T-31, T-32, T-33, T-34, T-35, T-36, T-37, T-38, T-39, T-40, T-41, T-42, T-43, T-44, T-45, T-46, T-47, T-48, T-49, T-50 | deep | high | yes | single-agent | AC-001, AC-002, AC-003, AC-004, AC-005, AC-006, AC-007, AC-008, AC-009, AC-010, AC-011, AC-012, AC-013, AC-014, AC-015, AC-016, AC-017, AC-018, AC-019, AC-020, AC-021, AC-022, AC-023, AC-024, AC-025, AC-026, AC-027, AC-028, AC-029, AC-030, AC-031, AC-032, AC-033, AC-034, AC-035, AC-036, AC-037, AC-038, AC-039, AC-040, AC-041, AC-042, AC-043, AC-044, AC-045, AC-046, AC-047, AC-048, AC-049, AC-050 | W-final | ready |
| T-31 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/31-enterprise-transfer-queued-contract.md</Path> | 真实Notify返回QUEUED时send不抛DELIVERY_FAILED，transfer与通知同事务提交 | T-22 | deep | critical | yes | single-agent | AC-031 | W-legacy / G-legacy | ready |
| T-32 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/32-remove-tracked-local-secrets.md</Path> | 新 clone 的当前检出与发布产物不含真实凭据；历史对象仍可能保留披露值，必须另外轮换。部署者从未跟踪的本地文件或环境变量注入。公开模板可启动到明确的缺配置错误，日志不回显 secret。 | — | deep | high | yes | single-agent | AC-032 | W-security | done |
| T-33 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/33-safe-cors-defaults.md</Path> | 默认同源访问正常；跨域仅接受显式受信 Origin。生产带凭证通配配置拒绝启动；本地开发复用 Vite 同源代理。 | — | deep | high | yes | single-agent | AC-033 | W-security | done |
| T-34 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/34-inbox-without-realtime.md</Path> | 有效会话始终可经 REST 读取；开关只控制实时连接。盒子展示单份摘要与明确加载/失败/空状态，切身份不串数据。 | — | standard | medium | yes | single-agent | AC-034 | W-visible | done |
| T-35 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/35-sms-cross-layer-snapshot.md</Path> | 合法短信由真实 runtime→dispatcher→适配器发送一次；逻辑快照可解释且不泄露验证码，本地可判定校验失败不等回执。 | — | deep | high | yes | single-agent | AC-035 | W-notify | done |
| T-36 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/36-atomic-in-app-delivery.md</Path> | 同 intent 的不同收件人并发投递均可读取且关系唯一；本地消息、关系与投递结果原子提交，实时事件仅提交后发送。 | — | deep | high | yes | single-agent | AC-036 | W-notify | done |
| T-37 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/37-retryable-provider-idempotency.md</Path> | Outbox 独占重试次数与节奏；明确未发送的可重试失败允许新一次物理发送，ACCEPTED/DELIVERED/UNKNOWN 保留防重。 | — | deep | high | yes | single-agent | AC-037 | W-notify | done |
| T-38 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/38-precise-notification-retry.md</Path> | URL 唯一定位 intent；指定 delivery 只重试其所属一项，无可重试任务返回真实状态及零计数；外部 UNKNOWN 明确拒绝自动重发。 | T-36, T-37 | deep | high | yes | single-agent | AC-038 | W-notify | done |
| T-39 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/39-enforce-notification-deadlines.md</Path> | 统一截止时刻驱动验证码 TTL 与 command；提交/重试拒绝到期，Worker 发请求前检查，过期任务结束且 Provider 调用为零。 | T-38 | deep | high | yes | single-agent | AC-039 | W-notify | done |
| T-40 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/40-retract-notice-and-read-snapshot.md</Path> | 撤回只停止该发布版本未开始发送的任务，保留已送达内容与审计；本人从收件箱读快照，无需公告管理权限。 | T-38, T-39, T-41 | deep | high | yes | single-agent | AC-040 | W-journey | done |
| T-41 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/41-paged-personal-inbox.md</Path> | 完整收件箱使用项目 PageQuery/PageResult 分页，稳定 create_time/message_id 排序；本人第501条可取，顶部只取最近摘要。全部已读仍作用本人全部消息。 | T-34 | deep | high | yes | single-agent | AC-041 | W-journey | done |
| T-42 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/42-mail-attachment-contract.md</Path> | 正文邮件无需伪造链接；专用 demo-mail 场景以主题/正文包装模板发送。显式附件字段经授权/冻结/持久化传递，零附件不访问 OSS。 | T-35, T-37, T-44 | deep | high | yes | single-agent | AC-042 | W-close | ready |
| T-43 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/43-measure-notify-fanout.md</Path> | 代表性规模有可重复 SQL/时延/锁等待基线；保持现有总量上限与持久聚合语义，优先减少插入往返，测量不足不引入新计数状态机。 | T-36, T-38, T-39 | standard | medium | yes | single-agent | AC-043 | W-close | ready |
| T-44 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/44-optional-oss-diagnostics.md</Path> | 业务仅校验当前对象/配置/权限/预期访问类型，远端操作按实际结果反馈；管理员诊断独立，坏的可选存储不阻断核心就绪。 | — | deep | high | yes | single-agent | AC-044 | W-oss | done |
| T-45 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/45-bounded-oss-diagnostic-facts.md</Path> | 诊断仅报告观察事实与范围：读403为未知，单对象匿名读取只证明该对象，PRIVATE未知不能宣称全桶安全。 | T-44 | deep | high | yes | single-agent | AC-045 | W-oss | in_progress |
| T-46 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/46-serialize-oss-restore-cleanup.md</Path> | 所有切指针/删来源入口共用对象→工单锁序。恢复先成功则清理不得删来源；清理已获得合法执行权则恢复明确拒绝。 | — | deep | high | yes | single-agent | AC-046 | W-oss | ready |
| T-47 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/47-latest-oss-query-wins.md</Path> | A慢B快最终行、total、preview、loading、error均属于B；卸载或旧失败不污染当前页面。 | — | standard | medium | yes | single-agent | AC-047 | W-visible | done |
| T-48 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/48-simple-dev-start-and-explicit-repair.md</Path> | 一个脚本提供显式start/build/doctor/repair子命令（菜单仅薄包装），普通再次启动不深度修复且尊重Spring/Vite环境优先级。 | T-32 | standard | medium | yes | single-agent | AC-048 | W-close | ready |
| T-49 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/49-single-source-storage-config.md</Path> | 默认存储来自DB，历史对象按service；静态上传策略有安全代码默认值，只有部署差异/必要额度可覆盖，SINGLE无需MULTIPART参数。 | T-44, T-48 | standard | medium | yes | single-agent | AC-049 | W-close | ready |
| T-50 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/50-reject-unimplemented-notify-modes.md</Path> | 统一路径只承诺ALL/ASYNC/default priority=0；非支持值在写意图前明确拒绝，所有仓内生产调用同批迁移，不增加高级编排引擎。 | T-39 | deep | high | yes | single-agent | AC-050 | W-notify | done |

## 3. 依赖 DAG

blocked_by只表达行为前提，不用人为链冒充串行理由。旧票原依赖保持，T-30依赖其他49票；其关闭程序在G-legacy中避免“为复核旧票重复实现”。新票不依赖T-30，故无循环。

```text
T-32 → T-48 ──────────────┐
T-44 → T-45              ├→ T-49
T-44 ─────────────┐       │
T-35 ─────────────┼→ T-42 │
T-37 ─────────────┘       │
T-36 + T-37 → T-38 → T-39 → T-50
T-38 + T-39 + T-41 → T-40
T-34 → T-41
T-36 + T-38 + T-39 → T-43
T-33 / T-46 / T-47 为独立行为根
全部旧合同关闭＋T-32—50 → T-30 → G-complete → 单独授权A归档
```

T-49同时依赖T-44与T-48。完整真实依赖从frontmatter重建，图是摘要。

## 4. 合同覆盖矩阵

| Contract ID | 覆盖 Ticket | 验证接缝 | 状态 | 说明 |
|---|---|---|---|---|
| AC-001 | T-01, T-30 | not-required: 仓库静态/脚本合同由正负夹具及本地workflow检查覆盖，无在线业务边界 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-002 | T-02, T-30 | required: 用唯一凭据canary调用签发/失败接口，检查HTTP sink、OperLogEvent与数据库均不含明文 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-003 | T-03, T-30 | required: 通过真实HTTP发送定长/chunked边界请求、伪签名大正文及正常签名正文 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-004 | T-04, T-30 | required: 隔离单/双Nginx链发IPv4/IPv6和伪造XFF请求，比较白名单、限流、审计来源 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-005 | T-05, T-30 | required: 真实Redis以屏障复现A过期/B接管/A失败/C被拒及正常失败重试 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-006 | T-06, T-30 | required: HTTPS隔离SSO验证Cookie、CORS、PKCE、过期与单次兑换；显式dev HTTP例外 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-007 | T-07, T-30 | required: 专用SSO Playwright跑/admin与/home base、复杂state、过期与重新登录 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-008 | T-08, T-30 | required: 本地三Origin HTTPS部署候选，登录、刷新、过期恢复与health可用 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-009 | T-09, T-30 | not-required: 构建产物扫描与脚本负向夹具覆盖；线上组合由T-08/T-30验收 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-010 | T-10, T-30 | required: 隔离文件系统完整stage、缺件/中断/混源注入、固定版本消费者与容器重建恢复 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-011 | T-11, T-30 | required: 两App登录注册/下载/错误回归及生产bundle密钥/ECB扫描 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-012 | T-12, T-30 | required: 离线/超时logout、并发401、空角色恢复和切Client重登 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-013 | T-13, T-30 | required: 禁用注册直接访问、验证码错误后刷新重试、慢旧响应及键盘提交 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-014 | T-14, T-30 | required: 新个人身份证双面、企业条件材料经真实MySQL/OSS提交，刷新/失败/越权覆盖 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-015 | T-15, T-30 | not-required: 方法级迁移表、全Profile/消费者编译和原行为测试覆盖，无独立新交互 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-016 | T-16, T-30 | required: 真引擎授权矩阵及快速A/B切换、B失败、确认期间切任务、重复办理 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-017 | T-17, T-30 | required: 真实iframe发合法close及外部window伪造消息，刷新卸载不残留 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-018 | T-18, T-30 | required: 真实上传后URL失败、导入网络/业务/401/取消、重试与资源回收 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-019 | T-19, T-30 | not-required: 受控Promise组件测试覆盖乱序和取消，既有App回归由T-30执行 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-020 | T-20, T-30 | not-required: 受影响包诊断、typecheck和边界单测直接覆盖；相关业务E2E属于原票 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-021 | T-21, T-30 | required: 320/768/1440、200%缩放、键盘/读屏语义和渲染对比检查 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-022 | T-22, T-30 | required: 真实MySQL/Redis双worker、过期/reclaim、Attempt失败与finish冲突注入 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-023 | T-23, T-30 | required: 两实例重启/并发重复/跨provider同eventId/提交失败/早到回调；真实签名适配 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-024 | T-24, T-30 | required: 隔离Redis kill进程、嵌套申请失败、降低并发在途收束、rate窗口更新 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-025 | T-25, T-30 | required: 真MySQL并发同树/跨根移动及注入后代更新失败，树不变量成立 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-026 | T-26, T-30 | required: 各受影响资源代表读/写/批量删除与越权请求，旧CRUD方法拒绝、生成合同一致 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-027 | T-27, T-30 | required: 真实树新建、移动、越权/非法父/后代父、带子节点删除和合法叶删除 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-028 | T-28, T-30 | required: 慢provider下提交耗时、Redis失败/丢wake后poll及双worker fence | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-029 | T-29, T-30 | not-required: 逐文件hash/规则去向/路径引用和事实检查直接覆盖文档交付 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-030 | T-30, T-30 | required: 同一候选完整运行SSO/Profile/workflow/Notify/Third/树/三App发布及失败恢复 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-031 | T-31, T-30 | required: 真Profile→Notify QUEUED、worker受理→确认及DB/Redis部分失败、重复确认/错用户/绑定变更 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-032 | T-32, T-30 | Git 跟踪清单、合成配置加载、日志脱敏；实际轮换是 G-security-external 的外部动作 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-033 | T-33, T-30 | 真实 Servlet/CorsFilter 测试与 Spring profile 绑定 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-034 | T-34, T-30 | 现有 push.test.ts、notice 组件、真实 Admin 登录→打开盒子 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-035 | T-35, T-30 | DispatchNotificationServiceTest、NotifyDispatcherUnitTest、CaptchaNotifyCallerUnitTest 的真实跨层组合 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-036 | T-36, T-30 | 扩展 NotifyAtomicResultIntegrationTest；真实 DSTransactional 代理、MySQL 双连接、提交故障与 AFTER_COMMIT | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-037 | T-37, T-30 | NotifyIdempotencyDispatcherUnitTest、RedisNotifyIdempotencyStoreIntegrationTest、真实 runtime/Dispatcher 组合 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-038 | T-38, T-30 | HTTP ID 冲突、真实 DB requeue 并发、API/领域合同测试 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-039 | T-39, T-30 | 可控时钟 runtime/worker、CaptchaNotifyCallerUnitTest、真实 DB 过期任务终结 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-040 | T-40, T-30 | 真实发布→撤回→Worker→inbox；普通用户浏览器详情与越权 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-041 | T-41, T-30 | 真实 MySQL分页＋HTTP 登录身份过滤＋前端分页组件/浏览器 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-042 | T-42, T-30 | DemoNotifyCallerUnitTest、NotifyAttachmentDispatcherUnitTest、真实 owner/快照引用＋假邮件物理适配器 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-043 | T-43, T-30 | 真实 MySQL代表规模、SQL计数、现有原子结果/fence回归 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-044 | T-44, T-30 | OssStorageReadiness*、OssLifecycle*、OssUpload*；核心启动＋最小权限MinIO与health组 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-045 | T-45, T-30 | OssAccessDiagnosticUnitTest、受限MinIO读权限与诊断展示 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-046 | T-46, T-30 | OssStorageMigrationIntegrationTest：真实MySQL两连接＋阻塞替身＋MinIO对象存在验收 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-047 | T-47, T-30 | OssPage受控Promise组件测试，已有presentation.test.ts回归 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-048 | T-48, T-30 | shell fake命令夹具＋真实Linux初次/二次启动；Windows由支持环境实际验收 | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-049 | T-49, T-30 | OssUploadPropertiesUnitTest、配置绑定测试、默认存储切换真实MinIO | covered | 规划覆盖；当前not-run，不代表通过 |
| AC-050 | T-50, T-30 | 公共入口负向合同、每类生产调用方、历史WAIT样本终结 | covered | 规划覆盖；当前not-run，不代表通过 |

## 5. 并行与路径所有权

执行已授权的gpt-6-sol/xhigh原生子代理、current/direct-parent单产品writer，无新worktree；Lead独占SpecDev/Evidence/父分支与真实验收，逐票委派一名产品writer。所有票共享workspace:current-exclusive；虽然图有独立根，Wave不授权并行。

| 共享资源 | 涉及票 | 串行处理 |
|---|---|---|
| runtime/result/DAO/Outbox、Notify API | T-35—40/42/43/50及旧22/23/28/31 | 按Goal队列独占；每次先回读上一commit与新合同 |
| inbox/notice UI与transport/OpenAPI | T-34/38/40/41/50 | 先轻量可见性，分页后再详情组合；生成物由当前合同票正式工具更新 |
| OSS生命周期/路由/迁移/配置 | T-44/45/46/42/49，旧14/18 | T-44先解耦；T-46共锁；附件与配置消费已集成接口 |
| 公共YAML/local示例/脚本 | T-32/33/44/48/49 | 同一Lead逐票写，保留前票安全默认；不得互相覆盖 |
| 六SQL基座及发布合同 | T-42/旧9/14/23/30 | T-42本次结构owner；其他修改先变更owner；T-30只验收及已登记测试修复 |
| SSO/认证/跨change共用配置 | 本change与OIDC change | 本轮相邻只读；执行时检查owner/未闭合动作，重叠暂停，不抢占 |

## 6. Gate、Wave 与集成点

正式编排归<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>；推荐串行队列：T-32 → T-33 → T-34 → T-47 → T-29 → T-01 → T-35 → T-36 → T-37 → T-38 → T-39 → T-50 → T-41 → T-40 → T-44 → T-45 → T-46 → T-42 → T-43 → T-48 → T-49 → T-30。历史票根据受影响闭包在相关新票前后复验，先关闭可证明的独立项，最终T-30汇合。

## 7. 横切契约与风险

凭据轮换/真实数据修复/部署与归档是独立动作Gate；未授权不执行。旧共同result不是当前通过；不因本轮只改文档而更改旧Evidence。当前附件生产实现缺失，OSS硬规则替代待审；两个条件须关闭才可Ready。

## 8. 同步规则

Ticket frontmatter状态/依赖/路径有变即更新Map/plan-data投影，plan_revision递增。摘要漂移先读真实入口diff，不覆盖历史Skill执行记录。范围变化回到ADR/Spec；不在Goal暗改行为。所有source/evidence记录精确HEAD/命令/退出码/测试数/skip及未验证。

## 9. 总控与恢复

从<Path>{roots.workflows}/specdev/P-goal-plan/P-goal-plan.md</Path>进入plan/run/resume/replan/verify。只读控制：

```bash
node <Path>{roots.workflows}/specdev/common/tools/ticket-control.mjs</Path> --map <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path> --repo .
```

当前控制器应返回空frontier，因为Spec未Ready且Goal未授权；这不是计划结构失败。先恢复<Path>{roots.workflows}/specdev/G-grill-with-docs/G-grill-with-docs.md</Path>回答D节点，再按S→T→P发布正式Ready。缺执行授权时P仍保持ready_for_execution=false。

取消票不满足依赖：必须按Goal“历史票关闭后的依赖重算”记录合同事实、更新下游边与责任票后重算；当前50票均Ready，无在途writer。

Revision138执行：T-32先行；额外写集为backend/pom.xml资源排除、scripts/start-dev.sh显式本地导入和配置绑定测试。后续T-48继续拥有启动职责重构，本票只完成安全消费。用户已激活Goal，run可用。

## revision140派单

T-33唯一产品writer为原生子代理cors_audit（gpt-6-sol/xhigh），Lead仍为票/父分支/E2E owner；原ticket写集在此次租约内授予该子代理。其他agent只读。固定base `dd8179e1cd394b092bfb36b8b91c0e04386563ce`，完整Packet见 evidence/dispatch-T-33.md。

T-33审查写集补充：backend/wta-modules/wta-sso/AGENTS.md由Lead独占修正其过时CORS说明，不改变已确认来源合同。

## revision142 — T-34派单

T-34唯一产品writer为cors_audit（gpt-6-sol/xhigh），Lead保持状态/提交/E2E owner；Ticket写集在该轮租约内授予子代理。base `6fcbfeb50747b67bcaf4bf9af1d964cc7d760c04`；完整Packet见 evidence/dispatch-T-34.md。2done、1in_progress、47ready。

T-34前置类型修复已声明OssPage.vue精确写集：只补两表列OssVO泛型，实际列表竞态由T-47继续负责。另，附加skill-facts检查发现部署Skill固定temp/relase与工程事实检查禁止该目录冲突；现场维护依据该Skill生成了私有报告，当前报告须保全。该仓库治理矛盾登记T-29修复，不把本次附加检查写成passed；T-34不改变该检查器或删除私密恢复数据。

T34独立规范轴发现打开盒子与初始查询重叠时未保证补查。已在修改前追加Navbar.vue精确写集；仅打开动作fresh=true，登录被动查询仍合并。前两轮证据保留，下一轮补对应UI/Promise回归。

T34已按I流程在第3轮停止并完成Lead复盘；旧3次不删除，新Packet见evidence/dispatch-T-34-recovery.md。Lead唯一writer，真实E2E独占测试资源，不与编译/类型门禁并行。未改变超时或AC，全部票目标保持。

T34恢复候选e78d886真实场景1/1且clean，但双轴发现新驱动失败路径资源清理/选集来源问题，未验收Done。新Packet T-34-20260923-03由cors_audit唯一writer修.py及无Docker回归；产品消息行为/超时不变，Lead独占后续E2E。

## revision143当前

T32/T33/T34 done，47ready，无在途writer；T34result 177eb5bd889afd2ab54f4a8e162dc8358d80f140，真实及前端门禁通过。下一T47，T29治理矛盾保留。

## revision144当前

T32/T33/T34 done，T47 in_progress，46ready。T47 base 97e1ee9e1ad40de75deffd025379a6a5488c4882；cors_audit仅写OSS目录，Lead独占治理/提交/验收。

T47首候选2d60e1a全前端728/静态/构建及clean通过，但规范轴发现可选预览阻塞列表。Packet02保持写集，列表结果先提交、预览同代渐进，不改授权/旧响应边界；仍in_progress。

## revision145当前

T32/T33/T34/T47 done，46ready，无在途writer；T47result 7cd6fb22b28b7d464b9648ba77f2a2308e5b2016。下一先重排T29→T01→T35，闭合facts路径冲突后进入Notify。

## revision146当前

当前部署Skill要求旧私有目录但facts拒绝该目录，T01当前复验须等T29修复；历史T01实现早于T29且仍在父链，旧施工边已履行。本轮T29无需等重复验收，改为T29→T01，不取消合同或伪造Done。 队列T29→T01→T35；4done/1in_progress/45ready。T29 base 7a1810288d3292ceeb4987f488b13353af1a1286；12条精确新增写集与私有备份/恢复/无覆盖迁移边界见Ticket及Packet。历史证据不改写。

## revision147当前

5done/45ready；T29 result 9f055ba15d9d5a828fb08cdfb0b24efa32642889。当前facts及文档审计通过，私有资料已完整备份/恢复演练后迁移，维护脚本和唯一部署报告路径一致且0600。下一T01完整可信门禁复验，不以T29绿色直接取消旧票；随后T35。

## revision148当前

T01完整当前复验启动，无产品写者；5done/1in_progress/44ready。只在全部合同证明且无需新增实现后，才裁决取消历史重复施工并保持AC001的T30最终责任。

## revision149当前

当前源码已满足AC-001且无需新增产品实现；历史非空3aa047b及其父链保留，T29已完成当前路径冲突修复。按Goal历史票无改动出口，取消的是本轮重复施工，不取消AC-001，不把缺失历史clean单独作为理由，不生成空commit或新result。AC-001最终组合复验继续由T30承担。 当前5done/1cancelled/44ready，无产品writer；下一T35。T09与T30重复施工边已逐项移除，替代当前Evidence <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-01-replan-2026-09-23.md</Path>；T09自身合同及T30全部AC最终复验仍保留。

## revision150当前

T35短信真实跨层修复启动，base 8b758ea8074a63835659b9731e3a4c74c5c029c5，cors_audit单产品writer；新增3路径先登记于Ticket，Lead隔离环境与治理独占。5done/1cancelled/1in_progress/43ready。

## revision151当前

T35新增5个安全实现/测试/语义文档路径及java-api-compatibility绑定已在编辑前登记；修复REDACT事件原文与Captcha/通知普通日志泄露接缝，不改供应商原始参数、HTTP传输、FULL或幂等摘要。

## revision152当前

T35新增6个精确写集先于实施登记；REDACT_SENSITIVE 通知的供应商消息标识仅保留内部持久化用于回执关联；query、重复提交 receipt 和 monitor 公开投影隐藏该值，FULL 原行为保持。监控查询按本次有界结果批量读取 Intent 审计策略，不引入逐行查询；空ID集合不扫描全表。新增 NotifyAuditSupport 可统一策略与公开投影判断，NotificationReceipt 仅补公开字段的安全语义说明，不改签名。测试覆盖真实供应商返回手机号/验证码作为ID、内部值保留与公开值隐藏、FULL、重复提交及回执关联。

## revision155 IN_APP有界尝试预算

代码事实确认：原claim不计次数，结果事务回滚也回滚attempt_count，不能宣称现有重领机制有限。采用原有Outbox字段、既有代理端口和统一锁序：确定参数预检后，beginInAppAttempt独立短DSTransactional按Intent→Outbox→Delivery锁及数据库fence消耗一次预算；成功返回才进入消息/关系/结果的原子事务。IN_APP outbox.attempt_count表示已开始尝试（含随后回滚/崩溃），Delivery与Attempt只记录原子提交的结果；其他渠道保持原含义。消息事务不再次消耗预算。

预算耗尽则在锁内将IN_APP Delivery FAILED、Outbox DEAD_LETTER并刷新聚合，零persist；预算事务失败或提交结果不确定时停止，不能猜测已获得预算。消息事务提交ACK丢失时依已持久DONE/DELIVERED抑制重投，DB暂不可读则等待安全恢复。完全不可写期间不能保证提交终态，但不得在未取得持久预算时调用persist；恢复后仍在预算上限内收束。允许SQL失败后保留独立预算，不允许消息/关系/投递结果部分提交。

验证补充：预算先提交后消息回滚、max边界/耗尽零persist、预算提交ACK丢失及失效lease、同lease重入不能越过最大物理次数。既有写集覆盖port/usecase/runtime/DAO/Mapper/XML；不改worker/claim，不新增表/状态机，不转移给T38，外部渠道未知合同不变。

## revision156 预算字段语义文档写集

编辑前增加两条精确路径：NotifyOutbox.java字段Javadoc，以及唯一六SQL中的10-cde-base-ddl.sql，仅notify_outbox.attempt_count/last_error_code中文注释。已有DDL“领取次数”不符合旧结果计数也不符合新预算语义，须同步为IN_APP已开始尝试预算、外部渠道已提交结果次数，固定IN_APP_ATTEMPT_RESERVED内部标记。无列/类型/索引/结构变化、不重放存量基座；全新隔离六SQL装载复核仍必需。预算上限指一次自动调度周期；合法人工重试新周期由T38精确API合同负责。


### revision158 T37预登记

## revision158 当前实施与合同细化

base `38032d24335c52cafea855b19d51fb36295162ef`；cors_audit唯一产品writer，Lead独占治理、commit、服务与E2E。自动Outbox/common覆盖本票；人工retry的UNKNOWN拒绝与精确ID归T38，最终全部入口由T30汇合，不用Redis TTL冒充持久exactly-once。

结果采用机器可判定的未发送可重试、未发送终结、已接受、结果未知事实；旧FAILED/PROVIDER_REJECTED和任何未明确分类异常均无重发权。仅全部目标明确未发送且可重试，当前owner才能CAS为RETRYABLE；保留digest/请求身份及剩余TTL，同requestId每次独立nonce防ABA。新claim仅同digest CAS可重取；旧owner不能complete/release/转态新claim；转态失败/ACK未知失败关闭，不删键或全清缓存。已接受/UNKNOWN保留防重，混合结果不得整批释放。外部SMS与MAIL调用异常均保守UNKNOWN；T35 ACQUIRE前零发送准备路径保持。

生产来源候选为受控单次请求的腾讯单号码30秒限频结构化拒绝；严格校验供应商、响应类型、单匹配号码、固定Code、RequestId、无Error/SerialNo及Fee=0，其他类别不放入allowlist。具体SDK单次请求来源须由源码/API事实证明；不能证明的blend保持未知，不新增旁路安全注册平台。Sms4jBlendRegistry明确关闭SDK内部无差别重试。供应商实际联调不在本票，官方响应语义支持的生产解析器与合成响应跨层验收须分开说明。MAIL附件错误未有暂态类型证明前不整类标为可重试；附件完整合同仍归T42。

新增写集在编辑前登记：common幂等异常阶段、SMS notify目录/本地测试、MAIL Adapter和SMS4J Registry；公共模型/store/receipt签名与序列化消费者按JavaAPI技能同步，仓内直接切换，不恢复兼容桥。notification.md在本票写集，必须同步保守重试与旧缓存处置事实；永久ADR不改。

先可观察失败测试再最小实现；真实Redis检验owner CAS/20并发/digest/TTL/旧状态/损坏，真实MySQL+Redis+Dispatcher+Adapter+Outbox验证拒绝后两次物理调用、真实重新claim与到期退避、接受与未知零重发、COMPLETE/转态故障、租约fence。无skip冒充验收；T35/T36回归按受影响输入执行。禁止全缓存删除、改生产凭据或实际厂商发送。

## revision159 单次SDK请求的实例事实闭环

编辑前新增精确写集：<Path>backend/wta-common/wta-common-sms/src/main/java/org/namewta/common/sms/config/SmsAutoConfiguration.java</Path>。在现有Sms4jBlendRegistry维护自身以maxRetries=0创建的实际代理实例identity；使用已验证的BaseProviderFactory.createSms→SmsProxyFactory.getProxySmsBlend→SmsFactory.register同一引用，保留原SDK初始化所需钩子，不将void create后再get的可覆盖对象盲认证。remove先撤认证；注册/更新失败不保留认证，按账号并发更新需一致，不暴露配置对象。common-sms notify目录内小型SmsSingleAttemptBlendVerifier SPI由Registry实现，Resolver经AutoConfiguration ObjectProvider注入，缺失/identity不匹配即不把拒绝标成可重试；严格腾讯结构allowlist只有该证明成立才启用。已有Registry拥有这一事实，不新增第二套全局注册平台，不反射SDK，不让common反向依赖业务。固定对象捕获到send，避免查验A发送B。公共SPI/构造/配置方法调用者与测试同步。

### revision161 T02当前修复

## revision161 当前实际缺口与最小修复

base `c16966167526f9b6ab6eb213265034b3bbe53e46`。T37已在3a87bf7通过187+13+67零skip，current单产品writer转交cors_audit实施T02；Lead独占治理、提交、隔离真实验收。

当前真实链：UserLoginSuccessListener把已签发tokenValue放入UserOnlineDTO.tokenId，前端在线设备操作把它放入 /monitor/online/{tokenId} 与 /monitor/online/myself/{tokenId}；SysLogFilter原始path和GlobalExceptionHandler原始URI会复制凭据。LogAspect目前使用匹配路由模板，未证实OperLogEvent/DB已有该泄漏，不扩大断言。历史T02不是无需施工票，保留旧实现/证据，新增真实修复提交及当前验收；历史canonical先按原字节保存在evidence/T-02-history-before-2026-09-23.md，原引用日志不变；验收完成后当前canonical使用验证器要求的evidence/T-02.md，replan入口指向它，不补造旧时点。

复用已有LogSanitizer提供共享安全路径策略，HTTP sink和异常日志使用；必要时操作日志统一策略但保留路由模板。精确处理上述正常凭据路径及实际context/path语义，保留 /monitor/online/list、普通路由、操作者、耗时、失败状态。不得以改前端/HTTP接口/会话存储规避日志缺陷，不发动对任意用户可控元数据的无限脱敏；X-Request-Id格式加固仅建议，不是本票新增blocker。

先取得真实可观察日志红灯，再最小实现。当前候选验收需真实签发或既有真实token生产链的canary通过正常在线操作URL进入HTTP，检查HTTP sink、异常日志、OperLogEvent和真实MySQL行零凭据且审计非空，正常响应仍保留可用token。隔离测试若采用替身必须明确边界，不把任意塞入metadata的字符串冒称生产凭据链；原有签名/加解密/SSE/正文边界消费者回归。仅生产代码白名单路径的最小修复，测试均在现有admin测试根及common根，无需改业务API。

允许测试读取合成MySQL密码的子进程环境，避免JVM系统属性/日志泄漏；环境root/app凭据不进入argv/XML/Evidence。真实服务与Maven由Lead协调，不并行构建。不得删除历史日志/业务表或轮换凭据；G-security-external已独立关闭，用户AI/qcloud撤销确认不重新索要。治理原始日志按字节保留含Maven尾空格，产品与治理源文件的diff-check独立通过，不修改原日志制造全量空格绿灯。

## revision162 当前验收

Revision162: T02 accepted at617a369; current real39 zeroSkip, consumers70+HTTPS1/Chrome6 zeroSkip, default1000 includes150 environment skips explicitly excluded; full/core packages verified. Initial consumer skip and pnpm environment failure retained.9done/1cancelled/40ready; T03 current no-new-work adjudication then T38. Goal active; no archive.

## revision163 当前处置

Revision163: T03 cancelled as no new product work after current617a369 real bounded HTTP/signature/heap and consumers/full-core proof. Historical214de538 remains; AC003 retained by T30 and only redundant implementation edge removed.9done/2cancelled/39ready; nextT38; Goal active.

## revision164 T38写集与派单

Revision164: T38 started fromb47ff8b after acceptedT02 and no-new-workT03 closure.9done/2cancelled/1in_progress/38ready. cors_audit sole product writer, Lead governance/commit/isolated acceptance; exact ID and safe actual requeue contracts; no new schema or Client/owner model.

补充硬写集（其余以Ticket frontmatter为准）：
- <Path>backend/wta-api/src/main/java/org/namewta/notify/api/NotificationApplicationService.java</Path>
- <Path>frontend/packages/web-domains/notify/src/index.ts</Path>
- <Path>frontend/packages/web-domains/notify/src/index.test.ts</Path>
- <Path>frontend/packages/web-domains/notify/src/NotificationPage.test.ts</Path>
- <Path>backend/wta-modules/wta-notify/src/test/java/org/namewta/notify/adapter/event/NotifyOutboxWakePublisherTest.java</Path>
- <Path>.agents/skills/engineering-standards/references/notification.md</Path>

## revision165 T38字典写集

revision165：NotifyDelivery已有真实PENDING状态，但notify_delivery_status基座字典缺该值，重试后监控会显示未知。提前扩50-cde-base-dml.sql写集，只补唯一PENDING=待投递字典项，保留同六SQL基座与其他初始化；前端Delivery类型/字典渲染同步，不新增后端状态。不得执行生产DML或重放基座；当前新隔离库验收，存量Tag差异由T30持有。

## revision166 当前验收

Revision166: T38 accepted at a3b289e, backend sourceA7a6f75 explicit; final real14 and frontend736 zeroSkip, defaultA854 executed/163 environment skips, fullA/coreB and complete frontend gates passed. Live Redis config failure preserved; correct raw HTTP provenance and clean owned cleanup.10done/2cancelled/38ready; nextT39; Goal active, no archive.

## revision167 T39写集与派单

Revision167: T39 in_progress from bec94ae after T38 closure; 10done/2cancelled/1in_progress/37ready. cors_audit sole product writer; Lead integration/governance. Absolute Captcha/Redis deadline, safe expiry and reclaimed-provider uncertainty; no production data repair.

新增硬写集（其他以Ticket为准）：
- <Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/port/NotifyDispatchResultPort.java</Path>
- <Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/domain/entity/NotifyOutbox.java</Path>
- <Path>backend/wta-common/wta-common-redis/src/main/java/org/namewta/common/redis/utils/RedisUtils.java</Path>
- <Path>backend/wta-common/wta-common-redis/src/test/java/org/namewta/common/redis/utils/RedisUtilsDeadlineIntegrationTest.java</Path>
- <Path>.agents/skills/engineering-standards/references/notification.md</Path>
- <Path>.agents/skills/wta-common-modules-guide/references/other-utils.md</Path>

## revision168 当前验收

Revision168: T39 accepted at6e1d7f8/tree602161; C1 real117/3failure retained, C2 real118 zeroSkip; default861 executed/181 environment skips, full/core and applicable static gates passed; dual reviews and exact owned cleanup complete.11done/2cancelled/37ready; nextT50. Goal active, no production deployment/repair/archive.

## revision169 T50启动

Revision169: T50 active fromd544f02 after T39 closure;11done/2cancelled/T50in_progress/36ready. ALL+ASYNC+0 early rejection, all12 production callers, fenced historical unsupported disposition and real OpenAPI regeneration. cors_audit sole writer; Lead build/services/commit/governance. Goal active, no production operations.

## revision170 T50 Profile测试路径

- <Path>backend/wta-modules/wta-profile/wta-profile-person/src/test/java/org/namewta/profile/person/service/impl/PersonRebindNotificationTest.java</Path> => cors_audit，仅当前T50消费者断言。
- <Path>backend/wta-modules/wta-profile/wta-profile-enterprise/src/test/java/org/namewta/profile/enterprise/service/impl/EnterpriseTransferServiceTest.java</Path> => cors_audit，仅当前T50消费者断言。

## revision171 T50恢复

Revision171: T50 prior batch3 attempts retained; B real135 assertions passed but exact-clean gate failed during overlapping Vite build. Lead review and new Dispatch02 saved before reset0; serial-only recovery, no relaxed checks. API README provenance correction is within existing directory scope. 11done/2cancelled/T50in_progress/36ready; Goal active.

## revision172 当前验收

Revision172: T50 accepted at84ce0a9/treec118348; prior batch3 retained and Lead-reviewed recovery batch1 real135 zeroSkip/source-clean/owned cleanup passed. A3 default870 executed/197skip/full/live and B frontend736/core reused only for identical inputs; C static+OpenAPI check passed.37 paths within17 scope entries;12done/2cancelled/36ready. NextT41; Goal active, no production queue action/deployment/archive.

## revision173 T41启动

Revision173: T41 active from d7d534cb after T50 closure;12done/2cancelled/T41in_progress/35ready. Bounded personal JOIN pagination/detail, global unread/read-all, session fencing and real 501 evidence. cors_audit sole writer; Lead serial build/services/commit/governance. Goal active, no production operations.

## revision174 — 读投影路径纠正

Revision174: before production edits, correct T41 Mapper read projection to domain/model/read/NotifyInboxRow.java per FILES-002/005; no dto exception. 23 declared entries, same red-only phase;12done/2cancelled/T41in_progress/35ready.

## revision175 — T41后端检查点，前端在实施

Revision175: T41 backend checkpoints A1 b08105f failed (fixture MPJ result mapping), A2 662b351 passed3, A3 9d748aa passed4 after atomic first-time preservation. Full JAR/live OpenAPI fetched and generated in42f07eb. Frontend sole writer cors_audit active;12done/2cancelled/T41in_progress/35ready. This is partial implementation feedback, not complete-ticket integration; default full suite/frontend/browser still pending.

## revision176 — T41测试夹具修复

Revision176: complete T41 candidate B 05d0f34 failed integration attempt1 at frontend tests: missing Notice SFC SSR context and existing manifest registry test reaches browser Router through new user Store import. Architecture/lint/typecheck passed; later stages not run. Preserve raw failed evidence; add exact registry test path before fixture repair.24 declared entries;12done/2cancelled/T41in_progress/35ready. No product assertion weakened; Goal active.

## revision177 — T41停止自动重派

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
