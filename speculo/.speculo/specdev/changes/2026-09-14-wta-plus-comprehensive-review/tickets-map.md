---
schema_version: 3
plan_contract_version: 1
plan_revision: 179
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

用户Goal执行中。当前12done/2cancelled/T41 in_progress/35ready，cors_audit唯一产品writer，Lead验收；Ticket41 revision174的23条路径为当前硬写集，新增DTO/session port及迁移测试均已登记。

分层保持Notify layered、System classic；公开API由wta-api，SQL仅六份基座，前端依赖方向不变。外部I/O与本地消息事务分开；外部UNKNOWN不盲重试；元数据只查DB，移除诊断门禁必须先保留本地权限/访问类型校验。用户此前“无兼容窗口”不取消外部协议、安全或数据保护。

### 项目 Skill 读取矩阵

先完整读本Map，再读匹配Skill入口与必要references，再读当前Ticket。最低集合不是allowlist；新增触发项由Lead先改绑定/Map后执行。真实入口及sha256在每票frontmatter，不以“读过”替代Skill实际步骤。

| Applies To | Project Skill | Trigger / Scope | Read Timing | Purpose |
|---|---|---|---|---|
| ALL | <Path>.agents/skills/engineering-standards/SKILL.md</Path> | 架构/API/数据库/权限/质量门禁及交付 | Map后、Ticket前，verify再次按scope | 硬约束与真实验证 |
| T-29 | <Path>.agents/skills/deploy-namewta-environment/SKILL.md</Path> | 私有发布目录备份、恢复校验与路径事实修正 | 实施与验证前 | 保留权限、秘密边界和可恢复备份 |
| T-35, T-36, T-37, T-38, T-41, T-42, T-50 , T-39 | <Path>.agents/skills/java-api-compatibility/SKILL.md</Path> | 按票路径和API/模块/公共能力实际触发 | Map后Ticket前；implement/verify按绑定 | 真实入口路由及消费者/验证边界 |
| T-06, T-07, T-08, T-11, T-12, T-13, T-14, T-15, T-16, T-17, T-18, T-19, T-22, T-23, T-24, T-25, T-26, T-27, T-28, T-29, T-30, T-31, T-34, T-35, T-36, T-37, T-38, T-39, T-40, T-41, T-42, T-43, T-44, T-45, T-46, T-47, T-49, T-50 | <Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path> | 按票路径和API/模块/公共能力实际触发 | Map后Ticket前；implement/verify按绑定 | 真实入口路由及消费者/验证边界 |
| T-02, T-03, T-04, T-05, T-11, T-14, T-23, T-24, T-26, T-28, T-31, T-33, T-35, T-36, T-37, T-42, T-45, T-46, T-49 , T-39 | <Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path> | 按票路径和API/模块/公共能力实际触发 | Map后Ticket前；implement/verify按绑定 | 真实入口路由及消费者/验证边界 |
| T-14, T-15, T-16, T-22, T-23, T-24, T-25, T-26, T-28, T-29, T-30, T-31, T-34, T-35, T-36, T-37, T-38, T-39, T-40, T-41, T-42, T-43, T-44, T-45, T-46, T-47, T-49, T-50 | <Path>.agents/skills/wta-module-guide/SKILL.md</Path> | 按票路径和API/模块/公共能力实际触发 | Map后Ticket前；implement/verify按绑定 | 真实入口路由及消费者/验证边界 |

## 2. 执行清单

50票中12done、2cancelled、T41 in_progress、35ready；AC-001/003仍由T30复验。

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
| T-40 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/40-retract-notice-and-read-snapshot.md</Path> | 撤回只停止该发布版本未开始发送的任务，保留已送达内容与审计；本人从收件箱读快照，无需公告管理权限。 | T-38, T-39 | deep | high | yes | single-agent | AC-040 | W-journey | ready |
| T-41 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/41-paged-personal-inbox.md</Path> | 完整收件箱使用项目 PageQuery/PageResult 分页，稳定 create_time/message_id 排序；本人第501条可取，顶部只取最近摘要。全部已读仍作用本人全部消息。 | T-34 | deep | high | yes | single-agent | AC-041 | W-journey | in_progress |
| T-42 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/42-mail-attachment-contract.md</Path> | 正文邮件无需伪造链接；专用 demo-mail 场景以主题/正文包装模板发送。显式附件字段经授权/冻结/持久化传递，零附件不访问 OSS。 | T-35, T-37, T-44 | deep | high | yes | single-agent | AC-042 | W-close | ready |
| T-43 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/43-measure-notify-fanout.md</Path> | 代表性规模有可重复 SQL/时延/锁等待基线；保持现有总量上限与持久聚合语义，优先减少插入往返，测量不足不引入新计数状态机。 | T-36, T-38, T-39 | standard | medium | yes | single-agent | AC-043 | W-close | ready |
| T-44 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/44-optional-oss-diagnostics.md</Path> | 业务仅校验当前对象/配置/权限/预期访问类型，远端操作按实际结果反馈；管理员诊断独立，坏的可选存储不阻断核心就绪。 | — | deep | high | yes | single-agent | AC-044 | W-oss | ready |
| T-45 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/45-bounded-oss-diagnostic-facts.md</Path> | 诊断仅报告观察事实与范围：读403为未知，单对象匿名读取只证明该对象，PRIVATE未知不能宣称全桶安全。 | T-44 | deep | high | yes | single-agent | AC-045 | W-oss | ready |
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
T-38 + T-39 → T-40
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
