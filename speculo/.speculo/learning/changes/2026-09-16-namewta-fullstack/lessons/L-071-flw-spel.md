---
lesson_id: L-071
objective_ids: [OBJ-71]
claimed_cells:
  - A:FlwSpelController.*
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: five-doors-on-disk
    minutes: 10
  - segment: classic-chain-and-catalog
    minutes: 11
  - segment: visuals-and-worked-examples
    minutes: 6
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-008, S-010, S-L071-01, S-L071-02, S-L071-03, S-L071-04, S-L071-05, S-L071-06, S-L071-07, S-L071-08]
---

# Lesson 071：宏观五扇表达式窗——`FlwSpelController` 的目录本，不是计算器

## 学完你能做什么

打开 `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/controller/FlwSpelController.java`，你能**口述这块宏观表达式目录柜台**：墙上挂的是「可以拿去当办理人标识的预览字符串」，不是正在算谁该盖章的那台计算器，也不是图纸上的跳转条件。口试名单就是矩阵 **(a)** 这一格，符号以**磁盘**为准：

**`A:FlwSpelController.*`**：同一门牌 `/workflow/spel` 上，**正好 5 个** Java 公开方法。矩阵那一行把五个名字写全了：`list` / `getInfo` / `add` / `edit` / `remove`。口试按磁盘，不要发明 `export`，也不要把 Service 上的 `queryList` / `selectSpelByTaskAssigneeList` / `selectRemarksBySpels` 背成第六、第七、第八扇 HTTP。

OBJ-71 原文只要你能口述 `FlwSpelController`。本课还要把 **classic 目录本**讲完，否则「能口述」会退化成背五个路径：

- 登记表：`wta-workflow` 是 **classic**。调用链是 **Controller → `IFlwSpelService` → `FlwSpelServiceImpl`（自己抱 `FlwSpelMapper`）→ Mapper Java default / `BaseMapperPlus`**。`FlwSpelMapper.xml` 是**空壳**。没有 UseCase，没有 DAO，门卫**不**抱 Warm-Flow `DefService`。
- 类上 `@ConditionalOnEnable`：`warm-flow.enabled=true` 才进 Spring。关开关，这五扇窗整栋消失，不是返回空分页。
- 这本目录**不执行**表达式。运行时谁被解析成用户，走 Warm-Flow 引擎对 `$` / `#` 开头标识的求值（矩阵 **deferred(vendor-engine)**）；设计器点选走邻居 `FlwTaskAssigneeServiceImpl` 的插件口。本课只认：五扇 HTTP 怎样把一行写进 `flow_spel`。

本课**不宣称**你会拆分类树（L-067）、定义发布/导入（L-068）、实例与 `WorkflowService`（L-069）、任务枢纽（L-070）、请假 `submitAndFlowStart`（L-072），或把 `createWorkflowDefinitionService` / `createWorkflowWebDomain` 当本格 covered（L-073）。厨房五枪 URL 只当**对照**：证明五扇窗被浏览器扣了扳机。菜单里那颗「导出」F 型按钮、Vo 上的 Excel 注解，**没有**对应映射。

2026-09-17 工作树先钉死**包边界**（口试先数窗，再数链）：

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| 第六扇 HTTP `export` | **没有。** 菜单有 `workflow:spel:export`；`FlowSpelVo` 贴了 `@ExcelProperty`；Controller **零** `ExcelUtil` / `/export` |
| `queryList` 整表扁列表 | **没有 HTTP。** 接口有；门卫 `list` 走的是 `queryPageList` + `PageResult` |
| 设计器「SpEL表达式」页签 | **不是本 Controller。** `HandlerSelectService.getHandlerSelect` → `selectSpelByTaskAssigneeList` |
| 办理人回显中文备注 | **不是本 Controller。** `selectRemarksBySpels` |
| `DELETE /{id}` 单个删 | **没有。** 路径变量是 `Long[] ids`。厨房 `deleteSpel(['s1','s2'])` 拼 `/workflow/spel/s1,s2`，**这次后端绑得上** |
| 分类那种扁 `List` | **没有。** SpEL 的 `list` **分页** |
| 本课执行 `#{@spelRuleComponent...}` | **没有。** 目录只存字符串。Bean `SpelRuleComponent` 是运行时邻居 |
| 跳转条件 `spel@@#{...}` | **不是本表。** 那是定义边的 `skipCondition`（请假 JSON 种子），L-068 的图纸，不是这五扇 |
| `wta-workflow` 有 `FlwSpel*Test` | **2026-09-17 没有。** 本课证据是生产源码 + 前端契约/E2E，不是模块单测 |
| 给表达式房间加 UseCase/DAO | **本课不发动。** 登记表 classic；保持现状 |

## 先把宏观地图放在桌上

L-003 已经把 `wta-workflow` 钉在 classic 列：既有 Warm-Flow 流程能力，通过公开 `WorkflowService` 接入，不改内部层次。L-002 说过它只进 **bundle-full**；`bundle-core` 的注释写明「不装配工作流引擎」。L-067 是抽屉标签，L-068 是墙上菜谱，L-069 / L-070 是正在炒的菜和窗口号票。本课走进**表达式目录本**：一页一行「预览字符串」，设计器把这一行抄进节点的 `permissionFlag`。目录自己不跑流程。

把流程楼想成办事大厅。分类是抽屉标签。定义是墙上菜谱。实例是正在办的那叠材料。任务是窗口号票。SpEL 是柜台上那本**「可以用的暗号本」**：一页写 `${initiator}`（流程发起人），一页写 `#{@spelRuleComponent.selectDeptLeaderById(#initiatorDeptId)}`（按发起人部门找负责人）。办理人页签从这本抄一行贴到节点上。真正把暗号翻译成工号的，是大厅后面的引擎，不是这本目录的管理员。

2026-09-17 工作树：`controller/` 一共 **六份** Java，全部 `@ConditionalOnEnable` + `@RequestMapping("/workflow/...")`：

```text
已登录的管理员（Admin-Token；无类上 @SaIgnore）
        │
        v
 /workflow/category/*     FlwCategoryController     L-067
 /workflow/definition/*   FlwDefinitionController   L-068
 /workflow/instance/*     FlwInstanceController     L-069
 /workflow/task/*         FlwTaskController         L-070
 /workflow/spel/*         FlwSpelController         ← 本课五扇
 /workflow/leave/*        TestLeaveController       L-072
        │
        v  classic
 IFlwSpelService
        │
        v
 FlwSpelServiceImpl        自己抱 FlwSpelMapper → flow_spel（逻辑删）
                           另给设计器：selectSpelByTaskAssigneeList
                           另给回显：selectRemarksBySpels
        │
        ├─（HTTP 不走）FlwTaskAssigneeServiceImpl  SPEL 页签 / 回显
        └─（HTTP 不走）Warm-Flow 对 $/# 求值；SpelRuleComponent 是被点名的 Bean
```

菜单种子在 `30-cde-workflow.sql`：C 型页 `workflow/spel/index`，perms `workflow:spel:list`，备注写成「流程**达**式定义菜单」（掉了一个「表」字，口试报事实）。F 型按钮五串：`query` / `add` / `edit` / `remove` / **`export`**。五扇窗只用前四串再加 list；**export 是孤儿权限**。web-domain 权限组 `workflow-spel` 没收 export 串，厨房也没有 `exportSpel`。

表就一张：`flow_spel`。种子两行：

| id | component_name | method_name | method_params | view_spel | remark | status |
| --- | --- | --- | --- | --- | --- | --- |
| `1762400000000000001` | `spelRuleComponent` | `selectDeptLeaderById` | `initiatorDeptId` | `#{@spelRuleComponent.selectDeptLeaderById(#initiatorDeptId)}` | 根据部门id获取部门负责人 | `0` |
| `1762400000000000002` | NULL | NULL | `initiator` | `${initiator}` | 流程发起人 | `0` |

第二行证明目录**允许**没有组件、没有方法，只留一个变量暗号。请假图纸种子把 `${initiator}` 写进申请人节点的 `permissionFlag`——那是**抄走的字符串**，不是本课 HTTP 在运行时被调用。

**类比：** 这是柜台上的**暗号本**。管理员可以增一页、改一页、撕一页、翻页看。设计器来借书时走侧门，只借 `status=0` 的页。窗口办事的时候不翻这本：它看的是已经贴在菜谱节点上的那一行字。

**类比失效处：**

1. 「暗号本」不是「翻译机」。`WorkflowPermissionHandler.convertPermissions` 对 SPEL 标识走 `fetchUsersByStorageIds`，SPEL 分支**返回空用户列表**。引擎要先自己把 `$` / `#` 算成工号（厂商实现，本课不拆）。
2. 「翻页」不是分类那种整表扁列表。本课 `list` 是 `PageResult`。
3. 「撕一页」这里真是批量 `/{ids}`。不要把 L-067 的单个 `Long` 搬过来。
4. 「导出按钮」在菜单种子里。后端窗、厨房枪、Vue 工具栏**三处都没有**。
5. 「停用」不会让已经贴在已发布图纸上的字符串消失。删除的 Valid 是 TODO 空壳，不查 `permissionFlag`。
6. `bundle-core` 没有这间房。开关关掉，五扇整排消失。

## 核心概念与机制

### 直觉讲解

小孩子版只记十二句：

1. **一块牌子，五扇窗。** `@RequestMapping("/workflow/spel")`。矩阵 `.*` = 磁盘 5 个方法，不是「CRUD 四个字」，也不是「带导出的标准六件套」。
2. **classic，不是五层。** 门卫 → 接口 → `ServiceImpl` 抱 Mapper。不要发明 `FlwSpelUseCase`。
3. **列表分页。** `GET /list` 回 `R<PageResult<FlowSpelVo>>`。对照分类课的扁 `List`。
4. **唯一键是预览字符串。** 保存前 `viewSpel` 撞车 → `ServiceException("SpEL表达式已存在，请勿重复添加")` → 全局处理器变 `R.fail`（默认 500）。组件名 + 方法名**不**做唯一。
5. **新增/修改要两块必填。** `AddGroup` / `EditGroup`：`viewSpel` `@NotBlank`，`status` `@NotBlank`。`id` 在 Edit 组**没有** `@NotNull`——和分类课不一样。
6. **删除是批量，Valid 是空的。** `DELETE /{ids}`，`deleteWithValidByIds(..., true)` 里 TODO 注释，一行业务闸都没有。逻辑删。
7. **五扇全贴权限字。** 没有分类课那种「树不查权限」的例外。
8. **防重只罩写名两扇。** `@RepeatSubmit()` 默认 5000ms，只贴 `add` / `edit`。删除连点不靠这注解。
9. **操作日志三扇。** `add` INSERT、`edit` UPDATE、`remove` DELETE；title 都是「流程spel表达式定义」。list / getInfo 不打 `@Log`。
10. **侧门三枪不是 HTTP。** `queryList`、设计器分页、备注回显。口试把它们指出来，**不要**画进 `.*`。
11. **页面替你拼暗号。** Vue 预览框只读。有组件+方法 → `#{@组件.方法(#参)}`；只有一个参数名 → `${参}`。种子第二行就是后一种。
12. **开关是整栋电闸。** `@ConditionalOnEnable` = `warm-flow.enabled=true`。默认 `application.yml` 为 true。ServiceImpl、`SpelRuleComponent` 同样贴了这张纸。

**类比失效边界：** 「暗号本」**不**覆盖 Warm-Flow 怎么求值 `#{}` / `${}`，也**不**覆盖跳转条件 `spel@@`。类比还不等于「前端五个按钮」——菜单多了一颗导出，页上没有。类比也不等于「停用了设计器就看不见所以运行时也不会算」——运行时读的是节点上已抄走的字符串。

### 精确定义与 English term

| 中文口头 | English term | 精确定义（本课，以 2026-09-17 工作树为准） |
| --- | --- | --- |
| 流程 SpEL 控制器 | `FlwSpelController` | classic `@RestController`，`/workflow/spel`，继承 `BaseController`。五个公开映射。类上 `@ConditionalOnEnable` `@Validated` `@RequiredArgsConstructor` |
| 条件启用 | `@ConditionalOnEnable` | `@ConditionalOnProperty(value="warm-flow.enabled", havingValue="true")`。Controller 与 `FlwSpelServiceImpl`、`SpelRuleComponent` 都贴了 |
| 经典三层 | classic | 登记表：`wta-modules/wta-workflow`。Controller → Service / ServiceImpl → Mapper。ServiceImpl 允许持 Mapper；本课不发动迁 layered |
| 业务对象 | `FlowSpelBo` | 入参。`AddGroup`/`EditGroup`：`viewSpel`、`status` `@NotBlank`。`id` **两组都不强制**。`componentName` / `methodName` / `methodParams` / `remark` 无校验。`params` 默认空 `HashMap` |
| 视图对象 | `FlowSpelVo` | 出参。Excel 注解齐（id / 组件 / 方法 / 参数 / 预览 / 状态字典 / 备注 / 创建时间），但本 Controller **没有**导出窗 |
| 实体 | `FlowSpel` | `@TableName("flow_spel")`，`@TableLogic delFlag`，`@TableId id` |
| 表达式服务 | `IFlwSpelService` / `FlwSpelServiceImpl` | 模块内门面。接口 8 法；HTTP 只用其中 5 个委托（list 用 `queryPageList`，不是 `queryList`） |
| 预览字符串 | `viewSpel` | 目录的业务主键语义。唯一闸按这一列 `eq`。设计器入库标识也是这一串 |
| 状态 | `status` | 字符 `0` 正常 / `1` 停用。`SystemConstants.NORMAL = "0"`。设计器侧门强制只查正常 |
| 警告信封 | `R.warn` | 本 Controller **不用**。没有 601 |
| 失败信封 | `R.fail` | 唯一闸 `ServiceException`；`toAjax(false)` 在 rows==0 时也走 fail |
| 防重复提交 | `@RepeatSubmit()` | 只贴 `add` / `edit`。默认间隔 5000ms |
| 办理人类型 | `TaskAssigneeEnum.SPEL` | 描述「SpEL表达式」，**code 前缀是空串**。`$` 或 `#` 开头的 storageId 被认成 SPEL |
| 规则组件 | `SpelRuleComponent` | Spring Bean 名默认 `spelRuleComponent`。一枪 `selectDeptLeaderById`。**不是**本课 HTTP |
| 资源标签 | `workflowSpelResource` | 前端 `controller: 'FlwSpelController'`，`basePath: '/workflow/spel'`。对照用，本课不认前端格子 |

### 机制/因果链

#### 1. 类上合同：电闸、门牌、电话

文件：`.../controller/FlwSpelController.java`。

类注解：`@ConditionalOnEnable` `@Validated` `@RequiredArgsConstructor` `@RestController` `@RequestMapping("/workflow/spel")`。

构造注入**只有** `IFlwSpelService flwSpelService`。全文搜不到 `DefService`、`LoginHelper`、Excel、引擎类型。

`application.yml`：`warm-flow.enabled: true`。LiteFlow `enable: ${warm-flow.enabled:true}` 跟着这把电闸。关电闸：本 Controller Bean 不存在，HTTP 是 404/未映射，不是空 `rows=[]`。

类上没有 `@SaIgnore`。**五扇都贴** `@SaCheckPermission`。已登录才能进楼，进了楼每一扇还要对应权限字。

#### 2. `list`：`GET /workflow/spel/list`

权限 `workflow:spel:list`。无 `@Log`。入参 `FlowSpelBo bo` + `PageQuery pageQuery`。BO **不走** Add/Edit 组，所以预览字符串和状态都可以空——空就不进 WHERE。

委托 `queryPageList` → `buildQueryWrapper`：

- `orderByAsc(id)`
- `like`：`componentName` / `methodName` / `remark`（有字才加）
- `eq`：`methodParams` / `viewSpel` / `status`（有字才加）
- **不读** `bo.params`。设计器侧门塞进去的 `beginTime` / `endTime` 在这张表上是**空操作**

返回 `R.ok(PageResult<FlowSpelVo>)`。厨房 `listSpel` 对齐这条 URL。

管理页陷阱：`SpelPage.vue` 的 `queryParams.status` **默认 `'0'`**，搜索表单却**没有**状态筛选项。所以日常翻页只看见正常行；停用行还在库里，页上像蒸发了。

#### 3. `getInfo`：`GET /workflow/spel/{id}`

权限 `workflow:spel:query`。`@PathVariable Long id`，`@NotNull(message = "主键不能为空")`。无 `@Log`。

`queryById` → `selectVoById`。**没有**空行闸。无行时 `R.ok(null)`，不是 404，也不是 `R.fail`。编辑页要把 `res.data` 空值当失败，后端不会帮你 fail。

厨房 `getSpel` 会 `encodeURIComponent`：测试把 `'spel?#'` 编成 `/workflow/spel/spel%3F%23`。后端路径变量仍是 `Long`，正常 id 是雪花数字。

#### 4. `add`：`POST /workflow/spel`

权限 `workflow:spel:add`。`@Log INSERT`。`@RepeatSubmit()`。Body `@Validated(AddGroup.class) @RequestBody FlowSpelBo`。

`insertByBo`：MapStruct 转实体 → `validEntityBeforeSave` → `insert`。成功把生成的 id 写回 `bo.setId`。`toAjax(boolean)`：true → `R.ok()`，false → `R.fail()`。

唯一闸：`viewSpel` 非空时，`eq(viewSpel)` + `neIfPresent(id)` + `exists()`。新增 id 为空，`neIfPresent` 不生效，全表（逻辑未删）撞车即抛。

前端对话框**不校验** `viewSpel`（rules 只有 status）。预览由 `updateViewSpel` 拼：

- 三框都空 → `viewSpel=''` → 后端 `@NotBlank` 挡住
- 只有参数且恰好一个 → `` `${param}` ``
- 缺组件或方法（但不是「只有单参数」那种）→ 字面量 **`请填写组件名称和方法名`**。这串能过 NotBlank，也能当唯一键入库——口试要能指出这个裂缝
- 组件+方法 → `` `#{@comp.method(#a,#b)}` ``；无参则 `()`

e2e 锁死的成功 body：`componentName=spelRuleComponent`，`methodName=resolveOwner`，`methodParams=deptId`，`viewSpel=#{@spelRuleComponent.resolveOwner(#deptId)}`，`status=0`。**没有** remark，**没有** id。

#### 5. `edit`：`PUT /workflow/spel`

权限 `workflow:spel:edit`。`@Log UPDATE`。`@RepeatSubmit()`。`EditGroup`。

`updateByBo`：同样过唯一闸再 `updateById`。

裂缝：`id` 在 Edit 组**没有** `@NotNull`。页面改行会先 `getSpel` 把 id 填进表单，正常 PUT 带 id。若有人直接 PUT 且省略 id：

- `updateById` 对空 id 通常 0 行 → `toAjax(false)` → `R.fail()`
- 更糟：唯一闸 `neIfPresent(id)` 在 id 为空时**不排除自己**。你带着库里已有的 `viewSpel` 去「改」，会先被「已存在」挡住，轮不到 0 行更新

不要把分类课「Edit 组 id 必填」说成本课。

#### 6. `remove`：`DELETE /workflow/spel/{ids}`

权限 `workflow:spel:remove`。`@Log DELETE`。**没有** `@RepeatSubmit`。`@NotEmpty` 的 `Long[] ids`。

门卫：`deleteWithValidByIds(List.of(ids), true)`。`isValid==true` 时方法体只有 TODO 注释。不查是否已贴进 `flow_node.permissionFlag`，不查是否被设计器选中。然后 `deleteByIds`——`@TableLogic`，是逻辑删。

成功/失败仍是 `toAjax`：删到 0 行（id 本就不在或已删）→ `R.fail()`。

对照分类课：分类是单个 `Long`，厨房逗号删绑不上；本课数组，厨房逗号删**就是**合同。OpenAPI 路径写成 `/{ids}`，详情写成 `/{id}`，动词不同，不打架。

#### 7. 同一厨师的侧门（不是 `.*`）

`queryList(bo)`：同一套 wrapper，不分页。Controller **不调用**。

`selectSpelByTaskAssigneeList(TaskAssigneeBody)`：设计器 SPEL 页签。自己 new 一个 `FlowSpelBo`：

- `viewSpel = handlerCode`（精确 eq）
- `remark = handlerName`（后面走 like）
- `status = "0"`
- `params.beginTime/endTime` 被放进 map，**wrapper 不用**

再 `queryPageList`。转换成 `TaskHandler` 的映射是：

`storageId = viewSpel`，`handlerCode = ""`，`handlerName = remark`，`groupName = ""`，`createTime = createTime`。

设计器再包一层：SPEL 的 type code 是空串，所以入库标识仍是 `viewSpel` 本身；空白 handlerCode 显示成「无」；空白分组显示「默认分组」。

`selectRemarksBySpels(List<String>)`：按 `viewSpel IN (...)` 取备注，给设计器回显。空列表直接空 Map。备注空则 value 是 `""`。

`FlwTaskAssigneeServiceImpl.fetchUsersByStorageIds`：SPEL 类型**故意**返回空用户列表。`convertPermissions` 因此不能靠本目录把暗号展开成工号。口试说到这里停：求值在引擎，本课不拆厂商。

`SpelRuleComponent.selectDeptLeaderById`：部门没负责人 → `ServiceException("当前部门未设置负责人，请联系管理员操作。")`。这是**运行时**被 `#{}` 点到才炸，不是保存目录时炸。

## 图、表或文本图

**图 1：宏观五扇窗与 classic 目录本**

```text
 warm-flow.enabled=true
        │  @ConditionalOnEnable
        v
 FlwSpelController     /workflow/spel
        │
        ├─ GET    /list          list     perm list    → queryPageList → PageResult
        ├─ GET    /{id}          getInfo  perm query   → queryById（可 null）
        ├─ POST   /              add      perm add     → 唯一闸 → insertByBo     RepeatSubmit Log INSERT
        ├─ PUT    /              edit     perm edit    → 唯一闸 → updateByBo     RepeatSubmit Log UPDATE
        └─ DELETE /{ids}         remove   perm remove  → TODO Valid → 逻辑删     Log DELETE
                │
                v
        IFlwSpelService          8 法；上列只用 5 个委托
                │
                v
        FlwSpelServiceImpl
                ├─ FlwSpelMapper  → flow_spel（逻辑删；XML 空壳）
                ├─ selectSpelByTaskAssigneeList   （设计器；非 HTTP）
                └─ selectRemarksBySpels           （回显；非 HTTP）
```

**图题 / caption：** 宏观同一门牌五扇窗，classic 三跳，电闸在类上。alt：五个 Java 方法映射到 `/workflow/spel`；ServiceImpl 抱 Mapper；设计器两枪不画成 REST。

**文字等价物：** 已登录的管理员打到 `/workflow/spel`。五扇都要对应权限字。门卫不写 SQL。厨师 `FlwSpelServiceImpl` 自己抱 Mapper，XML 是空的。设计器另走 `selectSpelByTaskAssigneeList` / `selectRemarksBySpels`，不经过这五个映射。关掉 `warm-flow.enabled`，整份 Controller 不进容器。

**图 2：五扇合同对照表**

| HTTP | 动词 | Java | 权限 | 写库？ | 成功形状 |
| --- | --- | --- | --- | --- | --- |
| `/list` | GET | `list` | `workflow:spel:list` | 否 | `R.ok(PageResult)` |
| `/{id}` | GET | `getInfo` | `workflow:spel:query` | 否 | `R.ok(Vo)`，无行也 ok |
| `/` | POST | `add` | `workflow:spel:add` | 是（过唯一闸） | `toAjax`；撞车 500 |
| `/` | PUT | `edit` | `workflow:spel:edit` | 是（过唯一闸） | 同上；id 非校验必填 |
| `/{ids}` | DELETE | `remove` | `workflow:spel:remove` | 是（无业务闸） | `toAjax`；0 行则 fail |

**图题 / caption：** 矩阵 `.*` 五行与磁盘 1:1。alt：分页不是扁列表；删除是 ids 数组；没有 export 行。

**文字等价物：** 口试按这张表念：路径、动词、Java 名、权限串、动不动库、成功/失败信封。发明 export 或把 list 说成扁 List，格子不满。厨房五枪 URL 与这五行对齐；菜单第六颗 export 不在表上。

**图 3：页面怎样把三框变成 viewSpel（对照，格子仍只认 Java）**

```text
 componentName / methodName / methodParams
        │  SpelPage.updateViewSpel（只读预览，不是手填）
        ├─ 全空                 → ""
        ├─ 无组件无方法、恰好 1 个参数 → ${param}          ← 种子「流程发起人」
        ├─ 缺组件或方法           → 「请填写组件名称和方法名」  ← 能过后端 NotBlank
        └─ 有组件+方法            → #{@comp.method(#a,#b)}  ← 种子「部门负责人」
                │
                v  POST /workflow/spel
        validEntityBeforeSave(viewSpel 唯一)
                │
                v
        flow_spel 一行
                │  设计器抄 viewSpel → 节点 permissionFlag
                v
        运行时引擎对 $ / # 求值（非本课）
```

**图题 / caption：** 目录存的是拼好的字符串。alt：两种合法暗号；一种能入库的垃圾预览。

**文字等价物：** 管理员并不手写 `#{}`。页面按三框拼。只有参数时走 `${}`，这是种子第二行。组件方法齐时走 `#{@bean.method(#var)}`，这是种子第一行。缺一半字段时预览变成一句中文提示，后端仍可能收下。真正算谁来盖章，不经过这五扇窗。

**图的边界：** 不画 LiteFlow 启动链（L-070）。不画 `WorkflowService.startWorkFlow`（L-069）。不保证以后会补 export 窗。不把 `skipCondition` 的 `spel@@` 画进 `flow_spel`。不把 e2e 夹具 id `'s1'` 当成生产主键。不把设计器 SPEL 页签画成第六扇 REST。

## 正例、反例与边界

**正例 1 — 打开暗号本。** 持 `workflow:spel:list` 的人打开 `workflow/spel/index`。页 `listSpel(queryParams)` → `GET /workflow/spel/list`。默认带着 `status=0`、`pageNum=1`、`pageSize=10`。种子两行都是正常，都出现。

**正例 2 — 按种子第一行的配方新增。** 点新增。组件 `spelRuleComponent`，方法 `selectDeptLeaderById`，参数 `initiatorDeptId`。预览变成 `#{@spelRuleComponent.selectDeptLeaderById(#initiatorDeptId)}`。若这串已在种子里，唯一闸 500「请勿重复添加」。e2e 改用 `resolveOwner` / `deptId`，不撞种子。

**正例 3 — 只填一个参数名，做成发起人暗号。** 组件、方法留空，参数填 `initiator`。预览 `${initiator}`。这是种子第二行的配方。保存后设计器 SPEL 页签能看见备注「流程发起人」（若你写了备注）。

**正例 4 — 改备注。** 行上修改先 `GET /{id}` 填表，再 PUT。`viewSpel` 不变则唯一闸 `neIfPresent(id)` 放行。

**正例 5 — 批量撕两页。** 勾选两行，删除。厨房 `DELETE /workflow/spel/s1,s2`。后端 `Long[]`。逻辑删后，同一 `viewSpel` 可以再新增——唯一闸看不见已删行。

**正例 6 — 设计器来借书（对照，非本格）。** 办理人类型选「SpEL表达式」。`selectSpelByTaskAssigneeList` 只出 `status=0`。选中后 storageId 就是 `viewSpel` 原串，因为 SPEL 的 code 前缀是空的。

**正例 7 — 关电闸。** `warm-flow.enabled=false` 时本类不注册。不要指望还能 `GET /list` 拿空分页。

**正例 8 — 厨房 URL 对齐五扇。** `frontend/packages/domains/workflow/src/index.test.ts` 期望 `get /workflow/spel/list`、`get /workflow/spel/s1`、`post /workflow/spel`、`put /workflow/spel`、`delete /workflow/spel/s1,s2`。格子仍只认 Java 窗，不认工厂。

**反例 1 — 「五扇之外还有 export，和分类一样。」** 分类课后端**有** `POST /export`。本课后端**没有**。两边菜单都有 export 串，不要记混谁真有窗。

**反例 2 — 「`list` 是扁 `List`，和分类一样。」** `PageResult`。矩阵名单不要把分页说丢。

**反例 3 — 「删除是单个 id，厨房逗号是前端 bug。」** 本课数组是合同。分类课才是单个 Long。

**反例 4 — 「`queryList` / 设计器两枪也是 `FlwSpelController.*`。」** 接口有、映射无。`.*` 按 Controller 公开方法数。

**反例 5 — 「保存时会调用 `SpelRuleComponent` 试算。」** 唯一闸只比字符串。部门没负责人要等到运行时点到那一枪。

**反例 6 — 「这是 layered，Service 不许碰 Mapper。」** 登记 classic；Impl 必须能指到 `spelMapper`。

**反例 7 — 「`permissionFlag` 就是 SpEL 表外键。」** 节点上存的是抄走的字符串，没有 FK。删目录行不会级联擦图纸。

**反例 8 — 「`skipCondition` 的 `spel@@#{@testLeaveServiceImpl.eval(#leaveDays)}` 也在这张表。」** 不在。那是定义边条件，请假 JSON 种子，Bean 也不是 `spelRuleComponent`。

**反例 9 — 「停用后设计器和运行时都算不了。」** 设计器侧门滤 `status=0`。运行时读节点上已抄的字。停用只影响以后再来借书的人。

**反例 10 — 「Edit 组会校验 id。」** 不会。分类课会；本课 BO 没有那条。

**反例 11 — 「唯一闸按组件+方法。」** 按 `viewSpel`。同一 Bean 方法只要拼出不同预览就能并存。

**反例 12 — 「设计器时间筛选能缩小 SpEL 列表。」** `beginTime`/`endTime` 进了 `params`，wrapper 不读。

**反例 13 — 「`convertPermissions` 会把 `${initiator}` 展开成发起人工号。」** SPEL 分支用户列表为空。展开不在这本目录。

**反例 14 — 「OBJ-71 包含 `createWorkflowDefinitionService`。」** 那是 OBJ-73。

**边界 1 — getInfo 无行仍 200。** 编辑页空 data 要自己当失败。

**边界 2 — 前端默认 status=0。** 管理页搜索框改不了状态；停用行要改 query 或直接打 HTTP 才看得到。

**边界 3 — 垃圾预览可入库。** 「请填写组件名称和方法名」过 NotBlank。第二次再提交同一句会撞唯一。

**边界 4 — 多参数但没有组件。** 前端不走 `${}` 分支（只允许单参数）。预览落到「请填写…」。不要口述「多参数也能 `${a,b}`」。

**边界 5 — 逻辑删。** 唯一闸、列表、设计器都受 `@TableLogic` 影响。已删行可重新插入同一 `viewSpel`。

**边界 6 — `toAjax(false)`。** insert/update/delete 影响 0 行是失败信封，不是 200 带 false。唯一闸走的是异常，不是 false。

**边界 7 — 权限藏按钮 ≠ 授权。** `v-hasPermi` 藏新增/改/删。直接打 HTTP 仍由 `@SaCheckPermission` 决定。list 权不够则连分页都没有。

**边界 8 — RepeatSubmit 只罩写名。** 删除连点不靠这注解。列表、详情也不罩。

**边界 9 — 无 Java 测试类。** 2026-09-17 工作树搜不到 `FlwSpel*Test`。本课证据是 Controller/Service/Mapper/DDL/前端对照，不是单测方法名。

**边界 10 — bundle。** 模块在 `bundle-full` 才进 admin；`bundle-core` 不含 workflow。电闸是运行时；bundle 是编译装配。别混成一句话。

**边界 11 — Vo 的 Excel 注解是死元数据。** 有人看到 `@ExcelProperty` 就说「有导出」。没有映射就没有窗。

**边界 12 — 菜单备注错字。** 「流程达式定义」。F 型按钮文案同样掉字。口试报 SQL 事实，不要替它改成「表达式」再假装种子已经修过。

## 变式与迁移

1. **和 L-067 对照。** 分类七扇：扁列表、有 export、单个删、树无权限字、删前三道 601。本课五扇：分页、无 export、批量删、五扇全有权限字、删除无黄灯。不要把两张表背串。
2. **和 L-068 对照。** 定义发布闸看中间节点 `permissionFlag` 空不空，**不解析** `#{}`。本目录是给设计器挑选字符串的。跳转条件 `spel@@` 写在边上，不写在 `flow_spel`。
3. **和 L-069 / L-070 对照。** 启动办理不打 `/workflow/spel`。任务窗认 taskId；合同窗认 businessId。暗号本不启流。
4. **和 L-073 对照。** 厨房五枪、`segment()` 编码、SpelPage 只读预览、权限组漏 export，都是前端课的格子。本课只借用它们当「谁扣扳机」的证据，**不**把 `A:createWorkflowDefinitionService` 标 covered。
5. **和部门负责人运行时对照。** `SpelRuleComponent` 调 `DeptService.selectDeptLeaderById`。那是被表达式点名的 Bean，不是第六扇 REST。部门没负责人的中文句只在运行时出现。
6. **和用户/角色办理人对照。** `TaskAssigneeEnum` 里 USER/ROLE/DEPT/POST 有前缀；SPEL 前缀为空，靠 `$`/`#` 识别。回显时 SPEL 走备注 Map，用户走昵称 Map。
7. **以后若要导出。** 先在 Controller 加 `POST /export`（分类课有样），再给厨房加枪，再给 Vue 按钮。种子 F 型已经在。不要让菜单继续假装有窗。
8. **以后若要删前拦「已被图纸引用」。** 必须查 `flow_node.permissionFlag` / 定义 JSON。今天 TODO 空壳，直接逻辑删。
9. **以后若要 Edit 组强制 id。** 给 `FlowSpelBo.id` 加 `@NotNull(groups=EditGroup)`。今天靠页面先 GET。
10. **以后若要管理页能看停用行。** 搜索表单补 status，或去掉 queryParams 默认 `'0'`。今天默认把它藏起来。
11. **关模块。** 可选接入用 `ObjectProvider<WorkflowService>`（集成指南）。SpEL REST 不是可选合同；关电闸就是没窗。
12. **迁移口诀：** 先数五扇 HTTP → 再数 classic 三跳（门卫不抱引擎）→ 再数「有分页、无 export、批量 ids」→ 再数唯一键是 `viewSpel` 不是 Bean 方法 → 再数目录 ≠ 求值、≠ `spel@@` 跳转。跳步会出现「把分类的 export/扁列表说过来」「把设计器页签说成 REST」「把停用说成运行时失效」。

## 常见误区

1. **「OBJ-71 是整个 workflow 模块。」** 只认 SpEL Controller。`.*` 是这一份 Java 的五个方法。
2. **「classic 等于每扇窗都一样。」** 定义/实例门卫会直接抱引擎服务；本课不会。
3. **「标准 CRUD 一定有 export。」** 菜单有，窗没有。
4. **「`list` 不分页。」** 分页。分类才不分页。
5. **「删除不能批量。」** 能。分类不能。
6. **「`wf_spel`。」** 表是 `flow_spel`。
7. **「保存会编译/试跑表达式。」** 只比字符串唯一。
8. **「`permissionFlag` 外键到 id。」** 抄的是 `viewSpel` 文本。
9. **「设计器时间筛选有效。」** wrapper 不读 params。
10. **「Edit 必须带 id，否则校验 400。」** 校验不管 id；空 id 走唯一闸或 0 行 fail。
11. **「侧门三枪算 `.*`。」** 不算。
12. **「本课覆盖厨房工厂。」** L-073。
13. **「`convertPermissions` 翻译 SPEL。」** 空用户列表。
14. **「给目录加五层才算现代化。」** 登记表禁止借机把 classic 房间改 layered。
15. **「跨模块读表达式走本课 REST。」** 跨模块合同是 `WorkflowService`，不包含 SpEL CRUD。
16. **「`${initiator}` 和 `#{@bean.method}` 是两种 Controller。」** 同一张表、同一扇 add。差别在页面怎么拼、引擎怎么认前缀。
17. **「停用等于删除。」** 停用仍占唯一键（未逻辑删）；设计器借不到；已抄走的图纸不管。
18. **「页面校验了 viewSpel。」** 只校验 status。预览只读，靠拼接。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `FlwSpelController.java`。用手指点 5 个映射。圈每一扇的权限字。圈 `add`/`edit` 的 `@RepeatSubmit` 与 `@Log`。圈 `remove` 的 `Long[]` 和没有 RepeatSubmit。确认没有 `/export`。
2. 打开 `IFlwSpelService.java`。数 8。把没有对应映射的 3 个名字抄在纸上（`queryList`、`selectSpelByTaskAssigneeList`、`selectRemarksBySpels`）。
3. 打开 `FlwSpelServiceImpl.java`。圈 `validEntityBeforeSave` 只比 `viewSpel`。圈 `deleteWithValidByIds` 的 TODO。圈 `buildQueryWrapper` 没有 `params`。圈设计器那枪强制 `status=NORMAL` 以及 `convertToHandlerList` 四个函数参数的顺序。
4. 打开 `FlowSpelBo.java`。确认 Edit 组没有 id `@NotNull`。打开 `FlowSpel.java` 圈 `@TableName("flow_spel")` 与 `@TableLogic`。打开空的 `FlwSpelMapper.xml`。
5. 打开 `30-cde-workflow.sql` 的 `flow_spel` 两行种子和菜单 1801–1806。核对 export 串存在、Controller 不存在。打开 `SpelPage.vue` 的 `updateViewSpel` 与 `queryParams.status: '0'`。打开厨房测试五条 URL。
6. 打开 `TaskAssigneeEnum.isSpelExpression` 与 `FlwTaskAssigneeServiceImpl` 的 SPEL 空用户分支。打开 `SpelRuleComponent`。不要把这三处画进本课 `.*`。不要改这些文件。

## 总结、词汇表与下一步

- **宏观五扇表达式窗：** 同一门牌 `/workflow/spel`，磁盘 5 个方法。classic 目录本：门卫只打电话给 `IFlwSpelService`；厨师抱 Mapper，不抱引擎。电闸是 `warm-flow.enabled`。
- **(a) `FlwSpelController.*`：** `list` 分页、`getInfo` 可空、`add`/`edit` 唯一闸 + 5 秒防重、`remove` 批量逻辑删且 Valid 空。不要把 `export`、设计器两枪、`queryList`、厨房工厂算进这一格。
- **目录 ≠ 计算器。** 存的是 `viewSpel` 字符串。`$` / `#` 的求值、`spel@@` 跳转、部门负责人 Bean，都是邻居。
- **两种合法暗号。** `${initiator}` 与 `#{@spelRuleComponent.selectDeptLeaderById(#initiatorDeptId)}` 是同一张表的两行种子。页面按「只有参数」或「组件+方法」拼出来。

词汇表：`FlwSpelController` / `IFlwSpelService` / `FlwSpelServiceImpl` / `FlowSpel` / `FlowSpelBo` / `FlowSpelVo` / `flow_spel` / `viewSpel` / `@ConditionalOnEnable` / `@RepeatSubmit` / `TaskAssigneeEnum.SPEL` / `SpelRuleComponent` / `selectSpelByTaskAssigneeList` / `selectRemarksBySpels` / classic。

下一步：分类树是 OBJ-67。图纸发布/导入是 OBJ-68。实例窗与 `WorkflowService` 是 OBJ-69。任务枢纽是 OBJ-70。请假 `submitAndFlowStart` 是 OBJ-72。厨房与菜单如何喊这五扇是 OBJ-73。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` Controller | 业务模块公开入口；本课类在 `wta-workflow/controller` | `FlwSpelController.java` | 2026-09-17 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-workflow` = classic；不按 layered 口述 | 登记表 classic 行 | 2026-09-17 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/30-cde-workflow.sql` | 表 `flow_spel`；两行种子；菜单 C/F 含孤儿 export | `CREATE TABLE flow_spel`；INSERT 两行；`sys_menu` 176140…1801–1806 | 2026-09-17 |
| S-L071-01 | `FlwSpelController.java` | 五扇映射；权限字；RepeatSubmit 仅 add/edit；`Long[]` 删除；无 export | 类上 `/workflow/spel` 及各方法 | 2026-09-17 |
| S-L071-02 | `IFlwSpelService.java`；`FlwSpelServiceImpl.java` | classic 厨师；8 法；唯一闸；TODO 删除；设计器两枪；wrapper 忽略 params | insert/update/delete/query/selectSpel* | 2026-09-17 |
| S-L071-03 | `FlowSpel.java`；`FlowSpelBo.java`；`FlowSpelVo.java`；`FlwSpelMapper.java`；`FlwSpelMapper.xml` | 表名；校验组无 id；Excel 死注解；空 XML | `@TableName`；Add/Edit 组；空 mapper | 2026-09-17 |
| S-L071-04 | `ConditionalOnEnable.java`；`application.yml` `warm-flow`；`BaseController.toAjax`；`RepeatSubmit`；`GlobalExceptionHandler` | 电闸 property；rows>0；5s；`ServiceException` → `R.fail` | 注解与 yml 408–423 行附近 | 2026-09-17 |
| S-L071-05 | `TaskAssigneeEnum.java`；`FlwTaskAssigneeServiceImpl.java`；`WorkflowPermissionHandler.java` | `$`/`#` 识别；SPEL 空用户；回显走备注；对照非本格 | `isSpelExpression`；`fetchUsersByType`；`convertPermissions` | 2026-09-17 |
| S-L071-06 | `SpelRuleComponent.java`；`FlowConstant.INITIATOR*` | 运行时 Bean；变量名 `initiator` / `initiatorDeptId` | `selectDeptLeaderById` | 2026-09-17 |
| S-L071-07 | `frontend/packages/domains/workflow/src/{index.ts,index.test.ts,spel/index.ts}` | 五枪 URL；无 export；`deleteSpel` 逗号拼接；资源标签 | `createWorkflowDefinitionService`；测试 `/s1,s2` | 2026-09-17 |
| S-L071-08 | `web-domains/workflow/src/spel/SpelPage.vue`；`index.ts` permissions；`e2e/workflow-definition.spec.ts` | 只读预览拼接；默认 status=0；权限组无 export；新增 mutation | `updateViewSpel`；permissions `workflow-spel`；e2e POST body | 2026-09-17 |

*生成依据：L-contract / OBJ-71 / 仓库源码核实。*
