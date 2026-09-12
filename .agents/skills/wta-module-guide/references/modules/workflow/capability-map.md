# wta-workflow 能力地图

条目描述不够明确时，按该条目给出的仓库路径读取源码确认，不得凭空补类型、方法、REST 或事件字段。`org.dromara.warm.flow.*` 是引擎代码，不是本模块自有包。路径相对工作区，前缀 `backend/`（磁盘目录名大小写可能显示为 `WTA-Plus-namewta/`）。

## 目录

- [模块身份与依赖](#模块身份与依赖)
- [包结构](#包结构)
- [公开合同 wta-api](#公开合同-wta-api)
- [内部 IFlw 服务](#内部-iflw-服务)
- [REST 人机入口](#rest-人机入口)
- [开关与装配](#开关与装配)
- [LiteFlow 编排](#liteflow-编排)
- [事件分层](#事件分层)
- [办理人三层](#办理人三层)
- [状态、表单与历史](#状态表单与历史)

## 模块身份与依赖

| 能力 | 说明 | 路径 |
|---|---|---|
| 业务模块，非独立应用 | Maven `artifactId=wta-workflow`，`<description>` 为「工作流模块」。由 `wta-modules` 聚合，仅 `wta-admin` 作为运行时依赖装配进主应用。 | `backend/wta-modules/wta-workflow/pom.xml`；`backend/wta-modules/pom.xml`（`<module>wta-workflow</module>`）；`backend/wta-admin/pom.xml`（`artifactId=wta-workflow`） |
| 引擎与编排版本 | Warm-Flow `1.8.9`（根 POM `warm-flow.version`）；LiteFlow `2.16.1.2`（根 POM `liteflow.version`），模块经 `wta-common-liteflow` 引入。官方文档入口见根 POM 注释 http://warm-flow.cn/ | `backend/pom.xml`；`backend/wta-modules/wta-workflow/pom.xml` |
| 直接依赖 | `wta-common-push`、`wta-common-doc`、`wta-common-mail`、`wta-common-sms`、`wta-common-mybatis`、`wta-common-web`、`wta-common-log`、`wta-common-excel`、`wta-common-translation`、`wta-common-security`、`wta-api`、`warm-flow-mybatis-plus-sb4-starter`、`warm-flow-plugin-ui-sb-web`、`wta-common-liteflow`。不依赖 `wta-system` 源码模块。 | `backend/wta-modules/wta-workflow/pom.xml` |
| 反向消费 system | 办理人数据经 `org.namewta.system.api.*`（`TaskAssigneeService` / `UserService` / `DeptService` / `RoleService` / `PostService`）。system 不反向依赖 workflow。 | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/service/impl/FlwTaskAssigneeServiceImpl.java`；`backend/wta-api/src/main/java/org/namewta/system/api/TaskAssigneeService.java` |
| 其他业务模块现状 | `wta-demo` / `wta-ai` / `wta-job` 源码无 `WorkflowService` / `org.namewta.workflow` 引用。需要向用户发送流程消息时统一提交 `NotificationCommand`，由 Notify 收件箱按 `workflow` 路径归类。 | `backend/wta-api/src/main/java/org/namewta/notify/api/NotificationApplicationService.java`；`backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/adapter/inapp/InAppNotificationPortImpl.java` |

其他业务模块不要 Maven 依赖 `wta-workflow`；编译期通过 `wta-api` 引用公开合同，运行时由 `wta-admin` 提供 Bean。

## 包结构

源码根：`backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/`

| 包 | 职责 | 路径 |
|---|---|---|
| `controller` | REST，全部 `@RequestMapping("/workflow/...")` 且 `@ConditionalOnEnable` | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/controller/` |
| `service` / `service.impl` | 模块门面与实现；`WorkflowServiceImpl` 实现公开合同 | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/service/` |
| `domain` / `domain.bo` / `domain.vo` / `domain.context` | 实体、BO/VO、LiteFlow 上下文 | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/domain/` |
| `mapper` | 待办/已办等查询封装（部分查询是 Java default 方法，不是 XML） | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/mapper/` |
| `listener` | `WorkflowGlobalListener`（占用 Warm-Flow `GlobalListener`）、`WorkflowSideEffectListener` | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/listener/` |
| `handler` | 事件发布、权限、异常、流程图扩展 | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/handler/` |
| `event` | 模块内副作用事件（非 `wta-api`） | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/event/` |
| `liteflow.start` / `complete` / `operation` / `instance` | 启动/办理/任务操作/删实例编排组件 | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/liteflow/` |
| `rule` | SpEL 规则组件 | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/rule/` |
| `config` | `WarmFlowConfig` 空配置入口 | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/config/WarmFlowConfig.java` |
| `common` / `common.enums` / `common.constant` | 开关注解、枚举、常量 | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/common/` |

资源：

- Mapper XML：`backend/wta-modules/wta-workflow/src/main/resources/mapper/workflow/`（`FlwTaskMapper.xml`、`FlwHisTaskMapper.xml`、`FlwInstanceMapper.xml`、`FlwInstanceBizExtMapper.xml`、`FlwUserMapper.xml`、`FlwCategoryMapper.xml`、`FlwSpelMapper.xml`、`TestLeaveMapper.xml`）
- LiteFlow：`backend/wta-modules/wta-workflow/src/main/resources/liteflow/task-chain.el.xml`、`instance-chain.el.xml`

没有名为 `IFlwFormService` 或 `IFlwHisService` 的类型。历史能力拆在任务已办分页与实例轨迹；表单只作为 `formCustom` / `formPath` 透出。

## 公开合同 wta-api

唯一推荐 Java 门面：`backend/wta-api/src/main/java/org/namewta/workflow/api/WorkflowService.java`
实现：`backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/service/impl/WorkflowServiceImpl.java`（`@ConditionalOnEnable` + `@Service`）

| 方法 | 行为（以接口 JavaDoc + 实现为准） |
|---|---|
| `deleteInstance(List<String> businessIds)` | 转 `IFlwInstanceService.deleteByBusinessIds` |
| `getBusinessStatusByTaskId(Long taskId)` | 任务所属实例的 `flowStatus`，没有则空串 |
| `getBusinessStatus(String businessId)` | 按业务 id 查实例状态，没有则空串 |
| `setVariable(Long instanceId, Map)` / `instanceVariable(Long instanceId)` | 写/读实例变量 |
| `getInstanceIdByBusinessId(String businessId)` | 业务 id → 实例 id，没有则 `null` |
| `startWorkFlow(StartProcessDTO)` | Bean 拷到 `StartProcessBo` 后调 `IFlwTaskService.startWorkFlow`，返回 `StartProcessReturnDTO` |
| `completeTask(CompleteTaskDTO)` | Bean 拷到 `CompleteTaskBo` 后办理；后台无人会话须 `variables.put("ignore", true)` |
| `completeTask(Long taskId, String message)` | 便捷办理，内部自动 `ignore=true` |
| `startCompleteTask(StartProcessDTO)` | `@Transactional`：先 `startWorkFlow`，再办理首任务；`messageType` 固定为 `MessageTypeEnum.SYSTEM_MESSAGE`（`"1"` 站内信） |

DTO（`backend/wta-api/src/main/java/org/namewta/workflow/api/domain/`）：

| 类型 | 路径 | 关键字段 |
|---|---|---|
| `StartProcessDTO` | `backend/wta-api/src/main/java/org/namewta/workflow/api/domain/StartProcessDTO.java` | `businessId`、`flowCode`、`handler`、`variables`、`bizExt`；`getVariables()` 剔空值并懒创建 Map |
| `CompleteTaskDTO` | `backend/wta-api/src/main/java/org/namewta/workflow/api/domain/CompleteTaskDTO.java` | `taskId`、`fileId`、`flowCopyList`、`messageType`、`message`、`notice`、`handler`、`variables`、`ext` |
| `StartProcessReturnDTO` | `backend/wta-api/src/main/java/org/namewta/workflow/api/domain/StartProcessReturnDTO.java` | record：`processInstanceId`、`taskId` |
| `FlowInstanceBizExtDTO` | `backend/wta-api/src/main/java/org/namewta/workflow/api/domain/FlowInstanceBizExtDTO.java` | `id`、`instanceId`、`businessId`、`businessCode`、`businessTitle` |
| `FlowCopyDTO` | `backend/wta-api/src/main/java/org/namewta/workflow/api/domain/FlowCopyDTO.java` | record：`userId`、`nickName` |

公开事件（`org.namewta.workflow.api.event`）：

| 类型 | 路径 | 字段 |
|---|---|---|
| `ProcessEvent` | `backend/wta-api/src/main/java/org/namewta/workflow/api/event/ProcessEvent.java` | `flowCode`、`instanceId`、`businessId`、`nodeType`（注释：0 开始 / 1 中间 / 2 结束 / 3 互斥 / 4 并行）、`nodeCode`、`nodeName`、`status`、`params`、`submit`（`true` 表示申请人节点办理） |
| `ProcessTaskEvent` | `backend/wta-api/src/main/java/org/namewta/workflow/api/event/ProcessTaskEvent.java` | 另含 `taskId`；无 `submit` |
| `ProcessDeleteEvent` | `backend/wta-api/src/main/java/org/namewta/workflow/api/event/ProcessDeleteEvent.java` | 仅 `flowCode` + `businessId` |

发布者：`backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/handler/FlowProcessEventHandler.java`（`SpringUtils.context().publishEvent`）。业务侧用 `@EventListener(condition="#processEvent.flowCode=='xxx'")`；当前不是 `@TransactionalEventListener`。

## 内部 IFlw 服务

目录：`backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/service/`
其他业务模块不要直接 import。

| 接口 | 方法要点 | 路径 |
|---|---|---|
| `IFlwDefinitionService` | `queryList`、`unPublishList`、`publish`、`exportDef`、`importJson`、`removeDef` | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/service/IFlwDefinitionService.java` |
| `IFlwInstanceService` | `selectRunningInstanceList`、`selectFinishInstanceList`、`selectCurrentInstanceList`、`queryByBusinessId`、`selectInstByBusinessId`、`deleteByBusinessIds` / `deleteByInstanceIds` / `deleteHisByInstanceIds`、`cancelProcessApply`、`flowHisTaskList`、`updateStatus`、`instanceVariable` / `updateVariable` / `setVariable`、`processInvalid` | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/service/IFlwInstanceService.java` |
| `IFlwTaskService` | `startWorkFlow`、`completeTask`、`setCopy`、`pageByTaskWait` / `pageByTaskFinish` / `pageByAllTaskWait` / `pageByAllTaskFinish` / `pageByTaskCopy`、`updateAssignee`、`backProcess`、`getBackTaskNode`、`terminationTask`、`selectById`、`getNextNodeList`、`taskOperation`、`urgeTask`、`isTaskEnd` | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/service/IFlwTaskService.java` |
| `IFlwCommonService` | `sendMessage`（多载）、`sendResultMessage`、`applyNodeCode` | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/service/IFlwCommonService.java` |
| `IFlwTaskAssigneeService` | `fetchUsersByStorageIds` | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/service/IFlwTaskAssigneeService.java` |
| `IFlwNodeExtService` | `parseNodeExt`：按钮权限 / 抄送 / 变量 | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/service/IFlwNodeExtService.java` |
| `IFlwCategoryService` | 分类 CRUD / 树 / 名称 / 校验 | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/service/IFlwCategoryService.java` |
| `IFlwSpelService` | SpEL 定义 CRUD、`selectSpelByTaskAssigneeList`、`selectRemarksBySpels` | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/service/IFlwSpelService.java` |
| `ITestLeaveService` | 请假示例：`queryById` / `insertByBo` / `submitAndFlowStart` / `updateByBo` / `deleteWithValidByIds`。不是公共门面 | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/service/ITestLeaveService.java` |

定义管理控制器同时直接注入 Warm-Flow `DefService`：`backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/controller/FlwDefinitionController.java`。实例激活/挂起走 `InsService`：`FlwInstanceController.active`（`PUT /workflow/instance/active/{id}?active=`）。这是分层选择，不是第二套公开 API。

任务操作编码：`backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/common/enums/TaskOperationEnum.java` — `delegateTask` / `transferTask` / `addSignature` / `reductionSignature`。

## REST 人机入口

六个控制器均带 `@ConditionalOnEnable`。前端工作流页走这些 HTTP，不是 `WorkflowService`。

### `/workflow/definition` — `FlwDefinitionController`

路径：`backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/controller/FlwDefinitionController.java`

| 方法 | HTTP | 权限注解 |
|---|---|---|
| 已发布列表 | `GET /list` | `workflow:definition:list` |
| 未发布列表 | `GET /unPublishList` | `workflow:definition:list` |
| 详情 | `GET /{id}` | `workflow:definition:query` |
| 新增 | `POST` | `workflow:definition:add` |
| 修改 | `PUT` | `workflow:definition:edit` |
| 发布 / 取消发布 | `PUT /publish/{id}`、`PUT /unPublish/{id}` | `workflow:definition:publish` |
| 删除 | `DELETE /{ids}` | `workflow:definition:remove` |
| 复制 | `POST /copy/{id}` | `workflow:definition:copy` |
| 导入 | `POST /importDef` | `workflow:definition:import` |
| 导出 | `POST /exportDef/{id}` | `workflow:definition:export` |
| XML 字符串 | `GET /xmlString/{id}` | `workflow:definition:query` |
| 激活 | `PUT /active/{id}` | `workflow:definition:active` |

大量方法直接调 `DefService`。

### `/workflow/instance` — `FlwInstanceController`

路径：`backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/controller/FlwInstanceController.java`

| 方法 | HTTP |
|---|---|
| 运行中 / 已结束 / 我的发起 | `GET /pageByRunning`、`GET /pageByFinish`、`GET /pageByCurrent` |
| 按业务 id 详情 | `GET /getInfo/{businessId}` |
| 删实例 | `DELETE /deleteByBusinessIds/{businessIds}`、`DELETE /deleteByInstanceIds/{instanceIds}`、`DELETE /deleteHisByInstanceIds/{instanceIds}` |
| 撤销 | `PUT /cancelProcessApply` |
| 激活或挂起 | `PUT /active/{id}?active=`（`true` → `insService.active`，`false` → `insService.unActive`） |
| 历史轨迹 | `GET /flowHisTaskList/{businessId}` |
| 变量 | `GET /instanceVariable/{instanceId}`、`PUT /updateVariable` |
| 作废 | `POST /invalid` |

### `/workflow/task` — `FlwTaskController`

路径：`backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/controller/FlwTaskController.java`

| 方法 | HTTP | 权限 |
|---|---|---|
| 启动 | `POST /startWorkFlow` | 无 `@SaCheckPermission` |
| 办理 | `POST /completeTask` | 无 `@SaCheckPermission` |
| 当前用户待办 | `GET /pageByTaskWait` | 无 list 权限 |
| 当前用户已办 | `GET /pageByTaskFinish` | |
| 全部待办 | `GET /pageByAllTaskWait` | `workflow:task:list` |
| 全部已办 | `GET /pageByAllTaskFinish` | `workflow:task:list` |
| 抄送 | `GET /pageByTaskCopy` | |
| 任务详情 | `GET /getTask/{taskId}` | |
| 下一节点 | `POST /getNextNodeList` | |
| 终止 | `POST /terminationTask` | |
| 委派/转办/加减签 | `POST /taskOperation/{taskOperation}` | |
| 改办理人 | `PUT /updateAssignee/{userId}` | `workflow:task:edit` |
| 驳回 | `POST /backProcess` | |
| 可驳回节点 | `GET /getBackTaskNode/{taskId}/{nowNodeCode}` | |
| 当前办理人 | `GET /currentTaskAllUser/{taskId}` | |
| 催办 | `POST /urgeTask` | `workflow:task:edit` |

当前用户待办：`GET /workflow/task/pageByTaskWait` → `IFlwTaskService.pageByTaskWait`（`LoginHelper.getUserIdStr()`）→ `FlwTaskMapper.getListRunTask`。路径：`backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/mapper/FlwTaskMapper.java`。SQL 语义：`nodeType` 为中间节点（`NodeType.BETWEEN`）+ `flow_user.type in ("1","2","3")` + `processedBy=当前用户` + 实例状态 `waiting`；`formPath` 用 `COALESCE(NULLIF(TRIM(t.form_path), ''), NULLIF(TRIM(d.form_path), ''))`。全部待办把 `userId` 传 `null`。抄送查询 `flow_user.type = "4"`。

`flow_user.type` 对照本模块枚举：`backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/common/enums/TaskAssigneeType.java` — `"1"` 审批人、`"2"` 转办人、`"3"` 委托人、`"4"` 抄送人。与 Warm-Flow jar 内部 `TaskAssigneeType` 是否逐值等同，未反编译验证。

### 其他控制器

| 前缀 | 控制器 | 路径 | HTTP 要点 |
|---|---|---|---|
| `/workflow/category` | `FlwCategoryController` | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/controller/FlwCategoryController.java` | `GET /list`、`POST /export`、`GET /{categoryId}`、`POST`、`PUT`、`DELETE /{categoryId}`、`GET /categoryTree` |
| `/workflow/spel` | `FlwSpelController` | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/controller/FlwSpelController.java` | `GET /list`、`GET /{id}`、`POST`、`PUT`、`DELETE /{ids}` |
| `/workflow/leave` | `TestLeaveController` | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/controller/TestLeaveController.java` | 见 [leave-sample.md](leave-sample.md)；提交入口 `POST /submitAndFlowStart` |

Warm-Flow UI 插件自带 `/warm-flow*` 控制器来自第三方 jar，本仓库无源码。安全排除：`backend/wta-admin/src/main/resources/application.yml` 含 `/warm-flow-ui/config` 与 `/warm-flow/save-json`。

## 开关与装配

| 能力 | 说明 | 路径 |
|---|---|---|
| `@ConditionalOnEnable` | `@ConditionalOnProperty(value="warm-flow.enabled", havingValue="true")` | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/common/ConditionalOnEnable.java` |
| 默认开启 | `warm-flow.enabled: true`，另有 `ui: true`、`top-text-show: true`、`node-tooltip: true`、`token-name: ${sa-token.token-name},clientid`；LiteFlow `enable: ${warm-flow.enabled:true}`，`rule-source: classpath:liteflow/*.el.xml` | `backend/wta-admin/src/main/resources/application.yml` |
| 配置入口 | 空 `@Configuration`，仅作启用入口 | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/config/WarmFlowConfig.java` |
| 关闭后果 | `WorkflowService` Bean 不存在（实现类带 `@ConditionalOnEnable`）；其他模块硬注入会启动失败。可选接入用 `ObjectProvider` 或条件装配。 | `WorkflowServiceImpl` |
| 例外 | `FlowExceptionHandler` 未加 `@ConditionalOnEnable`，关闭工作流时该 advice 仍可能注册。捕获 `FlowException` 返回 `R.fail` | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/handler/FlowExceptionHandler.java` |

## LiteFlow 编排

启动/办理/任务操作/删实例是模块内 LiteFlow，不是业务可插拔 SPI。不要为接入审批新增 chain 组件。

| Chain | 资源 | 组件类（均在 `liteflow/` 下） | 入口 |
|---|---|---|---|
| `startProcessChain` | `backend/wta-modules/wta-workflow/src/main/resources/liteflow/task-chain.el.xml` | `start/StartPrepareRequestComponent.java`（`startPrepareRequest`）、`StartExistsComponent`、`StartResumeComponent`、`StartPrepareInstanceComponent`、`StartExecuteComponent` | `FlwTaskServiceImpl.startWorkFlow`：`@Lock4j(keys={"#startProcessBo.flowCode + #startProcessBo.businessId"})` |
| `completeTaskChain` | 同上 | `complete/CompletePrepareComponent.java`、`CompleteExecuteComponent`、`CompleteNeedAutoPassComponent`、`CompleteAutoPassComponent` | `FlwTaskServiceImpl.completeTask`：`@Lock4j(keys={"#completeTaskBo.taskId"})` |
| `taskOperationChain` | 同上 | `operation/TaskOpPrepareComponent.java`、`TaskOpLoadComponent`、`TaskOpExecuteComponent`、`TaskOpNeedNotifyComponent`、`TaskOpNotifyComponent` | 委派/转办/加减签 |
| `deleteInstanceChain` | `backend/wta-modules/wta-workflow/src/main/resources/liteflow/instance-chain.el.xml` | `instance/InstanceDeleteLoadComponent.java`、`InstanceDeleteExistsComponent`、`InstanceDeleteEventComponent`、`InstanceDeleteExecuteComponent` | 删实例前发 `ProcessDeleteEvent` |

启动绑定键是字符串 `businessId`。`StartPrepareRequestComponent` 校验非空（空则「启动工作流时必须包含业务ID」），写入变量 `initiator` / `initiatorDeptId` / `businessId`，再按 `FlowInstance.businessId` 查是否已有实例。路径：`backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/liteflow/start/StartPrepareRequestComponent.java`。

新实例：`StartExecuteComponent` 调 `insService.start(businessId, flowParams)`，`flowStatus=draft`，扩展写入 `flow_instance_biz_ext`。路径：`backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/liteflow/start/StartExecuteComponent.java`。已有实例走 `StartResumeComponent`。`flowCode` 必须已发布，否则 `StartPrepareInstanceComponent` 抛「流程【code】未发布」。

`businessCode` 默认时间戳，源码有 `TODO: 按照自己业务规则生成编号`：`backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/liteflow/start/StartPrepareInstanceComponent.java`。

办理：`CompleteExecuteComponent` 把 `variables.ignore` / `ignoreDepute` / `ignoreCooperate` 传给 `FlowParams`，`taskService.skip` 跳转，实例状态写成 `waiting`，历史状态 `pass`。路径：`backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/liteflow/complete/CompleteExecuteComponent.java`。草稿/撤销/退回再次办理时 `CompletePrepareComponent` 写入 `submit=true`。弹窗指定下一办理人写入 `pass:nodeCode` / `back:nodeCode`（`TaskStatusEnum.PASS/BACK` + `:` + 节点编码），由全局监听器 `assignment` 消费。

## 事件分层

**给业务模块的回调（订阅这些）：** `org.namewta.workflow.api.event.*`。触发点：`WorkflowGlobalListener.finish`（提交、状态变化、下一任务创建）；`InstanceDeleteEventComponent`（删实例前）。`create()` 在 `WorkflowGlobalListener` 中为空。不要再实现 Warm-Flow `GlobalListener`。

路径：`backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/listener/WorkflowGlobalListener.java`

状态回写链路：Warm-Flow 任务 finish → `WorkflowGlobalListener.finish` → `determineFlowStatus` → `FlowProcessEventHandler.processHandler` → 业务 `@EventListener`。申请人提交（变量 `SUBMIT==true`）与普通办理分开发布。退回到申请人节点会额外再发 `back` 并改实例状态。`determineFlowStatus`：实例已是终态（`BusinessStatusEnum.initialState`：cancel/back/invalid/termination）则返回实例状态；否则若无剩余任务则更新为 `finish`；非终态且任务未结束返回 `null`，此时不发总体 `ProcessEvent`，但仍可能发 `ProcessTaskEvent`。

**模块内副作用（业务通常不订阅）：**

| 事件 | 路径 | 处理 |
|---|---|---|
| `WorkflowCopyEvent` | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/event/WorkflowCopyEvent.java` | 抄送落库 |
| `WorkflowTaskMessageEvent` | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/event/WorkflowTaskMessageEvent.java` | 待办消息 |
| `WorkflowResultMessageEvent` | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/event/WorkflowResultMessageEvent.java` | 结果消息 |

处理者：`backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/listener/WorkflowSideEffectListener.java`。消息通道：`backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/common/enums/MessageTypeEnum.java` — `"1"` 站内信、`"2"` 邮箱、`"3"` 短信。不要在业务侧重复发待办消息。

## 办理人三层

不要让业务模块自己写 `PermissionHandler`。

1. **设计器选人**：`FlwTaskAssigneeServiceImpl` 同时实现 Warm-Flow `HandlerSelectService`。tabs 为用户/角色/部门/岗位/SpEL。路径：`backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/service/impl/FlwTaskAssigneeServiceImpl.java`；前缀枚举 `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/common/enums/TaskAssigneeEnum.java`（`role:` / `dept:` / `post:`；用户无前缀；`$`/`#` 开头视为 SpEL）。用户数据来自 `backend/wta-api/src/main/java/org/namewta/system/api/TaskAssigneeService.java`（实现 `backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysTaskAssigneeServiceImpl.java`）。`getUsersByType(SPEL)` 返回空列表，SpEL 不走用户批量查询。
2. **运行时权限**：`WorkflowPermissionHandler` 实现 Warm-Flow `PermissionHandler`。`permissions()` / `getHandler()` 返回登录用户 id；`convertPermissions` 把 storageId 展开成用户 id 列表。路径：`backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/handler/WorkflowPermissionHandler.java`
3. **流转指定人**：`WorkflowGlobalListener.assignment` 读取 `pass:nodeCode` / `back:nodeCode` 覆盖下一任务 `permissionList`；申请节点强制加入发起人 `createBy`。路径：`backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/listener/WorkflowGlobalListener.java`。SpEL 规则：`selectDeptLeaderById` 经 `DeptService.selectDeptLeaderById`。路径：`backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/rule/SpelRuleComponent.java`

## 状态、表单与历史

业务状态枚举在 common-core，不是 workflow 模块私有：`backend/wta-common/wta-common-core/src/main/java/org/namewta/common/core/enums/BusinessStatusEnum.java` — `draft` / `waiting` / `finish` / `cancel` / `invalid` / `back` / `termination`。业务表 `status` 与 `ProcessEvent.status` 对齐该枚举。另有启动/撤销/驳回/作废校验方法（`checkStartStatus` 等）。

任务历史状态：`backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/common/enums/TaskStatusEnum.java` — 含 `pass` / `back` / `waiting` / `cancel` / `invalid` / `termination` 等。

表单是路由元数据，不是独立表单引擎。`FlowTaskVo` 带 `formCustom`、`formPath`：`backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/domain/vo/FlowTaskVo.java`。定义/历史任务 VO 同样带这两字段。待办 SQL 用 `COALESCE` 选任务或定义的 `form_path`。前端 workflow web-domain 把 `formPath` 当 Vue 路由，query 带 `id`（业务 `businessId`）、`type`、`taskId`：`frontend/packages/web-domains/workflow/src/task/TaskListPage.vue` 与 `src/instance/{InstancePage,MyDocumentPage}.vue`。没有 App API 门面，也没有表单设计 CRUD Controller。

历史：实例轨迹 `IFlwInstanceService.flowHisTaskList` + REST `GET /workflow/instance/flowHisTaskList/{businessId}`；已办分页 `pageByTaskFinish`（`FlwHisTaskMapper.getListFinishTask`：`backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/mapper/FlwHisTaskMapper.java`）。不要虚构额外的历史或表单 API。

流程图悬浮提示：`FlwChartExtServiceImpl` 实现 Warm-Flow `ChartExtService`：`backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/service/impl/FlwChartExtServiceImpl.java`。
