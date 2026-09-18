---
lesson_id: L-070
objective_ids: [OBJ-70]
claimed_cells:
  - A:FlwTaskController.*
  - B:IFlwTaskService.startWorkFlow,completeTask,backProcess,urgeTask,terminationTask
  - D:失败路径 工作流驳回/终止
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: sixteen-doors-on-disk
    minutes: 8
  - segment: five-hub-side-effects
    minutes: 11
  - segment: visuals-and-worked-examples
    minutes: 7
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 5
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-008, S-015, S-L070-01, S-L070-02, S-L070-03, S-L070-04, S-L070-05, S-L070-06, S-L070-07, S-L070-08]
---

# Lesson 070：宏观任务枢纽——`FlwTaskController` 十六扇窗与启动/办理/驳回/催办/终止

## 学完你能做什么

打开 `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/controller/FlwTaskController.java`，你能**口述任务柜台的十六扇公开窗**，再单独把 OBJ-70 点名的五支枢纽枪说完：**启动、办理、驳回、催办、终止各自写什么、锁什么、失败时单据停在哪**。不要把「终止」说成 `WorkflowService.terminateInstance`，也不要把「驳回」说成实例窗上的撤销/作废。

口试名单就是这三格，符号以**磁盘**为准：

1. **`A:FlwTaskController.*`**：一块门牌 `@RequestMapping("/workflow/task")`，**磁盘真实 16 个公开方法**。矩阵 (a) 那一行把十六个名字写全了：`startWorkFlow`、`completeTask`、`pageByTaskWait`、`pageByTaskFinish`、`pageByAllTaskWait`、`pageByAllTaskFinish`、`pageByTaskCopy`、`getTask`、`getNextNodeList`、`terminationTask`、`taskOperation`、`updateAssignee`、`backProcess`、`getBackTaskNode`、`currentTaskAllUser`、`urgeTask`。口试按磁盘，不要把接口 `IFlwTaskService` 多出来的内部法（`setCopy` / `isTaskEnd` / `selectByInstId` 等）背成 HTTP。
2. **`B:IFlwTaskService.startWorkFlow,completeTask,backProcess,urgeTask,terminationTask`**：五支会改流程世界的枪。启动和办理走 LiteFlow 链；驳回/终止直接调 WarmFlow `TaskService`；催办**不改**任务行，只发消息。
3. **`D:失败路径 工作流驳回/终止`**：矩阵原文符号是 `backProcess / terminationTask`。口试要能把**任务窗这两枪**的失败出口和邻居 `WorkflowService.terminateInstance`（L-069，按业务 ID、可幂等）分开。本课**不**把那扇跨模块合同标 covered。

模块模式是 **classic**：`Controller → IFlwTaskService → FlwTaskServiceImpl`，再伸手到 WarmFlow 引擎的 `TaskService` / `InsService` / `DefService`。登记表把 `wta-modules/wta-workflow` 标 classic：「保持现状；通过公开 Workflow API 接入，不改内部层次」。磁盘上**没有** UseCase、**没有** DAO。启动/办理/任务操作把编排交给 LiteFlow XML，**不是** layered 五层。不要把 `startProcessChain` 口述成 UseCase。

本课不宣称你会拆 `FlwCategoryController`（L-067）、定义发布/导入（L-068）、`FlwInstanceController` 与 `WorkflowService.*`（L-069）、`FlwSpelController`（L-071）、请假 `submitAndFlowStart`（L-072）、或厨房 `createWorkflowDefinitionService` / 菜单 `createWorkflowWebDomain`（L-073）。厨房把十六扇窗几乎都接到了同一座 `createWorkflowDefinitionService` 上——那是对照，不是本格。WarmFlow 引擎内部 `skip` / `termination` 的厂商实现是矩阵 **deferred(vendor-engine)**：本课只认 NAMEWTA 包一层怎么包它、失败时抛什么。

`@ConditionalOnEnable`：`warm-flow.enabled=true` 这整份 Controller 才进 Spring。关掉开关，十六扇窗整排消失，不是某一扇 404。

## 先把宏观地图放在桌上

L-003 已经把 `wta-workflow` 钉在 classic 列。L-067…L-069 是分类、图纸、实例房间。本课走进**任务柜台**：单据已经有一张图纸、常常已经有一个实例，人要在这里**排队、盖章、退回、催一催、或当场撕掉**。

四条河都叫 workflow，货不一样：

| 名字听起来像 | 实际是什么 | 本课认不认 |
| --- | --- | --- |
| `FlwCategoryController` / `FlwDefinitionController` | 分类树、流程图纸发布 | L-067 / L-068 |
| `FlwInstanceController` / `cancelProcessApply` / `processInvalid` | 实例列表、撤销、作废 | L-069 |
| `WorkflowService`（`wta-api`） | 邻居模块按 **businessId** 启停办 | L-069；启动/办理会**转交**本课五枪里的两支 |
| `FlwTaskController` | 任务 HTTP 十六扇 | **本课 (a)** |
| `IFlwTaskService` 五支枢纽 | 启动/办理/驳回/催办/终止副作用 | **本课 (b)** |
| `backProcess` / `terminationTask` 失败出口 | 任务不在、状态闸、引擎异常、附件对不上 | **本课 (d)** |
| `FlwSpelController` | 条件表达式目录 | L-071 |
| `TestLeaveController.submitAndFlowStart` | 示例请假一枪写单并发流 | L-072 |
| `createWorkflowDefinitionService` | 浏览器厨房，URL 几乎对齐本课十六扇 | L-073；本课只借来证明路径，不盖章 |

2026-09-17 工作树：权威 Controller 就是上面那一份。权威实现是 `.../service/impl/FlwTaskServiceImpl.java`。权威编排是 `backend/wta-modules/wta-workflow/src/main/resources/liteflow/task-chain.el.xml` 三条链：`startProcessChain`、`completeTaskChain`、`taskOperationChain`。**没有** `backProcessChain` / `terminationChain` / `urgeChain`。驳回、终止、催办写在 ServiceImpl 方法体里。

跨模块合同 `org.namewta.workflow.api.WorkflowService` 的实现是同模块 `WorkflowServiceImpl`：`startWorkFlow` / `completeTask` 只是 `BeanUtil.toBean` 再喊 `IFlwTaskService`。`terminateInstance` **不**喊 `terminationTask`——它走 `taskService.terminationByInsId`，锁键 `'workflow:terminate:' + #businessId`。两扇终止门，钥匙不同。

```text
已登录的管理员 / 已登录办理人
        │
        v
 /workflow/task/*          FlwTaskController     十六扇
        │
        v
 IFlwTaskService           FlwTaskServiceImpl    classic
        │
        ├─ startWorkFlow  ──LiteFlow──► startProcessChain
        │                                 续提交 或 新开实例(DRAFT)
        ├─ completeTask   ──LiteFlow──► completeTaskChain
        │                                 skip(PASS) + 可选自动过
        ├─ taskOperation  ──LiteFlow──► taskOperationChain
        │                                 委派/转办/加签/减签
        ├─ backProcess    ────────────► TaskService.skip(REJECT)
        ├─ terminationTask────────────► TaskService.termination(taskId)
        ├─ urgeTask       ────────────► sendMessage（不改任务行）
        └─ 查询五扇 + getTask + next + backNodes + assignee + users
                          │
                          v
              WarmFlow TaskService / InsService / DefService
              历史附件：WorkflowHistoryOssOwner（办理、驳回）
              办完：WorkflowGlobalListener.finish → 事件 → 抄送/消息
```

**类比：** 把任务柜台想成办事大厅的**窗口条**。左边五扇是「我的待办 / 我的已办 / 我的抄送」和「全楼待办 / 全楼已办」（后两扇要持 `workflow:task:list` 通行证）。正中是盖章台：开一张新号、在当前号上盖「过」、盖「退」、按铃催人、把整叠材料盖「终止」。旁边还有委派/转办/加签/减签、批量换办理人、问下一站、问能退到哪一站。大厅后面的**总机**（`WorkflowService`）是给别的科室打电话用的：它们报的是**业务单号**，不是窗口上的任务号。

**类比失效处：**

1. 「窗口条」不是 layered 的 UseCase。启动和办理是 LiteFlow 积木；驳回/终止是 ServiceImpl 里几行 `FlowParams`。
2. 「开一张新号」遇到同一 `businessId` 已有实例时，**不会**再开第二张。它走续提交，而且查找**只按业务单号，不按 `flowCode`**。你换了一张图纸编码再点启动，仍可能回到旧实例。
3. 「盖终止」在窗口上要带 **taskId**；总机终止要带 **businessId**。窗口找不到任务就喊「任务不存在」；总机找不到实例会说「没有活动实例」（幂等）。不要把两扇门的失败口吻说成一句。
4. 「催办」不是再盖一次章。任务行、实例状态都不变。只是给办理人发消息，跳转路径是 `/task/taskWaiting`。
5. 「全楼待办」和「我的待办」不是同一条 SQL 少了 userId 那么简单：我的待办还会把实例状态锁成 `waiting`；全楼待办**不加**这把锁。
6. BO 注释写「`nodeCode` 目前未使用，直接驳回到申请人」——**过期**。实现把 `bo.getNodeCode()` 交给 `FlowParams.nodeCode`。退到申请人节点才把实例打成 `back`，退到别的中间节点打成 `waiting`。
7. 厂商引擎怎么跳节点，本课不拆。失败时你能指的是 NAMEWTA 闸：任务空、实例空、`BusinessStatusEnum.checkBackStatus` / `checkInvalidStatus`、`FlowException` → `FlowExceptionHandler` 变 `R.fail`、附件对不上抛 `ServiceException` 并回滚。

## 核心概念与机制

### 直觉讲解

小孩子版只记十二句：

1. **先数窗。** `FlwTaskController` 16 个映射。`IFlwTaskService` 23 个方法。多出来的 7 个（`setCopy`、`selectByIdList`、`selectHisTaskById`、`selectByInstId(s)`、`isTaskEnd`、`getByNodeCode`）是给监听器和邻居用的，**没有** HTTP。
2. **一块牌子 `/workflow/task`。** 写操作几乎全是 POST；换办理人是 **PUT** `/updateAssignee/{userId}`。厨房测试把路径 id 做了 `encodeURIComponent`，后端路径变量仍是 `Long`。
3. **启动 = 按业务单号找或开。** 锁键 `flowCode + businessId`。已有实例：`checkStartStatus`，合并变量，返回**当前**任务。没有实例：必须已发布定义，`insService.start` 状态 **`draft`**，第一个任务必须正好 1 个（申请人）。
4. **办理 = 当前任务 `skip(PASS)`。** 锁键 `taskId`。任务没了文案是「流程任务不存在或任务已审批」。草稿/撤销/退回再办会塞 `submit=true`。定义开了 `autoPass` 时，同一登录人后面的待办会被引擎自动盖「流程引擎自动审批」。
5. **驳回 = `skip(REJECT)`。** 没有 LiteFlow。要 `taskId` + 目标 `nodeCode`。实例状态闸是 `checkBackStatus`。历史状态永远 `back`。
6. **终止（任务窗）= `taskService.termination(taskId)`。** 没有 `ignore(true)`，没有按业务 ID 的锁。实例若在，闸是 `checkInvalidStatus`。成功把实例打成 `termination`。
7. **催办 = 发消息。** 权限 `workflow:task:edit`。空名单返回 `false` → `toAjax` 变成 `R.fail()`。不写库。
8. **启动/办理/驳回/终止/换人/操作带 `@RepeatSubmit`。催办没有。** 催办可以连点，只是多发几封提醒。
9. **办理和驳回用 `@DSTransactional`，因为可能改 WarmFlow 表还要协调 OSS 引用。** 启动和终止任务窗用普通 `@Transactional`。催办无事务。
10. **办完之后的抄送/待办消息不在 Controller 里写。** `WorkflowGlobalListener.finish` 看 `hisStatus` 是不是 pass/back，再发事件；`WorkflowSideEffectListener` 才 `setCopy` / `sendMessage`。
11. **读任务要过 `checkTaskReadAccess`。** 超管、或持 `workflow:task:list` / `workflow:task:edit`、或自己是审批/转办/委托/抄送关系人、或自己是发起人。否则 `NotPermissionException("无权访问该流程任务")`。
12. **失败不是「返回 false 但单据已经终止」。** 五支枢纽里，启动/办理/驳回/终止成功路径几乎总是 `true` 或 DTO；失败靠抛异常。唯一爱返回 `false` 的是催办和空的 `updateAssignee`。

**类比补一句：** 把 `businessId` 想成档案袋编号，把 `taskId` 想成今天柜台上这一张号票。开袋看编号；盖章看号票。把号票丢了，窗口不会去猜袋子——它报「任务不存在」。总机（`WorkflowService.terminateInstance`）只认袋子，号票丢了它仍可能按编号把整袋盖终止。

### 精确定义与 English term

| 中文口头 | English term | 磁盘落点 |
| --- | --- | --- |
| 任务柜台 | task controller | `FlwTaskController`，`/workflow/task` |
| 任务服务 | task service | `IFlwTaskService` / `FlwTaskServiceImpl` |
| 启动流程 | start workflow | `startWorkFlow(StartProcessBo)` → `StartProcessReturnDTO(processInstanceId, taskId)` |
| 续提交 | resume existing instance | LiteFlow `startExists` → `startResume`；`BusinessStatusEnum.checkStartStatus` |
| 办理 / 通过 | complete / pass | `completeTask`；`SkipType.PASS`；历史 `TaskStatusEnum.PASS` |
| 驳回 | reject / back | `backProcess`；`SkipType.REJECT`；历史 `TaskStatusEnum.BACK` |
| 终止（任务窗） | terminate by task | `terminationTask(FlowTerminationBo)` → `taskService.termination(taskId)` |
| 终止（合同窗） | terminate by business id | `WorkflowService.terminateInstance`；**L-069**，本课只对照 |
| 催办 | urge | `urgeTask(FlowUrgeTaskBo)` → `sendMessage`，路径 `PATH_TASK_WAITING` |
| 任务操作 | delegate / transfer / add-sign / reduce-sign | `taskOperation/{taskOperation}`；`TaskOperationEnum` 四码 |
| 我的待办 | personal waiting | `pageByTaskWait`；SQL 带当前用户 **且** 实例 `waiting` |
| 全楼待办 | all waiting | `pageByAllTaskWait`；`userId=null`，**不加** waiting 锁 |
| 抄送 | copy | 办理人类型 `"4"`；列表 `pageByTaskCopy`；写入 `setCopy` |
| 申请人节点 | apply node | `IFlwCommonService.applyNodeCode` = 定义上第一个 BETWEEN 节点 |
| 业务状态 | business status | `BusinessStatusEnum`：`draft/waiting/back/cancel/finish/invalid/termination` |
| 任务历史状态 | history task status | `TaskStatusEnum`：pass/back/termination/copy/depute/transfer/sign… |
| 忽略权限变量 | ignore flag | 变量键 `"ignore"`；办理链读 `VAR_IGNORE`；任务窗终止**不**设 |
| 条件装配 | conditional on enable | `@ConditionalOnEnable` ← `warm-flow.enabled=true` |
| 失败关闭（本课） | fail closed | 驳回/终止找不到对象或状态非法就抛；**不是**合同窗那种 `NO_ACTIVE_INSTANCE` |

`runningStatus()` = draft / waiting / back / cancel。`finishStatus()` = finish / invalid / termination。`initialState()` = cancel / back / invalid / termination（监听器用它判断「已经是结果态，不必再猜 FINISH」）。

### 机制/因果链

#### 1. 十六扇窗怎么挂在门上

类上：`@ConditionalOnEnable` `@Validated` `@RequiredArgsConstructor` `@RestController` `@RequestMapping("/workflow/task")`，`extends BaseController`。只注入 `IFlwTaskService`。

| # | HTTP | 动词 | Java | 权限注解 | RepeatSubmit | 校验 | 成功形状 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `/startWorkFlow` | POST | `startWorkFlow` | 无 | 是 | `@Validated(AddGroup)`：`businessId`、`flowCode` 非空 | `R.ok("提交成功", StartProcessReturnDTO)` |
| 2 | `/completeTask` | POST | `completeTask` | 无 | 是 | AddGroup：`taskId` 非空 | `toAjax(boolean)` |
| 3 | `/pageByTaskWait` | GET | `pageByTaskWait` | 无 | 否 | 无 | `R<PageResult<FlowTaskVo>>` |
| 4 | `/pageByTaskFinish` | GET | `pageByTaskFinish` | 无 | 否 | 无 | `R<PageResult<FlowHisTaskVo>>` |
| 5 | `/pageByAllTaskWait` | GET | `pageByAllTaskWait` | `workflow:task:list` | 否 | 无 | 待办分页 |
| 6 | `/pageByAllTaskFinish` | GET | `pageByAllTaskFinish` | `workflow:task:list` | 否 | 无 | 已办分页 |
| 7 | `/pageByTaskCopy` | GET | `pageByTaskCopy` | 无 | 否 | 无 | 抄送分页 |
| 8 | `/getTask/{taskId}` | GET | `getTask` | 无（方法内读闸） | 否 | 路径 Long | `R<FlowTaskVo>`，找不到 **`R.ok(null)`** |
| 9 | `/getNextNodeList` | POST | `getNextNodeList` | 无 | 否 | 无分组 | 下一 **BETWEEN** 节点 |
| 10 | `/terminationTask` | POST | `terminationTask` | 无 | 是 | **Controller 未 `@Validated`** | **`R.ok(Boolean)`**，不是 `toAjax` |
| 11 | `/taskOperation/{taskOperation}` | POST | `taskOperation` | 无 | 是 | `@Validated` 类级；分组在链里 | `toAjax` |
| 12 | `/updateAssignee/{userId}` | **PUT** | `updateAssignee` | `workflow:task:edit` | 是 | 无 | `toAjax`；空列表 `false` |
| 13 | `/backProcess` | POST | `backProcess` | 无 | 是 | AddGroup：`taskId` 非空 | `toAjax` |
| 14 | `/getBackTaskNode/{taskId}/{nowNodeCode}` | GET | `getBackTaskNode` | 无 | 否 | 路径 | 可驳回节点列表 |
| 15 | `/currentTaskAllUser/{taskId}` | GET | `currentTaskAllUser` | 无（读闸） | 否 | 路径 | `List<UserDTO>` |
| 16 | `/urgeTask` | POST | `urgeTask` | `workflow:task:edit` | **否** | **Controller 未走 AddGroup** | `toAjax` |

启动、办理、驳回、终止**方法上没有** `@SaCheckPermission`。不等于匿名：类上没有 `@SaIgnore`，仍过登录拦截。引擎内部还可能按办理人校验；后台邻居把变量 `ignore=true` 才能跳过。

`toAjax(false)` 来自 `BaseController`：`R.fail()`，没有业务文案。催办空名单就是这种「失败但没抛」。

#### 2. (b) `startWorkFlow`：开袋或续写

```text
@Transactional + @Lock4j(flowCode + businessId)
        │
        v
startPrepareRequest
  空 businessId →「启动工作流时必须包含业务ID」
  变量写入 initiator / initiatorDeptId / businessId
  按 businessId 查 FlowInstance（不问 flowCode）
        │
        ├─ startExists == true  → startResume
        │     checkStartStatus(已有实例状态)
        │     无当前任务 →「流程实例缺少任务…」
        │     mergeVariable + 更新实例 + saveOrUpdate bizExt
        │     返回 (instanceId, 当前第一任务 id)
        │
        └─ 无实例 → startPrepareInstance + startExecute
              未发布 →「流程【code】未发布…」
              读定义 ext.autoPass 写入变量
              空 businessCode 用当前毫秒数顶上
              insService.start(..., flowStatus=draft)
              任务数 0 →「流程启动失败，未生成任务」
              任务数 >1 →「请检查流程第一个环节是否为申请人！」
              返回 (新 instanceId, 唯一任务 id)
```

`checkStartStatus` 挡住：waiting（已在批）、finish、invalid、termination、空白。**放行** draft / back / cancel——这三种可以再启动（续写）。接口注释「启动任务」不要读成「每次都 insert 新实例」。

邻居 `WorkflowService.startWorkFlow(StartProcessDTO)` 只是转 Bo。`startCompleteTask` 是启动成功立刻再 `completeTask` 第一枪，带系统消息类型——那是合同窗组合拳，HTTP 任务柜台**没有**「启动并办理」这一扇。请假示例走 L-072。

#### 3. (b) `completeTask`：盖「过」

```text
@DSTransactional + @Lock4j(taskId)
        │
        v
completePrepare
  任务空 →「流程任务不存在或任务已审批！」
  实例空 →「流程实例不存在」
  把抄送名单 / 消息类型 / 通知塞进变量
  draft|cancel|back → variables.submit = true
  弹窗办理人写成  pass:nodeCode  与  back:nodeCode  两套键
        │
        v
completeExecute
  skipType=PASS，flowStatus=waiting，hisStatus=pass
  ignore / ignoreDepute / ignoreCooperate 从变量读取
  capture 附件 → taskService.skip → reconcileCreated
        │
        v
completeNeedAutoPass？
  实例变量 autoPass==true → 同一 LoginHelper 用户的后续待办
  递归 skip，留言改成「流程引擎自动审批！」
  自动过不继承抄送和通知，避免连发
```

方法在 LiteFlow 不抛时 **永远 `return true`**。失败=异常。附件：`WorkflowHistoryOssOwner.capture` 记下 skip 前已有历史主键；skip 后必须出现新历史行，且 `ext` 里的 OSS id 集合等于请求 `fileId`，再 `ossService.reconcileReferences("flow_his_task", hisId, …)`。对不上就抛「流程办理未生成预期的历史任务 / 附件与办理请求不一致 / 未持久化附件」，动态数据源事务回滚。单测 `WorkflowHistoryOssOwnerUnitTest.affectedWorkflowBoundariesUseDynamicDatasourceTransactions` 锁死 `completeTask` 与 `backProcess` 带 `@DSTransactional`。

#### 4. (b) `backProcess`：盖「退」——也是 (d) 的一半

没有 LiteFlow，没有 `@Lock4j`。

1. `flowTaskMapper.selectById(taskId)` 空 → `ServiceException("任务不存在！")`。
2. `insService.getById` 空 → `"流程实例不存在"`。
3. `BusinessStatusEnum.checkBackStatus(inst.getFlowStatus())`：
   - 已退回 / 已完成 / 已作废 / 已终止 / 已撤销 / 状态空白 → 抛对应「该单据已…」
   - **放行 waiting 与 draft**（draft 能驳回，是因为实例还在运行态集合里）
4. `applyNodeCode(definitionId)`：定义缺第一个 BETWEEN 节点 → 「流程定义缺少申请人节点」。
5. `FlowParams`：`skipType=REJECT`，`hisStatus=back`，`nodeCode=bo.getNodeCode()`，`hisTaskExt=fileId`。实例 `flowStatus`：**目标等于申请人节点 → `back`，否则 `waiting`**。
6. 同样 `capture` → `taskService.skip` → `reconcileCreated`。
7. 成功 `return true`。

监听器 `finish`：`hisStatus` 为 back 时仍可能发待办消息，但**退回到申请人**时 `shouldSendTaskMessage` 返回 false，只给发起人发结果消息（`WorkflowResultMessageEvent`），避免「已退回」和「你有新待办」两封一起砸到申请人桌上。若画线退回申请人而实例还不是 `initialState`，监听器会**再写一次**实例状态为 `back`。

**失败路径（d）要能当场说的出口：**

| 输入 | 闸 | 单据事后 |
| --- | --- | --- |
| 没有这条待办 | 「任务不存在！」 | 不 skip |
| 有任务无实例 | 「流程实例不存在」 | 不 skip |
| 实例已是 back/finish/invalid/termination/cancel | `checkBackStatus` 文案 | 不 skip |
| 目标节点编码在 BO 里，但注释假装没用 | 仍按 `nodeCode` 跳 | 跳错节点是调用方的锅，不是「总是退申请人」 |
| 引擎 `FlowException` | `FlowExceptionHandler` → `R.fail(message)` | `@DSTransactional` 回滚 |
| skip 成功但附件对不上 | `ServiceException` | 回滚，历史行与 OSS 引用不落地 |
| HTTP 连点 | `@RepeatSubmit` | 第二枪进不了方法 |
| 并发两枪同一 taskId | **服务层无 Lock4j** | 引擎层行为属厂商；不要口述成「和 complete 一样锁了 taskId」 |

`BackProcessBo.variables` 在 ServiceImpl **没有**塞进 `FlowParams.variable`。塞进去的只有 `messageType` / `messageNotice`。不要说「驳回会把前端 variables 原样合并进实例」。

#### 5. (b) `terminationTask`：盖「终止」——也是 (d) 的另一半

```text
@Transactional(rollbackFor = Exception.class)   // 不是 DSTransactional
无 Lock4j
        │
        v
taskService.getById(taskId)  空 →「任务不存在！」
        │
        v
实例非空 → checkInvalidStatus
  finish / invalid / termination / 空白 → 抛
  放行 draft / waiting / back / cancel
实例为空 → 跳过状态闸，仍去 termination(taskId)
        │
        v
FlowParams.message=comment
          .flowStatus=termination
          .hisStatus=termination
  没有 ignore(true)
taskService.termination(taskId)
return true
```

Controller 返回 `R.ok(boolean)`。成功时 body 是 `true`，不是 `toAjax` 那种无 data 的 `R.ok()`。`FlowTerminationBo` 是 record，字段 `taskId` 带 AddGroup `@NotNull`，但窗上**没有** `@Validated(AddGroup.class)`。`taskId==null` 时 `getById(null)` 走「任务不存在」，不是 Bean Validation 400。

**和三扇「看起来都像终止」的门对表：**

| 门 | 钥匙 | 引擎 API | ignore | 终态 | 找不到时 |
| --- | --- | --- | --- | --- | --- |
| **本课** `POST /workflow/task/terminationTask` | taskId | `termination(taskId)` | 否 | `termination` | 抛「任务不存在」 |
| L-069 `WorkflowService.terminateInstance` | businessId + reason | `terminationByInsId` | **是** | `termination` | `NO_ACTIVE_INSTANCE`（缺实例或已 finish/invalid/termination） |
| L-069 `FlwInstanceController` 作废 | 实例 id | `terminationByInsId` | 是 | **`invalid`** | 实例空仍可能调用引擎；闸是同一套 `checkInvalidStatus` |
| L-069 撤销 `cancelProcessApply` | 申请撤销 | 另一条 | — | **`cancel`** | `checkCancelStatus` |

合同测试 `WorkflowTerminationContractTest` 锁的是 **`WorkflowServiceImpl.terminateInstance`**：空白参数 `IllegalArgumentException`、引擎抛错不得报成功、已终态不调引擎、方法上 `@DSTransactional` + 锁 `'workflow:terminate:' + #businessId`。**不要**把这份测试说成 `IFlwTaskService.terminationTask` 的证据。任务窗没有那把业务 ID 锁，也没有幂等枚举。

**失败路径（d）要能当场说的出口：**

| 输入 | 闸 | 单据事后 |
| --- | --- | --- |
| 任务不存在 | 「任务不存在！」 | 不 termination |
| 实例已 finish/invalid/termination | 「该单据已完成/作废/终止」 | 不 termination |
| 实例状态空白 | 「流程状态为空！」 | 不 termination |
| 实例已是 back 再点终止 | **放行** | 可以终止一张已退回的单 |
| 办理人不是自己且未 ignore | 引擎权限失败 `FlowException` | 回滚 |
| 引擎抛错 | 原样冒泡 / `R.fail` | 不得把 HTTP 200 + true 说成已终止 |
| 与合同窗同时打 | 两把锁不是同一把 | 任务窗不串 `workflow:terminate:{businessId}` |
| RepeatSubmit | 挡连点 | 挡的是同一浏览器连点，不是跨模块并发 |

任务窗终止**不**走 `WorkflowHistoryOssOwner`。不要把办理/驳回的附件回滚故事套到终止上。

#### 6. (b) `urgeTask`：按铃，不改章

权限 `workflow:task:edit`。无事务、无锁、无 RepeatSubmit。空 `taskIdList` → `false`。`currentTaskAllUser` 会对每个 id 做读闸；一个都看不见 → `false` → `R.fail()`。有人则 `flwCommonService.sendMessage(messageType, message, "单据审批提醒", users, PATH_TASK_WAITING)` → `true`。

BO 上 `taskIdList` / `message` 的 `@NotNull(AddGroup)` 在这扇窗**没启用**。空内容仍可能进 Service，然后 `false`。

#### 7. 其余十一扇，口试要能指，不必当 (b)

- **待办 SQL** `FlwTaskMapper.getListRunTask`：只要 BETWEEN 节点，办理人类型 `"1"|"2"|"3"`（审批/转办/委托）。`userId` 非空时再 `flow_status = waiting`。全楼待办把 `userId` 传 null，waiting 锁消失。
- **已办 SQL** `FlwHisTaskMapper.getListFinishTask`：排除历史状态 copy；我的已办再限制 BETWEEN + `approver=userId`。接口 `pageByTaskFinish` 的 JavaDoc 写成「查询当前租户所有待办」——**注释错了**，实现是已办。
- **抄送** 类型 `"4"`，走历史任务关联，不和待办混表。`setCopy` 若找不到历史行会抛「流程历史任务不存在，无法添加抄送记录」。
- **getTask**：任务空返回 null（HTTP 仍 200）。非空则读闸、补按钮权限/默认抄送/节点变量/`applyNode` 布尔。
- **getNextNodeList**：合并实例变量与请求变量，只返回 BETWEEN；顺手 `ExpressionUtil.evalVariable` 把办理人填进 `permissionFlag`。
- **getBackTaskNode**：任务空抛。当前节点找不到就原样返回空列表。任务上若已有 **委托（DEPUTE）关系人**，直接返回**当前节点自己**，不走前置搜索。节点配了 `anyNodeSkip` 则只返回那个固定驳回点。否则沿 skip 边倒着搜历史里出现过的 BETWEEN。
- **taskOperation**：非法 path 变量 → `"Invalid operation type …"`。委派/转办校验 AddGroup（要 `userId`）；加签/减签校验 EditGroup（要 `userIds`）。或签节点禁止加/减签。超管 `flowParams.ignore(true)`。引擎：`depute` / `transfer` / `addSignature` / `reductionSignature`。
- **updateAssignee**：先删这些任务的办理人，再全部写成审批人类型 `"1"` 指向同一个 `userId`。空列表 `false`。

### 图、表或文本图

**图题 / caption：** 宏观任务柜台十六扇。alt：`/workflow/task` 下一排查询窗和一排盖章窗；启动办理走 LiteFlow；驳回终止催办走 ServiceImpl。

```text
                    /workflow/task
        ┌───────────────┼───────────────┐
        │ 查询                              │ 盖章
        │ GET wait/finish/copy              │ POST startWorkFlow
        │ GET allWait/allFinish (list权)    │ POST completeTask
        │ GET getTask / backNodes / users   │ POST backProcess
        │ POST nextNodeList                 │ POST terminationTask
        │                                   │ POST urgeTask (edit权)
        │                                   │ POST taskOperation/{op}
        │                                   │ PUT  updateAssignee/{userId}
        └───────────────┬───────────────┘
                        v
                 IFlwTaskService
        start/complete ──► liteflow/task-chain.el.xml
        back/terminate ──► TaskService.skip / termination
        urge           ──► sendMessage only
```

**文字等价物：** 图顶是共享前缀 `/workflow/task`。左列只读：三扇「我的」列表、两扇「全楼」列表（要 list 权）、详情、可驳回节点、当前办理人、下一节点。右列会改世界：启动、办理、驳回、终止、催办、四种任务操作、批量换人。启动和办理的箭头指向 XML 链；驳回和终止指向引擎；催办指向消息，不指向引擎写。

**图的边界：** 不画 `/workflow/instance/*`。不画 `WorkflowService.terminateInstance`。不把厨房方法名画进右列当 HTTP。不保证每个 App 都打齐十六扇；admin-web 厨房在 L-073。

**图题 / caption：** 驳回失败与终止失败停在闸前。alt：backProcess 与 terminationTask 在 skip/termination 之前的对象闸和状态闸；对照合同窗幂等终止。

```text
POST /backProcess {taskId, nodeCode, fileId, message}
    任务空 ──────────────► 抛「任务不存在」     实例不动
    实例空 ──────────────► 抛「流程实例不存在」 不 skip
    状态∈{back,finish,invalid,termination,cancel,空白}
                       ► checkBackStatus 文案   不 skip
    否则 capture → skip(REJECT) → reconcile
         引擎/附件失败 ► 抛 + DS 事务回滚

POST /terminationTask {taskId, comment}
    任务空 ──────────────► 抛「任务不存在」     不 termination
    有实例且 ∈{finish,invalid,termination,空白}
                       ► checkInvalidStatus     不 termination
    否则 termination(taskId) 无 ignore
         引擎失败 ► 抛；无 NO_ACTIVE_INSTANCE

对照（非本格）：WorkflowService.terminateInstance(businessId, reason)
    空白参数 ► IllegalArgumentException
    无实例/已终态 ► NO_ACTIVE_INSTANCE，不调引擎
    运行态 ► terminationByInsId + ignore + 业务ID锁
```

**文字等价物：** 左列驳回必须先拿到活着的任务和实例，再过「不能已经是结果/撤销」的闸，最后才让引擎按 REJECT 跳到 `nodeCode`；附件对账失败会把刚才的跳转一起撤掉。中列终止只认任务号，终态单据拒绝再终止，已退回的单据反而可以终止；找不到任务就是错，不是幂等成功。右列是邻居总机：它认档案袋编号，缺袋子或袋子已结束都算「没有活动实例」，并且会忽略办理权限、按业务 ID 串行。口试 (d) 只要求左列加中列；右列用来防止说成同一扇门。

**图的边界：** 不画作废 `invalid`、撤销 `cancel` 的实例窗。不画厂商 `skip` 内部图。不把 RepeatSubmit 画成分布式锁。

## 正例、反例与边界

**正例 1：** 数 16 个映射。打开 `FlwTaskController.java`，从 `startWorkFlow` 数到 `urgeTask`。对照矩阵 (a) 那一行十六个名字，一个不漏。确认 `updateAssignee` 是 `@PutMapping`，催办是唯一带 `workflow:task:edit` 的 POST。

**正例 2：** 接口比窗多。打开 `IFlwTaskService.java`，数到 23。圈出没有 `@GetMapping/@PostMapping` 的：`setCopy`、`selectByIdList`、`selectHisTaskById`、`selectByInstId`、`selectByInstIds`、`isTaskEnd`、`getByNodeCode`。`isTaskEnd` 被监听器拿去判断该不该把实例写成 finish。

**正例 3：** 启动链在 XML。打开 `liteflow/task-chain.el.xml` 的 `startProcessChain`：`startPrepareRequest` → `IF(startExists, startResume, THEN(startPrepareInstance, startExecute))`。打开 `StartPrepareRequestComponent`：查询条件只有 `FlowInstance::getBusinessId`。

**正例 4：** 续提交挡「审批中再开」。`StartResumeComponent` 第一句 `checkStartStatus`。打开 `BusinessStatusEnum.checkStartStatus`：waiting 文案「该单据已提交过申请,正在审批中！」。

**正例 5：** 新开实例是 draft，首节点必须一人。`StartExecuteComponent`：`flowStatus(DRAFT)`；`taskList.size()>1` 抛「请检查流程第一个环节是否为申请人！」。

**正例 6：** 办理任务空的文案带「或任务已审批」。`CompletePrepareComponent` 第 48 行，和驳回的「任务不存在！」不是同一句。

**正例 7：** 草稿再办等于重新提交。同文件 `isDraftOrCancelOrBack` 时 `variables.put(SUBMIT, true)`。监听器看到 submit 会发「申请人提交」那类流程事件。

**正例 8：** 办理/驳回动态事务。`WorkflowHistoryOssOwnerUnitTest.affectedWorkflowBoundariesUseDynamicDatasourceTransactions` 断言 `completeTask`、`backProcess` 有 `@DSTransactional`。`terminationTask` **不在**这个断言里。

**正例 9：** 驳回目标节点真的用了。`FlwTaskServiceImpl.backProcess`：`.nodeCode(bo.getNodeCode())`，并且 `applyNodeCode.equals(bo.getNodeCode()) ? BACK : WAITING`。打开 `BackProcessBo` 那行过期注释，用实现否定它。

**正例 10：** 终止任务窗没有 ignore。`terminationTask` 的 `FlowParams.build()` 只有 message / flowStatus / hisStatus 三行。对照 `WorkflowServiceImpl.terminateInstance` 第四行 `.ignore(true)`。

**正例 11：** 催办不写任务。`urgeTask` 方法体：空列表 false；`currentTaskAllUser`；`sendMessage`；true。没有 `taskService.` 调用。

**正例 12：** 厨房 URL 对齐十六扇。`frontend/packages/domains/workflow/src/index.test.ts` 期望列表含 `post /workflow/task/startWorkFlow` … `urgeTask`，以及 `get /workflow/task/getTask/task%2F1`。格子仍只认 Java 窗，不认工厂。

**正例 13：** 合同窗终止幂等。`WorkflowTerminationContractTest.missingOrTerminalInstanceIsAnIdempotentNoActiveResult`：`verify(engineTasks, never()).terminationByInsId`。用它对照任务窗「任务不存在就抛」。

**正例 14：** classic 房间。登记表 `03-backend-module-modes.md`：`wta-modules/wta-workflow` = classic。Controller 包在 `controller/`，服务在 `service/impl/`，没有 `usecase/`。

**反例 1：** 「`IFlwTaskService` 有的方法 Controller 都有。」少了 7 个内部法。`setCopy` 由 `WorkflowSideEffectListener.handleCopy` 调用。

**反例 2：** 「启动每次 new 一个实例。」同一 `businessId` 走 `startResume`。

**反例 3：** 「续提交会检查你这次带来的 `flowCode` 是否同一张图纸。」查找键只有 `businessId`。

**反例 4：** 「`completeTask` 失败返回 `false`。」失败抛异常；成功恒 `true`。`false` 出现在催办空名单和空的换人列表。

**反例 5：** 「驳回总是退回申请人，`nodeCode` 没用。」实现用了。注释过期。

**反例 6：** 「`POST /terminationTask` 就是 `WorkflowService.terminateInstance`。」钥匙、引擎 API、ignore、锁、幂等、返回类型全不同。

**反例 7：** 「终止已退回的单会 `checkBackStatus`。」终止走 `checkInvalidStatus`，back **允许**终止。`checkBackStatus` 只属于驳回。

**反例 8：** 「催办会把任务标成超时/催办状态。」`TaskStatusEnum.TIMEOUT` 存在，催办方法不用它。

**反例 9：** 「我的待办和全楼待办只差 userId。」我的待办还有 `eq flow_status waiting`。全楼待办没有。

**反例 10：** 「`getTask` 找不到会抛，和办理一样。」`selectById` 返回 null，Controller `R.ok(null)`。

**反例 11：** 「任务窗终止也 `@DSTransactional` 并协调 OSS。」普通 `@Transactional`，不调 `WorkflowHistoryOssOwner`。

**反例 12：** 「本课覆盖 `createWorkflowDefinitionService`。」那是 OBJ-73。厨房把实例窗、请假窗也塞进同一座工厂，更不能整座盖章。

**反例 13：** 「作废和终止是同一扇 HTTP。」作废在实例柜台 `processInvalid`，终态 `invalid`。本课终态 `termination`。

**反例 14：** 「`pageByTaskFinish` 按接口注释是全租户待办。」注释错。实现是已办历史。

**边界：**

- `warm-flow.enabled=false`：整类不加载。LiteFlow `enable` 跟着同一开关，避免禁用后仍解析任务链。
- `startWorkFlow` 的锁是 `flowCode + businessId` 字符串拼接，不是 `'workflow:terminate:'` 那种前缀。不同 flowCode、同一 businessId 的两把锁**不是**同一把——但续提交查找又只认 businessId。这是口试里的裂缝，不要抹平。
- 办理链读 `ignore`，后台 `WorkflowService.completeTask(taskId, message)` 会塞 `ignore=true`。浏览器厨房的 `completeTask` 默认不塞。不要把「系统代办」和「人在窗口盖章」说成同一权限模型。
- `getBackTaskNode` 在已委托任务上返回当前节点列表：产品含义是「委托人场景不提供自由前置驳回菜单」，不是「可以驳回到自己」。
- 抄送消息走 `PATH_TASK_COPY`，催办走 `PATH_TASK_WAITING`，结果消息（完成/退回）发给发起人。三条路径不要混。
- 矩阵 (a) 把十六扇聚成一行；函数表仍要逐方法。本课 (b) 只点名五支，其余十一扇是 (a) 的地图，不是第二组 (b)。
- `FlowUrgeTaskBo` 的类注释写成「流程变量参数」——又一份过期注释。以字段 `taskIdList` / `messageType` / `message` 为准。

## 变式与迁移

- **变式 A：业务模块要在后台无登录人时往前推一格。** 走 `WorkflowService.completeTask` 并自己把 `ignore=true` 放进变量（合同窗已有一枪只带 taskId+留言的重载）。不要给浏览器任务窗偷偷加 ignore，那会让任意登录者代过别人的待办。

- **变式 B：按档案袋编号终止，且「没有单 / 已经结束」都算成功。** 那是 L-069 的 `terminateInstance`。不要复用 `POST /workflow/task/terminationTask` 再自己吞「任务不存在」。

- **变式 C：人在待办页点退回。** 先 `GET /getBackTaskNode/{taskId}/{nowNodeCode}` 拿菜单，再 `POST /backProcess` 带选中的 `nodeCode`。不要相信 BO 注释直接写死申请人。退到申请人后实例应是 `back`，申请人再办会走 `submit=true`。

- **变式 D：只要提醒，不要改状态。** `urgeTask`。要 list/edit 权。空办理人名单是 `R.fail()`，不是 200。

- **变式 E：同一人连续多个节点。** 定义 ext 开 `autoPass`。只自动过**当前登录人**仍是办理人的后续任务，并行别人的分支不会被代过。自动过的留言是固定中文，不会复用你刚才写的意见。

- **变式 F：启动并立刻办掉第一格。** HTTP 任务柜台没有这一扇。合同窗 `startCompleteTask` 或请假 `submitAndFlowStart`（L-072）。不要在口试里发明 `/startWorkFlowAndComplete`。

- **变式 G：换办理人。** `PUT /updateAssignee/{userId}` body 是任务 id 列表。它把类型统一写成审批人 `"1"`，委托/转办关系会被清掉。要保留委托语义应走 `taskOperation/delegateTask`。

- **变式 H：只读监控。** 全楼待办/已办要 `workflow:task:list`。详情仍可能靠同一权限绕过「必须是关系人」。不要把 list 权理解成只能看分页不能看详情——`checkTaskReadAccess` 把 list/edit 当成管理读取范围。

- **迁移口诀：** 先数十六扇 HTTP → 再数服务 23 法、7 个无窗 → 再分三条 LiteFlow 与三支手写枪 → 启动认袋子编号、盖章认号票 → 驳回闸 `checkBackStatus`、终止闸 `checkInvalidStatus` → 任务窗终止非幂等、合同窗终止幂等 → 催办只按铃。跳步会出现「把 terminateInstance 说成本课」「把驳回说成总是退申请人」「把催办说成改了状态」。

## 常见误区

1. **「`FlwTaskController.*` 就是五支枢纽。」** 十六扇。五支是 (b)。查询窗也要能指路径。
2. **「启动等于 insert 实例。」** 可能是续提交。
3. **「续提交按 flowCode+businessId 查实例。」** 查实例只按 businessId；锁键才拼了 flowCode。
4. **「办理失败返回 false，单据半通过。」** 失败抛；成功恒 true；事务回滚。
5. **「驳回不用 nodeCode。」** 用。注释骗你。
6. **「终止、作废、撤销是同义词。」** 三态：`termination` / `invalid` / `cancel`。三扇不同的窗。
7. **「`terminationTask` 幂等。」** 任务不在就抛。幂等的是合同窗。
8. **「任务窗终止 ignore 了权限。」** 没有 `.ignore(true)`。
9. **「催办会写 `TaskStatusEnum`。」** 只 `sendMessage`。
10. **「我的待办 = 全楼待办 minus 权限。」** 差在 waiting 锁和 userId，不只差注解。
11. **「`getTask` 404。」** 200 + data null。
12. **「本课覆盖 `WorkflowService.*`。」** L-069。本课只借用对照。
13. **「本课覆盖厨房工厂。」** L-073。
14. **「LiteFlow 也编排驳回。」** XML 三条链：start / complete / taskOperation。
15. **「`wta-workflow` 该改成 layered。」** 登记表 classic；本课不发动重构。
16. **「附件失败只丢文件，流程已经跳走。」** 办理/驳回在同一 `@DSTransactional` 里 reconcile，失败回滚。
17. **「`checkBackStatus` 和 `checkInvalidStatus` 挡住同一组状态。」** 驳回额外挡 back 与 cancel；终止不挡 back/cancel/draft/waiting。
18. **「超管才能启动/办理。」** 这四扇写窗方法上无权限字；登录即可打到 Service，再交给引擎办理人校验。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `FlwTaskController.java`。用手指点 16 个映射。把五支枢纽（start / complete / back / urge / termination）圈出来。圈 `updateAssignee` 的 PUT、`urgeTask` 的权限、`terminationTask` 的 `R.ok(Boolean)`、催办没有 `@RepeatSubmit`。
2. 打开 `IFlwTaskService.java`。数 23。把没有对应映射的 7 个名字抄在纸上。打开 `WorkflowSideEffectListener`，确认 `setCopy` 从这里进。
3. 打开 `liteflow/task-chain.el.xml`。确认没有 back/terminate/urge 链。顺着 `StartPrepareRequestComponent` 圈 `eq(FlowInstance::getBusinessId)`。顺着 `StartExecuteComponent` 圈 `DRAFT` 和「第一个环节是否为申请人」。
4. 打开 `FlwTaskServiceImpl.backProcess` 与 `terminationTask`。圈 `checkBackStatus` / `checkInvalidStatus`、`SkipType.REJECT`、`taskService.termination`、有没有 `ignore`、有没有 `@Lock4j`、事务注解是哪一种。打开 `BackProcessBo` 那行「未使用」注释，对照 `.nodeCode(bo.getNodeCode())`。
5. 打开 `BusinessStatusEnum` 四套 check。把「驳回放行 waiting/draft、终止放行 back/cancel」写在纸上。打开 `WorkflowServiceImpl.terminateInstance`，对照幂等和锁键。打开 `FlowExceptionHandler`，确认引擎异常变 `R.fail`。
6. 打开 `FlwTaskMapper.getListRunTask` 带 `userId` 时的 `WAITING` 条件，对照 `pageByAllTaskWait` 传入的 `null`。打开 `urgeTask`，确认没有 `taskService`。打开厨房测试里十六扇 URL，只作为对照，不把工厂标进本格。

## 总结、词汇表与下一步

- **宏观任务枢纽：** 一块 `/workflow/task` 牌子，十六扇 classic 窗。查询认列表和读闸；盖章认号票（taskId）和档案袋（启动时的 businessId）。
- **(a) `FlwTaskController.*`：** 磁盘 16 法。矩阵这一行写全了。PUT 只有换人。全楼列表和催办/换人才有权限字。启动/办理/驳回/终止/操作有 RepeatSubmit。
- **(b) 五支枪：** 启动（锁 flowCode+businessId，续写或 draft 新开）；办理（锁 taskId，PASS + 可选 autoPass + OSS）；驳回（REJECT，无任务锁，OSS，状态闸 checkBackStatus）；催办（只消息，可 `false`）；终止（termination(taskId)，无 ignore，无业务 ID 锁，状态闸 checkInvalidStatus）。
- **(d) 驳回/终止失败：** 对象不在就抛；非法状态就抛；引擎/附件失败回滚。**不是**合同窗的 `NO_ACTIVE_INSTANCE`。已退回可终止、不可再驳回。
- **不是实例窗，不是请假示例，不是厨房工厂，不是厂商引擎内部。**

词汇表：`FlwTaskController` / `IFlwTaskService` / `startWorkFlow` / `completeTask` / `backProcess` / `urgeTask` / `terminationTask` / `StartProcessReturnDTO` / `businessId` / `taskId` / `SkipType` / `BusinessStatusEnum` / `TaskStatusEnum` / `checkBackStatus` / `checkInvalidStatus` / `startProcessChain` / `completeTaskChain` / `WorkflowHistoryOssOwner` / `WorkflowGlobalListener` / `WorkflowService.terminateInstance` / `@ConditionalOnEnable` / `@DSTransactional` / `@Lock4j`。

下一步：分类树是 OBJ-67。图纸发布/导入是 OBJ-68。实例窗与 `WorkflowService` 跨模块合同是 OBJ-69（含按业务 ID 的幂等终止）。SpEL 目录是 OBJ-71。请假 `submitAndFlowStart` 是 OBJ-72。厨房与菜单如何喊这十六扇是 OBJ-73。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` Controller / Service / `wta-api` | 任务柜台公开入口与 classic 实现 | `wta-workflow` controller/service | 2026-09-17 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-workflow` 登记 classic，不改内部层次 | 登记表 workflow 行 | 2026-09-17 |
| S-015 | `backend/wta-api/.../workflow/api` | `WorkflowService` 合同窗与 `StartProcessReturnDTO`；本课只对照 | `WorkflowService.java`、`StartProcessReturnDTO.java` | 2026-09-17 |
| S-L070-01 | `.../controller/FlwTaskController.java` | 十六扇映射、权限、RepeatSubmit、返回形状 | 全文件 16 个 `@*Mapping` | 2026-09-17 |
| S-L070-02 | `.../service/IFlwTaskService.java` 与 `impl/FlwTaskServiceImpl.java` | 23 法、五支枢纽事务/锁、驳回/终止闸、催办不写库 | 接口全文件；实现 `startWorkFlow`…`urgeTask` | 2026-09-17 |
| S-L070-03 | `.../resources/liteflow/task-chain.el.xml` 与 `liteflow/start|complete|operation/*` | 三条链；启动按 businessId 续写；办理 PASS+autoPass | XML 与各 `*Component` | 2026-09-17 |
| S-L070-04 | `BusinessStatusEnum` | `checkStartStatus` / `checkBackStatus` / `checkInvalidStatus` / running vs finish | `wta-common-core/.../BusinessStatusEnum.java` | 2026-09-17 |
| S-L070-05 | `WorkflowServiceImpl` + `WorkflowTerminationContractTest` | 合同窗终止幂等、ignore、业务 ID 锁；**不是**任务窗 | `terminateInstance`；测试类四例 | 2026-09-17 |
| S-L070-06 | `WorkflowHistoryOssOwner` + `WorkflowHistoryOssOwnerUnitTest` | 办理/驳回附件对账；`@DSTransactional` 锁 complete/back | `capture` / `reconcileCreated`；`affectedWorkflowBoundaries…` | 2026-09-17 |
| S-L070-07 | `WorkflowGlobalListener` / `WorkflowSideEffectListener` / `FlowExceptionHandler` | 办完抄送与退回申请人少发待办；引擎异常 `R.fail` | `finish` / `handleCopy` / `handleFlowException` | 2026-09-17 |
| S-L070-08 | `frontend/packages/domains/workflow/src/index.ts` 与 `index.test.ts` | 厨房 URL 对齐十六扇（对照，格子不认工厂） | `startWorkflow`…`urgeTask`；测试 URL 列表 | 2026-09-17 |

*生成依据：L-contract / OBJ-70 / 仓库源码核实。*
