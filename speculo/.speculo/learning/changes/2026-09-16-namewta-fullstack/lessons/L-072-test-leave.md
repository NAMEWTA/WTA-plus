---
lesson_id: L-072
objective_ids: [OBJ-72]
claimed_cells:
  - A:TestLeaveController.list,export,get,add,submitAndFlowStart,edit,remove
  - B:TestLeaveController.submitAndFlowStart
  - D:主路径 工作流请假
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: seven-doors-on-disk
    minutes: 8
  - segment: write-start-and-happy-path
    minutes: 12
  - segment: visuals-and-worked-examples
    minutes: 7
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-008, S-010, S-015, S-L072-01, S-L072-02, S-L072-03, S-L072-04, S-L072-05, S-L072-06, S-L072-07, S-L072-08]
---

# Lesson 072：宏观请假样例——七扇 `TestLeaveController` 与写单并发流

## 学完你能做什么

打开 `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/controller/TestLeaveController.java`，你能**口述请假示例柜台的七扇公开窗**，再单独把「写请假并发起流程」说完，最后把矩阵主路径 **`TestLeave.submitAndFlowStart → 任务完成`** 从落库走到申请人首枪办掉、单据变成待审。不要把「保存草稿」说成已经开火，也不要把浏览器「提交审批」那条两枪路径说成 `submitAndFlowStart`。

口试名单就是这三格，符号以**磁盘**为准：

1. **`A:TestLeaveController.list,export,get,add,submitAndFlowStart,edit,remove`**：一块门牌 `@RequestMapping("/workflow/leave")`，**正好七**个公开 HTTP 方法。矩阵把详情窗写成 `get`，Java 方法名是 **`getInfo`**，路径是 `GET /{id}`。口试按磁盘七扇，不要把 `ITestLeaveService.queryList` 或实现类上的 `eval` 背成第八扇 REST。
2. **`B:TestLeaveController.submitAndFlowStart`**：矩阵原文「写请假并发起流程」。同一事务里 `insertOrUpdate` 请假行，再喊 `WorkflowService.startCompleteTask`。失败抛「流程发起异常」，整张单回滚。
3. **`D:主路径 工作流请假`**：矩阵原文 `TestLeave.submitAndFlowStart → 任务完成`。本课把这条河从请假柜台走到**申请人节点被办掉、请假行写成 `waiting`、下一格待办出现**。组长/主管后面那些盖章枪属于 L-070 的任务窗，本课只认它们是主路径的下游，**不**把 `FlwTaskController.*` 标 covered。

模块模式是 **classic**：`Controller → ITestLeaveService → TestLeaveServiceImpl`（自己抱 `TestLeaveMapper`，流程只经 `wta-api` 的 `WorkflowService`）。登记表把 `wta-modules/wta-workflow` 标 classic：「保持现状；通过公开 Workflow API 接入，不改内部层次」。磁盘上**没有** UseCase、**没有** DAO。`TestLeaveMapper.xml` 是空壳。请假样例虽然和引擎住同一间房间，也**不许**注入 `IFlwTaskService`。

`@ConditionalOnEnable`：`warm-flow.enabled=true` 这整份 Controller、ServiceImpl 才进 Spring。关掉开关，七扇窗整排消失，监听器也不再回写 `test_leave`。

本课不宣称你会拆分类（L-067）、定义发布/导入（L-068）、实例大厅与 `WorkflowService.*` 整份合同（L-069）、任务枢纽十六扇（L-070）、SpEL 目录（L-071）、或厨房 `createWorkflowDefinitionService` / 菜单 `createWorkflowWebDomain`（L-073）。厨房 URL 和请假页只当**对照**：证明七扇里哪些被浏览器扣了扳机、哪条按钮其实没打本课这枪。Warm-Flow 厂商 `skip` 内部标 deferred。

OBJ-72 原文只要你能口述 `TestLeaveController` 含 `submitAndFlowStart`。本课还要把 **(b) 写单并发流** 和 **(d) 请假主路径**钉到方法、事务、变量和监听器，否则「能口述」会退化成背路径。

## 先把宏观地图放在桌上

L-003 已经把 `wta-workflow` 钉在 classic 列。L-069 把请假标成「对讲机的活样本」，没拆这七扇。L-070 说过任务柜台**没有**「启动并办理」这一扇 HTTP，那一枪在合同窗 `startCompleteTask`，请假样例是它的业务包装。本课走进**请假柜台**：一张自己的业务表 `test_leave`，七扇自己的 HTTP，流程一律打电话给 `WorkflowService`。

四条河都叫请假，货不一样：

| 名字听起来像 | 实际是什么 | 本课认不认 |
| --- | --- | --- |
| `TestLeaveController` 七扇 | `/workflow/leave` 的 REST | **本课 (a)** |
| `submitAndFlowStart` 写单并发流 | 落 `test_leave` + `startCompleteTask` | **本课 (b)** |
| `submitAndFlowStart → 任务完成` | 申请人首枪被办掉，单据 `waiting` | **本课 (d)** |
| `POST /` 新增草稿 | 只写行，默认 `draft`，**不开火** | 本课 (a) 要能指；不是 (b) |
| 浏览器「提交审批」 | `addLeave`/`updateLeave` 再 `startWorkflow` | L-073 对照；**不是** (b) |
| `WorkflowService.startCompleteTask` | 合同窗组合拳 | L-069 的格子；本课认请假怎么喊它 |
| `FlwTaskController.completeTask` | 组长/主管盖章 | L-070；主路径下游，不盖章本格 |
| `eval(leaveDays)` | 实现类给 leave6 的 SpEL | 对照；不是 HTTP，不是 L-071 目录 |
| `createWorkflowDefinitionService.submitLeave` | 厨房一枪对齐本课 (b) | L-073 |

2026-09-17 工作树：权威 Controller 就是上面那一份。权威接口 `ITestLeaveService` 七法（比窗多一个内部 `queryList`，给导出用）。权威实现 `TestLeaveServiceImpl`。权威表 `test_leave`，DDL 在 `release-artifacts/docker/infrastructure/mysql/init/30-cde-workflow.sql`。权威示例图纸 `release-artifacts/workflow/leave/leave1.json` … `leave6.json`。默认编码 **`leave1`**。

```text
已登录的请假人 / 管理员
        │
        v
 /workflow/leave/*          TestLeaveController     七扇
        │
        v
 ITestLeaveService          TestLeaveServiceImpl    classic
        │
        ├─ query* / export  ──► TestLeaveMapper（空 XML）
        ├─ insertByBo       ──► 算天数、applyCode、默认 draft
        ├─ submitAndFlowStart ──► insertOrUpdate
        │                         params.ignore=true
        │                         WorkflowService.startCompleteTask
        ├─ updateByBo       ──► 只改行，不算天数，不开火
        └─ delete           ──► 先删行，再 deleteInstance
                                      │
                                      v
                               wta-api WorkflowService
                               start + 办申请人第一枪
                                      │
                                      v
                               ProcessEvent (flowCode startsWith leave)
                               回写 test_leave.status
```

**类比：** 请假柜台是学校门口的**假条窗口**。保存草稿=把假条塞进抽屉，条上盖「草稿」。后端发起=窗口阿姨自己把假条塞进抽屉，同时按对讲机「按这张学号开火，并把申请人那一格代盖掉」。之后年级组长、主任还要盖章——那是任务楼的事。不要把假条窗口说成盖章柜台。

**类比失效边界：** 假条窗口**住在流程楼里**，不是档案科那种隔壁房间；但它仍然只拿对讲机，不自己跑进灶台。类比也不等于「页面上三个按钮都打同一枪」——「保存 / 提交审批 / 后端发起」三条河。类比还不等于返回体里的 `status` 已经是库里的 `waiting`：实现把内存对象转 Vo，监听器写库，两份不一定同步。

## 核心概念与机制

### 直觉讲解

先记住三张号码，再背方法名：

- **请假主键 `id`。** 业务房间自己的号。启动时变成字符串 `businessId`。详情窗路径类型是 **`Long`**。
- **申请编号 `applyCode`。** 新单用 `System.currentTimeMillis()` 拼成串。它**不是**流程变量里的 `businessCode`，除非调用方把它们写成同一个。
- **流程编码 `flowCode`。** 空则 **`leave1`**。页面下拉有 leave1…leave6，但「后端发起」那一枪默认**带不走**下拉值。

再记住两支笔：

- **`POST /workflow/leave`（add）。** 只写假条。天数重算。没状态就盖 `draft`。对讲机沉默。
- **`POST /workflow/leave/submitAndFlowStart`。** 写假条（有 id 就更新，没 id 就插入）**并且** `startCompleteTask`。变量里自己塞 `ignore=true`。合同窗**不会**代写 ignore。

小孩子版只记十二句：

1. **先数七扇。** 一块牌子 `/workflow/leave`。矩阵 `get` = `getInfo`。
2. **classic 三跳。** 门卫 → 接口 → `TestLeaveServiceImpl` 抱 Mapper。空 XML。
3. **开关。** `warm-flow.enabled`。Controller 和 ServiceImpl 都贴了。
4. **权限六颗。** `list` / `query` / `add` / `edit` / `remove` / `export`。提交和新增**共用** `add`。
5. **查用 GET，导出/提交用 POST，改用 PUT，删用 DELETE。** 写库的增/提交/改有 `@RepeatSubmit`（默认 5 秒）和 `@Log`。提交的日志类型也是 **INSERT**。删除走 `toAjax(boolean)`。
6. **天数两端都算。** `ChronoUnit.DAYS.between(start, end) + 1`。页面 `calculateLeaveDays` 用毫秒地板除再 `+ 1`，同一天跨夜也是 2。
7. **草稿不开火。** `add` 不是 (b)。
8. **(b) 一枪两步。** 落库成功才点火；`startCompleteTask == false` 抛「流程发起异常」；`@Transactional` 整单回滚。
9. **代盖申请人。** `ignore=true` 进变量，合同窗再 `completeTask` 第一枪，消息类型写死站内信 `"1"`。草稿实例被办时，办理链会塞 `submit=true`。
10. **门铃回写状态。** `ProcessEvent.flowCode.startsWith("leave")`。提交时强制 `waiting`。删实例可能再删假条；假条已删则空回。
11. **返回 Vo 可能是旧照片。** 监听器更新的是库行，方法返回的是点火前那份内存对象。
12. **页面「提交审批」不是本枪。** 它先 `addLeave` 再打任务窗 `startWorkFlow`，弹窗让人自己盖申请人。e2e 测的是那条。本课 (b)(d) 认「后端发起」。

### 精确定义与 English term

| 中文口诀 | English term | 磁盘上的东西 |
| --- | --- | --- |
| 请假示例窗 | leave sample controller | `TestLeaveController`；`/workflow/leave` |
| 矩阵详情名 | get vs getInfo | 矩阵 `get`；Java `getInfo`；`GET /{id}` |
| 写单并发流 | submit and start flow | `POST /submitAndFlowStart` → `ITestLeaveService.submitAndFlowStart` |
| 启动并办首枪 | start and complete first | `WorkflowService.startCompleteTask` |
| 忽略办理校验 | ignore flag | 变量 `ignore=true`；`FlowConstant.VAR_IGNORE` |
| 业务单号 | business id | 请假 `id` 的字符串 |
| 申请编号 | apply code | `test_leave.apply_code`；新单毫秒串 |
| 流程编码 | flow code | 空/`""` → `"leave1"` |
| 请假天数 | leave days | 闭区间天数；**不**自动进流程变量 |
| 草稿 | draft | `BusinessStatusEnum.DRAFT`；仅 `insertByBo` 默认 |
| 待审核 | waiting | 提交监听器强制写入 |
| 已完成 | finish | 主路径末端；由后续办理事件回写 |
| 总体流程门铃 | process event | `ProcessEvent`；`startsWith("leave")` |
| 任务创建门铃 | process task event | `ProcessTaskEvent`；样例只打日志 |
| 删除门铃 | process delete event | `ProcessDeleteEvent`；可能再删假条 |
| 条件装配 | conditional on enable | `@ConditionalOnEnable` → `warm-flow.enabled=true` |
| 分层登记 | classic workflow | 登记表：经公开 Workflow API 接入 |
| 天数 SpEL 样例 | leave-days eval | `TestLeaveServiceImpl.eval`；`leaveDays <= 2` |

方法签名不要混：

| 窗 / 法 | 参数 | 返回 | 失败 |
| --- | --- | --- | --- |
| `GET /list` | `TestLeaveBo` + `PageQuery` | `R<PageResult<TestLeaveVo>>` | 普通查询失败 |
| `POST /export` | 同查询条件 | Excel 流，`void` | 权限/导出异常 |
| `GET /{id}` | `Long` `@NotNull` | `R<TestLeaveVo>` | 无行也 `R.ok(null)` |
| `POST /` | `AddGroup` body | `R<TestLeaveVo>` 草稿 | 校验失败；天数依赖日期 |
| `POST /submitAndFlowStart` | `AddGroup` body | `R<TestLeaveVo>` | 流程 false → 500「流程发起异常」；事务回滚 |
| `PUT /` | `EditGroup` body（要 id） | `R<TestLeaveVo>` | 不重算天数、不开火 |
| `DELETE /{ids}` | `Long[]` | `toAjax` | 先删行再删实例 |
| `startCompleteTask`（合同，非本窗） | `StartProcessDTO` | `boolean` | 定义未发布等由任务链抛 |

HTTP 合同管的是这七扇。`WorkflowService` **没有** `@RequestMapping`。浏览器碰到的是 `/workflow/leave/*`；厨房把提交枪命名为 `submitLeave`，URL 仍是本课这扇。

### 机制/因果链

#### 1. 七扇窗怎么挂在门上

类注解：`@ConditionalOnEnable` `@Validated` `@RequiredArgsConstructor` `@RestController` `@RequestMapping("/workflow/leave")`，继承 `BaseController`。只注入 `ITestLeaveService`。

| HTTP | 动词 | Java | 权限 | `@Log` | `@RepeatSubmit` | 写库？ |
| --- | --- | --- | --- | --- | --- | --- |
| `/list` | GET | `list` | `workflow:leave:list` | 无 | 无 | 否 |
| `/export` | POST | `export` | `workflow:leave:export` | EXPORT「请假」 | 无 | 否（Excel 流） |
| `/{id}` | GET | `getInfo` | `workflow:leave:query` | 无 | 无 | 否 |
| `/` | POST | `add` | `workflow:leave:add` | INSERT | 默认 5s | 是，草稿 |
| `/submitAndFlowStart` | POST | `submitAndFlowStart` | **同一颗 add** | INSERT | 默认 5s | 是，并发流 |
| `/` | PUT | `edit` | `workflow:leave:edit` | UPDATE | 默认 5s | 是，不开火 |
| `/{ids}` | DELETE | `remove` | `workflow:leave:remove` | DELETE | **无** | 假条 + 实例 |

`AddGroup`：`leaveType` 非空，`startDate`/`endDate` 非空。`id` 只在 `EditGroup` 必填。`flowCode`、`applyCode`、`leaveDays`、`params` **没有**校验注解。日期注解是 `yyyy-MM-dd`，页面实际常送 `YYYY-MM-DD HH:mm:ss`；Jackson 仍能进 `LocalDateTime`，天数按日历差。

查询包装器只认：`leaveType` 等值、`startLeaveDays` 下限、`endLeaveDays` 上限，按 `createTime` 倒序。不按状态筛。厨房 `LeaveQuery` 连 `leaveType` 都没暴露——对照，不把工厂标 covered。

菜单种子在 `30-cde-workflow.sql`，不在 `50-cde-base-dml.sql`：列表页挂在**测试菜单** `1761400000000000005` 下；另有隐藏 `leaveEdit` 挂在工作流菜单，权限字却是 `workflow:leave:edit`。设计器隐藏菜单也误贴了同一颗 `leave:edit`——那是种子脏数据，不是第七扇窗。

#### 2. (b) 写请假并发起流程

`submitAndFlowStart` 方法体顺序（不要背成「先启动再 insert」）：

1. `leaveDays = DAYS.between(start, end) + 1`。截止日期算一天。
2. `id == null` 才生成 `applyCode`。已有草稿再提交，沿用旧编号。
3. `MapstructUtils.convert` 成 `TestLeave`，`insertOrUpdate`。成功才 `bo.setId`。
4. **`bo.getParams().put("ignore", true)`**。注释写明：后端发起要忽略办理人校验。`params` 字段默认 `new HashMap<>()`；JSON 若显式送 `params: null`，这里会 NPE。
5. 组装 `StartProcessDTO`：`businessId = leave.id.toString()`；`flowCode` 空则 `"leave1"`；`variables = bo.getParams()`。`handler` 在注释里示范 `"0"`，HTTP 路径**没设**。
6. `workflowService.startCompleteTask(startProcess)`。`false` → `new ServiceException("流程发起异常")`。
7. 返回 `convert(leave, TestLeaveVo.class)`——**不是**再 `queryById`。

对比 `insertByBo`（add）：同样算天数、生成 `applyCode`，但走 `insert` 不是 `insertOrUpdate`；状态空白则 `DRAFT`；**不**碰 `WorkflowService`。

对比 `updateByBo`（edit）：不重算天数、不生成编号、不点火。页面只允许 `draft` / `cancel` / `back` 点修改，那是前端闸 `isEditableLeaveStatus`，后端 edit **没有**这道状态闸。

`startCompleteTask`（`WorkflowServiceImpl`，L-069 的实现落点，本课只认请假怎么用）：同一事务里 `startWorkFlow` 再 `completeTask(首 taskId)`；办理消息类型写死 `MessageTypeEnum.SYSTEM_MESSAGE` 即 `"1"`；把调用方变量原样带过去。它**不**代写 `ignore`。请假样例自己写。档案网关同一枪不写 ignore——那是 L-034/L-040，本课只要能把两套样本分开。

`TestLeaveServiceImpl` 即使和任务服务同模块，注入的仍是 `WorkflowService`。这就是登记表那句「经公开 Workflow API 接入」的活教材：示例业务也不许直接抱 `IFlwTaskService`。

#### 3. (d) 主路径：submitAndFlowStart → 任务完成

以已发布的 **leave1「请假申请-普通」** 为河床。图纸节点：开始 → **申请人**（`${initiator}`）→ 组长（角色）→ 部门主管（角色或）→ 结束。`formPath` 指向 `/workflow/leaveEdit/index`。

逐步因果：

```text
POST /workflow/leave/submitAndFlowStart   AddGroup + leave:add + RepeatSubmit
        │
        ├─ 写 test_leave（insertOrUpdate）
        │     新单 applyCode=毫秒串；status 此时常常仍是 null（不像 add 默认 draft）
        │
        ├─ variables = params ∪ {ignore:true}
        │     注意：leaveDays / leaveType / userList 不会自动进变量
        │
        └─ startCompleteTask
              ├─ startWorkFlow / startProcessChain
              │     必须有 businessId；补 initiator、initiatorDeptId、businessId
              │     按 businessId 查已有实例：没有则新开，flowStatus=draft
              │     未发布 → 「流程【leave1】未发布…」整单回滚
              │     首环节任务数 ≠ 1 → 「请检查流程第一个环节是否为申请人！」
              └─ completeTask(首 taskId) / completeTaskChain
                    实例仍是 draft → variables.submit=true
                    skip(PASS) + ignore=true  → 申请人节点被办掉
                    实例写成 waiting；下一任务「组长」落地
                    门铃 ProcessEvent(submit=true, flowCode=leave1)
                          TestLeave.processHandler
                          status 强制 WAITING；applyCode 空才抄 businessCode
                    门铃 ProcessTaskEvent  → 样例只 log
```

矩阵说的「任务完成」，在这条河里首先是**申请人那一格被办完**——这正是 `startCompleteTask` 存在的理由，任务 HTTP 没有对称窗。主路径要继续走到终态 `finish`，需要组长、主管再打 L-070 的 `POST /workflow/task/completeTask`。每一次办理仍发 `ProcessEvent`，请假监听器把 `test_leave.status` 写成事件里的流程状态；办到结束就是 `finish`。本课口试要能把这两段切开：**首枪在 (b) 里完成；后续盖章借用任务窗，不把任务窗标进 OBJ-72。**

leave1 不读 `leaveDays` 变量，所以 (b) 不塞天数也能走通普通河。leave2 用 `le@@leaveDays|2` / `gt@@leaveDays|2`，leave6 用 `spel@@#{@testLeaveServiceImpl.eval(#leaveDays)}`（`<= 2` 为真）。**后端发起若不把天数放进 `params`，条件河会缺变量。** 浏览器「提交审批」那条会在 `startWorkflow` 的 variables 里放 `{ leaveDays, userList: ['1','3','4'] }`，并带 `bizExt.businessTitle/businessCode`。两条河不要并成一句。

已有运行实例再点火：启动链按 **businessId** 续写，`checkStartStatus` 会挡 `waiting` / `finish` / `invalid` / `termination`。草稿再走 (b) 等于续提交。因为 (b) 先 `insertOrUpdate` 再启动，启动失败会把刚才那次假条更新一起回滚。

#### 4. 门铃、删除、天数工具

三个 `@EventListener`，条件都是 `flowCode.startsWith("leave")`，所以 leave1…leave6 全进，精确等于 `leave1` 的注释是「正常使用」的建议，示例故意放宽。

- `processHandler`：**没有**空行保护。`selectById` 后直接 `setStatus`。先写事件状态，若 `submit==true` 再改成 `waiting`。`hisTaskExt` / `handler` / `message` 读出来不落库，注释说「自行根据业务实现」。
- `processTaskHandler`：只打日志。节点 key 分支是注释示例，磁盘没写。
- `processDeleteHandler`：有空行保护。实例删除链会按门铃；请假自己的 `deleteWithValidByIds` **先删假条再 `deleteInstance`**，门铃回来发现行已空就 return。顺序反了会二次删，空保护让它幂等。

`eval(Integer leaveDays)` 是实现类**公开方法**，不在 `ITestLeaveService` 上。SpEL 按 Spring bean 名 `testLeaveServiceImpl` 调用。它不是第七扇半 HTTP，也不是 `FlwSpelController` 的目录行。

#### 5. 其余六扇，口试要能指，不必当 (b)

- **list：** 分页。条件只有类型和天数区间。
- **export：** 同一套 `queryList`，`ExcelBuilder` 表名「请假」，列来自 `TestLeaveVo` 的 `@ExcelProperty`。厨房**没有** `exportLeave`；列表页 `download('/workflow/leave/export', …)`。
- **getInfo：** 权限是 query 不是 list。
- **add：** 草稿河。
- **edit：** 改行。前端可编辑状态是 draft/cancel/back。
- **remove：** 批量路径 `/{ids}`。先假条后实例。

### 图、表或文本图

**图题 / caption：** 宏观请假柜台七扇与 classic 抽屉。alt：`/workflow/leave` 下七个映射；写单并发流走 WorkflowService；Mapper XML 为空。

```text
 warm-flow.enabled=true
        │  @ConditionalOnEnable
        v
 TestLeaveController          /workflow/leave
        │
        ├─ GET    /list                      list                 perm list
        ├─ POST   /export                    export               perm export
        ├─ GET    /{id}                      getInfo（矩阵 get）  perm query
        ├─ POST   /                          add                  perm add     草稿
        ├─ POST   /submitAndFlowStart        submitAndFlowStart   perm add     (b)(d)
        ├─ PUT    /                          edit                 perm edit
        └─ DELETE /{ids}                     remove               perm remove
                │
                v
        ITestLeaveService
                │
                v
        TestLeaveServiceImpl
                ├─ TestLeaveMapper → test_leave（空 XML）
                └─ WorkflowService  → startCompleteTask / deleteInstance
```

**文字等价物：** 图顶是开关。中间一排七扇共享前缀 `/workflow/leave`。提交和新增共用 add 权，日志类型都是 INSERT。厨师自己抱 Mapper，流程只经对讲机。关掉开关，窗和厨师一起从容器消失。

**图的边界：** 不画十六扇任务窗。不画 `createWorkflowDefinitionService` 方法名当 HTTP。不把 `eval` 画成 REST。

**图题 / caption：** (b)(d) 写单并发流到申请人任务完成。alt：insertOrUpdate 后 startCompleteTask；ignore 代盖申请人；监听器把假条写成 waiting。

```text
浏览器「后端发起」                对照：按钮「提交审批」（非本枪）
 submitLeave(form)                 addLeave/updateLeave
        │                          再 startWorkflow({leaveDays,userList,bizExt})
        v                          再弹窗 completeTask
 POST /submitAndFlowStart
        │
        v
 insertOrUpdate test_leave
 params.ignore = true
 flowCode? leave1
        │
        v
 startCompleteTask
   startWorkFlow ──► 实例 draft + 申请人任务
   completeTask  ──► ignore + submit + PASS
        │
        ├─ 申请人任务完成          ← 矩阵「任务完成」的第一段
        ├─ 下一格「组长」待办
        └─ ProcessEvent submit
              test_leave.status = waiting

后续（L-070 窗，主路径下游，本格不盖章）
   组长 completeTask → 部门主管 completeTask → 结束
   ProcessEvent status=finish → 假条 finish
```

**文字等价物：** 左列是 OBJ-72 要走完的河：一枪写假条、对讲机开火并代盖申请人、门铃把假条改成待审。右列是页面另一个按钮：先存草稿再打任务启动窗，申请人那一格留给人点。口试若把右列说成本课 (b)，格子不满。后续组长主管盖章必须发生，单据才会 `finish`；那是主路径的尾巴，枪在任务楼。

**图的边界：** 不保证 leave2/leave6 在 (b) 缺 `leaveDays` 变量时走哪条条件边。不把档案 `startCompleteTask` 画进假条表。

**图题 / caption：** 假条状态谁写。alt：add 写 draft；submit 监听器写 waiting；后续事件写 finish/back/cancel；删除两入口。

```text
add.insertByBo          ──► status=draft（空白才写）
submitAndFlowStart 落库 ──► 常常仍 null，等门铃
ProcessEvent submit     ──► waiting（强制）
ProcessEvent 非 submit  ──► 事件 status（finish/back/cancel/invalid/termination）
列表页「撤销」          ──► 打实例窗 cancelProcessApply（要 instance:cancel）
delete 假条窗           ──► 删行 + deleteInstance
实例窗删实例            ──► 门铃再删假条（已空则跳过）
```

**文字等价物：** 草稿章是新增自己盖的。待审章是门铃盖的，不是 `submitAndFlowStart` 在 convert 之前写的。所以同一毫秒的 HTTP 响应里，`status` 可能还是空，库里已经是 waiting。撤销按钮不走请假 Controller。

## 正例、反例与边界

**正例 1：** 数 7 个映射。打开 `TestLeaveController.java`，从 `list` 数到 `remove`。对照矩阵那一行七个名字：把 `get` 对上 `getInfo`。确认没有第八个 `@*Mapping`。

**正例 2：** 接口比窗多 0 个对外名、多 1 个内部查询。`ITestLeaveService` 七法：`queryById` / `queryPageList` / `queryList` / `insertByBo` / `submitAndFlowStart` / `updateByBo` / `deleteWithValidByIds`。`queryList` 只给 export。`eval` 不在接口上。

**正例 3：** 提交与新增共用 add 权。两扇都是 `@SaCheckPermission("workflow:leave:add")`，都是 `@Log INSERT`，都是 `@RepeatSubmit()`。

**正例 4：** 天数闭区间。`2026-09-01` 到 `2026-09-02` → `DAYS.between=1` → `leaveDays=2`。页面同一组时间 `calculateLeaveDays` 也是 2。e2e `leave add calculates days…` 断言天数输入框为 `'2'`。

**正例 5：** (b) 默认 leave1。`StringUtils.isEmpty(bo.getFlowCode()) ? "leave1" : bo.getFlowCode()`。页面「后端发起」`submitLeave({ ...form })`，`LeaveForm` **没有** `flowCode` 字段，下拉值留在 Vue ref 里，打不进这枪。

**正例 6：** ignore 是请假自己塞的。`bo.getParams().put("ignore", true)` 然后 `startProcess.setVariables(bo.getParams())`。打开 `WorkflowServiceImpl.startCompleteTask`：没有 `put("ignore")`。打开 `CompleteExecuteComponent`：`.ignore(Convert.toBool(variables.getOrDefault(VAR_IGNORE, false)))`。

**正例 7：** 首枪完成靠 draft→submit。`StartExecuteComponent` 新开实例 `flowStatus(DRAFT)`。`CompletePrepareComponent` 见 draft/cancel/back 就 `variables.put(SUBMIT, true)`。`WorkflowGlobalListener.finish` 见 submit 发 `processHandler(..., true)`。请假监听器强制 `WAITING`。

**正例 8：** 删除先假条后实例。`deleteWithValidByIds`：`leaveMapper.deleteByIds` 然后 `workflowService.deleteInstance(ids 转字符串)`。与 L-069 大厅 `deleteByBusinessIds` 会合到同一条删除链。

**正例 9：** 门铃匹配前缀。`#processEvent.flowCode.startsWith('leave')` 覆盖 leave1…leave6。leave6 JSON 的 skipCondition 点名 `@testLeaveServiceImpl.eval(#leaveDays)`。

**正例 10：** 厨房六枪对齐六扇写/读，缺 export。`index.test.ts` 期望含 `post /workflow/leave/submitAndFlowStart`，没有 export URL。列表页 export 走 `runtime.download`。格子仍只认 Java 窗。

**正例 11：** 审批只读页藏起三颗写按钮。e2e 待办进 `leaveEdit?type=approval`：保存 / 提交审批 / 后端发起 count 为 0，只剩「办理任务」。那是任务河进假条表单，不是 (b)。

**正例 12：** classic 房间。登记表 `03-backend-module-modes.md`：`wta-modules/wta-workflow` = classic。Controller 在 `controller/`，服务在 `service/impl/`，没有 `usecase/`。Mapper XML 空。

**反例 1：** 「`submitAndFlowStart` 就是页面『提交审批』。」磁盘上『提交审批』走 `addLeave`/`updateLeave` + `startWorkflow`。『后端发起』才是本枪。

**反例 2：** 「`add` 也会启动流程。」`insertByBo` 零对讲机调用。

**反例 3：** 「矩阵 `get` 是 Java 方法名 `get`。」方法名 `getInfo`。

**反例 4：** 「提交有独立权限字 `workflow:leave:submit`。」没有这颗种子。共用 `add`。

**反例 5：** 「`startCompleteTask` 自动 ignore。」合同窗不写。请假 `params` 写。档案样本不写。

**反例 6：** 「(b) 会把 `leaveDays` 放进流程变量。」变量就是 `params` 加 ignore。天数只写在假条列。条件图要天数，调用方得自己放进 `params`。

**反例 7：** 「返回 Vo 的 status 一定是 waiting。」返回的是点火前内存对象；门铃写库。

**反例 8：** 「请假 Controller 注入了 `IFlwTaskService`。」只注入 `ITestLeaveService`；实现注入 `WorkflowService`。

**反例 9：** 「`eval` 是第八扇 HTTP / SpEL 目录行。」它是 ServiceImpl 方法，给 leave6 表达式用。目录窗是 L-071。

**反例 10：** 「删除假条不会动流程。」`deleteInstance` 会。反过来删实例可能门铃删假条。

**反例 11：** 「本课覆盖 `createWorkflowDefinitionService`。」那是 OBJ-73。工厂把请假枪和任务枪塞进同一座，更不能整座盖章。

**反例 12：** 「本课覆盖 `FlwTaskController.completeTask`。」那是 OBJ-70。主路径尾巴借用它，格子不认它。

**反例 13：** 「`updateByBo` 会重算天数并续流。」两件都不做。

**反例 14：** 「请假列表挂在工作流菜单下。」可见列表种子父节点是测试菜单 `1761400000000000005`。工作流菜单下那条 `leaveEdit` 是隐藏 C。

**反例 15：** 「`processHandler` 和删除监听一样空行保护。」提交监听没有；删除监听有。错 businessId 的 leave* 事件会 NPE。

**反例 16：** 「`RepeatSubmit` 罩住删除和导出。」只有 add / submitAndFlowStart / edit。

**边界：**

- `warm-flow.enabled=false`：窗、厨师、门铃整组不加载。不是某扇 404。
- (b) 用 `AddGroup`，所以**改草稿再后端发起**时，body 仍按新增校验（类型和日期），不要求 `EditGroup` 的 id 注解；但有 id 就会 `insertOrUpdate` 走更新。id 与校验组不对齐，是口试裂缝，不要抹平。
- 未登录定时任务走 (b) 需要自己 `startProcess.setHandler(...)`。现在 HTTP 路径靠 `LoginHelper` 补 initiator。
- `insertOrUpdate` 失败（`flag==false`）时**不点火、不抛**，仍返回那份 Vo。这是静默空操作，不是「流程发起异常」。
- 页面可编辑状态与后端 edit 闸不一致：后端会改 waiting 行，若有人绕过按钮直接 PUT。
- 列表「撤销」要 `workflow:instance:cancel`，不是 leave 权。
- leave1 首环节必须是申请人且只有一个任务，否则启动链抛，(b) 回滚假条。
- 矩阵 (a) 把七扇聚成一行；函数表仍要逐方法。(b) 只点名提交枪。(d) 是主路径，不是第八扇 HTTP。

## 变式与迁移

- **变式 A：只要把假条放进抽屉，先不找领导。** `POST /workflow/leave`（add）。不要打 submitAndFlowStart。状态应是 draft。以后再 (b) 或走页面「提交审批」。

- **变式 B：后台一枪写单并把申请人代过。** 本课 (b)。自己把 `ignore=true` 放进 `params`（实现已经放了）。条件图还要把 `leaveDays` 放进同一份 `params`。不要改 `WorkflowServiceImpl` 给所有邻居写死 ignore。

- **变式 C：人要在弹窗里自己盖申请人，并带上天数、候选人、业务标题。** 页面「提交审批」：先 add/update，再 `POST /workflow/task/startWorkFlow`。那是 L-070 + L-073。不要在口试里把它说成本课这一扇。

- **变式 D：普通河走到结束。** (b) 之后，组长、部门主管依次 `completeTask`。每次门铃回写假条。终态 `finish`。驳回/终止是 L-070 的 (d)，不是本课主路径。

- **变式 E：按天数分流。** 发布 leave2 或 leave6，(b) 的 `flowCode` 显式传入，`params.leaveDays` 必须在。leave6 走 `@testLeaveServiceImpl.eval`。不要以为假条列上的天数引擎会自己读。

- **变式 F：业务房间要学这一套。** 学请假：自己的表、自己的 Controller，流程只喊 `WorkflowService`。删除先处理业务行再 `deleteInstance`，并准备好听门铃的二次删除。不要注入 Mapper 到引擎表。档案房间已经用同一枪 `startCompleteTask`，但不写 `test_leave`。

- **变式 G：只要提醒假条被删。** 监听 `ProcessDeleteEvent`。不要在删除监听里再 `deleteInstance`，会递归门铃。

- **变式 H：导出。** `POST /export` + `workflow:leave:export`。厨房没枪，页面 `download`。不要把缺工厂方法说成后端缺窗。

- **迁移口诀：** 先数七扇与权限（提交=add）→ 再分草稿枪 / 并发流枪 / 只改行枪 → 再走 (b) 落库、ignore、leave1、startCompleteTask → 再认门铃写 waiting、返回 Vo 可能旧 → 再把后续盖章留给任务楼 → 最后把页面「提交审批」从本枪里踢出去。跳步会出现「add 就开火」「提交审批等于 submitAndFlowStart」「天数自动进变量」「本课覆盖任务窗」。

## 常见误区

1. **「OBJ-72 包含 `FlwTaskController`。」** 那是 OBJ-70。本课只把后续 complete 当主路径下游。
2. **「OBJ-72 包含 `WorkflowService.*` 整份合同。」** 那是 OBJ-69。本课认请假喊了 `startCompleteTask` / `deleteInstance`。
3. **「OBJ-72 包含厨房工厂。」** 那是 OBJ-73。
4. **「七扇里有启动流程 HTTP 名叫 startWorkFlow。」** 启动在任务楼或对讲机。请假这扇叫 `submitAndFlowStart`。
5. **「`get` 和 `getInfo` 是两扇。」** 一扇。
6. **「提交有单独权限。」** 与新增同字。
7. **「保存草稿等于已提交。」** draft 不是 waiting。
8. **「后端发起会带上页面选的 leave2。」** `LeaveForm` 不带 `flowCode`，后端默认 leave1。
9. **「ignore 是合同窗的默认值。」** 请假自己 put。
10. **「天数在变量里。」** 在列里。变量要另放。
11. **「返回体 status 已是 waiting。」** 可能仍是 null。
12. **「示例既然住在 workflow 模块，就可以注入任务服务。」** 登记表禁止；实现也没这么做。
13. **「`eval` 要写进 Controller。」** 不要。
14. **「删除假条只删 Excel 那一行。」** 还删实例。
15. **「监听器空行都安全。」** 提交监听不安全。
16. **「`wta-workflow` 该把请假改成 layered UseCase。」** 登记表 classic；本课不发动重构。
17. **「主路径在 submitAndFlowStart 返回的那一刻已经 finish。」** 那一刻只办完申请人。finish 在后面的盖章。
18. **「列表页撤销打 `/workflow/leave`。」** 打实例 `cancelProcessApply`。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `TestLeaveController.java`。用手指点 7 个映射。把矩阵 `get` 对上 `getInfo`。圈提交与新增同一颗 `workflow:leave:add`、同一份 INSERT 日志、同一份 RepeatSubmit。圈删除没有 RepeatSubmit、导出是 `void` Excel。
2. 打开 `ITestLeaveService.java` 与 `TestLeaveServiceImpl.java`。数接口 7 法。圈 `eval` 不在接口。顺着 `submitAndFlowStart` 圈：天数 `+ 1`、空 id 才写 applyCode、`insertOrUpdate`、`params.ignore`、默认 `leave1`、`startCompleteTask`、false 抛「流程发起异常」、返回的是内存 `leave` 不是 `queryById`。对照 `insertByBo` 的 draft 默认。
3. 打开 `WorkflowServiceImpl.startCompleteTask`。确认不写 ignore、消息类型 `"1"`、先 start 再 complete 首 taskId。打开 `CompleteExecuteComponent` 的 `.ignore(...)` 与 `CompletePrepareComponent` 的 `SUBMIT`。
4. 打开 `leave1.json`。按 开始 → 申请人 → 组长 → 部门主管 → 结束 走一遍。打开 `leave2.json` 的 `le@@leaveDays|2` 与 `leave6.json` 的 `@testLeaveServiceImpl.eval`。问自己：后端发起若不把天数放进 params，哪条河仍通。
5. 打开三个 `@EventListener`。圈 `startsWith('leave')`。圈提交监听没有空保护、删除监听有。圈 submit 分支强制 `WAITING`。打开 `deleteWithValidByIds` 的先删行再 `deleteInstance`。
6. 打开 `LeaveEditPage.vue` 的 `persist`。把 `draft` / `start` / `direct` 三条河标在纸上。圈 `direct` 才 `submitLeave`。圈 `start` 的 `startWorkflow` 变量。打开 `index.test.ts` 请假 URL 列表，确认没有 export。打开 `30-cde-workflow.sql` 请假菜单父节点。打开登记表 classic 行。

## 总结、词汇表与下一步

- **宏观请假样例：** 一块 `/workflow/leave` 牌子，七扇 classic 窗，一张 `test_leave`。流程只经对讲机。开关是 `warm-flow.enabled`。
- **(a) 七扇：** list / export / getInfo(矩阵 get) / add / submitAndFlowStart / edit / remove。提交与新增同权同日志类型。导出是流。删除先假条后实例。
- **(b) 写单并发流：** 算闭区间天数 → 落库 → `ignore=true` → 默认 leave1 → `startCompleteTask`。失败回滚。不自动带天数变量。返回 Vo 可能未刷新。
- **(d) 主路径：** 这一枪把申请人任务办完，假条门铃写成 `waiting`，下一格待办出现；组长主管盖章走到 `finish` 借用 L-070，不盖进本格。
- **不是任务窗，不是整份 WorkflowService，不是厨房工厂，不是 SpEL 目录。**

词汇表：`TestLeaveController` / `ITestLeaveService` / `submitAndFlowStart` / `insertByBo` / `test_leave` / `applyCode` / `leaveDays` / `flowCode` / `leave1` / `startCompleteTask` / `ignore` / `BusinessStatusEnum` / `draft` / `waiting` / `finish` / `ProcessEvent` / `ProcessDeleteEvent` / `eval` / `@ConditionalOnEnable` / classic / `workflow:leave:add`。

下一步：分类树是 OBJ-67。图纸发布/导入是 OBJ-68。实例窗与对讲机合同是 OBJ-69。任务枢纽十六扇与驳回/终止失败是 OBJ-70。SpEL 目录是 OBJ-71。厨房与请假页如何喊这七扇是 OBJ-73。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` Controller / Service | 请假柜台公开入口与 classic 实现 | `wta-workflow` controller/service | 2026-09-17 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-workflow` 登记 classic，经公开 Workflow API 接入 | 登记表 workflow 行 | 2026-09-17 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/30-cde-workflow.sql` | `test_leave` DDL；请假菜单与权限种子 | 表定义；菜单 1761400000000011638 起 | 2026-09-17 |
| S-015 | `backend/wta-api/.../workflow/api` | `WorkflowService.startCompleteTask` / `deleteInstance` 合同 | `WorkflowService.java`；`StartProcessDTO` | 2026-09-17 |
| S-L072-01 | `.../controller/TestLeaveController.java` | 七扇映射、权限、Log、RepeatSubmit、返回形状 | 全文件 7 个 `@*Mapping` | 2026-09-17 |
| S-L072-02 | `ITestLeaveService.java` 与 `impl/TestLeaveServiceImpl.java` | (b) 落库+ignore+leave1；草稿默认；删除顺序；三只门铃；`eval` | 接口全文；实现 `insertByBo`…`processDeleteHandler` | 2026-09-17 |
| S-L072-03 | `WorkflowServiceImpl.startCompleteTask`；`CompletePrepareComponent`；`CompleteExecuteComponent`；`StartExecuteComponent` | 组合拳不写 ignore；draft 新开；submit 标记；skip 读 ignore | 各方法/组件 `process` | 2026-09-17 |
| S-L072-04 | `domain/TestLeave.java`；`TestLeaveBo.java`；`TestLeaveVo.java`；`TestLeaveMapper.xml` | 表字段、校验组、空 XML | 各文件 | 2026-09-17 |
| S-L072-05 | `release-artifacts/workflow/leave/leave1.json` / `leave2.json` / `leave6.json` | 普通河节点；天数条件；SpEL eval | `flowCode` 与 skipCondition | 2026-09-17 |
| S-L072-06 | `BusinessStatusEnum`；`FlowConstant`；`FlowProcessEventHandler`；`WorkflowGlobalListener.finish` | draft/waiting/finish；SUBMIT/VAR_IGNORE；门铃怎么发 | 枚举与 listener `finish` | 2026-09-17 |
| S-L072-07 | `frontend/packages/domains/workflow/src/index.ts` 与 `index.test.ts` | 厨房 list/get/add/submit/update/delete；无 export 枪 | `submitLeave`；测试 URL 列表 | 2026-09-17 |
| S-L072-08 | `LeaveEditPage.vue`；`LeaveListPage.vue`；`runtime-actions.ts`；`frontend/e2e/workflow-runtime.spec.ts` | 三按钮三河；可编辑/可撤销闸；e2e 测的是提交审批不是后端发起 | `persist`；`isEditableLeaveStatus`；leave add 用例 | 2026-09-17 |

*生成依据：L-contract / OBJ-72 / 仓库源码核实。*
