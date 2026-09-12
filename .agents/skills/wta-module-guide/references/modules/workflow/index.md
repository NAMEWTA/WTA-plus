# wta-workflow 模块索引

`wta-workflow` 是当前存量 Warm-Flow 模块。本轮保持其内部 `IFlw*`、LiteFlow chain、Warm-Flow handler 和 REST 实现不变；其他业务模块只通过 `wta-api` 的 `WorkflowService` 和公开事件接入。

## 何时读取

| 任务 | 继续读取 |
|---|---|
| 模块结构、公开合同、REST 和事件能力 | [capability-map.md](capability-map.md) |
| 新业务模块启动流程、办理、状态回写、删除和可选开关 | [integration-guide.md](integration-guide.md) |
| 复制仓库内请假流程接入样例 | [leave-sample.md](leave-sample.md) |
| Profile 中 Workflow 接入 | [../profile/index.md](../profile/index.md) |

## 跨模块最小面

- 业务模块 POM 只依赖 `wta-api`，不依赖 `wta-workflow`。
- 注入 `org.namewta.workflow.api.WorkflowService`；需要回写业务状态时订阅 `ProcessEvent`、`ProcessTaskEvent`、`ProcessDeleteEvent`。
- 待办、已办和抄送走 `/workflow/task/*`，业务模块不得重查 `flow_task`/`flow_user`。
- 不要实现 Warm-Flow `GlobalListener`、`PermissionHandler`，也不要复制工作流内部 LiteFlow chain。
- `WorkflowService` 受 `warm-flow.enabled` 条件控制；可选接入用 `ObjectProvider` 或条件装配处理 Bean 缺失。

## 边界提醒

流程表单只是定义上的 `formPath` 路由元数据，不是业务模块可调用的表单引擎。`businessId` 必须先对应已落库业务主键；流程状态应与 `BusinessStatusEnum` 对齐。具体字段和实现路径以 capability/integration reference 及源码为准。
