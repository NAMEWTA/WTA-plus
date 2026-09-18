---
lesson_id: L-069
objective_ids: [OBJ-69]
claimed_cells:
  - A:FlwInstanceController.*
  - A:WorkflowService.*
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: two-windows-on-disk
    minutes: 9
  - segment: stop-delete-variable-and-api
    minutes: 10
  - segment: visuals-and-worked-examples
    minutes: 8
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-008, S-015, S-L069-01, S-L069-02, S-L069-03, S-L069-04, S-L069-05, S-L069-06, S-L069-07, S-L069-08]
---

# Lesson 069：宏观两扇窗——大厅 `FlwInstanceController` 与后厨 `WorkflowService`

## 学完你能做什么

打开两份 Java，你能**口述流程实例怎么被管、怎么被邻居房间点菜**，而不把任务枢纽、请假示例或浏览器工厂再讲成另一课。

口试名单就是矩阵 **(a)** 这两格，符号以**磁盘**为准：

1. **`A:FlwInstanceController.*`**（`wta-workflow`，门牌 `@RequestMapping("/workflow/instance")`）：**正好十三**个公开 HTTP 方法。管理员拿着 `Admin-Token` 排队：看运行中 / 已结束 / 我发起的、按业务 id 查详情和轨迹、读改变量、激活或挂起、撤销、作废、三条删除。大厅**没有**「启动流程」窗，也**没有**「办理任务」窗。
2. **`A:WorkflowService.*`**（`wta-api`，包 `org.namewta.workflow.api`）：跨模块点菜单。接口上**十一**个方法声明（十个名字，`completeTask` 两个重载）。邻居 POM 只依赖 `wta-api`，**不许**注入 `IFlwInstanceService`，**不许**碰 Warm-Flow 的 Mapper。全仓 `implements WorkflowService` **只有** `WorkflowServiceImpl`。

OBJ-69 要你当场说完的那句是：**大厅管跑着的单；后厨点菜单给请假/档案房间启动、办第一枪、查状态、改变量、按业务 id 删实例或终止。两扇窗共享实例服务的删除与变量读取，但启动/办理委托任务服务——那是 L-070 的格子。终止不是作废，撤销不是删除，`WorkflowService` 不是一条 HTTP 路径。**

登记表把 `wta-modules/wta-workflow` 标 **classic**，旁注「通过公开 Workflow API 接入，不改内部层次」。本课把这句钉到**方法、动词、状态桶、锁和事件**，不是再背一遍五层目录。磁盘上**没有** UseCase，**没有** DAO。

本课不宣称你会拆 `FlwCategoryController`（L-067）、定义发布/导入（L-068）、`FlwTaskController` / `IFlwTaskService` 的启动办理驳回催办终止（L-070）、SpEL（L-071）、`TestLeaveController.submitAndFlowStart` 整条请假（L-072）、或 `createWorkflowDefinitionService` / `createWorkflowWebDomain`（L-073）。今天只认：**十三扇大厅窗、十一份后厨点菜单、它们怎样会合、怎样故意不合。**

## 先把宏观地图放在桌上

L-003 已经把 workflow 钉在 classic 列。L-005 点过 `org.namewta.workflow.api.WorkflowService` 这个名字，没拆方法。L-034 / L-040 / L-035 / L-041 的档案网关已经隔着这扇窗喊 `startCompleteTask` / `terminateInstance` / `instanceVariable`，格子留给本课。L-002 说过：邻居只经 `wta-api`，不进 workflow 房间的抽屉。

2026-09-17 工作树：大厅类在 `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/controller/FlwInstanceController.java`。点菜单接口在 `backend/wta-api/src/main/java/org/namewta/workflow/api/WorkflowService.java`。填窗人是同模块 `service/impl/WorkflowServiceImpl.java`。实例里屋是 `IFlwInstanceService` / `FlwInstanceServiceImpl`——**不在** `wta-api`。

`org.namewta.workflow.api` 这一棵**正好十份**类型：接口 1、`domain/` 6、`event/` 3。没有 HTTP 注解，没有 Mapper。

```text
管理员浏览器                         邻居房间（profile / 请假）
  /workflow/instance/*                 注入 WorkflowService
        │                                      │
        v                                      v
 FlwInstanceController                 wta-api  WorkflowService
  十三扇窗（GET/PUT/POST/DELETE）        十一法（无 @RequestMapping）
        │                                      │
        │  大半委托                             │  删/变量/查状态 → IFlwInstanceService
        │  IFlwInstanceService                  │  启动/办理     → IFlwTaskService
        │                                      │  终止          → 引擎 TaskService
        v                                      v
        └──────── classic 房间 wta-workflow ────┘
              Warm-Flow InsService / TaskService
              LiteFlow deleteInstanceChain
              flow_instance + 业务扩展表
```

| 名字听起来像 | 实际是什么 | 本课认不认 |
| --- | --- | --- |
| `FlwInstanceController` | 管理员大厅，十三扇 HTTP | **本课** |
| `IFlwInstanceService` | 房间内部实例服务 | 本课认「大厅和点菜单怎样会合」；格子不标它 |
| `WorkflowService` | 跨模块 Java 合同 | **本课** |
| `WorkflowServiceImpl` | 唯一填窗人 | 本课认落点 |
| `InsService.active` | 引擎暂停/开动传送带 | 大厅 `active` 直接喊它 |
| `FlwTaskController` / `IFlwTaskService.startWorkFlow` | 任务枢纽 HTTP / 里屋 | L-070；点菜单会**转交**，不把任务窗标 covered |
| `TestLeaveController` | 请假示例 | L-072；本课只认它是点菜单的活样本 |
| `createWorkflowDefinitionService` | 浏览器厨房，工厂名不是 `createWorkflowService` | L-073 |
| `PersonWorkflowGateway` / `EnterpriseWorkflowGateway` | 档案房间自己的门缝 | L-034…L-041；本课认它们喊了哪三法 |

**类比：** 食堂有两道门。食客（管理员）从**大厅窗口**看「哪几锅还在煮、哪几锅已经端走、我点过什么、这张小票上写了哪些配料」。后厨之间另有一部**对讲机**（`wta-api`）：档案科、请假科不会自己跑进灶台翻锅，只会喊「按这张业务单号开火并办第一口」「这张单别批了，留底」「这口汤现在什么火候」。

**类比失效处：**

1. 大厅窗口**开火**不在本课这十三扇——开火窗挂在任务楼（`/workflow/task/startWorkFlow`）。对讲机上的 `startWorkFlow` / `startCompleteTask` 不是 HTTP。
2. 「运行中」不是「正在审批」。磁盘 `runningStatus()` 含 `draft` / `waiting` / `back` / **`cancel`**。申请人撤销之后，单子还挂在运行中板上，不进已结束板。
3. 作废、终止、撤销、删除是**四件不同的事**，不是同义词。对讲机的 `terminateInstance` **不是**大厅的 `POST /invalid`。
4. 「只读状态」`getBusinessStatus` 找不到返回**空串**；大厅 `getInfo` 找不到会**抛**引擎文案。两套语义。
5. 对讲机 `deleteInstance` 的业务 id 是 `List<String>`；大厅按业务 id 删除的路径变量是 `List<Long>` 再 `Convert.toStr`。非数字业务 id 走得了 API，走不了那扇 DELETE 窗。
6. `@ConditionalOnEnable` 绑的是配置键 `warm-flow.enabled=true`。关掉之后大厅 Bean 和 `WorkflowServiceImpl` 都不进容器；档案网关 `getIfAvailable()` 会拿到 `null`，不是大厅还在、对讲机消失。

## 核心概念与机制

### 直觉讲解

先记住三张号码，再背方法名：

- **业务单号 `businessId`。** 请假主键、档案申请主键，都是业务房间自己的号。对讲机几乎全按它找锅。大厅详情窗的路径却是 `{businessId}` 且类型为 **`Long`**。
- **厨房票根 `instanceId`。** Warm-Flow 自己的实例主键。变量、激活、作废、按实例删除认它。
- **任务号 `taskId`。** 对讲机 `getBusinessStatusByTaskId` / `completeTask` 认它。大厅实例类**没有**任务号参数。

再记住两块板：

- **运行中板** `BusinessStatusEnum.runningStatus()`：`draft`、`waiting`、`back`、`cancel`。大厅 `GET /pageByRunning` 用这只桶。
- **已结束板** `finishStatus()`：`finish`、`invalid`、`termination`。大厅 `GET /pageByFinish` 用这只桶。

再记住四种「把火关掉」的手势，口试最容易揉：

| 手势 | 谁喊 | 引擎动作 | 写下的流程状态 | 历史还在？ | 还在运行中板？ |
| --- | --- | --- | --- | --- | --- |
| 撤销 `cancelProcessApply` | 大厅 PUT | `taskService.revoke` | `cancel` | 是 | **是**（`cancel` 在 running 桶） |
| 作废 `invalid` | 大厅 POST | `terminationByInsId` | `invalid` | 是 | 否 |
| 终止 `terminateInstance` | 对讲机 | `terminationByInsId` | `termination` | 是 | 否 |
| 删除 | 大厅 DELETE 或对讲机 `deleteInstance` | LiteFlow `deleteInstanceChain` | 行没了 | 运行删走 `insService.remove`；历史删还会清任务/历史/附件 | 没了 |

小孩子版只记十四句：

1. **先数大厅。** 十三法，一块门牌 `/workflow/instance`。
2. **先数对讲机。** 十一法声明，零 HTTP。
3. **谁填窗。** 只有 `WorkflowServiceImpl`。`wta-profile` **implements 的是自己的 Gateway**，不是 `WorkflowService`。
4. **classic 房间。** Controller → ServiceImpl → Mapper / 引擎。删除多走一条 LiteFlow 链。
5. **开关。** `warm-flow.enabled`。类上 `@ConditionalOnEnable`。LiteFlow 的 `enable` 跟同一个键。
6. **权限九颗。** `list` / `query` / `remove` / `cancel` / `active` / `currentList` / `variableQuery` / `variable` / `invalid`。种子在 `30-cde-workflow.sql`。
7. **查用 GET，改用 PUT/POST/DELETE。** 这是**今天磁盘上的动词**。API-005 的「变更走 POST」是棘轮，本课不把十三扇改写成 POST。写操作都有 `@Log`；`invalid` 的 `businessType` 磁盘是 **`INSERT`**，不是 UPDATE。
8. **撤销只许申请人或超管。** `LoginHelper.isSuperAdmin()` 或 `createBy` 等于当前用户。删除事件组件同一把尺。
9. **作废按实例 id，终止按业务 id。** 不要对调。
10. **终止幂等。** 没实例或已在结束桶 → `NO_ACTIVE_INSTANCE`，不喊引擎。空白参数抛 `IllegalArgumentException`。运行桶以外、结束桶以外 → `Unsupported workflow status for termination`。
11. **变量两支笔。** 大厅 `updateVariable` 只能改**已经存在的 key**，value 是 `String`，key 不存在返回 `false`。对讲机 `setVariable` 是引擎 `mergeVariable`，实例没有就**静默不写**。读取双方都走 `instanceVariable`，形状是 `{ variableList: [{key,value}…], variable: 原串 }`。
12. **启动/办理不在大厅。** 对讲机把 DTO `BeanUtil.toBean` 成 BO，交给 `IFlwTaskService`。两参数 `completeTask(taskId, message)` **写死** `variables.ignore=true`。
13. **`startCompleteTask` 是邻居最爱的一枪。** 同一事务里 `startWorkFlow` 再办首任务，消息类型写死站内信 `"1"`。它**不**自动放 `ignore`；请假示例自己把 `ignore` 塞进变量。
14. **删除会先按门铃。** LiteFlow 在真正 `remove` 之前发 `ProcessDeleteEvent`（只有 `flowCode` + `businessId`）。请假监听器可能跟着删业务行。

**类比失效边界：** 食堂类比**不**覆盖「挂起」。挂起是 `activityStatus` 传送带暂停，`flowStatus` 可以仍是 `waiting`。类比也**不**等于「对讲机 terminate 会检查当前登录人」——它 `ignore(true)`，不看申请人。类比还不等于「浏览器能 import `WorkflowService`」——不能。

### 精确定义与 English term

| 中文口诀 | English term | 磁盘上的东西 |
| --- | --- | --- |
| 大厅实例窗 | instance controller | `FlwInstanceController`；`/workflow/instance` |
| 跨模块点菜单 | workflow API | `org.namewta.workflow.api.WorkflowService` |
| 唯一填窗人 | API adapter/impl | `WorkflowServiceImpl`；`@Service` + `@ConditionalOnEnable` |
| 房间实例服务 | instance service | `IFlwInstanceService`（**不在** api 包） |
| 业务单号 | business id | 字符串；大厅两扇路径却是 `Long` |
| 厨房票根 | instance id | Warm-Flow `FlowInstance.id` |
| 运行中桶 | running status | `draft,waiting,back,cancel` |
| 已结束桶 | finish status | `finish,invalid,termination` |
| 激活状态 | activity status | 挂起/激活；与 `flowStatus` 不是同一列 |
| 申请人撤销 | cancel apply | `PUT /cancelProcessApply` → `taskService.revoke` → `cancel` |
| 管理员作废 | invalidate | `POST /invalid` → `terminationByInsId` → `invalid` |
| 邻居终止 | terminate | `terminateInstance` → `terminationByInsId` → `termination` |
| 删运行实例 | delete instance | `history=false` → `insService.remove` |
| 删历史实例 | delete historic | `history=true` → 清任务/历史/OSS 再 `removeByIds` |
| 删除编排 | delete chain | LiteFlow `deleteInstanceChain` |
| 删除门铃 | delete event | `ProcessDeleteEvent` |
| 变量列表形状 | variable view | `variableList` + 原串 `variable` |
| 合并变量 | merge variable | `setVariable` → `taskService.mergeVariable` |
| 覆盖已有变量 | update existing key | `updateVariable`；缺 key 则 false |
| 启动并办首枪 | start and complete first | `startCompleteTask` |
| 忽略权限 | ignore flag | 变量 `ignore=true`；系统后台办理 |
| 条件装配 | conditional on enable | `@ConditionalOnEnable` → `warm-flow.enabled=true` |
| 终止锁 | terminate lock | `@Lock4j(keys="'workflow:terminate:' + #businessId")` |
| 分层登记 | classic workflow | 登记表：公开 Workflow API 接入 |

方法签名不要混：

| 合同 / 窗 | 参数 | 返回 | 找不到实例 |
| --- | --- | --- | --- |
| `GET /getInfo/{businessId}` | `Long` | `FlowInstanceVo` | 抛 `NOT_FOUNT_INSTANCE` |
| `GET /flowHisTaskList/{businessId}` | `String` | `{list, instanceId}` | 抛 |
| `GET /instanceVariable/{instanceId}` 与 api 同名法 | `Long` | `{variableList, variable}` | 抛 |
| `getBusinessStatus` | `String businessId` | 状态串 | **空串** |
| `getBusinessStatusByTaskId` | `Long taskId` | 状态串 | 任务/历史都没有 → **空串** |
| `getInstanceIdByBusinessId` | `String` | `Long` | **`null`** |
| `setVariable` | id + map | `void` | **不写、不抛** |
| `terminateInstance` | 业务 id + 原因 | `WorkflowTerminationResult` | `NO_ACTIVE_INSTANCE` 且 `instanceId=null` |
| `deleteInstance` | `List<String>` | `boolean` | LiteFlow 找不到 → `false` |

HTTP 合同（API-005）管的是浏览器窗。`WorkflowService` **没有** `@RequestMapping`。浏览器碰到的是 `/workflow/instance/*` 和任务楼 `/workflow/task/*`，不是 Java 接口名。

### 机制/因果链

#### 1. 大厅十三扇在磁盘上的真实位置

类注解：`@ConditionalOnEnable` `@Validated` `@RequiredArgsConstructor` `@RestController` `@RequestMapping("/workflow/instance")`，`extends BaseController`。注入 **两** 个对象：`InsService insService`（只给 `active` 用）和 `IFlwInstanceService flwInstanceService`。

| Java 方法 | HTTP | 权限 | 写？ | `@Log` / 防重 |
| --- | --- | --- | --- | --- |
| `selectRunningInstanceList` | `GET /pageByRunning` | `list` | 否 | 无 |
| `selectFinishInstanceList` | `GET /pageByFinish` | `list` | 否 | 无 |
| `selectCurrentInstanceList` | `GET /pageByCurrent` | `currentList` | 否 | 无；wrapper 再 `eq createBy = 当前用户` |
| `getInfo` | `GET /getInfo/{businessId}` | `query` | 否 | 无；`businessId` 是 `Long` |
| `flowHisTaskList` | `GET /flowHisTaskList/{businessId}` | `query` | 否 | 无；`businessId` 是 `String` |
| `instanceVariable` | `GET /instanceVariable/{instanceId}` | `variableQuery` | 否 | 无 |
| `updateVariable` | `PUT /updateVariable` | `variable` | 是 | `@Log` UPDATE + `@RepeatSubmit` |
| `active` | `PUT /active/{id}?active=` | `active` | 是 | `@Log` UPDATE + `@RepeatSubmit`；`true→insService.active` / `false→unActive` |
| `cancelProcessApply` | `PUT /cancelProcessApply` | `cancel` | 是 | `@Log` UPDATE + `@RepeatSubmit`；**没有** `@Validated` |
| `invalid` | `POST /invalid` | `invalid` | 是 | `@Log` **INSERT** + `@RepeatSubmit` + `@Validated` |
| `deleteByBusinessIds` | `DELETE /deleteByBusinessIds/{businessIds}` | `remove` | 是 | `@Log` DELETE；路径 `List<Long>` → `Convert.toStr` |
| `deleteByInstanceIds` | `DELETE /deleteByInstanceIds/{instanceIds}` | `remove` | 是 | `@Log` DELETE |
| `deleteHisByInstanceIds` | `DELETE /deleteHisByInstanceIds/{instanceIds}` | `remove` | 是 | `@Log` DELETE；`@DSTransactional` 在服务法上 |

列表查询共用 `buildQueryWrapper`：`FlowInstance` 别名 `fi` **左连** 定义表 `fd` 和业务扩展 `biz`。可筛节点名、流程名/编码、分类（含子分类 id）、业务 id、发起人 id 列表，按创建时间倒序。运行中/已结束只是再 `.in(flowStatus, 对应桶)`。`pageByCurrent` 不按桶切，只钉当前登录人。

`getInfo`：`selectInstByBusinessId(str)` → 抄成 `FlowInstanceVo` → 再按 `definitionId` 补流程名/编码/版本/表单/分类。定义没有也抛。

`flowHisTaskList`：先查实例；运行中任务抄成 `FlowHisTaskVo`，状态写死待审、清空 `updateTime` / `runDuration`，办理人从引擎用户表按任务 id 拼；再查历史中间节点，按更新时间倒序。返回 **`Map.of("list", 运行中在前历史在后, "instanceId", id)`**。不是一张纯历史表。

`cancelProcessApply`：按业务 id 找实例 → 找定义 → 非超管必须是发起人 → `checkCancelStatus`（已撤销/已完成/已作废/已终止/已退回/空状态都停）→ `revoke`，流程状态与历史状态都写成 `cancel`，`ignore(true)`。`FlowCancelBo` 是 record：`businessId` + `message`。校验注解挂在 `AddGroup` 上，而 Controller **没** `@Validated`，所以空业务 id 不会在入口被框架拦住，会落到「找不到实例」。

`processInvalid`：按实例 id `insService.getById`；找到才 `checkInvalidStatus`（已完成/已作废/已终止/空停；**允许**草稿、待审、退回、已撤销）。找不到仍喊 `terminationByInsId`。状态写成 `invalid`，历史任务状态用 `TaskStatusEnum.INVALID`。`FlowInvalidBo` 的 `@NotNull` 也在 `AddGroup`；方法只有 `@Validated`（默认组），组对不上时入口同样可能放行。

`updateVariable`：`FlowVariableBo(instanceId, key, value)` 三个字段都非空才有意义。实例没有抛；**map 里没有这个 key 就打 error 日志并 `return false`**，不会新增键。value 类型是 `String`。写回用 `FlowEngine.jsonConvert.objToStr`。

`active` **绕过** `IFlwInstanceService`，直接引擎。返回 `R<Boolean>`。

#### 2. 对讲机十一法怎样转交

`WorkflowServiceImpl` 同样 `@ConditionalOnEnable` `@Service`。注入 `IFlwInstanceService`、`IFlwTaskService`、引擎 `TaskService`。

| 方法 | 转交给谁 | 额外闸 |
| --- | --- | --- |
| `deleteInstance` | `flwInstanceService.deleteByBusinessIds` | 与大厅按业务删除同一条 LiteFlow |
| `terminateInstance` | 自己查实例 + `taskService.terminationByInsId` | `@Lock4j` + `@DSTransactional`；空白 `strip` 后拒绝；结束桶幂等；运行桶才终止；引擎返回 null 抛「did not confirm」 |
| `getBusinessStatus` / `getInstanceIdByBusinessId` | `selectInstByBusinessId` | 空串 / null |
| `getBusinessStatusByTaskId` | `selectByTaskId`：先运行任务，没有再历史任务，再实例 | 空串 |
| `setVariable` / `instanceVariable` | 同名实例服务 | 见上表 |
| `startWorkFlow` | `flwTaskService.startWorkFlow(BeanUtil.toBean(dto, StartProcessBo))` | 任务服务自己有 `@Lock4j(flowCode+businessId)` 与 LiteFlow `startProcessChain` |
| `completeTask(CompleteTaskDTO)` | `completeTask(CompleteTaskBo)` | 任务服务 `@DSTransactional` + `@Lock4j(taskId)`；`ignore` 由调用方放进 variables |
| `completeTask(Long, String)` | 手造 BO，**强制** `ignore=true` | 注释写明：系统后台无用户信息 |
| `startCompleteTask` | 先 start 再 complete 首任务 | 类上 `@Transactional`；complete 的 `messageType` 写死 `["1"]`（站内信）；**不**自动 ignore |

`StartProcessDTO`：`businessId`、`flowCode`、`handler`、`variables`（get 时丢掉 null 值）、`bizExt`（空则 new 一个 `FlowInstanceBizExtDTO`）。返回 record `StartProcessReturnDTO(processInstanceId, taskId)`。

`CompleteTaskDTO`：任务 id、附件、抄送 `FlowCopyDTO(userId, nickName)`、消息类型、意见、通知、办理人、变量、扩展 ossId 串。

`WorkflowTerminationResult` 是 record：`Status.TERMINATED | NO_ACTIVE_INSTANCE` + `instanceId`。档案网关今天**丢掉**这个返回值，只把运行期异常包成 `*_WORKFLOW_TERMINATE_FAILED`。缺 Bean 时网关根本不进 `terminateInstance`，直接 `*_WORKFLOW_UNAVAILABLE`。

#### 3. 删除链：同一条河，三道入口

三道入口都 `LiteFlowUtils.execute("deleteInstanceChain", context)`：

- `byBusinessIds`（大厅 DELETE 业务号、对讲机 `deleteInstance`）
- `byInstanceIds`（大厅 DELETE 运行实例）
- `byHistoryInstanceIds`（大厅 DELETE 历史；`history=true`）

链：`instanceDeleteLoad` → 若存在 → `instanceDeleteEvent` → `instanceDeleteExecute`；不存在则 `noop`，结果保持 load 写下的 `false`。

Load：有业务号就 `in businessId`；否则 `selectByIds`。空列表打 warn。

Event：**先**权限（超管或发起人），**再**按定义编码发 `ProcessDeleteEvent`。定义缺失只 warn，跳过门铃，仍继续删。

Execute：先删抄送用户（历史任务状态为 copy）。`history=false`：`insService.remove`。`history=true`：按实例清任务用户、运行任务、历史 OSS、历史任务，再 `insService.removeByIds`，结果写死 `true`。

运行删除用 `@Transactional`；历史删除用 `@DSTransactional`。对讲机 `deleteInstance` 没有自己的锁，跟大厅按业务删除同一把里屋钥匙。

#### 4. 邻居何时喊对讲机（只标时刻，不把档案/请假五层再走一遍）

| 时刻 | 喊哪一法 | 不喊 |
| --- | --- | --- |
| 个人/企业申请提交 | `startCompleteTask`；变量 `profileType` / `snapshotVersion` / `submissionId`；`flowCode` 来自配置 `profile.person.flowCode` / `profile.enterprise.flowCode` | 不喊大厅 HTTP |
| 管理端 decide 先停流 | `terminateInstance(businessId, reason)` | 不是 `POST /invalid` |
| 读快照版本 | `instanceVariable` 里找 `variableList` 的 `snapshotVersion` | 不是 `GET /getInfo` |
| 请假 `submitAndFlowStart` | `startCompleteTask`；默认 `flowCode=leave1`；变量里自带 `ignore=true` | 不经任务 HTTP |
| 请假批量删 | 先删请假行，再 `deleteInstance` | 门铃监听可能再删一次（已删则空回） |
| 管理员在实例页作废 | 大厅 `POST /invalid` | 不经 `terminateInstance` |
| 管理员点「我发起的」撤销 | 大厅 `PUT /cancelProcessApply` | 状态进 `cancel`，仍在运行中板 |

`startCompleteTask` 的首任务办理**没有**把 `ignore` 写死。档案网关也不写。能不能跳过办理人校验，要看任务链和定义，不在本课展开——口试只要说「这枪存在、它转交 L-070 的两法、请假自己塞了 ignore、档案没塞」。

### 图、表或文本图

**图 1：宏观两栋楼、两扇窗**

```text
  浏览器 / admin-web
    createWorkflowDefinitionService  （工厂名不是 createWorkflowService）
    GET/PUT/POST/DELETE /workflow/instance/*
         │
         v
  FlwInstanceController  十三扇
         │  除 active 外 → IFlwInstanceService
         │  active → InsService
         v
  classic wta-workflow
    FlwInstanceServiceImpl
    WorkflowServiceImpl implements WorkflowService
    FlwTaskServiceImpl            ← L-070
         ▲
         │  只经 api
  wta-profile / 其它模块
    Spring*WorkflowGateway.getIfAvailable()
    TestLeaveServiceImpl（同模块也走接口）
```

- **alt：** 大厅是 HTTP；对讲机是 Java；实现都在 classic 房间；邻居伸手只到 api。
- **caption：** 图 1——OBJ-69 的空间关系。`WorkflowService` 的实现不在 `wta-api` 包内。
- **文字等价物：** 管理员改实例走 `/workflow/instance`。档案和请假要启动、终止、读变量，注入 `WorkflowService`。浏览器既 import 不到这个接口，也不该把接口名当成 URL。
- **图的边界：** 不画任务办理 LiteFlow、不画定义发布、不画请假表字段。不保证以后会给 `terminateInstance` 开一扇对称 HTTP。

**图 2：大厅十三扇 vs 厨房映射**

| 大厅窗 | 2026-09-17 浏览器工厂有没有同名枪 |
| --- | --- |
| `pageByRunning` / `pageByFinish` / `pageByCurrent` | 有：`pageRunningInstances` 等 |
| `flowHisTaskList` | 有：`flowHistory` |
| `cancelProcessApply` | 有：`cancelProcess` PUT |
| `instanceVariable` / `updateVariable` | 有 |
| `deleteByInstanceIds` / `deleteHisByInstanceIds` | 有 |
| `invalid` / `active` | 有：`invalidateInstance` / `setInstanceActive` |
| `getInfo` | **工厂没有** |
| `deleteByBusinessIds` | **工厂没有**（对讲机 `deleteInstance` 才按业务号删） |

- **alt：** 十三扇不全在厨房；缺的两扇仍是本课大厅格子。
- **caption：** 图 2——不要用前端工厂行数反推 Controller 方法数。
- **文字等价物：** 口试按 Java 十三法。L-073 再讲厨房。今天只要能指出：详情窗和按业务号删除窗在后端在、在 `createWorkflowDefinitionService` 里不在。
- **图的边界：** 不把工厂缺枪说成「后端没有」。也不把工厂有枪说成本课覆盖了前端。

**图 3：四种关火（成功才往下）**

```text
[撤销] PUT /cancelProcessApply {businessId, message}
   │  无实例 / 非发起人非超管 / checkCancelStatus 失败 → 停
   v
 taskService.revoke → flowStatus=cancel  （仍在运行中板）

[作废] POST /invalid {id, comment}
   │  有实例则 checkInvalidStatus；然后无论是否找到都 terminationByInsId
   v
 flowStatus=invalid  （已结束板）

[终止] WorkflowService.terminateInstance(businessId, reason)
   │  空白 → IllegalArgumentException
   │  无实例或已在结束桶 → NO_ACTIVE_INSTANCE（不喊引擎）
   │  不在运行桶 → Unsupported …
   │  锁：workflow:terminate:{businessId}
   v
 terminationByInsId ignore=true → TERMINATED + instanceId
   （已结束板；历史保留）

[删除] deleteBy* / deleteInstance
   │  load 空 → false
   │  非发起人非超管 → 停
   v
 门铃 ProcessDeleteEvent → remove / 清历史
```

- **alt：** 撤销留在运行中；作废与终止都走引擎终止但章不同；删除才碎纸并按门铃。
- **caption：** 图 3——OBJ-69 的时间关系。任务 HTTP 只作为「不是这条河」。
- **文字等价物：** 申请人抽回申请用撤销。管理员在实例页盖作废章用 `/invalid`。档案管理端停掉审批用对讲机终止。业务房间要连同流程票根一起扔掉用删除。四句话不能并成「关掉流程」。
- **图的边界：** 不画催办、驳回、转办。那些是 L-070。不画 `ProcessEvent` 如何回写请假状态——L-072 的监听器。

**图 4：对讲机字段怎么抄**

| DTO / 结果 | 字段 | 谁再用 |
| --- | --- | --- |
| `StartProcessDTO` | `businessId` `flowCode` `handler` `variables` `bizExt` | 启动；`getVariables()` 丢 null |
| `StartProcessReturnDTO` | `processInstanceId` `taskId` | `startCompleteTask` 拿 `taskId` 办第一枪 |
| `CompleteTaskDTO` | 任务、抄送、消息、意见、变量、`ext` | 完整办理 |
| `FlowInstanceBizExtDTO` | 实例/业务/编码/标题 | 扩展表，不是 `flow_instance` 主列 |
| `WorkflowTerminationResult` | `TERMINATED` / `NO_ACTIVE_INSTANCE` + id | 合同测试锁死；档案网关目前不读 status |
| `ProcessEvent` | 编码、实例、业务、节点、状态、params、submit | 请假总体监听 |
| `ProcessDeleteEvent` | **只有** `flowCode` `businessId` | 删除门铃；没有 instanceId |

- **alt：** 点菜单的纸比大厅 VO 窄；删除事件更窄。
- **caption：** 图 4——跨模块合同带哪些字段。
- **文字等价物：** 邻居启动至少要业务号和流程编码。终止只交业务号和原因。读变量拿到的是列表形状，档案网关按 `key==snapshotVersion` 找，找不到抛快照不可用，不是空串。
- **图的边界：** `sso` 那套目录视图与本课无关。不要把 `LoginUser` 画进工作流 api 包。

## 正例、反例与边界

**正例 A：运行中板含已撤销。** 申请人 `PUT /cancelProcessApply` 成功后，`GET /pageByRunning` 仍可能刷到这张单，`flowStatus=cancel`。它**不会**出现在 `pageByFinish`。口试若说「撤销等于结束」，对照 `runningStatus()` 名单。

**正例 B：档案提交只对讲、不走大厅。** `SpringPersonWorkflowGateway.start`：没有 `WorkflowService` Bean 或没有 `profile.person.flowCode` → `PERSON_WORKFLOW_UNAVAILABLE`。有则 `startCompleteTask`。失败码 `PERSON_WORKFLOW_START_FAILED`。企业镜像同一形状。HTTP `/workflow/instance` 零命中。

**正例 C：管理端停流走终止，不是作废。** 个人/企业 `decide` 经网关 `terminateInstance`。合同测试 `WorkflowTerminationContractTest`：待审实例 → 引擎 `terminationByInsId`，消息=原因，流程状态 `termination`，历史状态 `termination`，`ignore=true`，返回 `TERMINATED`。已是 `finish/invalid/termination` → `NO_ACTIVE_INSTANCE` 且 **never** 喊引擎。

**正例 D：请假删除走对讲机业务号。** `TestLeaveServiceImpl.deleteWithValidByIds` 先删请假行，再 `workflowService.deleteInstance(ids 转字符串)`。与大厅 `deleteByBusinessIds` 会合到同一条链。链上门铃可能再进 `processDeleteHandler`；监听器发现行已空就 return。

**正例 E：两参数办理写死 ignore。** `completeTask(taskId, "同意")` 一定 `variables.ignore=true`。完整 DTO 重载不代写。系统后台/无登录用户走短重载；要抄送、附件、消息类型走 DTO。

**正例 F：变量读取给档案网关。** `instanceVariable` 把 map 打成 `variableList`。网关只认列表里 `key` 为 `snapshotVersion` 且值为正整数的那一项。大厅 `GET /instanceVariable/{id}` 同一份形状。

**反例 1：** 「`WorkflowService` 就是 `/workflow/instance`。」接口没有映射。启动甚至不在这十三扇。

**反例 2：** 「`IFlwInstanceService` 在 `wta-api`。」它在 `org.namewta.workflow.service`。邻居编译期伸不到。

**反例 3：** 「`wta-profile` 里有一份 `implements WorkflowService`。」没有。只有 Gateway 适配器拿 `ObjectProvider<WorkflowService>`。

**反例 4：** 「终止 = 作废 = 撤销。」三枚章、两套入口、两个状态桶归属。

**反例 5：** 「`terminateInstance` 找不到就抛，跟 `getInfo` 一样。」它返回 `NO_ACTIVE_INSTANCE`。`getInfo` 才抛。

**反例 6：** 「`setVariable` 和 `updateVariable` 一样。」一支合并、一支只改已有 key；一支静默、一支缺实例就抛。

**反例 7：** 「`pageByRunning` 等于待我审批。」待办在任务楼 `pageByTaskWait`（L-070）。运行中板是实例维度，含草稿和已撤销。

**反例 8：** 「`cancel` 进已结束板。」名单在 `runningStatus`。

**反例 9：** 「大厅 `active` 走 `IFlwInstanceService`。」走 `InsService`。挂起不是 `flowStatus=invalid`。

**反例 10：** 「`startCompleteTask` 自动 ignore。」磁盘不写。请假变量里自己放。

**反例 11：** 「浏览器工厂叫 `createWorkflowService`。」厅堂是 `createWorkflowDefinitionService`。L-007 已钉过。本课再钉：不要用工厂名去猜 Java 接口名。

**反例 12：** 「前端有十三枪所以后端十三扇都映射了。」缺 `getInfo`、缺 `deleteByBusinessIds`。

**反例 13：** 「删除历史只是列表换桶。」`history=true` 会清任务、历史任务、历史 OSS。运行删除走 `insService.remove`。

**反例 14：** 「`getBusinessStatus` 空就是抛错。」空串。调用方必须自己认「没有流程」。

**反例 15：** 「`invalid` 的 `@Log` 是 UPDATE。」磁盘 `BusinessType.INSERT`。

**反例 16：** 「对讲机 `completeTask` 就是大厅某扇窗。」大厅没有办理窗。

**边界：**

- `checkCancelStatus` 拒绝已退回；`checkInvalidStatus` **允许**已退回、已撤销、草稿。作废比撤销更「什么都能盖」——仍拒绝已完成/已作废/已终止。
- `terminateInstance` 把 `cancel` 视为运行桶，所以**已撤销的单仍可被邻居终止**，变成 `termination` 进结束板。这是枚举名单的后果，不是大厅撤销按钮的下一步。
- `processInvalid` 在实例为 null 时仍喊引擎：与终止的幂等相反。
- `selectByTaskId`：运行任务没有就回退历史任务。已办任务仍能问到实例状态。
- `flowHisTaskList` 的 `businessId` 是 String，`getInfo` 是 Long。同一块业务号，两扇窗类型不同。
- `FlowCancelBo` / `FlowInvalidBo` / `FlowVariableBo` 的约束组是 `AddGroup`，与入口 `@Validated` 默认组不对齐时，框架不会按注解挡空字段。
- `RepeatSubmit` 在撤销/激活/改变量/作废上；三条 DELETE **没有**。
- 关掉 `warm-flow.enabled`：大厅和对讲机实现一起消失。档案网关把「没有 Bean」和「terminate 抛错」分成两种码；管理端再包一层 `*_ADMIN_WORKFLOW_*`。那是 L-034/L-040 的失败码精度，本课只保证对讲机侧：Bean 不在容器里。
- 菜单种子在 `release-artifacts/docker/infrastructure/mysql/init/30-cde-workflow.sql`，不是 `50-cde-base-dml.sql`。

## 变式与迁移

1. **变式 A：档案决定停流。** 走 `terminateInstance`，不要新开大厅 HTTP，也不要复用 `/invalid`（章是 `invalid`，权限是实例作废，且按实例 id）。要改语义先改 api 合同测试 `WorkflowTerminationContractTest`。
2. **变式 B：业务房间删除单据。** 学请假：先删（或先标）业务行，再 `deleteInstance`。准备好听门铃的二次删除。不要注入 `FlowInstanceMapper`。
3. **变式 C：只要状态不要票根。** `getBusinessStatus` / `getInstanceIdByBusinessId`。空串/null 是合法「没流程」，不要学 `getInfo` 去 catch 异常当没有。
4. **变式 D：系统作业办一枪。** 短重载 `completeTask(taskId, message)`。不要用它传抄送和附件。
5. **变式 E：提交并跳过申请人节点。** `startCompleteTask`。若办理人校验会挡，调用方把 `ignore=true` 放进 `variables`（请假样本），不要改 `WorkflowServiceImpl` 给所有邻居写死 ignore——档案路径今天没写。
6. **变式 F：改一个已存在的变量。** 管理员用大厅 `updateVariable`。邻居要合并新 key 用 `setVariable`。不要把「改标题」做成新增 key 的 `updateVariable`（会 `false`）。
7. **变式 G：非数字业务 id。** API 可以。大厅 `getInfo/{Long}` 与 `deleteByBusinessIds/{List<Long>}` 不行。新业务若用 UUID，删和详情应走 api 或改路径类型——那是改合同，不是本课现状。
8. **变式 H：只想挂起不想关火。** `PUT /active/{id}?active=false`。列表仍按 `flowStatus` 桶走；不要把挂起的待审单期望出现在已结束板。
9. **以后若要给终止开 HTTP。** 新窗应委托 `WorkflowService.terminateInstance`，不要复制一份 `terminationByInsId`。权限不要偷 `invalid`。
10. **换模块模式。** 给邻居加 `IFlwInstanceService` 依赖会同时违反登记表「经公开 Workflow API 接入」和 L-002。要扩能力，先扩 `WorkflowService` 方法，再改唯一实现。
11. **和 SSO 目录对照。** L-059 的窗是只读用户/Client；本课的窗会写引擎状态。两者都住在 `wta-api`，都没有 HTTP，都只有一份 implements。不要把「api 包」说成同一种副作用。
12. **迁移口诀：** 先数大厅十三扇与动词 → 再数对讲机十一法与转交对象 → 再数四件关火（撤销/作废/终止/删除）落在哪块板 → 再数变量两支笔 → 再数找不到实例的三种回报（抛 / 空串或 null / 幂等 NO_ACTIVE）→ 最后数谁在何时喊（档案 start+terminate+读变量，请假 start+delete）。跳步会出现「terminate 打 /invalid」「撤销等于删除」「WorkflowService 是前端工厂」。

## 常见误区

1. **「OBJ-69 包含 `FlwTaskController`。」** 那是 OBJ-70。本课只认对讲机转交了 `startWorkFlow` / `completeTask`。
2. **「OBJ-69 包含请假 Controller。」** 那是 OBJ-72。本课认它是点菜单活样本。
3. **「OBJ-69 包含 `createWorkflowDefinitionService`。」** 那是 OBJ-73。
4. **「把任务 start/complete HTTP 标 covered。」** 本课格子是 `FlwInstanceController.*` 与 `WorkflowService.*`。
5. **「`IFlwInstanceService` 是第二份 wta-api。」** 包名在 workflow 模块。
6. **「填窗人住在 `wta-api`。」** 住在 `wta-workflow`。
7. **「档案模块依赖 `wta-workflow`。」** 生产应只依赖 `wta-api`（加自己的 Gateway）。
8. **「撤销后去已结束页找。」** 去运行中页。
9. **「`runningStatus` 就是 waiting。」** 还有 draft/back/cancel。
10. **「`deleteHisByInstanceIds` 只删历史表一行。」** 清任务、历史、OSS、实例。
11. **「`instanceVariable` 直接返回引擎 map。」** 包了一层 list + 原串。
12. **「`setVariable` 找不到实例会抛。」** 不抛。
13. **「`getInfo` 的业务号是 String。」** 路径类型是 Long。
14. **「`invalid` 与 `terminateInstance` 同一枚章。」** `invalid` vs `termination`。
15. **「浏览器能 import `org.namewta.workflow.api`。」** 不能。那是 JVM 合同。
16. **「工厂缺枪等于后端缺窗。」** 后端十三扇都在。
17. **「`@Log` 的 INSERT 表示作废会插入业务单据。」** 只是操作日志类型枚举被写成 INSERT。
18. **「关掉 warm-flow 只关设计器 UI。」** `ui` 是另一键。`enabled` 关的是本课这些 Bean。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `FlwInstanceController.java`。数公开方法必须是十三。把 HTTP 动词、路径、权限、是否 `@Log` / `@RepeatSubmit` 勾在纸上。圈 `active` 用的是 `insService` 不是 `flwInstanceService`。圈 `getInfo` 的 `Long` 与 `flowHisTaskList` 的 `String`。圈 `invalid` 的 `BusinessType.INSERT`。
2. 打开 `WorkflowService.java`。数方法声明必须是十一（两个 `completeTask`）。确认没有 Spring Web 注解。再打开同包 `domain/` 与 `event/`，数到十份类型。
3. 打开 `WorkflowServiceImpl.java`。把每一法的委托对象标出来：实例服务 / 任务服务 / 引擎 `TaskService`。圈 `terminateInstance` 的 `@Lock4j` `@DSTransactional`、空白校验、结束桶幂等、运行桶检查、引擎 null 失败。圈短重载 `ignore=true` 与 `startCompleteTask` 的站内信 `"1"`。
4. 打开 `BusinessStatusEnum.runningStatus` / `finishStatus` / `checkCancelStatus` / `checkInvalidStatus`。把 `cancel` 画进运行中桶。对照撤销成功后应出现在哪张大厅列表。
5. 打开 `liteflow/instance-chain.el.xml` 与四个 `InstanceDelete*Component`。按 load → exists → event（权限+门铃）→ execute（运行 remove vs 历史清扫）走一遍。对照 `InstanceDeleteContext` 三个工厂方法。
6. 打开 `SpringPersonWorkflowGateway`（企业那份对着看）。圈三法：`startCompleteTask` / `terminateInstance` / `instanceVariable`。圈缺 Bean 的码。不要在本课改它们。
7. 打开 `WorkflowTerminationContractTest`。圈 TERMINATED、幂等 NO_ACTIVE、空白 IllegalArgument、锁键 `'workflow:terminate:' + #businessId`。
8. 打开 `frontend/packages/domains/workflow/src/index.ts` 里 instance 那一段。勾上有的枪，叉掉 `getInfo` 与 `deleteByBusinessIds`。再打开 `admin-web/.../services.ts` 圈工厂名 `createWorkflowDefinitionService`。

## 总结、词汇表与下一步

- **宏观两扇窗：** 管理员隔着 `/workflow/instance` 十三扇管实例；邻居隔着 `wta-api` 的 `WorkflowService` 十一法点菜。classic 房间填窗。实现开关是 `warm-flow.enabled`。
- **(a) `FlwInstanceController.*`：** 三张列表、详情、轨迹、变量读写、激活、撤销、作废、三条删除。启动/办理不在这里。`active` 直连引擎。撤销留在运行中桶。
- **(a) `WorkflowService.*`：** 删实例、终止、两套状态查询、变量读写、按业务号取票根、启动、两种办理、启动并办首枪。终止幂等且加锁。启动/办理转交 L-070。
- **关火四件套：** 撤销 `cancel`、作废 `invalid`、终止 `termination`、删除碎纸+门铃。不要并成一句「关掉」。
- **找不到的三种回报：** 大厅详情/轨迹/读变量抛；状态查询空串或 null；终止 `NO_ACTIVE_INSTANCE`。

词汇表：`FlwInstanceController` / `WorkflowService` / `WorkflowServiceImpl` / `IFlwInstanceService` / `businessId` / `instanceId` / `runningStatus` / `finishStatus` / `cancel` / `invalid` / `termination` / `deleteInstanceChain` / `ProcessDeleteEvent` / `variableList` / `setVariable` / `updateVariable` / `startCompleteTask` / `ignore` / `warm-flow.enabled` / `@Lock4j` / classic / `wta-api`。

下一步：分类是 OBJ-67，定义发布/导入是 OBJ-68，任务枢纽是 OBJ-70，SpEL 是 OBJ-71，请假 `submitAndFlowStart` 是 OBJ-72，浏览器工厂与页面是 OBJ-73。档案网关失败码精度在 OBJ-34 / OBJ-40。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` 与 `wta-api` | classic 房间公开入口；实例 Controller 十三法 | `FlwInstanceController` | 2026-09-17 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-workflow` classic；经公开 Workflow API 接入 | 登记表 workflow 行 | 2026-09-17 |
| S-015 | `backend/wta-api/src/main/java/org/namewta/workflow/api` | 点菜单不在 HTTP；十份类型 | `WorkflowService` + `domain/` + `event/` | 2026-09-17 |
| S-L069-01 | `FlwInstanceController.java` | 十三扇路径/动词/九权限；`active` 走 `InsService`；`invalid` 日志类型 INSERT | 类体 50–211 行 | 2026-09-17 |
| S-L069-02 | `WorkflowService.java`；`WorkflowServiceImpl.java` | 十一法声明；转交实例/任务/引擎；短重载 ignore；`startCompleteTask` 站内信 | 接口全文；实现 55–219 行 | 2026-09-17 |
| S-L069-03 | `IFlwInstanceService.java`；`FlwInstanceServiceImpl.java` | 列表桶、撤销校验、轨迹 map、变量两支笔、作废 | `runningStatus` 查询；`cancelProcessApply`；`instanceVariable`；`processInvalid` | 2026-09-17 |
| S-L069-04 | `BusinessStatusEnum.java` | 运行中含 cancel；结束含 invalid/termination；撤销/作废校验名单 | `runningStatus` / `finishStatus` / `checkCancelStatus` / `checkInvalidStatus` | 2026-09-17 |
| S-L069-05 | `instance-chain.el.xml`；`InstanceDelete*Component`；`InstanceDeleteContext` | 删除三入口、权限、门铃、运行/历史两条 execute | `deleteInstanceChain` | 2026-09-17 |
| S-L069-06 | `WorkflowTerminationContractTest.java`；`WorkflowTerminationResult.java` | 终止幂等、锁键、ignore、引擎失败不能报成功 | `wta-admin` 合同测试 | 2026-09-17 |
| S-L069-07 | `SpringPersonWorkflowGateway.java`；`SpringEnterpriseWorkflowGateway.java`；`TestLeaveServiceImpl.java` | 邻居喊 startCompleteTask / terminate / instanceVariable / deleteInstance | 网关 start/terminate；请假 submit/delete | 2026-09-17 |
| S-L069-08 | `ConditionalOnEnable.java`；`application.yml` `warm-flow`；`30-cde-workflow.sql` 菜单；`domains/workflow/src/index.ts` | 开关键；九颗权限种子；工厂缺 getInfo 与按业务删除 | `warm-flow.enabled`；菜单 1761400000000011621 起；工厂 instance 段 | 2026-09-17 |
