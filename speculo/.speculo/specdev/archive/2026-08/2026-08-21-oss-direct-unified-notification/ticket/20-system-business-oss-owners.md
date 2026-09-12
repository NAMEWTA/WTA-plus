---
schema_version: 3
artifact: ticket
change: 2026-08-21-oss-direct-unified-notification
id: T-20
title: 收口 System 模块 Business OSS Owner
status: done
planning_depth: deep
planning_depth_reason: 用户、公告、通知多个写入口的事务、删除语义与引用失败回滚
ready: true
risk: high
blocked_by: [T-19]
contract_ids: [AC-009, AC-010, AC-029]
owner: codex
expected_changes: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysUserServiceImpl.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysNoticeServiceImpl.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/notify/service/impl/SysNotifyMonitorServiceImpl.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/owner/system/**</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysUserServiceImpl.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysNoticeServiceImpl.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/notify/service/impl/SysNotifyMonitorServiceImpl.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/owner/system/**</Path>"]
read_only_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/OssService.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/mapper/**</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-20: 收口 System 模块 Business OSS Owner

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/20-system-business-oss-owners.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-20.md</Path>`

## 1. 战略与来源

- **目标：** 让 system 中每个已知持久化 OSS Owner 从创建开始维护完整引用，并将业务记录保存与引用变化置于同一动态数据源事务。
- **可观察产出：** 用户头像、公告正文和通知附件在 insert/update/delete 后都存在与真实业务行一致的引用；引用失败时业务写入不提交。
- **来源：** `AC-009`、`AC-010`、`AC-029`、`ADR-010`、`LOG-064`、`CODE`。
- **当前事实：** 个人资料更新已有单点 bind/unbind，但用户新增、管理端更新、注册和删除没有完整覆盖；公告和通知已有重复循环，未使用统一集合协调接缝。
- **Planning Depth 原因：** 跨三个 Owner 和多个写入口修改事务边界、删除策略及失败回滚，数据完整性事故半径高。

## 2. 决策状态

### 已锁定决策

- Owner 基线为 `sys_user.avatar`、`sys_notice.notice_content` 的 `oss://<id>` 标记、`sys_notify_log.attachment_oss_ids` JSON。
- 用户新增、注册、管理端更新和个人资料更新都以持久化前后 avatar 集合调用 `reconcileReferences`；生成主键后使用真实 `sys_user.user_id`。
- 用户没有明确恢复合同，逻辑删除与批量删除在同一事务内把 avatar 协调为空集合。
- 公告 insert/update/delete 和通知日志 record/remove/clean 全部迁移到集合协调 API，不保留业务侧原始 bind/unbind 循环。
- 受影响业务入口使用 `@DSTransactional`，不得继续用 Spring `@Transactional` 表达跨动态数据源业务事务。

### 已采用的低影响假设

- 继续复用现有公告 marker 解析和通知附件 JSON 解析，不改变数据库编码与 API DTO。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 三个 system Owner 的所有实际写/删入口、事务收口、合同/回滚测试 | T-19 集合协调、现有 marker/JSON 解析、Mapper | Workflow Owner、Owner manifest、历史数据回填、恢复功能、schema 变化 |

## 4. 要构建什么

当 system 业务创建或修改记录时，Owner 先取得旧值，保存业务行后以真实主键协调旧新 OSS 集合；删除前取得现有附件，删除业务行后协调为空。任何校验、业务保存或引用转换失败都会回滚整次操作。调用者成功返回后，业务字段与 `sys_oss_ref` 不会出现一边已提交、另一边缺失的状态。

## 5. 实现契约

- **入口或接缝：** SysUserServiceImpl 的 insert/register/update/profile/delete，SysNoticeServiceImpl 的 insert/update/delete，SysNotifyMonitorServiceImpl 的 record/remove/clean。
- **输入与输出：** 沿用现有业务接口；内部提取 `Set<Long>` 并调用 T-19 接缝，不新增客户端参数。
- **公共接口变化：** 无；只迁移公共 OssService 的使用方式。
- **不变量：** Owner 完成授权和业务语义；引用使用真实表名与真实主键；业务代码不直接计算 TEMP 状态。
- **状态或数据流：** load previous -> persist business row -> reconcile previous/current -> commit；delete 为 load previous -> delete/logically delete -> reconcile previous/empty -> commit。
- **错误与失败行为：** 引用转换失败必须回滚业务 insert/update/delete；禁止捕获后继续返回成功。
- **兼容要求：** 不保留旧行为或回填历史数据；fresh baseline 从第一条数据开始正确建引用。
- **安全与隐私要求：** 只有既有业务授权入口可触发引用变化；不增加仅凭 ossId 的通用绑定入口。

## 6. 执行路线

1. 建立三个 Owner 的 insert/update/delete 与引用失败回滚测试，先暴露当前用户入口缺口。
2. 将用户所有实际写入口统一为保存后使用真实 userId 协调，并把受影响事务改为 `@DSTransactional`。
3. 将用户删除/批量删除按“无恢复合同”语义协调为空。
4. 把公告和通知日志的原始 bind/unbind 循环迁移到 T-19 接缝，保留原编码与物理清理行为。
5. 扫描 system 生产代码确认不再调用公共原始 bind/unbind，并运行定向及后端全量门禁。

## 7. 路径访问契约

- **预计修改点：** 三个现有 ServiceImpl 与 system Owner 聚焦测试。
- **可写范围：** frontmatter 指定文件和测试目录；不得修改 OssService、生命周期核心或 workflow。
- **只读上下文：** T-19 接口、system domain 和 Mapper。
- **共享路径：** 无；T-19 完成后本 Ticket 与 T-21 可并行。
- **保留或不动：** schema、Controller/前端协议、通知附件快照编码、cleanup 配置。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | system Owner 服务测试 | `./mvnw -pl ruoyi-admin -am -Dmaven.test.skip=false -Dtest='org.dromara.test.oss.owner.system.*' -Dsurefire.failIfNoSpecifiedTests=false test` | 三个 Owner 的新增、修改、删除后字段与引用一致 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-20.md</Path>` |
| 失败路径 | 引用故障注入 | 对每类写操作注入 reconcile 异常 | 业务行、逻辑删除状态和引用全部回滚 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-20.md</Path>` |
| 回归 | 调用扫描与后端门禁 | `rg -n 'ossService\.(bind|unbind)\(' ruoyi-modules/ruoyi-system/src/main/java`，再执行 opt-in test/package | system 业务调用点为零，测试和 package 通过 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-20.md</Path>` |

- **Workspace checks：** 按新 Goal Plan 的 workspace 策略执行定向测试、`./mvnw -Dmaven.test.skip=false test` 与 `./mvnw -DskipTests package`。
- **E2E disposition：** not-required：没有新增 HTTP/UI 行为；业务事务、Mapper 交互和失败回滚由后端服务合同测试覆盖，用户已确认不建设 E2E。
- **E2E owner/environment：** Lead / current-workspace。
- **Integration evidence：** 记录 implementation commit、parent before/result SHA、测试结果和调用扫描。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-19 expand 完成后，一次迁移 system 三个 Owner；T-22 才允许收缩旧公共方法。
- **兼容窗口：** 不适用：当前基座无历史数据和外部 Java 消费者兼容负担。
- **监控信号：** 聚焦测试中的业务行/引用一致性、TEMP 状态和事务回滚；不新增生产遥测。
- **回滚或前向恢复：** 提交前依赖测试保证原子性；合入后优先前向修复，不以异步补偿掩盖不一致。
- **不可逆操作与批准点：** 不执行历史回填或 Provider 清理，不启用 TEMP 主动清理。
- **收缩条件：** system 生产代码公共 `bind/unbind` 调用为零并有 Evidence。

## 10. 验收标准

- [x] `AC-009`：三类 Owner 从 fresh insert 开始以真实业务主键建立唯一引用。
- [x] `AC-010`：修改与删除仅解除差集，最后解绑恢复 TEMP 宽限期。
- [x] `AC-029`：通知单删、批删和清空继续物理删除父子日志并解除全部附件引用。
- [x] 用户所有写入口和无恢复合同删除路径均已覆盖，引用失败证明业务事务回滚。
- [x] system 业务生产代码不再直接调用公共 `bind/unbind`。
- [x] 验证矩阵全部记录到 `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-20.md</Path>`。
- [x] 修改按 `DEV-T20-001` 受控扩展既有通知测试断言，并按新 Goal Plan 形成非空 implementation commit。
