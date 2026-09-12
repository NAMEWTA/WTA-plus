# 请假样例 TestLeave

仓库内唯一完整业务接入样例在 **workflow 模块内部**，不是 `wta-demo`。复制该模式到其他业务模块：注入 `WorkflowService`，换成自己的 `flowCode`。能力描述不清时读下列路径，不要凭空补步骤。

`leave1` 至 `leave6` 的流程定义 JSON 由父仓库 `release-artifacts/workflow/leave/` 统一拥有。需要节点编码时先读取对应版本资产，再对照运行库或设计器导出，不要编造网关条件。

## 源码位置

| 层 | 路径 |
|---|---|
| 实体 `test_leave`，含 `status` | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/domain/TestLeave.java` |
| BO（含可选 `flowCode`） | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/domain/bo/TestLeaveBo.java` |
| Mapper XML | `backend/wta-modules/wta-workflow/src/main/resources/mapper/workflow/TestLeaveMapper.xml` |
| 服务实现 | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/service/impl/TestLeaveServiceImpl.java` |
| 服务接口 | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/service/ITestLeaveService.java` |
| HTTP | `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/controller/TestLeaveController.java`（`@RequestMapping("/workflow/leave")`） |
| 前端领域资源 | `frontend/packages/domains/workflow/src/leave/index.ts`；service 合同与 transport 位于同包 `src/index.ts` |
| 前端领域页面 | `frontend/packages/web-domains/workflow/src/leave/LeaveListPage.vue`、`LeaveEditPage.vue` |
| Web manifest | `frontend/packages/web-domains/workflow/src/index.ts`（注册 `workflow/leave/index` 与 `workflow/leave/leaveEdit`） |
| 流程定义资产 | `release-artifacts/workflow/leave/leave1.json` 至 `leave6.json` |

控制器均 `@ConditionalOnEnable`。提交入口：`POST /workflow/leave/submitAndFlowStart`，权限 `workflow:leave:add`。

## HTTP

路径：`backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/controller/TestLeaveController.java`

| 能力 | HTTP | 权限 |
|---|---|---|
| 分页列表 | `GET /workflow/leave/list` | `workflow:leave:list` |
| 导出 | `POST /workflow/leave/export` | `workflow:leave:export` |
| 详情 | `GET /workflow/leave/{id}` | `workflow:leave:query` |
| 新增草稿 | `POST /workflow/leave` | `workflow:leave:add` |
| 提交并启动 | `POST /workflow/leave/submitAndFlowStart` | `workflow:leave:add` |
| 修改 | `PUT /workflow/leave` | `workflow:leave:edit` |
| 删除 | `DELETE /workflow/leave/{ids}` | `workflow:leave:remove` |

上表是当前 `TestLeaveController` 的真实存量合同，其中修改和删除仍使用 PUT/DELETE，属于工程规范 `MIG-CRUD-METHOD-LOG` 的待迁移状态。新增业务或实质修改该合同时必须改为 POST，并同步 controller、前端 transport、测试和 `@Log`；不要把样例的存量方法照抄为新接口。

## 提交并启动

`TestLeaveServiceImpl.submitAndFlowStart` 当前使用 Spring `@Transactional`：

1. 用起止日期计算 `leaveDays`（截止日期也算一天：`ChronoUnit.DAYS.between + 1`）。
2. `leaveMapper.insertOrUpdate(leave)`，先得到主键。
3. `bo.getParams().put("ignore", true)`（后端发起忽略权限）。
4. `StartProcessDTO.businessId = leave.id.toString()`；`flowCode` 默认 `"leave1"`，可由 `TestLeaveBo.flowCode` 覆盖。
5. `workflowService.startCompleteTask(startProcess)`：启动并办掉申请人首节点。失败抛 `ServiceException("流程发起异常")`。
6. 注释允许无登录用户时 `startProcess.setHandler("0")`（定时任务场景，当前未启用）。

不要先启动流程再落库。`businessId` 必须是已存在主键的字符串。

该事务注解也是当前存量状态；新增业务或实质修改时按工程规范使用 `@DSTransactional`，不要把 Spring `@Transactional` 复制到新业务事务。

新增草稿（不启动流程）：`insertByBo` 在 `status` 为空时写成 `BusinessStatusEnum.DRAFT`。

删除：`deleteWithValidByIds` 先 `leaveMapper.deleteByIds`，再 `workflowService.deleteInstance(ids 转字符串)`。

SpEL 辅助：`eval(leaveDays)` 返回 `leaveDays <= 2`，供流程定义判断请假天数。路径同 `TestLeaveServiceImpl`。

申请编号常量：`FlowConstant.BUSINESS_CODE`（`"businessCode"`）— `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/common/constant/FlowConstant.java`。

## 三个监听器

均在 `TestLeaveServiceImpl`，条件为 `flowCode.startsWith('leave')`。正式业务应改成 `== 'yourFlowCode'`（类内 JavaDoc 也如此建议）。

### ProcessEvent → 回写 status

```java
@EventListener(condition = "#processEvent.flowCode.startsWith('leave')")
public void processHandler(ProcessEvent processEvent)
```

- 按 `businessId` 加载 `test_leave`，`setStatus(processEvent.getStatus())`。
- `params` 可含 `hisTaskExt` / `handler` / `message`（样例只读取，未落库）。
- `submit==true`：若 `applyCode` 空则从 `FlowConstant.BUSINESS_CODE` 取；强制 `status=waiting`。
- 最后 `leaveMapper.updateById`。

### ProcessTaskEvent → 节点副作用占位

```java
@EventListener(condition = "#processTaskEvent.flowCode.startsWith('leave')")
public void processTaskHandler(ProcessTaskEvent processTaskEvent)
```

当前只打日志。注释预留 `if ("xxx".equals(processTaskEvent.getNodeCode()))`。新模块在这里按节点做副作用，不要改 `WorkflowGlobalListener`。

### ProcessDeleteEvent → 级联删行

```java
@EventListener(condition = "#processDeleteEvent.flowCode.startsWith('leave')")
public void processDeleteHandler(ProcessDeleteEvent processDeleteEvent)
```

按 `businessId` 查行，存在则 `deleteById`。从工作流侧删实例时走这条；业务侧 `deleteWithValidByIds` 会先删行再 `deleteInstance`，监听器需容忍行已不存在。

## 前端对应

`frontend/packages/domains/workflow/src/index.ts` 的 workflow service：

| 函数 | HTTP |
|---|---|
| `listLeaves` | `GET /workflow/leave/list` |
| `getLeave` | `GET /workflow/leave/{id}` |
| `addLeave` | `POST /workflow/leave` |
| `submitLeave` | `POST /workflow/leave/submitAndFlowStart` |
| `updateLeave` | `PUT /workflow/leave` |
| `deleteLeaves` | `DELETE /workflow/leave/{ids}` |

前端 PUT/DELETE 同样只是与当前后端存量合同对齐，不是新实现模板。

待办打开业务页使用任务的 `formPath`，query 携带业务 `id` / `taskId`，不是请假 service 自己查询待办。当前入口位于 `frontend/packages/web-domains/workflow/src/task/TaskListPage.vue`，实例入口位于 `src/instance/{InstancePage,MyDocumentPage}.vue`；导航由 workflow web-domain 通过 App runtime 执行。

## 复制到其他模块时改什么

保留：先落库 → `ignore=true` → `businessId=主键` → `startCompleteTask` 或 `startWorkFlow` → 三个 `@EventListener` → 删除时 `deleteInstance`。

替换：

- 实体表、`status` 字段、Service 类放到目标业务模块（该模块只需依赖 `wta-api`）。
- `flowCode` 改为已发布定义编码，监听条件改为精确匹配。
- `eval(leaveDays)` 换成自己的 SpEL Bean 方法，或不用。
- 前端：在所属 domain 增加业务服务，在对应 web-domain 增加页面/manifest，并由目标 App 显式组合；流程定义的 `formPath` 指向注册路由。
- HTTP 变更统一使用 POST 并配置准确、安全的 `@Log`；业务事务使用 `@DSTransactional`。不要复制本样例仍待迁移的 PUT/DELETE 和 Spring `@Transactional`。
- 不要把 `ITestLeaveService` 当公共门面，不要把请假 Controller 留在 `wta-workflow` 当新业务入口。
