---
schema_version: 3
artifact: ticket
change: 2026-08-21-oss-direct-unified-notification
id: T-06
title: common-notify 核心与渠道 Adapter
status: done
planning_depth: deep
planning_depth_reason: 新增公共模块、BOM 注册、渠道 SPI 和跨原子模块依赖方向，形成后续所有通知行为的公共契约。
ready: true
risk: high
blocked_by: []
contract_ids: [AC-015, AC-016, AC-017, AC-031]
owner: cursor-agent
expected_changes: ["<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-mail/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-sms/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/pom.xml</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-bom/pom.xml</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-mail/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-sms/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/pom.xml</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-bom/pom.xml</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/notify/core/**</Path>"]
read_only_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-log/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/**</Path>"]
shared_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-common/pom.xml</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-bom/pom.xml</Path>"]
shared_path_owners: ["<Path>ruoyi-vue-plus-namewta/ruoyi-common/pom.xml</Path> => T-06", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-bom/pom.xml</Path> => T-06"]
---

# Ticket T-06: common-notify 核心与渠道 Adapter

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/06-common-notify-core-adapters.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-06.md</Path>`

## 1. 战略与来源

- **目标：** 建立薄的渠道无关通知契约，并以 Mail/SMS Adapter 证明扩展点。
- **可观察产出：** 调用者只依赖 NotifyClient，可一次向一个 Channel 的多个物理目标同步发送并获得逐目标结果。
- **来源：** `ADR-001/006/007/008`、`AC-015/016/017/031`。
- **当前事实：** common-mail/common-sms 原子能力存在，但业务直接调用 Builder/SDK，没有统一请求、结果和异常。
- **Planning Depth 原因：** 公共 module/API、依赖方向和错误语义将约束未来所有渠道。

## 2. 决策状态

### 已锁定决策

- common-notify 不依赖 system；Mail/SMS 保持原子模块并通过 Adapter 实现 SPI。
- 一个请求一个 Channel，可多物理 PHONE/EMAIL/OPEN_ID；不支持 USER 解析。
- Provider 全局默认、`providerKey` 可覆盖；不按 Client 路由或 failover。
- 全部目标尝试后，部分失败抛携带完整结果的 `NotifyDeliveryException`；ACCEPTED 不叫 DELIVERED。
- Context Resolver 可记录 userId/traceId/可空 client_pk；后台 Job 无需 SYSTEM scope。

### 已采用的低影响假设

- channel 使用可扩展字符串标识，由 Registry 在启动时拒绝重复 Adapter。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| module/BOM、NotifyClient/Dispatcher、模型、SPI、异常、Event 模型、Mail/SMS Adapter | MailBuilder、SMS4J、Spring bean registry | system listener、幂等、附件实现、模板中心、站内信、自动重试 |

## 4. 要构建什么

调用方提交单 Channel、多个已解析物理目标和 typed content。Dispatcher 选择默认或显式 Provider，验证模板必须有 contentSnapshot，调用 Adapter 并保留每个目标结果。全部合法目标都会被尝试；任一失败后抛出含完整结果的 typed exception。Dispatcher 构造不可变 Event/context snapshot，但本 Ticket 不落库。

## 5. 实现契约

- **入口或接缝：** `NotifyClient.send(NotifyRequest)`、Channel Adapter/Context Resolver SPI、Mail/SMS Adapter。
- **输入与输出：** Spec 第 7 节 NotifyRequest/Target/Content/Result/Exception。
- **公共接口变化：** 新 Maven artifact、BOM 条目和 Spring 自动装配。
- **不变量：** common 不读取 SysUser；一个 Channel；物理目标；不把 client_pk 写入请求权威字段。
- **状态或数据流：** validate -> resolve context/provider -> adapter -> aggregate result/exception -> publish event seam。
- **错误与失败行为：** USER、模板缺快照、未知 channel/provider、目标失败可区分且不丢成功结果。
- **兼容要求：** 原 Mail/SMS API 保留；T-10 才收缩业务直接调用。
- **安全与隐私要求：** Event 允许完整正文/目标；禁止 credential、Authorization、signed URL 和原始内部异常 Secret。

## 6. 执行路线

1. 新建 module/BOM 注册并建立核心模型和多目标失败测试。
2. 实现 Registry、Dispatcher、Context Resolver 和事件发布接缝。
3. 实现模板快照验证、Provider 选择和 typed exception。
4. 在 mail/sms 原子模块实现 Adapter，验证 TO/CC/BCC 与逐目标语义。
5. 运行模块定向测试和 BOM/reactor 构建。

## 7. 路径访问契约

- **预计修改点：** common-notify、新 Adapter、common parent/BOM。
- **可写范围：** frontmatter 所列 common 路径。
- **只读上下文：** common-log Event 惯例和 system 依赖边界。
- **共享路径：** parent/BOM 唯一 owner T-06；T-07/T-08 只顺序扩展 common-notify。
- **保留或不动：** system、业务调用方、SysMessage/common-push。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | Dispatcher/Adapter tests | 默认/覆盖 Provider、多目标 Mail/SMS | 逐目标 ACCEPTED 与 context 正确 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-06.md</Path>` |
| 失败路径 | validation/partial failure | USER、缺 snapshot、未知 provider、部分失败 | Provider 零调用或完整异常结果 | 同上 |
| 回归 | Maven reactor | `./mvnw -pl ruoyi-common/ruoyi-common-notify,ruoyi-common/ruoyi-common-mail,ruoyi-common/ruoyi-common-sms -am -Dmaven.test.skip=false test` | 原子模块与 BOM 通过 | 同上 |

- **Workspace checks：** current 在 `current-workspace`，required 模式在 `source-worktree` 做非 E2E。
- **E2E disposition：** not-required：公共契约与 Adapter 可由 deterministic Provider test double/集成测试充分验证。
- **E2E owner/environment：** Lead / current-workspace；T-10 验证真实调用链。
- **Integration evidence：** implementation/source commit、direct-parent 或 candidate/result SHA、父分支包含关系和 Lead Evidence。

## 9. 发布、迁移与恢复

- **迁移顺序：** additive module/BOM -> Adapter -> T-07/T-08/T-09 -> T-10 调用迁移。
- **兼容窗口：** 原 Builder/SDK 暂留，业务迁移前不删除。
- **监控信号：** unknown channel/provider、validation、Provider latency/status 和 Event publish 失败。
- **回滚或前向恢复：** 调用迁移前可回滚 module；迁移后保持 API 并前向修复。
- **不可逆操作与批准点：** 无。
- **收缩条件：** T-10 静态扫描证明 Adapter 外已知直接调用为零。

## 10. 验收标准

- [x] `AC-015/016/017/031` 的公共契约和异常语义通过。
- [x] common-notify 不依赖 system，Client 不参与路由或隔离。
- [x] POM/BOM 仅由 T-06 修改。
- [x] 测试、提交 SHA、E2E disposition 与 Evidence 完整。
