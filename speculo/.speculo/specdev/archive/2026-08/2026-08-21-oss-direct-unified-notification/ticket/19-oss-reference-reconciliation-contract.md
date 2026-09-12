---
schema_version: 3
artifact: ticket
change: 2026-08-21-oss-direct-unified-notification
id: T-19
title: 建立 OSS 引用集合协调契约
status: done
planning_depth: deep
planning_depth_reason: 公共 OssService API、共享生命周期核心路径与后续 expand-contract 收缩
ready: true
risk: high
blocked_by: []
contract_ids: [AC-009, AC-010]
owner: codex
expected_changes: ["<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/OssService.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/service/OssLifecycleManager.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysOssServiceImpl.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/lifecycle/OssReferenceReconciliationUnitTest.java</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/OssService.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/service/OssLifecycleManager.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysOssServiceImpl.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/lifecycle/OssReferenceReconciliationUnitTest.java</Path>"]
read_only_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/oss/mapper/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/lifecycle/OssLifecycleManagerUnitTest.java</Path>"]
shared_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/OssService.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysOssServiceImpl.java</Path>"]
shared_path_owners: ["<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/OssService.java</Path> => T-19 expand owner; T-22 contract owner after migration", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysOssServiceImpl.java</Path> => T-19 expand owner; T-22 contract owner after migration"]
---

# Ticket T-19: 建立 OSS 引用集合协调契约

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/19-oss-reference-reconciliation-contract.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-19.md</Path>`

## 1. 战略与来源

- **目标：** 为每个 Business OSS Owner 提供只负责旧新 OSS 集合差异和引用转换的稳定公共接缝，消除各业务重复的 bind/unbind 循环。
- **可观察产出：** 调用方提交同一业务记录的旧、新 ossId 集合后，新增引用幂等建立、移除引用幂等解除，对象 TEMP 状态与引用计数按既有生命周期合同收敛。
- **来源：** `AC-009`、`AC-010`、`ADR-005`、`ADR-010`、`LOG-064`、`CODE`。
- **当前事实：** `<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/OssService.java</Path>` 只暴露单个 `bind/unbind`；用户、公告和通知各自计算或循环引用变化，未来 workflow 也需要相同机械规则。
- **Planning Depth 原因：** 修改跨模块公共 API 和 OSS 生命周期核心路径，并作为 T-20/T-21 迁移与 T-22 contract 的前置 expand。

## 2. 决策状态

### 已锁定决策

- 公共方法固定为 `void reconcileReferences(String refType, String refId, Collection<Long> previousOssIds, Collection<Long> currentOssIds)`。
- `null` 集合等价于空集合，重复 ID 去重；集合中的 null、非正数 ID，以及空白 `refType/refId` 明确拒绝，不静默修正。
- 差集按 ossId 排序，先绑定 `current - previous`，再解绑 `previous - current`；同一输入重复执行不产生额外有效引用。
- 接缝只处理机械集合差异和生命周期转换，不读取业务表、不执行授权、不推断 ACL、不接受业务回调。
- 原始 `bind/unbind` 暂留给迁移期间的现有调用方，最终由 T-22 在调用点归零后从公共 API 收缩。

### 已采用的低影响假设

- 复用现有 `OssLifecycleManager.bind/unbind` 的行锁、幂等和 TEMP 转换语义，不新增 DTO、数据库列或表。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| OssService 集合协调 API、system 实现、确定性集合差异及定向测试 | 单引用行锁、引用唯一性、TEMP/expireTime 转换、现有 Mapper | 业务授权、业务表读写、Owner 注册中心、运行时注解扫描、旧调用点迁移 |

## 4. 要构建什么

Business OSS Owner 在自身事务内传入真实物理表名、真实主键及保存前后的 ossId 集合。协调器验证并规范化输入，先建立新增引用，再解除已移除引用；任一步失败都向调用方抛出异常，使外层业务事务 fail-closed。集合不变或重复调用时，数据库引用和生命周期状态保持稳定。

## 5. 实现契约

- **入口或接缝：** `OssService.reconcileReferences(...)`，由 `SysOssServiceImpl` 委托 `OssLifecycleManager`。
- **输入与输出：** 输入为业务引用身份和保存前后 ossId 集合；成功无返回值，失败抛出现有可区分生命周期异常或参数异常。
- **公共接口变化：** expand 新增集合协调方法；本 Ticket 不删除 `bind/unbind`。
- **不变量：** `ref_type/ref_id` 是真实表名/主键且不是 ACL；差集确定、去重、幂等；协调器不反向依赖业务 module。
- **状态或数据流：** validate -> normalize/sort -> bind additions -> unbind removals；沿用 `@DSTransactional`，加入现有外层事务。
- **错误与失败行为：** 任一对象不存在、状态非法或写入失败时立即抛出；禁止吞错或 best-effort 继续。
- **兼容要求：** 当前是无历史负担的基座系统；只为仓库内绿色迁移临时保留旧 Java 方法，不形成生产兼容窗口。
- **安全与隐私要求：** 该 API 只允许后端 Owner 在完成业务授权后调用；不得增加 Controller 或前端通用 bind 入口。

## 6. 执行路线

1. 先以单元测试固定空集合、重复 ID、非法 ID、集合不变、部分增删和中途失败语义。
2. 在 `OssService` expand 集合协调方法，并在 system 实现中保持模块依赖方向。
3. 在生命周期层实现确定性差集，复用现有单引用转换而不复制状态机。
4. 验证失败能够传播并由同一动态数据源事务回滚全部引用变化。
5. 运行定向测试、ruoyi-admin opt-in 测试和后端 reactor package。

## 7. 路径访问契约

- **预计修改点：** OssService、SysOssServiceImpl、OssLifecycleManager 和一份聚焦协调测试。
- **可写范围：** 仅 frontmatter `writable_paths`；发现需要修改业务 Owner 时停止并留给 T-20/T-21。
- **只读上下文：** 生命周期 Mapper 与既有测试。
- **共享路径：** OssService 与 SysOssServiceImpl；T-19 只拥有 expand，迁移完成后的 contract 由 T-22 拥有。
- **保留或不动：** schema、上传/下载协议、cleanup 配置、业务服务。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | lifecycle 单元测试 | `./mvnw -pl ruoyi-admin -am -Dmaven.test.skip=false -Dtest=OssReferenceReconciliationUnitTest -Dsurefire.failIfNoSpecifiedTests=false test` | 新增、移除、不变和重复输入按确定差集收敛 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-19.md</Path>` |
| 失败路径 | 参数与故障注入 | 同一测试覆盖非法 ID 和中途 bind/unbind 失败 | 异常不被吞掉，已发生变化随事务回滚 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-19.md</Path>` |
| 回归 | 后端门禁 | `./mvnw -Dmaven.test.skip=false test` 后执行 `./mvnw -DskipTests package` | opt-in 测试与 reactor package 通过 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-19.md</Path>` |

- **Workspace checks：** 由新 Goal Plan 选择 current workspace/direct-parent 后执行上述检查。
- **E2E disposition：** not-required：无 HTTP/UI 新行为，公共 API 和事务失败语义由聚焦 Java 测试覆盖；用户已确认不建设 E2E。
- **E2E owner/environment：** Lead / current-workspace。
- **Integration evidence：** 记录 implementation commit、parent before/result SHA、测试输出及父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 本 Ticket expand；T-20/T-21 migrate；T-22 contract。
- **兼容窗口：** 不适用：无外部消费者或历史版本兼容承诺，旧方法只在同一仓库实施序列中短暂存在。
- **监控信号：** 定向测试中的引用计数、TEMP/expireTime 与异常传播；不新增生产指标。
- **回滚或前向恢复：** 本 Ticket 只新增 API 和委托，可整体回退；一旦业务迁移则以前向完成 T-22 为主。
- **不可逆操作与批准点：** 无数据库迁移或清理启用。
- **收缩条件：** T-20/T-21 完成且全仓业务调用 `OssService.bind/unbind` 为零，由 T-22 Evidence 证明。

## 10. 验收标准

- [x] `AC-009`：真实表名/主键的新增集合只创建唯一有效引用并使对象退出 TEMP。
- [x] `AC-010`：移除集合按多引用合同更新计数，最后解绑重新进入 TEMP 宽限期。
- [x] 协调器不读取业务表、不推断 ACL、不引入运行时注册或回调。
- [x] 失败注入证明引用差异在动态数据源事务内 fail-closed。
- [x] 验证矩阵全部执行并记录到 `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-19.md</Path>`。
- [x] 实际项目修改未超出 `writable_paths`，共享路径只完成 expand。
- [x] Ticket 已按新 Goal Plan 形成非空 implementation commit，并记录 direct-parent 验证结果。
