---
schema_version: 3
artifact: tickets-map
change: 2026-08-21-oss-direct-unified-notification
status: ready
---

# Tickets Map: OSS 浏览器直传与统一对外通知

- **Map：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/tickets-map.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/spec.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/</Path>`
- **下一步 Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/goal-plan.md</Path>`

## 1. 目标与拆分策略

T-01 至 T-11 交付原始合同；CR-001 后追加 T-12 至 T-18，修复权限、敏感审计、外部副作用一致性、上传续传、业务引用/下载和运维可观测性。旧 OSS HTTP 采用一次性 contract，不保留生产兼容入口。

AR-001/ADR-010 共识后追加 T-19 至 T-22：先 expand 机械引用集合协调契约，再由 system 与 workflow 两个业务 module 分别完成 Owner 迁移，最后以显式 Owner manifest、聚焦架构/合同测试和公共 API contract 建立 future ratchet。当前是 fresh baseline，不增加历史回填、旧数据迁移或生产兼容窗口。

Client 只参与认证授权求值；Ticket 中出现的 `client_pk` 只记录请求来自哪个 `sys_client.id`，不得成为 OSS/通知租户、所有权、Provider 路由、幂等作用域或隐式 SQL 过滤。`sys_oss_ref.ref_type/ref_id` 只保存真实物理表名和真实主键，便于反向定位与生命周期保护，不动态查表、不推导 ACL。

## 2. 执行清单

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | Contract IDs | Wave/Gate | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| T-01 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/01-oss-s3-direct-transfer-foundation.md</Path>` | S3 兼容 Multipart/HEAD/Copy 公共合同 | — | deep | high | yes | cursor-agent | AC-002/005/006/007/023/024/032 | Wave 1 | done |
| T-02 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/02-shared-persistence-migration.md</Path>` | OSS/Notify schema、回填、权限菜单 | — | deep | high | yes | cursor-agent | AC-005/008/009/010/011/025/029/032 | Wave 1 | done |
| T-03 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/03-oss-lifecycle-reference-download.md</Path>` | TEMP/ref/删除保护/授权下载 | T-01,T-02 | deep | high | yes | cursor-agent | AC-007..013/026/029/030 | Wave 2 | done |
| T-04 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/04-oss-upload-control-plane.md</Path>` | SINGLE/Multipart 上传控制面 | T-03 | deep | high | yes | cursor-agent | AC-001/002/004/005/006/007/032 | Wave 3 | done |
| T-05 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/05-oss-protocol-cutover.md</Path>` | 浏览器直传直下和旧协议收缩 | T-04 | deep | high | yes | cursor-agent | AC-001/003/004/012/013/014 | Wave 4 | done |
| T-06 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/06-common-notify-core-adapters.md</Path>` | NotifyClient 与 Mail/SMS Adapter | — | deep | high | yes | cursor-agent | AC-015/016/017/031 | Wave 1 | done |
| T-07 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/07-notify-redis-idempotency.md</Path>` | 五分钟可选 Redis 幂等 | T-06 | standard | high | yes | cursor-agent | AC-018..022 | Wave 2 | done |
| T-08 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/08-notify-attachment-snapshots.md</Path>` | Provider 前独立附件快照 | T-03,T-07 | deep | high | yes | cursor-agent | AC-023/024/025/026 | Wave 3 | done |
| T-09 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/09-notify-monitoring-backend.md</Path>` | Event 两层监控与全局管理 API | T-02,T-03,T-08 | deep | critical | yes | cursor-agent | AC-020/025..031 | Wave 4 | done |
| T-10 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/10-notify-caller-migration.md</Path>` | Captcha/Workflow/Demo 调用收口 | T-09 | standard | medium | yes | cursor-agent | AC-015/016/017/023/024/025/031 | Wave 5 | done |
| T-11 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/11-notify-monitoring-frontend.md</Path>` | 通知列表/详情/删除运维页面 | T-09 | deep | high | yes | cursor-agent | AC-027/028/029/030 | Wave 5 | done |
| T-12 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/12-review-attachment-authorization.md</Path>` | 通知附件入口授权 | — | standard | high | yes | codex | AC-013/023/024 | Remediation | done |
| T-13 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/13-review-notify-audit-cleanup.md</Path>` | 敏感审计、重复日志、物理清理 | T-12 | deep | critical | yes | codex | AC-020/025/028/029 | Remediation | done |
| T-14 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/14-review-oss-deletion-state.md</Path>` | 可恢复 Provider 删除状态 | T-13 | deep | critical | yes | codex | AC-007/008/010/011 | Remediation | done |
| T-15 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/15-review-upload-resume-policy.md</Path>` | 安全续传与命名策略 | T-14 | deep | high | yes | codex | AC-001/004/005/014/032 | Remediation | done |
| T-16 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/16-review-business-oss-adoption.md</Path>` | 业务引用与业务附件短链 | T-15 | deep | high | yes | codex | AC-009/010/013/028 | Remediation | done |
| T-17 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/17-review-frontend-cutover.md</Path>` | 策略化组件与富文本回显 | T-16 | standard | high | yes | codex | AC-003/004/014 | Remediation | done |
| T-18 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/18-review-operations-observability.md</Path>` | 生命周期 UI 与 Bucket 实检 | T-17 | standard | medium | yes | codex | AC-008/009/032 | Remediation | done |
| T-19 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/19-oss-reference-reconciliation-contract.md</Path>` | OSS 引用集合机械协调 API | — | deep | high | yes | codex | AC-009/010 | Owner Expand | done |
| T-20 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/20-system-business-oss-owners.md</Path>` | System 三类 Owner 全写路径与事务收口 | T-19 | deep | high | yes | codex | AC-009/010/029 | Owner Migrate | done |
| T-21 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/21-workflow-history-attachment-owner.md</Path>` | Workflow 历史任务附件引用闭环 | T-19 | deep | high | yes | codex | AC-009/010 | Owner Migrate | done |
| T-22 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/22-business-oss-owner-ratchet.md</Path>` | Owner manifest、架构棘轮与公共 API 收缩 | T-20,T-21 | deep | high | yes | codex | AC-008/009/010/011 | Owner Contract Gate | done |

Ticket frontmatter 是状态、依赖、深度和路径访问契约的权威；本表只同步投影。

## 3. 依赖 DAG

```text
T-01 [OSS foundation] ─┐
                       ├─→ T-03 [lifecycle/ref/download] ─→ T-04 [upload control] ─→ T-05 [protocol contract]
T-02 [schema/menu] ─────┤                 │
                       │                 └────────────────────────┐
T-06 [notify core] ─→ T-07 [idempotency] ─→ T-08 [snapshot] ─→ T-09 [observe/API] ─┬─→ T-10 [callers]
T-02 ───────────────────────────────────────────────────────────────┘               └─→ T-11 [UI]

T-12 ─→ T-13 ─→ T-14 ─→ T-15 ─→ T-16 ─→ T-17 ─→ T-18 [review remediation done]

T-19 [reference reconcile expand] ─┬─→ T-20 [system owners] ───┐
                                   └─→ T-21 [workflow owner] ──┴─→ T-22 [manifest + contract gate]
```

- 根 Ticket：T-01、T-02、T-06。
- 关键汇合点：T-03 汇合 common-oss/schema；T-08 汇合 OSS 生命周期/通知幂等；T-09 汇合 schema/附件/Event。
- T-05 是 OSS 破坏性 contract Gate；T-09 是通知 observe Gate；T-10/T-11 可在 T-09 后并行。
- T-19 是 Owner 能力 expand 根节点；T-20/T-21 是路径不相交的迁移候选；T-22 是二者汇合后的 contract 与架构 Gate。
- 每条边都是编译契约、schema 或可验证行为的真实开始条件，不用于表达人员交接。

## 4. 合同覆盖矩阵

| Contract ID | 覆盖 Ticket | 验证接缝 | 状态 | 说明 |
|---|---|---|---|---|
| AC-001 | T-04,T-05 | HTTP+浏览器网络 | covered | SINGLE 直传只返回 ossId |
| AC-002 | T-01,T-04 | OssClient+控制面 | covered | Multipart 批量签名 |
| AC-003 | T-05 | 浏览器人工故障验收 | covered | 失败 Part 重传 |
| AC-004 | T-04,T-05 | Ticket+浏览器 | covered | 同文件续传 |
| AC-005 | T-01,T-02,T-04 | Provider+DB | covered | Complete 幂等 |
| AC-006 | T-01,T-04 | 完成失败注入 | covered | 校验失败无 ossId |
| AC-007 | T-01,T-03,T-04 | Abort/cleanup | covered | 双层清理 |
| AC-008 | T-02,T-03,T-18,T-22 | DB/定时任务+fresh baseline | covered | TEMP 24 小时与清理启用前门禁 |
| AC-009 | T-02,T-03,T-16,T-18,T-19,T-20,T-21,T-22 | lifecycle+业务 Owner+架构清单 | covered | 真实表名/主键引用和 owner ratchet |
| AC-010 | T-02,T-03,T-16,T-19,T-20,T-21,T-22 | lifecycle+业务事务 | covered | 多引用、集合差异与最后解绑 |
| AC-011 | T-02,T-03,T-14,T-22 | 删除保护+fresh baseline | covered | 有引用拒绝删除且不进入清理候选 |
| AC-012 | T-03,T-05 | 集成测试+浏览器人工验收 | covered | 短时直下 URL |
| AC-013 | T-03,T-05 | 业务权限矩阵 | covered | 业务授权后 presign |
| AC-014 | T-05 | 前端门禁/人工验收/扫描 | covered | 所有已知前端迁移 |
| AC-015 | T-06,T-10 | Dispatcher+真实入口 | covered | 统一发送成功 |
| AC-016 | T-06,T-10 | 部分失败测试 | covered | 完整结果异常 |
| AC-017 | T-06,T-10 | 验证失败测试 | covered | USER/缺快照拒绝 |
| AC-018 | T-07 | Redis 集成 | covered | 首次占位与五分钟窗口 |
| AC-019 | T-07 | 并发集成 | covered | IN_PROGRESS |
| AC-020 | T-07,T-09 | Redis/Event/DB | covered | duplicate 逻辑日志 |
| AC-021 | T-07 | Redis 故障 | covered | Key 请求 fail-closed |
| AC-022 | T-07 | 无 Key 回归 | covered | Redis 故障不阻断 |
| AC-023 | T-01,T-08,T-10 | Copy+Provider | covered | 一通知一组快照 |
| AC-024 | T-01,T-08,T-10 | Copy 失败 | covered | Provider 零调用 |
| AC-025 | T-02,T-08,T-09,T-10 | Event+DB | covered | 两层日志与状态语义 |
| AC-026 | T-03,T-08,T-09 | Listener 失败 | covered | 同步结果不变、TEMP 回收 |
| AC-027 | T-09,T-11 | API+UI | covered | 全局脱敏列表 |
| AC-028 | T-09,T-11 | 权限+安全 UI | covered | 完整详情且 HTML 不执行 |
| AC-029 | T-02,T-03,T-09,T-11,T-13,T-20 | DB/API/UI+Owner 事务 | covered | 删除/清空与附件解绑 |
| AC-030 | T-03,T-09,T-11 | 多 Client 矩阵 | covered | 权限求值而非行隔离 |
| AC-031 | T-06,T-09,T-10 | Context/Job | covered | 无 Client 可发送 |
| AC-032 | T-01,T-02,T-04 | Context/运维诊断 | covered | 配置安全失败 |

## 5. 并行与路径所有权

- T-01 至 T-18 的既有 Goal Plan 已执行完成；它不包含 T-19 至 T-22，不能作为新 Ticket 的编排依据。
- T-19 至 T-22 包含公共 API、两个业务 module、expand-migrate-contract 和高风险数据完整性门禁，进入实现前必须重新运行 `<Path>{roots.workflows}/specdev/P-goal-plan/P-goal-plan.md</Path>`。
- T-20/T-21 在 DAG 和生产可写路径上可并行；实际 writer 上限、workspace 与 direct-parent/candidate 策略由新 Goal Plan 决定。
- T-02 是 `DDL.sql/DML.sql` 唯一 owner；T-06 是 common parent/BOM 唯一 owner。
- T-06 -> T-07 -> T-08 的 common-notify，以及 T-03 -> T-04 -> T-05 的 OSS 重叠路径通过真实依赖串行移交，不允许并行写。
- Lead 是 SpecDev 工件、父分支集成和人工验收 Evidence owner；本计划不运行 E2E 测试。

| Ticket A | Ticket B | Writable 交集 | 真实依赖 | 处理 |
|---|---|---|---|---|
| T-01 | T-02 | 无 | 否 | DAG 可并行，但 current 策略串行 |
| T-01 | T-06 | 无 | 否 | DAG 可并行，但 current 策略串行 |
| T-02 | T-06 | 无 | 否 | DAG 可并行，但 current 策略串行 |
| T-03 | T-07 | 无 | 否 | DAG 可并行，但 current 策略串行 |
| T-04 | T-08 | 无 | 否 | DAG 可并行，但 current 策略串行 |
| T-05 | T-09 | 无 | 否 | DAG 可并行，但 current 策略串行 |
| T-10 | T-11 | 无 | 否 | DAG 可并行，但 current 策略串行 |
| T-06 | T-07 | common-notify | 是 | T-06 完成后移交 |
| T-07 | T-08 | common-notify | 是 | T-07 完成后移交 |
| T-03 | T-05 | system OSS files | 是（传递） | T-05 最终 contract |
| T-08 | T-09 | system notify package | 是 | T-09 接管监控实现 |
| T-19 | T-20 | 无（T-20 只读 OssService） | 是 | T-19 expand 完成后迁移 system Owner |
| T-19 | T-21 | 无（T-21 只读 OssService） | 是 | T-19 expand 完成后迁移 workflow Owner |
| T-20 | T-21 | 无 | 否 | DAG 可并行，实际并发由新 Goal Plan 决定 |
| T-19 | T-22 | OssService/SysOssServiceImpl | 是（传递） | T-19 expand owner，T-22 在迁移后接管 contract |

## 6. Gate、Wave 与集成点

- **Historical Waves 1-5：** T-01 至 T-11 已完成 Foundations、State、Control/Snapshot、Contract/Observe 和 Consumers。
- **Historical Remediation：** T-12 至 T-18 已完成 review remediation。
- **Owner Wave A / Expand：** T-19。
- **Owner Wave B / Migrate：** T-20、T-21；依赖允许并行。
- **Owner Wave C / Contract Gate：** T-22。
- 当前共 17 个 Deep Ticket、5 个 Standard Ticket；新批次包含共享公共契约、两个 module 和 contract Gate，必须重新生成 Goal Plan。用户确认全部新增 Ticket 的 E2E disposition 为 not-required。
- 每个新增 Ticket 必须在所属后端仓库形成非空 implementation commit；workspace、集成顺序与 result SHA 规则由新 Goal Plan 锁定。

## 7. 横切契约与风险

- 后端永远不接收或返回浏览器文件字节；objectKey、Provider、uploadId、策略快照由服务端控制。
- Client/userType 只影响登录准入、角色、菜单、权限、数据权限和动态路由；不自动形成 OSS/通知数据隔离。
- `sys_oss_ref` 是轻量多态引用，无动态外键或 ACL；真实表改名时必须显式迁移 ref_type。
- Provider 同步发送，Event best-effort 监控；不可把 Event 当可靠队列或把 ACCEPTED 当 DELIVERED。
- 通知正文、OTP、Token 与完整目标永久明文保存是批准风险；不得因此泄露 Provider Secret，列表仍必须脱敏、HTML 不执行。
- T-05 前后端必须原子发布；启用直传前完成 CORS/Lifecycle 运维检查。
- Business OSS Owner 位于业务写入 module；业务行与引用转换同一 `@DSTransactional` 且 fail-closed，不使用运行时扫描、动态回调或提交后修复队列。
- Owner manifest 是测试/交付门禁而非生产注册中心；T-22 前后 `cleanup-enabled:false`、`cleanup-dry-run:true` 保持不变，真实启用仍需独立发布批准。
- SQL 只追加 NAMEWTA DDL/DSL；后端单元/集成测试集中在 ruoyi-admin 并显式 `-Dmaven.test.skip=false`，前端执行 lint、补充 type diagnostic 和 build；不建设 E2E 测试套件。

## 8. 同步规则

- Ticket 状态变化后同步执行清单；Ticket frontmatter 为依赖、状态、深度和路径权威。
- 新 Goal Plan 发布后，Wave/Gate/owner 以 `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/goal-plan.md</Path>` 为编排权威；当前旧文件只覆盖已完成的 T-01 至 T-18。
- Evidence 必须写入 `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-NN.md</Path>`，记录命令、结果、source/parent/candidate SHA 和偏差。
- 依赖、合同或路径变化后运行 `<Path>{roots.workflows}/specdev/common/tools/validate-specdev.mjs</Path>` 的 `--stage tickets` 校验。
- 内部工件始终使用完整根变量 Path 标签，不使用相对 Markdown 链接。
