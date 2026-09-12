---
schema_version: 3
artifact: ticket
change: 2026-08-21-oss-direct-unified-notification
id: T-09
title: 通知监控后端
status: done
planning_depth: deep
planning_depth_reason: 永久明文保存敏感通知内容并提供全局查询删除，涉及高影响隐私、安全权限、异步一致性和附件生命周期。
ready: true
risk: critical
blocked_by: [T-02, T-03, T-08]
contract_ids: [AC-020, AC-025, AC-026, AC-027, AC-028, AC-029, AC-030, AC-031]
owner: cursor-agent
expected_changes: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/notify/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/monitor/SysNotifyController.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/resources/mapper/system/SysNotify*.xml</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/pom.xml</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/notify/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/monitor/SysNotifyController.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/resources/mapper/system/SysNotify*.xml</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/pom.xml</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/notify/monitor/**</Path>"]
read_only_paths: ["<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysOperLogServiceImpl.java</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-09: 通知监控后端

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/09-notify-monitoring-backend.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-09.md</Path>`

## 1. 战略与来源

- **目标：** 将同步通知结果以 Event 异步写成一条逻辑日志和逐目标 Delivery，并提供全局运维 API。
- **可观察产出：** 可筛选列表、查看完整明文详情、删除/批删/清空；附件引用与父子日志一致维护。
- **来源：** `ADR-001/008/009`、`AC-020/025..031`。
- **当前事实：** common-log 有 async Event 先例，但通知没有表、Listener、API 或权限实现。
- **Planning Depth 原因：** OTP/Token/完整目标永久明文、全局同权查询是明确接受但事故半径极高的决定。

## 2. 决策状态

### 已锁定决策

- Event 只做 best-effort 监控；监听失败不改变 Provider 同步结果。
- 一逻辑通知一行 notify log，每个实际目标 attempt 一行 Delivery；duplicate 只有逻辑 SKIPPED_DUPLICATE，无 Delivery。
- 内容、参数、snapshot、完整目标永久明文，不加密、不自动清理；列表目标服务端脱敏，详情同 query 权限返回全部。
- 日志是全局数据；client_pk 仅展示来源 sys_client.id，可空，不做所有权、过滤、Provider 路由或幂等。
- remove 支持单/批/清空并记录 OperLog；父子一致删除并解绑附件引用。

### 已采用的低影响假设

- HTML 正文后端作为普通字符串返回，并附 contentType；执行安全由 T-11 文本/隔离预览保证。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| entity/mapper/service、async listener、list/detail/delete/clean API、权限、脱敏、附件 bind/unbind | T-02 schema、T-03 lifecycle、T-08 Event/SPI、OperLog | UI、正文加密/保留期、自动重试、回执、Client 行过滤 |

## 4. 要构建什么

Dispatcher 发布完整上下文 Event 后，异步 Listener 不再读取 ThreadLocal，写入逻辑行和 Delivery。成功、部分失败、失败及 duplicate 均按合同持久化；ACCEPTED 不映射为 DELIVERED。运维查询可按业务、Channel、Provider、状态、消息 ID、trace、时间和来源 Client 筛选，但 client_pk 不是隐式条件。详情返回完整敏感内容。删除在一致事务中处理父子与引用，并留下 OperLog。

## 5. 实现契约

- **入口或接缝：** `@Async @EventListener`、`/monitor/notify` list/detail/delete/clean、system notify Service。
- **输入与输出：** immutable Event -> 两层 DB；PageQuery -> masked list；id -> full detail/delivery/attachments。
- **公共接口变化：** 新 monitor HTTP API 与 `system:notify:list/query/remove` 权限实现。
- **不变量：** Listener 不读 LoginHelper/MDC Client ThreadLocal；一 actual target attempt 一 Delivery；duplicate 零 Delivery。
- **状态或数据流：** event -> transaction insert/bind refs；delete -> children/parent/ref unbind -> zero ref TEMP。
- **错误与失败行为：** Listener 失败仅记录应用错误；查询/删除无权拒绝；删除补偿不得留下半父子关系。
- **兼容要求：** 不改变同步 NotifyResult/Exception；现有 OperLog 行为不退化。
- **安全与隐私要求：** 明文永久存储是批准风险；Provider Secret/Auth/signed URL 不入库，列表只能脱敏，HTML 不加可信标记。

## 6. 执行路线

1. 建立 Event->DB、duplicate、partial failure、listener failure 和权限矩阵测试。
2. 映射 T-02 两表，建立实体/Mapper/查询投影与服务端脱敏。
3. 实现异步 Listener 和附件引用绑定，不读取异步 ThreadLocal。
4. 实现 list/detail API，确保无 client_pk 隐式过滤且详情完整。
5. 实现单/批/清空的 OperLog、父子一致删除和引用解绑。
6. 在 ruoyi-admin 运行 Event+DB+HTTP+权限集成测试与容量/敏感字段核对。

## 7. 路径访问契约

- **预计修改点：** system notify package、monitor Controller、Mapper XML、system POM。
- **可写范围：** 仅 frontmatter 所列；SQL 由 T-02 独占。
- **只读上下文：** schema、common Event、OperLog 先例。
- **共享路径：** 无；T-08 完成后接管 `system/notify/**`，T-10/T-11 不写本路径。
- **保留或不动：** common-notify、SQL、前端、SysMessage/common-push。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | Event+DB+HTTP | success/partial/fail/duplicate 后 list/detail | 两层行和完整/脱敏投影正确 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-09.md</Path>` |
| 失败路径 | Listener/权限/删除注入 | listener fail、无权、批删/清空失败 | 同步发送不变、无越权/半删除 | 同上 |
| 回归 | Maven + 多 Client 矩阵 | 两 Client 权限不同、Job 无 Client | 权限求值正确且日志不按 client_pk 隔离 | 同上 |

- **Workspace checks：** current 为 `current-workspace`；required 为 `source-worktree` 非 E2E，Lead 在 parent-candidate 验证集成。
- **E2E disposition：** not-required：用户明确不执行 E2E；Event、DB、HTTP 权限和引用行为集中由 ruoyi-admin 集成测试验证。
- **E2E owner/environment：** 不适用；Lead 在 current-workspace 核对敏感明文、全局查询、删除/清空和 Client/Job 测试矩阵。
- **Integration evidence：** implementation commit、parent before、candidate/result SHA、DB/API/OperLog/对象引用 Evidence。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-02 schema -> T-08 Event/快照 -> listener/API -> T-10/T-11 消费。
- **兼容窗口：** Listener 可在调用迁移前上线；T-10 后成为统一监控入口。
- **监控信号：** Event 收到/落库失败、父子数差异、unbound snapshot、表容量、删除量和权限拒绝。
- **回滚或前向恢复：** 可停用 listener/API，保留表和已写日志；附件引用异常以前向核对修复，禁止自动清空。
- **不可逆操作与批准点：** 生产授予 query/remove 权限和首次清空操作须由 Lead/运维明确批准；永久明文风险已由用户接受。
- **收缩条件：** T-10 证明已知调用统一发 Event，T-11 验证安全展示后开放菜单。

## 10. 验收标准

- [x] `AC-020/025/026/027/028/029/030/031` 全部通过。
- [x] client_pk 只显示来源，不作为 SQL 隐式条件或所有权。
- [x] 明文详情、列表脱敏、HTML 非可信、删除 OperLog 和附件解绑均有 Evidence。
- [x] ruoyi-admin 集成测试、提交 SHA 与 Evidence 完整。
