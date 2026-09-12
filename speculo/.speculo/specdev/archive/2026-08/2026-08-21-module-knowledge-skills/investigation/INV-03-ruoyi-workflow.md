---
artifact: wayfinder-ticket
id: INV-03
name: ruoyi-workflow 对外能力与业务接入
parent_map: <Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/wayfinder-map.md</Path>
label: wayfinder:research
status: closed
blocked_by: []
resolution: answered
---

# ruoyi-workflow 对外能力与业务接入

## 问题

ruoyi-workflow 对外公共服务能力是什么，业务模块如何接入？

## Research: ruoyi-workflow 对外能力与业务接入范式

- Decision / target: 为后续在 `<Path>.agents/skills/ruoyi-workflow-module-guide/</Path>` 编写 Skill 提供证据化能力地图；本文件为唯一 owning artifact。
- Scope / version: `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/</Path>` 当前工作树；引擎 Warm-Flow `1.8.9`（`<Path>ruoyi-vue-plus-namewta/pom.xml</Path>` `warm-flow.version`）；编排 LiteFlow `2.16.1.2`。对照公共合同 `<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/workflow/</Path>`，以及 demo/system/ai 是否实际引用。
- Stop condition: 其他模块如何启动流程、办理任务、查待办、绑定 `businessId` 的代码路径已落到真实文件；REST 与内部 Service 边界已区分；未知项单列。未创建 Skill、未改 status。

### R-001

- Claim: `ruoyi-workflow` 是 Warm-Flow 工作流业务模块，不是独立可部署应用。Maven `artifactId=ruoyi-workflow`，描述为「工作流模块」。父模块 `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/pom.xml</Path>` 聚合它；仅 `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/pom.xml</Path>` 把它作为运行时依赖装配进主应用。
- Type: code fact
- Source: `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/pom.xml</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/pom.xml</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-admin/pom.xml</Path>`
- Confidence: high
- Limits: 未运行 Maven 依赖树；只读 POM。
- Artifact impact: Skill 应写「业务模块通过 `ruoyi-api` 注入合同，不要 Maven 依赖 `ruoyi-workflow`」。

### R-002

- Claim: 模块 POM 的直接依赖是：`ruoyi-common-push`、`ruoyi-common-doc`、`ruoyi-common-mail`、`ruoyi-common-sms`、`ruoyi-common-mybatis`、`ruoyi-common-web`、`ruoyi-common-log`、`ruoyi-common-excel`、`ruoyi-common-translation`、`ruoyi-common-security`、`ruoyi-api`、`warm-flow-mybatis-plus-sb4-starter`、`warm-flow-plugin-ui-sb-web`、`ruoyi-common-liteflow`。不依赖 `ruoyi-system` 源码模块，通过 `org.dromara.system.api.*` 反向消费用户/部门/角色/岗位/办理人数据。
- Type: code fact
- Source: `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/pom.xml</Path>`；`<Path>ruoyi-vue-plus-namewta/pom.xml</Path>`（`warm-flow.version=1.8.9`、BOM 声明两个 Warm-Flow artifact）；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/impl/FlwTaskAssigneeServiceImpl.java</Path>`
- Confidence: high
- Limits: Warm-Flow starter 传递依赖未展开。官方文档入口在 POM 注释：<Url>http://warm-flow.cn/</Url>
- Artifact impact: Skill 依赖方向应写成 workflow → `ruoyi-api`/`common-*`/Warm-Flow，system 只提供办理人查询，不反向依赖 workflow。

### R-003

- Claim: 源码包全部位于 `org.dromara.workflow`，按职责分层：`controller`（REST）、`service`/`service.impl`（门面与实现）、`domain`/`domain.bo`/`domain.vo`/`domain.context`（实体与 LiteFlow 上下文）、`mapper`、`listener`、`handler`、`event`（模块内副作用事件）、`liteflow.start|complete|operation|instance`、`rule`、`config`、`common`/`common.enums`/`common.constant`。资源：`src/main/resources/mapper/workflow/*.xml`、`src/main/resources/liteflow/*.el.xml`。
- Type: code fact
- Source: `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/</Path>` 目录扫描（约 120 个文件）
- Confidence: high
- Limits: 未逐文件注释；包名以 Java 源码为准。
- Artifact impact: Skill 模块地图按上述包拆 references，不要把 Warm-Flow `org.dromara.warm.flow.*` 写成本模块自有代码。

### R-004

- Claim: 跨模块公开合同不在 `ruoyi-workflow` 内，而在 `ruoyi-api`：`org.dromara.workflow.api.WorkflowService`。方法面：`deleteInstance`、`getBusinessStatusByTaskId`、`getBusinessStatus`、`setVariable`、`instanceVariable`、`getInstanceIdByBusinessId`、`startWorkFlow`、`completeTask(CompleteTaskDTO)`、`completeTask(taskId, message)`（自动 `variables.ignore=true`）、`startCompleteTask`（启动后立即办理首任务）。DTO：`StartProcessDTO`（`businessId`/`flowCode`/`handler`/`variables`/`bizExt`）、`CompleteTaskDTO`、`StartProcessReturnDTO(processInstanceId, taskId)`、`FlowInstanceBizExtDTO`、`FlowCopyDTO`。
- Type: code fact
- Source: `<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/workflow/api/WorkflowService.java</Path>`；同目录 `domain/`；实现 `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/impl/WorkflowServiceImpl.java</Path>`
- Confidence: high
- Limits: 接口 JavaDoc 写明后台办理需 `completeTask.getVariables().put("ignore", true)`；`startCompleteTask` 固定 `messageType=SYSTEM_MESSAGE`。
- Artifact impact: Skill 对外 API 以 `WorkflowService` 为唯一推荐门面；`IFlw*` 视为模块内部。

### R-005

- Claim: 模块内部服务接口（不在 `ruoyi-api`，其他业务模块不应直接 import）：`IFlwDefinitionService`（定义列表/未发布列表/发布/导入导出/删除）、`IFlwInstanceService`（运行/结束/我的发起、按 businessId 查实例、删实例、撤销、作废、变量、历史轨迹）、`IFlwTaskService`（启动/办理/待办已办抄送/改办理人/驳回/终止/下一节点/委派转办加减签/催办）、`IFlwCommonService`（消息与申请人节点编码）、`IFlwTaskAssigneeService`（按 storageId 解析用户）、`IFlwNodeExtService`（节点扩展 JSON：按钮权限/抄送/变量）、`IFlwCategoryService`、`IFlwSpelService`、`ITestLeaveService`（请假示例，不是公共门面）。没有名为 `IFlwFormService` 或 `IFlwHisService` 的类型。
- Type: code fact
- Source: `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/</Path>` 全部接口文件
- Confidence: high
- Limits: 历史任务能力拆在 `IFlwTaskService.pageByTaskFinish` 与 `IFlwInstanceService.flowHisTaskList`；表单只作为 Warm-Flow 定义/任务字段 `formCustom`/`formPath` 透出。
- Artifact impact: Skill 的「definition/instance/task/his/form/listener」应对齐本条，不要虚构独立 form/his Service。

### R-006

- Claim: REST 控制器全部 `@RequestMapping("/workflow/...")` 且标注 `@ConditionalOnEnable`。路径：`/workflow/definition`（`FlwDefinitionController`，大量方法直接调 Warm-Flow `DefService`）、`/workflow/instance`（`FlwInstanceController`，激活/挂起走 `InsService`）、`/workflow/task`（`FlwTaskController`：`startWorkFlow`、`completeTask`、`pageByTaskWait`、`pageByTaskFinish`、`pageByAllTaskWait`、`pageByAllTaskFinish`、`pageByTaskCopy`、`taskOperation/{taskOperation}`、`backProcess`、`urgeTask` 等）、`/workflow/category`、`/workflow/spel`、`/workflow/leave`（请假示例）。前端待办/已办走这些 REST，不是 `WorkflowService`。
- Type: code fact
- Source: `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/controller/</Path>` 六个控制器
- Confidence: high
- Limits: `startWorkFlow`/`completeTask` REST 未加 `@SaCheckPermission`（与 list 类接口不同）；定义/实例管理接口有权限注解。未核对菜单 SQL 是否覆盖全部 perms。
- Artifact impact: Skill 应区分「人机 REST（前端工作流页）」与「业务模块 Java 门面」。

### R-007

- Claim: 开关是 `warm-flow.enabled`。`@ConditionalOnEnable` = `@ConditionalOnProperty(value="warm-flow.enabled", havingValue="true")`。默认 `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application.yml</Path>`：`warm-flow.enabled: true`、`ui: true`、`top-text-show: true`、`node-tooltip: true`、`token-name: ${sa-token.token-name},clientid`。LiteFlow `enable` 跟随该开关；`rule-source: classpath:liteflow/*.el.xml`。`WarmFlowConfig` 是空 `@Configuration`，仅作启用入口。安全排除含 `/warm-flow-ui/config`；另有 `/warm-flow/save-json` 出现在后续排除段。
- Type: code fact
- Source: `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/common/ConditionalOnEnable.java</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application.yml</Path>` warm-flow / liteflow / security.excludes；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/config/WarmFlowConfig.java</Path>`
- Confidence: high
- Limits: `FlowExceptionHandler` 未加 `@ConditionalOnEnable`，关闭工作流时该 advice 仍可能注册。关闭后 `WorkflowService` Bean 不存在，其他模块若硬注入会启动失败。
- Artifact impact: Skill 必须提示：`warm-flow.enabled=false` 时不要注入 `WorkflowService`；需要可选接入时用 `ObjectProvider` 或条件装配。

### R-008

- Claim: 启动流程的业务绑定键是 `businessId`（字符串）。`StartPrepareRequestComponent` 校验非空，写入变量 `initiator`/`initiatorDeptId`/`businessId`，再按 `FlowInstance.businessId` 查是否已有实例。新实例：`StartExecuteComponent` 调用 `insService.start(businessId, flowParams)`，状态 `draft`，并把 `FlowInstanceBizExt`（`instanceId`/`businessId`/`businessCode`/`businessTitle`）写入 `flow_instance_biz_ext`。已有实例：`StartResumeComponent` 校验可启动状态、合并变量、更新扩展表，返回已有 `processInstanceId`+当前任务 id。同一 `flowCode+businessId` 用 `@Lock4j` 防并发。
- Type: code fact
- Source: `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/liteflow/start/StartPrepareRequestComponent.java</Path>`；`StartExecuteComponent.java`；`StartResumeComponent.java`；`StartPrepareInstanceComponent.java`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/impl/FlwTaskServiceImpl.java</Path>` `startWorkFlow`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/resources/liteflow/task-chain.el.xml</Path>` `startProcessChain`
- Confidence: high
- Limits: `businessCode` 生成处有 `TODO: 按照自己业务规则生成编号`，默认用时间戳。未读 Warm-Flow `InsService.start` 源码，只观察到入参是业务 id。
- Artifact impact: Skill 接入指南必须要求先落库业务主键，再把 `id.toString()` 作为 `businessId`；`flowCode` 必须对已发布定义。

### R-009

- Claim: 办理任务路径：`WorkflowService.completeTask` → `IFlwTaskService.completeTask` → LiteFlow `completeTaskChain`（`completePrepare` → `completeExecute` → 可选 `completeAutoPass`）。`CompleteExecuteComponent` 把 `variables.ignore` / `ignoreDepute` / `ignoreCooperate` 传给 `FlowParams`，`taskService.skip` 跳转，办理后实例状态写成 `waiting`，历史状态 `pass`。草稿/撤销/退回再次办理时 `completePrepare` 写入 `submit=true`。弹窗指定下一办理人写入 `pass:nodeCode` / `back:nodeCode` 变量，由全局监听器 assignment 消费。
- Type: code fact
- Source: `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/liteflow/complete/CompletePrepareComponent.java</Path>`；`CompleteExecuteComponent.java`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/listener/WorkflowGlobalListener.java</Path>` `assignment`/`processTaskPermission`
- Confidence: high
- Limits: 自动通过链路 `CompleteAutoPassComponent` 未全文展开；委派/会签忽略标志的运行时效果依赖 Warm-Flow。
- Artifact impact: 后台无人会话办理必须设 `ignore=true`；人工待办办理走 REST `/workflow/task/completeTask`。

### R-010

- Claim: 当前用户待办查询不在 `WorkflowService`。代码路径：`GET /workflow/task/pageByTaskWait` → `IFlwTaskService.pageByTaskWait` → `FlwTaskMapper.getListRunTask(..., LoginHelper.getUserIdStr())`。SQL 语义：中间节点任务 + `flow_user.type in (1,2,3)` + `processedBy=当前用户` + 实例状态 `waiting`；关联定义、实例、`flow_instance_biz_ext`，`formPath` 取任务或定义非空值。全部待办把 userId 传 `null` 且需权限 `workflow:task:list`。已办走 `FlwHisTaskMapper.getListFinishTask`。抄送走 `pageByTaskCopy`。
- Type: code fact
- Source: `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/controller/FlwTaskController.java</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/impl/FlwTaskServiceImpl.java</Path>` `pageByTaskWait`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/mapper/FlwTaskMapper.java</Path>` `getListRunTask`
- Confidence: high
- Limits: `flow_user.type` 取值 `1/2/3` 的枚举含义未在本模块常量中完整注释，需对照 Warm-Flow/`TaskAssigneeType`。
- Artifact impact: Skill 应写「待办是工作流中心页能力；业务模块一般不自己查待办表」。

### R-011

- Claim: 仓库内唯一完整业务接入样例是模块自带请假：`TestLeaveServiceImpl`。模式：(1) 先 `insertOrUpdate` `test_leave`；(2) `params.ignore=true`；(3) `StartProcessDTO.businessId=leave.id`，`flowCode` 默认 `"leave1"`；(4) `workflowService.startCompleteTask`（启动并办掉申请人首节点）；失败抛 `流程发起异常`。(5) 删除单据时 `workflowService.deleteInstance(ids)`。(6) `@EventListener(condition="#processEvent.flowCode.startsWith('leave')")` 把 `ProcessEvent.status` 回写 `test_leave.status`，提交时强制 `waiting`。(7) `ProcessTaskEvent` 仅打日志，预留按 `nodeCode` 分支。(8) `ProcessDeleteEvent` 级联删请假行。(9) `eval(leaveDays)` 供流程 SpEL 判断请假天数。HTTP：`POST /workflow/leave/submitAndFlowStart`。前端：`<Path>plus-ui-namewta/src/api/workflow/leave/index.ts</Path>`。
- Type: code fact
- Source: `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/impl/TestLeaveServiceImpl.java</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/controller/TestLeaveController.java</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/domain/TestLeave.java</Path>`（`status` 字段）；`<Path>plus-ui-namewta/src/api/workflow/leave/index.ts</Path>`
- Confidence: high
- Limits: 这是 workflow 模块内部示例，不是 `ruoyi-demo` 模块。`leave1` 流程定义内容在 SQL/设计器数据中，本次未展开定义 JSON。
- Artifact impact: Skill 业务接入 playbook 应以本类为唯一成熟样例，并写明「把该模式复制到其他业务模块，注入 `WorkflowService`，用自己的 `flowCode`」。

### R-012

- Claim: `ruoyi-demo`、`ruoyi-ai`、`ruoyi-gen`、`ruoyi-job` 源码中无 `org.dromara.workflow` / `WorkflowService` / Warm-Flow 引用。`ruoyi-system` 仅把 `"workflow"` 当作站内信分类字符串（`SysMessageServiceImpl.CATEGORY_WORKFLOW`、`SysMessageBoxVo.workflowList`），不启动流程。`ruoyi-demo`/`ruoyi-ai`/`ruoyi-system` POM 都依赖 `ruoyi-api`，因此编译期可以引用 `WorkflowService`，但当前没有实现类调用。全仓库 `import org.dromara.workflow` 仅出现在 `ruoyi-api` 与 `ruoyi-workflow`。
- Type: code fact
- Source: 对 `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/</Path>`、`ruoyi-ai/`、`ruoyi-gen/`、`ruoyi-job/`、`ruoyi-system/` 的 grep；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysMessageServiceImpl.java</Path>`；各模块 `pom.xml` 的 `ruoyi-api` 依赖
- Confidence: high
- Limits: 未搜索测试资源与 SQL 脚本中的流程定义数据；未覆盖将来新增的 NAMEWTA 业务模块。
- Artifact impact: Skill 必须写「当前没有 demo/system/ai 业务接入实例；新模块按 TestLeave + `WorkflowService` 接入」。

### R-013

- Claim: 给业务模块的状态/任务/删除回调是 Spring 应用事件，类型在 `ruoyi-api`：`ProcessEvent`（`flowCode`/`instanceId`/`businessId`/`nodeType|Code|Name`/`status`/`params`/`submit`）、`ProcessTaskEvent`（另含 `taskId`）、`ProcessDeleteEvent`（仅 `flowCode`+`businessId`）。发布者 `FlowProcessEventHandler` 用 `SpringUtils.context().publishEvent`。触发点：`WorkflowGlobalListener.finish`（提交、状态变化、下一任务创建）；`InstanceDeleteEventComponent`（删实例前发删除事件）。业务侧用 `@EventListener(condition="#processEvent.flowCode=='xxx'")` 订阅。
- Type: code fact
- Source: `<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/workflow/api/event/ProcessEvent.java</Path>` 等三个事件；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/handler/FlowProcessEventHandler.java</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/listener/WorkflowGlobalListener.java</Path>` `finish`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/liteflow/instance/InstanceDeleteEventComponent.java</Path>`
- Confidence: high
- Limits: 这些事件不是 Warm-Flow 原生 listener SPI 的对外包装全集；`create()` 在 `WorkflowGlobalListener` 中为空。事件是否事务同步（`@TransactionalEventListener`）未使用，默认 `@EventListener` 与发布同线程。
- Artifact impact: Skill 状态回写应订阅 `ProcessEvent`，不要去实现 Warm-Flow `GlobalListener`（模块已占用该扩展点）。

### R-014

- Claim: 模块内还有非 API 副作用事件：`WorkflowCopyEvent`、`WorkflowTaskMessageEvent`、`WorkflowResultMessageEvent`，由 `WorkflowSideEffectListener` 处理抄送落库与站内信/邮件/短信。消息通道 `MessageTypeEnum`：`1` 站内信、`2` 邮箱、`3` 短信。流程业务状态枚举在 common-core：`BusinessStatusEnum`（`draft`/`waiting`/`finish`/`cancel`/`invalid`/`back`/`termination`）。任务操作 `TaskOperationEnum`：`delegateTask`/`transferTask`/`addSignature`/`reductionSignature`。
- Type: code fact
- Source: `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/listener/WorkflowSideEffectListener.java</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/event/</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-core/src/main/java/org/dromara/common/core/enums/BusinessStatusEnum.java</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/common/enums/MessageTypeEnum.java</Path>`；`TaskOperationEnum.java`
- Confidence: high
- Limits: 业务模块通常不订阅这组内部事件；它们是引擎完成办理后的通知实现。
- Artifact impact: Skill 把内部消息事件与 API 回调事件分开，避免业务重复发待办消息。

### R-015

- Claim: 办理人扩展点分三层。(1) 设计器选人：`FlwTaskAssigneeServiceImpl` 同时实现 Warm-Flow `HandlerSelectService`，tabs 为用户/角色/部门/岗位/SpEL；用户数据来自 `system.api.TaskAssigneeService`（实现 `SysTaskAssigneeServiceImpl`）。storageId 形如 `123`（用户）、`role:456`、`dept:`、`post:`，或 `$`/`#` 开头 SpEL。(2) 运行时权限：`WorkflowPermissionHandler` 实现 Warm-Flow `PermissionHandler`，当前权限标识是登录用户 id，`convertPermissions` 把 storageId 展开成用户 id 列表。(3) 流转指定人：`WorkflowGlobalListener.assignment` 读取 `pass:nodeCode`/`back:nodeCode` 覆盖下一任务 `permissionList`；申请节点强制加入发起人 `createBy`。SpEL 规则组件 `SpelRuleComponent.selectDeptLeaderById` 按发起人部门查负责人。
- Type: code fact
- Source: `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/impl/FlwTaskAssigneeServiceImpl.java</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/handler/WorkflowPermissionHandler.java</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/listener/WorkflowGlobalListener.java</Path>` `assignment`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/rule/SpelRuleComponent.java</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/TaskAssigneeService.java</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysTaskAssigneeServiceImpl.java</Path>`
- Confidence: high
- Limits: `getUsersByType(SPEL)` 返回空列表，SpEL 办理人解析不走用户批量查询这条路径。未验证设计器 UI 实际调用的 Warm-Flow 端点。
- Artifact impact: Skill 指派说明应指向这三层，而不是让业务模块自己写 PermissionHandler。

### R-016

- Claim: 状态回调链路：Warm-Flow 任务 finish → `WorkflowGlobalListener.finish` → `determineFlowStatus`（实例已是终态则用实例状态；否则若无剩余任务则更新为 `finish`）→ `FlowProcessEventHandler.processHandler` → 业务 `@EventListener`。申请人提交（`submit=true`）与普通办理分开发布。退回到申请人节点会额外再发 `back` 并改实例状态。流程图悬浮提示是另一扩展点：`FlwChartExtServiceImpl` 实现 Warm-Flow `ChartExtService`。节点按钮/抄送/变量：`IFlwNodeExtService.parseNodeExt`。异常：`FlowExceptionHandler` 捕获 `FlowException` 返回 `R.fail`。
- Type: code fact
- Source: `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/listener/WorkflowGlobalListener.java</Path>` `finish`/`determineFlowStatus`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/impl/FlwChartExtServiceImpl.java</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/handler/FlowExceptionHandler.java</Path>`
- Confidence: high
- Limits: `determineFlowStatus` 在非 initial 且任务未结束时返回 `null`，此时不发总体 ProcessEvent，但仍可能发 ProcessTaskEvent。
- Artifact impact: Skill 应提醒业务表状态以 `ProcessEvent.status` + `submit` 为准，并对齐 `BusinessStatusEnum`。

### R-017

- Claim: REST vs 内部 API 的实际消费关系：`ruoyi-admin` Maven 依赖 `ruoyi-workflow`（唯一业务模块依赖方）。其他模块不依赖该 artifact。Java 类型消费：`WorkflowService` 实现与 `TestLeaveServiceImpl` 在同一模块；无外部模块 import。定义管理大量走 Warm-Flow `DefService`/`InsService`（引擎服务，不是本模块 IFlw 接口）。前端工作流页通过 HTTP 调 `/workflow/*` 与 Warm-Flow UI；待办跳转用 `formPath`+`businessId`+`taskId`（`<Path>plus-ui-namewta/src/api/workflow/workflowCommon/index.ts</Path>` `routerJump`）。
- Type: code fact
- Source: `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/pom.xml</Path>`；全仓 `org.dromara.workflow` import 搜索；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/controller/FlwDefinitionController.java</Path>` `DefService` 字段；`<Path>plus-ui-namewta/src/api/workflow/workflowCommon/index.ts</Path>`
- Confidence: high
- Limits: Warm-Flow UI 插件自带的 `/warm-flow*` 控制器来自第三方 jar，本仓库无源码。
- Artifact impact: Skill 决策：新业务模块 = 依赖 `ruoyi-api` + 注入 `WorkflowService` + 监听 api.event；不要依赖 `ruoyi-workflow`、不要调用 `IFlw*`、不要直接调 `InsService`。

### R-018

- Claim: 启动/办理/任务操作/删实例的业务编排是 LiteFlow，不是业务模块可插拔 SPI。链：`startProcessChain`、`completeTaskChain`、`taskOperationChain`（`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/resources/liteflow/task-chain.el.xml</Path>`）；`deleteInstanceChain`（`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/resources/liteflow/instance-chain.el.xml</Path>`）。业务模块扩展点是事件与 SpEL/节点扩展，不是改这些 chain。
- Type: code fact
- Source: 上述两个 el.xml；对应 `liteflow/` 组件类
- Confidence: high
- Limits: 未逐步验证每条 chain 的异常回滚与锁范围之外的行为。
- Artifact impact: Skill 不要教业务模块新增 LiteFlow 组件来接入审批。

### R-019

- Claim: 「表单」在本模块中是路由元数据，不是独立表单引擎服务。`FlowDefinitionVo`/`FlowTaskVo`/`FlowHisTaskVo`/`FlowInstanceVo` 带 `formCustom`/`formPath`；待办查询用 SQL `COALESCE` 选择任务或定义的 `form_path`。前端 `routerJump` 把 `formPath` 当 Vue 路由，query 带业务 `id`、`type`、`taskId`。请假页 `<Path>plus-ui-namewta/src/views/workflow/leave/</Path>` 是该模式的页面侧。没有表单设计 CRUD Controller。
- Type: code fact
- Source: `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/domain/vo/FlowTaskVo.java</Path>`；`FlwTaskMapper.getListRunTask`；`<Path>plus-ui-namewta/src/api/workflow/workflowCommon/index.ts</Path>`；`<Path>plus-ui-namewta/src/views/workflow/leave/index.vue</Path>`
- Confidence: high
- Limits: Warm-Flow UI 是否另有在线表单设计器能力未在本仓库源码中证实。
- Artifact impact: Skill 表单一节应写「在流程定义上配置 `formPath` 指向业务页；业务页用 `id`/`taskId` 调自己的 CRUD + 工作流办理 REST」。

### R-020

- Claim: 集成 playbook（证据路径已闭合）：(1) 业务表增加 `status`（对齐 `BusinessStatusEnum`），主键作为 `businessId`。(2) 在设计器发布 `flowCode`，节点 `formPath` 指向业务页。(3) 业务 Service 注入 `org.dromara.workflow.api.WorkflowService`（模块已依赖 `ruoyi-api` 即可，由 `ruoyi-admin` 提供 Bean）。(4) 提交：先保存单据，再 `StartProcessDTO(businessId, flowCode, variables)`；后端无人会话加 `variables.ignore=true`，调用 `startCompleteTask` 或 `startWorkFlow`+`completeTask`。(5) 用 `@EventListener` 按 `flowCode` 监听 `ProcessEvent` 回写状态、`ProcessTaskEvent` 做节点副作用、`ProcessDeleteEvent` 清理单据。(6) 删业务数据时调 `deleteInstance`。(7) 人的待办/已办/抄送走 `/workflow/task/*`，不要在业务模块重查 `flow_task`。(8) 办理人配置走设计器（用户/角色/部门/岗位/SpEL），不要复制 `PermissionHandler`。
- Type: inference
- Source: 综合 R-004、R-008、R-009、R-010、R-011、R-013、R-015、R-017
- Confidence: high
- Limits: 推断基于现有样例与 API；多实例并行、子流程、外部表单引擎未在仓库出现。
- Artifact impact: 这是后续 Skill 的主推荐路径。

### Conflicts and Unknowns

- 冲突：无来源冲突。`IFlwDefinitionService` 与控制器里直接使用的 Warm-Flow `DefService` 并存，属于分层选择而非矛盾。
- 未知：Warm-Flow `InsService`/`TaskService`/`DefService` 的 jar 内部契约（跳转、会签、权限校验细节）未反编译。
- 未知：流程定义 JSON/`leave1` 等示例定义的完整节点图未在本次源码树中作为独立文件出现（预期在库表或导入 JSON）。
- 未知：`flow_user.type` 取值 `1/2/3` 与 `TaskAssigneeType` 的逐值对照未完整展开。
- 未知：Warm-Flow UI 插件是否提供独立表单设计器；本模块无 Form Service。
- 未知：`StartPrepareInstanceComponent` 的业务编号 TODO 最终规则。
- 未知：`warm-flow.enabled=false` 时其他模块若注入 `WorkflowService` 的启动失败形态未实测。
- 未知：模块无 `src/test/java`，待办过滤、事件时序、锁与事务边界无自动化证据。
- 未知：SnailJob/AI 等扩展应用是否单独装配 workflow（`ruoyi-extend` 未检索到引用）。

### Recommendation

- 把 `WorkflowService` + 三个 `org.dromara.workflow.api.event.*` 作为其他业务模块的唯一接入面；REST `/workflow/*` 留给工作流中心 UI。
- 复制 `TestLeaveServiceImpl.submitAndFlowStart` + 三个 `@EventListener`，不要 Maven 依赖 `ruoyi-workflow`，不要实现 `GlobalListener`/`PermissionHandler`。
- Skill 必须写：描述不清时按本文件给出的仓库路径读取源码，不得凭空补 API。
- 表单按 `formPath` 路由到业务页；his 用实例 `flowHisTaskList` 与任务已办分页，不要虚构 His/Form Facade。
