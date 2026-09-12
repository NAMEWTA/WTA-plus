---
schema_version: 3
artifact: ticket
change: 2026-08-21-oss-direct-unified-notification
id: T-21
title: 接入 Workflow 历史任务附件 Owner
status: done
planning_depth: deep
planning_depth_reason: WarmFlow 引擎边界、独立历史主键、任务事务与不可逆历史清理
ready: true
risk: high
blocked_by: [T-19]
contract_ids: [AC-009, AC-010]
owner: codex
expected_changes: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/oss/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/liteflow/complete/CompleteExecuteComponent.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/liteflow/instance/InstanceDeleteExecuteComponent.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/impl/FlwTaskServiceImpl.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/impl/FlwInstanceServiceImpl.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/owner/workflow/**</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/oss/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/liteflow/complete/CompleteExecuteComponent.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/liteflow/instance/InstanceDeleteExecuteComponent.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/impl/FlwTaskServiceImpl.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/impl/FlwInstanceServiceImpl.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/owner/workflow/**</Path>"]
read_only_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/OssService.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/workflow/api/domain/CompleteTaskDTO.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/domain/bo/**</Path>", "<Path>ruoyi-vue-plus-namewta/script/sql/ry_workflow.sql</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-21: 接入 Workflow 历史任务附件 Owner

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/21-workflow-history-attachment-owner.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-21.md</Path>`

## 1. 战略与来源

- **目标：** 让 workflow module 成为其历史任务附件的 Business OSS Owner，在流程办理和不可逆历史清理中维护真实引用。
- **可观察产出：** 完成或退回任务后，`flow_his_task.ext` 的附件绑定到实际历史任务主键；物理删除历史实例后相应引用解除，转换失败时流程数据库变化回滚。
- **来源：** `AC-009`、`AC-010`、`ADR-010`、`LOG-064`、`CODE`、本地 WarmFlow 1.8.9 字节码核验。
- **当前事实：** CompleteTaskDTO/CompleteTaskBo/BackProcessBo 的 `fileId` 被写入 `FlowParams.hisTaskExt`，最终持久化到 `flow_his_task.ext`，但没有建立 `sys_oss_ref`；物理历史删除也未解绑。
- **Planning Depth 原因：** 必须跨第三方引擎写入接缝解析独立历史主键，并协调任务办理、退回、物理删除和动态数据源事务。

## 2. 决策状态

### 已锁定决策

- Owner 为 workflow module；`ref_type=flow_his_task`，`ref_id` 必须是 `flow_his_task.id`。
- WarmFlow 1.8.9 将原任务 ID 写入 `task_id`，并通过 `DataFillHandler.idFill` 生成独立历史主键；不得用 taskId 代替 refId。
- 在引擎转换前记录该 taskId 已有历史 ID，转换后再次查询并取 ID 差集，只处理本次新生成且实际携带附件的历史记录。
- `ext/fileId` 沿用现有逗号分隔 ossId 编码；解析失败、对象不存在或引用失败均 fail-closed。
- 物理历史删除前按 instanceId 加载历史任务及其附件，以每条历史 `id` 协调到空集合，再执行引擎删除；普通非历史删除不提前解除仍保留的历史附件。
- 受影响流程服务边界使用 `@DSTransactional`，workflow 只依赖已有 `ruoyi-api` OssService，不依赖 ruoyi-system 实现。

### 已采用的低影响假设

- 新增 workflow 内部 `oss` Owner/helper 封装历史查询、解析和协调，避免 LiteFlow 组件复制规则；不修改 WarmFlow JAR。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 完成/退回产生的历史附件绑定、物理历史删除解绑、Owner helper、事务和测试 | WarmFlow hisTaskService、现有 fileId/ext 编码、T-19 OssService、ruoyi-api 依赖 | 修改引擎 JAR、schema、历史回填、附件下载 UI、运行时注册/扫描 |

## 4. 要构建什么

流程办理者提交附件并完成或退回任务时，workflow 在同一事务中调用 WarmFlow 建立历史记录，定位本次新增的真实历史主键，再为每条携带附件的记录协调引用。管理员执行不可恢复的历史实例删除时，workflow 在删除前加载对应历史附件并解绑。任一引用转换失败都会使流程动作或删除整体失败，而不是留下不可达附件或已删除业务行的引用。

## 5. 实现契约

- **入口或接缝：** CompleteExecuteComponent、FlwTaskServiceImpl 的退回/办理路径、InstanceDeleteExecuteComponent 与 FlwInstanceServiceImpl 的历史删除边界。
- **输入与输出：** 沿用现有 `fileId` 字符串和流程接口；Owner 内部转换为去重 ossId 集合并以历史主键调用 `reconcileReferences`。
- **公共接口变化：** 无；消费 T-19 的 ruoyi-api 公共方法。
- **不变量：** `task_id` 只用于定位，`id` 才是 refId；仅绑定引擎实际持久化的历史记录；引用不是流程 ACL。
- **状态或数据流：** capture previous history IDs -> engine transition -> query by taskId -> new ID diff -> parse ext -> reconcile empty/current；删除为 load histories -> reconcile current/empty -> engine physical delete。
- **错误与失败行为：** 找不到预期新增历史记录、ext 非法、OSS 不可绑定或解绑失败时抛出并回滚；禁止按 taskId 猜测或补写引用。
- **兼容要求：** fresh baseline 直接建立目标行为；不回填既有 flow_his_task。
- **安全与隐私要求：** 流程既有权限和任务办理校验保持 owner 决策入口；OssService 不替代流程授权。

## 6. 执行路线

1. 以测试固定 WarmFlow 历史 `id/task_id/ext` 映射、办理新增记录差集和删除前加载语义。
2. 建立 workflow 内部历史附件 Owner，集中解析 fileId/ext、查询历史和调用 T-19 接缝。
3. 接入完成与退回路径，并把实际外层服务事务改为 `@DSTransactional`。
4. 接入不可恢复历史删除，确保先解绑、后物理删除且失败整体回滚。
5. 扫描 workflow 的 hisTaskExt 写入口，确认所有持久化附件路径均被覆盖，再运行定向与后端门禁。

## 7. 路径访问契约

- **预计修改点：** workflow Owner helper、完成/退回组件或服务、实例删除组件或服务、聚焦测试。
- **可写范围：** 仅 frontmatter 指定 workflow 文件和独立测试目录；不得修改 OssService/system 实现。
- **只读上下文：** ruoyi-api DTO/OssService、workflow BO、flow_his_task schema。
- **共享路径：** 无；T-19 后可与 T-20 并行。
- **保留或不动：** WarmFlow 依赖版本、JAR、数据库 schema、前端和 cleanup 配置。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | workflow Owner/服务测试 | `./mvnw -pl ruoyi-admin -am -Dmaven.test.skip=false -Dtest='org.dromara.test.oss.owner.workflow.*' -Dsurefire.failIfNoSpecifiedTests=false test` | 完成/退回按真实历史 id 建引用，物理历史删除解除引用 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-21.md</Path>` |
| 失败路径 | 引擎与引用故障注入 | 覆盖未生成历史、非法 ext、bind/unbind 失败 | 不使用 taskId 猜测，流程动作或删除整体回滚 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-21.md</Path>` |
| 回归 | 写入口扫描与后端门禁 | 扫描 `hisTaskExt`/`fileId` 持久化路径，再执行 opt-in test/package | 所有历史附件写入口有 Owner 证据，后端构建通过 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-21.md</Path>` |

- **Workspace checks：** 按新 Goal Plan 执行定向测试、`./mvnw -Dmaven.test.skip=false test` 与 `./mvnw -DskipTests package`。
- **E2E disposition：** not-required：无前端/HTTP 合同变化，WarmFlow 接缝、主键定位和事务失败由聚焦集成/服务测试覆盖；用户已确认不建设 E2E。
- **E2E owner/environment：** Lead / current-workspace。
- **Integration evidence：** 记录 implementation commit、parent before/result SHA、WarmFlow 版本事实、测试和入口扫描。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-19 后独立接入 workflow；T-20 可并行；两者都完成后进入 T-22。
- **兼容窗口：** 不适用：fresh baseline，不支持历史附件回填或旧版本并行运行。
- **监控信号：** 聚焦测试中的历史 id/refId、引用计数、TEMP 状态与事务回滚；不新增生产指标。
- **回滚或前向恢复：** 合入前可回退 workflow 接入；一旦产生新数据则以前向修复 Owner 为主，不使用 best-effort 队列。
- **不可逆操作与批准点：** 历史物理删除沿用既有管理员动作；本 Ticket 不主动触发删除或启用 OSS 清理。
- **收缩条件：** workflow 所有 hisTaskExt 持久化写入口和物理历史删除均有合同 Evidence。

## 10. 验收标准

- [x] `AC-009`：新历史任务附件以 `flow_his_task.id` 建立唯一引用，对象退出 TEMP。
- [x] `AC-010`：物理历史删除解除对应引用，最后解绑重新进入 TEMP 宽限期。
- [x] 测试证明 `task_id` 只用于定位，不被用作 `ref_id`。
- [x] 完成、退回和不可恢复历史删除的引用失败均使流程数据库事务回滚。
- [x] workflow 继续只通过 ruoyi-api 调用 system 能力，不新增反向模块依赖。
- [x] 验证矩阵全部记录到 `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-21.md</Path>`。
- [x] 修改未超出 `writable_paths`，并按新 Goal Plan 形成非空 implementation commit。
