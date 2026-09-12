# 业务模块接入工作流

其他模块通过 `wta-api` 接入，不要 Maven 依赖 `wta-workflow`，不要调用 `IFlw*` 或 Warm-Flow `InsService` / `TaskService` / `DefService`。能力描述不清时，按文中路径读源码，不得凭空补调用。

完整样例见 [leave-sample.md](leave-sample.md)。能力边界见 [capability-map.md](capability-map.md)。

## 接入决策

| 场景 | 走哪条面 | 路径 |
|---|---|---|
| 业务单据提交、后台无人会话办理、按业务 id 删实例、查状态/变量 | 注入 `org.namewta.workflow.api.WorkflowService` | `wta-vue-plus-namewta/wta-api/src/main/java/org/dromara/workflow/api/WorkflowService.java` |
| 回写业务表状态、节点副作用、级联删单据 | `@EventListener` 订阅 `ProcessEvent` / `ProcessTaskEvent` / `ProcessDeleteEvent` | `wta-vue-plus-namewta/wta-api/src/main/java/org/dromara/workflow/api/event/` |
| 人在工作流中心办待办、看已办/抄送、驳回、催办、委派转办 | REST `/workflow/task/*`（前端工作流页） | `wta-vue-plus-namewta/wta-modules/wta-workflow/src/main/java/org/dromara/workflow/controller/FlwTaskController.java` |
| 设计/发布流程定义、分类、SpEL 规则 | REST `/workflow/definition` `/workflow/category` `/workflow/spel` + Warm-Flow UI | `wta-vue-plus-namewta/wta-modules/wta-workflow/src/main/java/org/dromara/workflow/controller/FlwDefinitionController.java`；`wta-vue-plus-namewta/wta-modules/wta-workflow/src/main/java/org/dromara/workflow/controller/FlwCategoryController.java`；`wta-vue-plus-namewta/wta-modules/wta-workflow/src/main/java/org/dromara/workflow/controller/FlwSpelController.java` |
| 办理人 | 设计器配置用户/角色/部门/岗位/SpEL；不要复制 `PermissionHandler` | `wta-vue-plus-namewta/wta-modules/wta-workflow/src/main/java/org/dromara/workflow/handler/WorkflowPermissionHandler.java` |

`wta-demo` / `wta-system` / `wta-ai` / `wta-job` 当前没有接入实现。新模块复制请假模式，换成自己的 `flowCode`。

唯一装配方：`wta-vue-plus-namewta/wta-admin/pom.xml` 依赖 `wta-workflow`。

## 禁止

- 在业务模块 `pom.xml` 增加 `wta-workflow` 依赖。
- import `org.namewta.workflow.service.IFlw*` 或 `org.dromara.warm.flow.*` 引擎服务。
- 实现 Warm-Flow `GlobalListener` / `PermissionHandler`（已由本模块占用：`WorkflowGlobalListener`、`WorkflowPermissionHandler`）。
- 为接入审批新增 LiteFlow 组件或改 `wta-vue-plus-namewta/wta-modules/wta-workflow/src/main/resources/liteflow/task-chain.el.xml` / `instance-chain.el.xml`。
- 在业务模块重查 `flow_task` / `flow_user` 做待办列表。待办是工作流中心页能力：`GET /workflow/task/pageByTaskWait`。
- 订阅 `WorkflowCopyEvent` 等模块内副作用事件并重复发待办消息。
- `warm-flow.enabled=false` 时硬注入 `WorkflowService`（Bean 不存在）。需要可选接入时用 `ObjectProvider<WorkflowService>` 或条件装配。

## 推荐步骤

1. **业务表加 `status`**，取值对齐 `BusinessStatusEnum`（`draft` / `waiting` / `finish` / `cancel` / `invalid` / `back` / `termination`）。路径：`wta-vue-plus-namewta/wta-common/wta-common-core/src/main/java/org/dromara/common/core/enums/BusinessStatusEnum.java`
2. **先落库业务主键**，再把 `id.toString()` 作为 `StartProcessDTO.businessId`。空 `businessId` 会在 `StartPrepareRequestComponent` 被拒绝（「启动工作流时必须包含业务ID」）。路径：`wta-vue-plus-namewta/wta-modules/wta-workflow/src/main/java/org/dromara/workflow/liteflow/start/StartPrepareRequestComponent.java`
3. **在设计器发布 `flowCode`**。未发布会抛「流程【code】未发布」。路径：`wta-vue-plus-namewta/wta-modules/wta-workflow/src/main/java/org/dromara/workflow/liteflow/start/StartPrepareInstanceComponent.java`。节点 `formPath` 指向业务 Vue 页。
4. **业务 Service 注入 `WorkflowService`**。模块已依赖 `wta-api` 即可；Bean 由 `wta-admin` 装配 `WorkflowServiceImpl`。路径：`wta-vue-plus-namewta/wta-modules/wta-workflow/src/main/java/org/dromara/workflow/service/impl/WorkflowServiceImpl.java`
5. **提交**：先 `insertOrUpdate` 单据，再组 `StartProcessDTO(businessId, flowCode, variables)`。后端无人会话加 `variables.ignore=true`。申请人首节点一并办掉时调 `startCompleteTask`；只要实例不要办首任务时调 `startWorkFlow`。
6. **监听**：
   - `ProcessEvent`：按 `flowCode` 过滤，把 `status` 回写业务表；`submit==true` 时请假样例强制写成 `waiting`。
   - `ProcessTaskEvent`：按 `nodeCode` 做节点副作用。
   - `ProcessDeleteEvent`：按 `businessId` 清理单据。
7. **删业务数据时**调 `workflowService.deleteInstance(ids)`（字符串业务 id 列表）。删实例链路会先发 `ProcessDeleteEvent`：`wta-vue-plus-namewta/wta-modules/wta-workflow/src/main/java/org/dromara/workflow/liteflow/instance/InstanceDeleteEventComponent.java`
8. **人的待办/已办/抄送**走 `/workflow/task/pageByTaskWait` 等；业务页用 `formPath` + query `id` / `taskId` 打开，办理走 `POST /workflow/task/completeTask`。

同一 `flowCode+businessId` 启动带 `@Lock4j`：`wta-vue-plus-namewta/wta-modules/wta-workflow/src/main/java/org/dromara/workflow/service/impl/FlwTaskServiceImpl.java`。不要并行对同一单据重复启动。

## 后台办理 vs 人工办理

| 调用方 | 方法 | 权限 |
|---|---|---|
| 业务 Service / 定时任务 | `WorkflowService.completeTask(CompleteTaskDTO)` 或 `completeTask(taskId, message)` | 必须 `ignore=true`（后一重载已自动设置，见 `WorkflowServiceImpl`） |
| 已登录用户在待办页办理 | `POST /workflow/task/completeTask` | 走 `WorkflowPermissionHandler.permissions()`（当前用户 id） |

`startCompleteTask` 固定 `messageType=SYSTEM_MESSAGE`（站内信 `"1"`）。自定义消息类型用 `startWorkFlow` + `completeTask(CompleteTaskDTO)`。

指定下一办理人：办理变量写入 `pass:nodeCode` / `back:nodeCode`（`TaskStatusEnum` + `:` + 节点编码），由 `WorkflowGlobalListener.assignment` 覆盖下一任务权限列表。路径：`wta-vue-plus-namewta/wta-modules/wta-workflow/src/main/java/org/dromara/workflow/listener/WorkflowGlobalListener.java`；弹窗映射：`wta-vue-plus-namewta/wta-modules/wta-workflow/src/main/java/org/dromara/workflow/liteflow/complete/CompletePrepareComponent.java`

## 状态回写要点

以 `ProcessEvent.status` + `submit` 为准，对齐 `BusinessStatusEnum`。

- 申请人提交：`submit=true`（草稿/撤销/退回再次办理时 `CompletePrepareComponent` 写入 `SUBMIT`），样例把业务状态写成 `waiting`。
- 普通办理：写 `processEvent.getStatus()`。
- 非终态且任务未结束时 `determineFlowStatus` 返回 `null`，此时没有总体 `ProcessEvent`，但仍可能有 `ProcessTaskEvent`。需要节点副作用时监听任务事件，不要假设每次办理都有 `ProcessEvent`。
- 监听器用默认 `@EventListener`（与发布同线程），不是事务同步监听。

事件类型路径：`wta-vue-plus-namewta/wta-api/src/main/java/org/dromara/workflow/api/event/`
发布：`wta-vue-plus-namewta/wta-modules/wta-workflow/src/main/java/org/dromara/workflow/handler/FlowProcessEventHandler.java`

## 表单页

在流程定义上配置 `formPath` 为业务路由。待办跳转：

```ts
// plus-ui-namewta/packages/web-domains/workflow/src/task/TaskListPage.vue
router.push({
  path: routerJumpVo.formPath,
  query: { id: routerJumpVo.businessId, type: routerJumpVo.type, taskId: routerJumpVo.taskId }
});
```

业务页用 `id` 调自己的 CRUD，用 `taskId` 调 `POST /workflow/task/completeTask`。不要在本模块找表单设计 CRUD。当前请假领域页位于 `plus-ui-namewta/packages/web-domains/workflow/src/leave/`；Admin 通过 workflow manifest 装配页面。

## 可选装配

`WorkflowServiceImpl` 带 `@ConditionalOnEnable`。`warm-flow.enabled=false` 时不要假设 Bean 存在。关闭后的启动失败形态未实测；需要关闭工作流仍能启动业务模块时，用 `ObjectProvider` 或 `@Autowired(required=false)` 并处理空引用。路径：`wta-vue-plus-namewta/wta-modules/wta-workflow/src/main/java/org/dromara/workflow/common/ConditionalOnEnable.java`
