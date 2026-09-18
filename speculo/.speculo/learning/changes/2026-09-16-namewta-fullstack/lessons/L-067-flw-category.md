---
lesson_id: L-067
objective_ids: [OBJ-67]
claimed_cells:
  - A:FlwCategoryController.*
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: seven-doors-on-disk
    minutes: 10
  - segment: classic-chain-and-gates
    minutes: 11
  - segment: visuals-and-worked-examples
    minutes: 6
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-008, S-010, S-L067-01, S-L067-02, S-L067-03, S-L067-04, S-L067-05, S-L067-06, S-L067-07, S-L067-08, S-L067-09]
---

# Lesson 067：宏观七扇分类窗——FlwCategoryController 的 classic 抽屉

## 学完你能做什么

打开 `backend/wta-modules/wta-workflow/src/main/java/org/namewta/workflow/controller/FlwCategoryController.java`，你能**口述七扇公开窗**：谁只读、谁写表、谁写 Excel 流、谁不贴权限字。口试名单就是矩阵 **(a)** 这一格，符号以**磁盘**为准：

**`A:FlwCategoryController.*`**：同一门牌 `/workflow/category` 上，**正好 7 个** Java 公开方法。chain 写成 `.*`，矩阵点名这七个：`list` / `export` / `getInfo` / `add` / `edit` / `remove` / `categoryTree`。口试按磁盘，不要漏 `export`，也不要把 Service 上的 `queryCategory`（Warm-Flow 设计器插件口）背成第八扇 HTTP。

OBJ-67 原文只要你能口述 `FlwCategoryController`。本课还要把 **classic 抽屉**讲完，否则「能口述」会退化成背路径：

- 登记表：`wta-workflow` 是 **classic**。调用链是 **Controller → `IFlwCategoryService` → `FlwCategoryServiceImpl`（自己抱 `FlwCategoryMapper`，删前还抱 Warm-Flow `DefService`）→ Mapper Java default / `BaseMapperPlus`**。`FlwCategoryMapper.xml` 是**空壳**。没有 UseCase，没有 DAO。
- 类上 `@ConditionalOnEnable`：`warm-flow.enabled=true` 才进 Spring。关开关，这七扇窗整栋消失，不是返回空列表。
- 失败分两套信封：重名 / 父节点是自己 → **`R.fail`（code 500）**；默认分类 / 有孩子 / 已绑定义 → **`R.warn`（code 601）**。浏览器 axios 把 601 当成 `kind: 'warning'`，**不是** 200。

本课**不宣称**你会拆 `FlwDefinitionController` 发布/导入（L-068）、`WorkflowService` 跨模块合同（L-069）、任务枢纽（L-070）、SpEL（L-071）、请假 `submitAndFlowStart`（L-072），或把 `createWorkflowDefinitionService` / `createWorkflowWebDomain` 当本格 covered（L-073）。厨房 URL 只当**对照**：证明七扇窗里哪些被浏览器扣了扳机，哪些种子有、厨房没收。Warm-Flow 引擎内部标 deferred。

2026-09-17 工作树先钉死**包边界**（口试先数窗，再数链）：

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| 第八扇 HTTP `queryCategory` | **没有。** 它是 `FlwCategoryServiceImpl` 实现 Warm-Flow `CategoryService` 的插件方法，不是 Controller 映射 |
| `DELETE /{categoryIds}` 批量删 | **没有。** 路径变量是单个 `Long categoryId`。厨房测试会拼 `/workflow/category/1,2`，后端绑不上 Long |
| `PageResult` 分页列表 | **没有。** `list` 回 `List<FlowCategoryVo>`，整表 |
| 分类页自己打 `categoryTree` | **管理页不打。** `CategoryPage.vue` 用 `listCategories` + 浏览器 `handleTree`；`categoryTree` 是定义/实例页的侧栏 |
| 厨房 `exportCategory` | **没有这支枪。** 后端有 `POST /export`，菜单有 F 型 `workflow:category:export`，web-domain 权限组**没收** export 串 |
| 表名 `wf_category` | **注释遗产。** `@TableName("flow_category")`；DDL 也是 `flow_category` |
| 默认分类 ID `100L` 能护住种子「OA审批」 | **护不住。** 常量 `FLOW_CATEGORY_ID = 100L`；种子根是 `1762300000000000100` |
| 给分类房间加 UseCase/DAO | **本课不发动。** 登记表 classic；保持现状 |

## 先把宏观地图放在桌上

L-003 已经把 `wta-workflow` 钉在 classic 列：既有 Warm-Flow 流程能力，通过公开 `WorkflowService` 接入，不改内部层次。本课走进**分类抽屉**，不是定义图纸、不是运行中的单子、不是跨模块那部对讲机。

把流程楼想成档案室。分类是**抽屉上的标签**：OA审批 / 假勤管理 / 请假。标签自己不跑流程。定义（L-068）才把一张图纸塞进某个标签；实例和任务（L-069 / L-070）才让单子在抽屉之间走。跨模块业务（档案申请、请假样例）打电话给 `wta-api` 的 `WorkflowService`，**不**打 `/workflow/category`。

2026-09-17 工作树：`controller/` 一共 **六份** Java，全部 `@ConditionalOnEnable` + `@RequestMapping("/workflow/...")`：

```text
浏览器 / admin-web
        │  Admin-Token；无类上 @SaIgnore
        v
/workflow/category/*     FlwCategoryController     ← 本课七扇
/workflow/definition/*   FlwDefinitionController   L-068（门卫还直接抱 DefService）
/workflow/instance/*     FlwInstanceController     L-069（另抱 InsService）
/workflow/task/*         FlwTaskController         L-070
/workflow/spel/*         FlwSpelController         L-071
/workflow/leave/*        TestLeaveController       L-072
        │
        v  classic
IFlwCategoryService
        │
        v
FlwCategoryServiceImpl     自己抱 FlwCategoryMapper
                           删前问 DefService.exists（分类字段是字符串化的 id）
                           另实现 CategoryService.queryCategory（设计器，非 HTTP）
        │
        v
flow_category（逻辑删 del_flag）
Redis 缓存名 flow_category_name#30d   只给「id → 名称」翻译，不给 list
```

六份 Controller 都在同一间 classic 房间里。**家具不一样**：分类 / SpEL / 请假更像普通柜台；定义和实例的门卫会直接伸手进 Warm-Flow 服务。不要把「workflow = classic」说成「每一扇窗都只抱自己的 Mapper」。分类这一扇：**门卫只打电话给 `IFlwCategoryService`**，不注入 `DefService`。

**类比：** 档案室门口有一排小窗，专门管抽屉标签。你可以看标签本、把标签本印成 Excel、看某一张标签、贴新标签、改标签、撕标签、再要一棵标签树给隔壁「图纸柜台」当目录。厨房（浏览器 domain）认识其中六枪；印 Excel 那一枪种子上有按钮字，管理页没画按钮。

**类比失效处：**

1. 标签树 **不是** 流程引擎。`categoryTree` 只返回 hutool `Tree<String>`。真正跑起来的是定义/实例/任务。
2. 「默认分类不许删」**不是**种子里的 OA 根。闸写死 `100L`，种子根是雪花 id。
3. 管理页表格看起来是树，HTTP `list` 却是**扁列表**。树是浏览器 `handleTree` 拼的。
4. 设计器里的分类下拉走 Service 的 `queryCategory()`，**不**走这七扇 HTTP。
5. `warm-flow.enabled=false` 时不是「分类接口还在、引擎关了」。Controller Bean 不注册。
6. 别的模块不要 Maven 依赖 `wta-workflow`。它们要流程，打 `WorkflowService`（L-069），不要复制本课 REST。

## 核心概念与机制

### 直觉讲解

小孩子版只记十二句：

1. **一块牌子，七扇窗。** `@RequestMapping("/workflow/category")`。矩阵 `.*` = 磁盘 7 个方法，不是「CRUD 四个字」。
2. **classic，不是五层。** 门卫 → 接口 → `ServiceImpl` 抱 Mapper。不要发明 `FlwCategoryUseCase`。
3. **整表，不分页。** `GET /list` 回 `List`。导出同一份 `queryList`，只是改走 Excel 流。
4. **树有两张脸。** 管理页：扁列表 + 前端拼树。图纸/实例页：`GET /categoryTree`，后端拼树，JSON 显示名键是 **`label`**。
5. **名字只在亲兄弟里唯一。** 校验是 `(categoryName, parentId)`，不是全局唯一。
6. **新增必须有真实的父行。** `selectById(parentId)` 为空 → `ServiceException("父级流程分类不存在!")`。表里没有 `category_id=0` 的行，所以 **不能靠 HTTP 再造一个根**。
7. **顶级的父不能改。** `parentId == 0` 的行，想换成别的父 → `ServiceException("不允许修改顶级分类的父级节点")`。
8. **不能给自己当爸爸。** 这闸在 **Controller** `edit`，不在 Service。
9. **删除三道黄灯。** 默认 id / 有孩子 / 已绑定义 → `R.warn` 601，**不调** `deleteWithValidById`。名字带 Valid，真正的校验在门卫。
10. **`categoryTree` 不贴权限字。** 只要已登录（类上没有 `@SaIgnore`，仍走会话）。所以没有 `workflow:category:list` 的人，定义页仍可能把树拉出来。
11. **开关是整栋电闸。** `@ConditionalOnEnable` = `warm-flow.enabled=true`。默认 `application.yml` 为 true。
12. **缓存只管译名。** `@Cacheable` 在 `selectCategoryNameById`，不在 `queryList`。改名/删除才 `@CacheEvict`。

**类比失效边界：** 「抽屉标签」**不**覆盖 Warm-Flow 表 `flow_definition.category` 怎么写成字符串。删前 `exists` 用 `categoryId.toString()` 去对那一列。类比也**不**等于「前端七个按钮」——导出枪在后端，管理页没扳机。类比还不等于「`100L` 是 OA 审批」——那是注释里的旧数字。

### 精确定义与 English term

| 中文口头 | English term | 精确定义（本课，以 2026-09-17 工作树为准） |
| --- | --- | --- |
| 流程分类控制器 | `FlwCategoryController` | classic `@RestController`，`/workflow/category`，继承 `BaseController`。七个公开映射。类上 `@ConditionalOnEnable` `@Validated` `@RequiredArgsConstructor` |
| 条件启用 | `@ConditionalOnEnable` | `@ConditionalOnProperty(value="warm-flow.enabled", havingValue="true")`。Controller 与 `FlwCategoryServiceImpl`、`CategoryNameTranslationImpl` 都贴了 |
| 经典三层 | classic | 登记表：`wta-modules/wta-workflow`。Controller → Service / ServiceImpl → Mapper。ServiceImpl 允许持 Mapper；本课不发动迁 layered |
| 业务对象 | `FlowCategoryBo` | 入参。`AddGroup`/`EditGroup`：`parentId` `@NotNull`，`categoryName` `@NotBlank`；`categoryId` 只在 Edit 组 `@NotNull`。`orderNum` 无校验 |
| 视图对象 | `FlowCategoryVo` | 出参。`parentName` 靠 `@Translation(type=CATEGORY_ID_TO_NAME, mapper="parentId")`。Excel 只导出 id / 名称 / 顺序 / 创建时间 |
| 实体 | `FlowCategory` | `@TableName("flow_category")`，`@TableLogic delFlag`。JavaDoc 仍写 `wf_category`，以注解和 DDL 为准 |
| 分类服务 | `IFlwCategoryService` / `FlwCategoryServiceImpl` | 模块内门面。Impl 另实现 Warm-Flow `CategoryService` |
| 名称唯一 | `checkCategoryNameUnique` | 同父同名 `exists` 则为不唯一。编辑用 `neIfPresent(categoryId)` 排除自己 |
| 默认分类闸 | `FlowConstant.FLOW_CATEGORY_ID` | **字面量 `100L`**。注释写「默认租户 OA 申请分类 ID」。种子 OA 根不是这个值 |
| 名称缓存 | `FLOW_CATEGORY_NAME` | 缓存名 `flow_category_name#30d`。`#30d` 是 TTL 后缀。只服务 id→名称 |
| 祖级链 | `ancestors` | 逗号分隔的祖先 id。新增：`父.ancestors + "," + parentId`。`StringUtils.SEPARATOR` 是 `","` |
| 警告信封 | `R.warn` | `HttpStatus.WARN = 601`。删除三道闸用它。axios 映射 `kind: 'warning'` 并 **reject** |
| 失败信封 | `R.fail` | `HttpStatus.ERROR = 500`。重名、父是自己。`toAjax(rows)` 在 rows==0 时也走 fail |
| 防重复提交 | `@RepeatSubmit()` | 只贴 `add` / `edit`。默认间隔 5000ms |
| 分类树 | `categoryTree` / `Tree<String>` | `selectCategoryTreeList` → `TreeBuildUtils.buildMultiRoot`。节点 id/parentId 转字符串；`setName` 写入显示名；全局 `nameKey` 被改成 `"label"` |
| 设计器分类口 | `CategoryService.queryCategory` | 返回 `org.dromara.warm.flow.core.dto.Tree`。**不是**本课 HTTP |
| 资源标签 | `workflowCategoryResource` | 前端 `controller: 'FlwCategoryController'`，`basePath: '/workflow/category'`。对照用，本课不认前端格子 |

### 机制/因果链

#### 1. 类上合同：电闸、门牌、电话

文件：`.../controller/FlwCategoryController.java`。

类注解：`@ConditionalOnEnable` `@Validated` `@RequiredArgsConstructor` `@RestController` `@RequestMapping("/workflow/category")`。

构造注入**只有** `IFlwCategoryService flwCategoryService`。全文搜不到 `DefService`、`LoginHelper`、`PageQuery`。

`application.yml`：`warm-flow.enabled: true`。LiteFlow `enable: ${warm-flow.enabled:true}` 跟着这把电闸。关电闸：本 Controller Bean 不存在，HTTP 是 404/未映射，不是空数组。

类上没有 `@SaIgnore`。六扇贴了 `@SaCheckPermission`；`categoryTree` **故意不贴**。已登录才能进楼，进了楼这棵树不查分类权限字。

#### 2. `list`：`GET /workflow/category/list`

权限 `workflow:category:list`。无 `@Log`。入参 `FlowCategoryBo bo` **不走** Add/Edit 组，所以名称/父 id 都可以空。

委托 `queryList` → `buildQueryWrapper`：

- `eqIfPresent`：`categoryId` / `parentId`
- `likeIfText`：`categoryName`
- 排序：`ancestors`、`parentId`、`orderNum`、`categoryId` 升序

返回 `R.ok(List<FlowCategoryVo>)`。**不是** `PageResult`。对照 `FlwSpelController.list` / `FlwDefinitionController.list` 都分页——分类这一格不要背错。

Vo 上的 `parentName` 在序列化时走 `CategoryNameTranslationImpl` → `selectCategoryNameById`（带 30 天缓存）。

管理页 `CategoryPage.vue` 拿到扁数组后 `handleTree(..., 'categoryId', 'parentId')` 才变成表格树。搜索只传名称时，结果可能缺父节点，前端会把它们画成**多个根**。这是扁列表 + 客户端拼树的代价，不是后端 `buildMultiRoot`。

#### 3. `export`：`POST /workflow/category/export`

权限 `workflow:category:export`。`@Log(title="流程分类", businessType=EXPORT)`。返回类型 **`void`**，不是 `R<>`。

同一份 `queryList`，然后 `ExcelBuilder.of(list, FlowCategoryVo.class).sheetName("流程分类").toResponse(response)`。Excel 列以 `@ExcelProperty` 为准：id、名称、顺序、创建时间。`parentName` / `ancestors` **没有** Excel 注解，不会进表。

前端：厨房 `WorkflowDefinitionService` **没有** export 方法；`CategoryPage` 工具栏没有导出按钮；web-domain `workflow-category` 权限组五串（list/query/add/edit/remove）**不含** export。种子 F 型第 5 颗仍是 `workflow:category:export`。口试说「有导出窗」指 **Java + 菜单字**，不指「分类页能点」。

#### 4. `getInfo`：`GET /workflow/category/{categoryId}`

权限 `workflow:category:query`。`@NotNull` 在路径变量上（类上 `@Validated` 让它生效）。`R.ok(queryById)`。查不到是 `R.ok(null)`，**不是** fail。管理页编辑先打这一枪，再 `Object.assign` 进表单。

#### 5. `add`：`POST /workflow/category`

权限 `workflow:category:add`。`@Log INSERT` + `@RepeatSubmit()`。`@Validated(AddGroup.class) @RequestBody FlowCategoryBo`。

门卫顺序：

1. `checkCategoryNameUnique` 为 false → `R.fail("新增流程分类'" + name + "'失败，流程分类名称已存在")`。**不 insert**。
2. `toAjax(insertByBo)`。`toAjax(int rows)`：`rows > 0` 才 `R.ok()`，否则 `R.fail()`（默认「操作失败」）。

`insertByBo` 里屋：

1. `selectById(parentId)`，null → `ServiceException("父级流程分类不存在!")`。
2. MapStruct 转实体。
3. `ancestors = 父.ancestors + "," + parentId`。
4. `categoryMapper.insert`。无 `@CacheEvict`（新 id 本来没译名缓存）。无 `@Transactional`（单 insert）。

种子根 `OA审批`：`category_id=1762300000000000100`，`parent_id=0`，`ancestors='0'`。给它加孩子会得到 `ancestors='0,1762300000000000100'`，和种子「假勤管理」同形。

e2e `workflow-definition.spec.ts` 锁：在分类页对「审批」行点新增，POST body `{ categoryName: '财务审批', parentId: 'c1', orderNum: 0 }`。失败夹具返回 code 500 时：可见「分类保存失败」，**零**「操作成功」，对话框不关，表不出现新行。

#### 6. `edit`：`PUT /workflow/category`

权限 `workflow:category:edit`。`@Log UPDATE` + `@RepeatSubmit()`。`EditGroup` 还要求 `categoryId`。

门卫顺序：

1. 重名 → `R.fail`（措辞换成「修改…已存在」）。
2. `parentId.equals(categoryId)` → `R.fail("…上级流程分类不能是自己")`。这是 **equals**，不是 Service 里对 `0L` 的 `==`。
3. `toAjax(updateByBo)`。

`updateByBo`：`@CacheEvict(cacheNames=FLOW_CATEGORY_NAME, key="#bo.categoryId")` + `@Transactional(rollbackFor=Exception.class)`。

1. 旧行不存在 → `ServiceException("流程分类不存在，无法修改")`。
2. 旧 `parentId == 0L` 且新父不是 0 → `ServiceException("不允许修改顶级分类的父级节点")`。这里是 **拆箱 `==`**，旧父为 null 会 NPE；种子顶级父是 `0`，不是 null。
3. 父没变：把旧 `ancestors` 写回，避免被 convert 清掉。
4. 父变了：新父必须存在；`newAncestors = 新父.ancestors + "," + 新父.categoryId`；再 `findInSet(categoryId, ancestors)` 找出子孙，把子孙 `ancestors` 里的旧前缀 `replaceFirst` 成新前缀，`updateBatchById`。

改名会踢掉**这一 id** 的译名缓存。子孙只改 ancestors 字符串，不改自己的名称缓存键。

#### 7. `remove`：`DELETE /workflow/category/{categoryId}`

权限 `workflow:category:remove`。`@Log DELETE`。**没有** `@RepeatSubmit`。路径是单个 `Long`，不是 `Long[]`。对照 `FlwSpelController.remove` 的 `/{ids}` 数组。

门卫三道黄灯，**短路且不删**：

| 条件 | 信封 | 文案 |
| --- | --- | --- |
| `FLOW_CATEGORY_ID.equals(categoryId)` 即 `100L` | `R.warn` 601 | `默认流程分类,不允许删除` |
| `hasChildByCategoryId`（存在 `parentId=该 id` 的行） | `R.warn` 601 | `存在下级流程分类,不允许删除` |
| `checkCategoryExistDefinition` | `R.warn` 601 | `流程分类存在流程定义,不允许删除` |

第三道：`new FlowDefinition(); setCategory(categoryId.toString()); defService.exists(definition)`。引擎里分类是**字符串**。不要说成「外键列是 Long」。

都过了才 `toAjax(deleteWithValidById)`。Service 方法**不再校验**，只 `@CacheEvict` + `deleteById`。`@TableLogic` → 逻辑删 `del_flag`。

种子「OA审批」有孩子，所以日常删根会撞第二道黄灯，**轮不到** `100L`。若有人先把子孙删光、定义也迁走，根 **可以被删**——常量护栏对种子 id 无效。这是磁盘事实，不是「应该如此」的产品承诺。

厨房 `deleteCategory` 把数组 `join(',')`。`index.test.ts` 锁 `/workflow/category/1,2`。管理页 `handleDelete` 只传 `row.categoryId` 一个。口试不要把「厨房能拼逗号」说成「后端批量删」。`1,2` 绑 `Long` 会在转换期失败，进不了三道黄灯。

axios：`code === 601` → `kind: 'warning'`，Promise **reject**。黄灯不是成功 toast。

#### 8. `categoryTree`：`GET /workflow/category/categoryTree`

**无** `@SaCheckPermission`，无 `@Log`，无 `@RepeatSubmit`。入参仍是 `FlowCategoryBo`（可当过滤）。`R.ok(selectCategoryTreeList)`。

空列表 → `newArrayList()`，不是 null。非空 → `TreeBuildUtils.buildMultiRoot`：id/parentId 转字符串；`setName(categoryName)`、`setWeight(orderNum)`。`TreeBuildUtils.DEFAULT_CONFIG` 把 hutool `nameKey` 设为 `"label"`，所以 JSON 显示名是 **`label`**，对齐前端 `CategoryTreeVO` 与定义页 `filter-field="label"`、`props.label = 'label'`。

`buildMultiRoot` 的根：父 id **不在本批 id 集合里** 的那些节点。全表时种子根的父是 `"0"`，而没有 id=`0` 的节点，于是 OA 审批成为根。

谁在扣扳机：`DefinitionPage` / `InstancePage` / `MyDocumentPage` 喊 `runtime.service.categoryTree()`。**`CategoryPage` 不喊。** 分类管理的下拉树走第二次 `listCategories()`。

#### 9. 同一厨师的侧门（不是 `.*`）

`FlwCategoryServiceImpl implements IFlwCategoryService, CategoryService`。`queryCategory()` 把扁列表改成 Warm-Flow `core.dto.Tree`（id/name/parentId）。设计器插件走这扇，**不要**画进 Controller 七扇。

`CategoryNameTranslationImpl` `@TranslationType(CATEGORY_ID_TO_NAME)`：列表 Vo 的父名、别的模块把分类 id 译成字，走缓存那一枪。它也 `@ConditionalOnEnable`。

`FlwInstanceServiceImpl` 按分类筛实例时，会 `flwCategoryMapper.selectCategoryIdsByParentId` 把**子孙 id 一齐**收进 `IN` 条件。分类树不只是标签本，还是实例查询的「含下级」展开器。展开器在实例课认，本课只要知道 Mapper 多了 `findInSet(ancestors)` 的 default 方法。

## 图、表或文本图

**图 1：宏观七扇窗与 classic 抽屉**

```text
 warm-flow.enabled=true
        │  @ConditionalOnEnable
        v
 FlwCategoryController     /workflow/category
        │
        ├─ GET  /list                 list          perm list     → queryList → 扁 List
        ├─ POST /export               export        perm export   → 同一 queryList → Excel 流
        ├─ GET  /{categoryId}         getInfo       perm query    → queryById（可 null）
        ├─ POST /                     add           perm add      → 唯一闸 → insertByBo
        ├─ PUT  /                     edit          perm edit     → 唯一闸 + 不能是自己 → updateByBo
        ├─ DELETE /{categoryId}       remove        perm remove   → 100L / 孩子 / 定义 → deleteById
        └─ GET  /categoryTree         categoryTree  （无 perm）   → buildMultiRoot → Tree<label>
                │
                v
        IFlwCategoryService
                │
                v
        FlwCategoryServiceImpl
                ├─ FlwCategoryMapper  → flow_category（逻辑删）
                ├─ DefService.exists  （仅删前）
                └─ CategoryService.queryCategory   （设计器；非 HTTP）
```

**图题 / caption：** 宏观同一门牌七扇窗，classic 三跳，电闸在类上。alt：七个 Java 方法映射到 `/workflow/category`；ServiceImpl 抱 Mapper，删前问引擎；设计器 `queryCategory` 不画成第八扇 REST。

**文字等价物：** 已登录的管理员打到 `/workflow/category`。六扇要对应权限字，树这一扇只要有会话。门卫不写 SQL。厨师 `FlwCategoryServiceImpl` 自己抱 Mapper，XML 是空的。只有删除前的「有没有定义」才问路对面 Warm-Flow 的 `DefService`。设计器另走 `queryCategory`，不经过这七个映射。关掉 `warm-flow.enabled`，整份 Controller 不进容器。

**图 2：七扇合同对照表**

| HTTP | 动词 | Java | 权限 | 写库？ | 成功形状 |
| --- | --- | --- | --- | --- | --- |
| `/list` | GET | `list` | `workflow:category:list` | 否 | `R.ok(List)` 扁列表 |
| `/export` | POST | `export` | `workflow:category:export` | 否 | Excel 流，`void` |
| `/{categoryId}` | GET | `getInfo` | `workflow:category:query` | 否 | `R.ok(Vo)`，无行也 ok |
| `/` | POST | `add` | `workflow:category:add` | 是（过唯一闸） | `toAjax`；重名 `R.fail` 500 |
| `/` | PUT | `edit` | `workflow:category:edit` | 是（过两闸） | 同上；父是自己也 500 |
| `/{categoryId}` | DELETE | `remove` | `workflow:category:remove` | 过三道黄灯才删 | 黄灯 `R.warn` 601 |
| `/categoryTree` | GET | `categoryTree` | **无** | 否 | `R.ok(List<Tree<String>>)` |

**图题 / caption：** 矩阵 `.*` 七行与磁盘 1:1。alt：导出是流；树无权限字；删除黄灯是 601 不是 200。

**文字等价物：** 口试按这张表念：路径、动词、Java 名、权限串、动不动库、成功/失败信封。漏 export 或把 categoryTree 说成要 `list` 权限，格子不满。厨房只覆盖 list/get/add/update/delete/tree 六枪；export 在表上仍在。

**图 3：删除三道黄灯与种子根**

```text
 DELETE /workflow/category/{id}
        │
        ├─ id == 100L ? ──是──► R.warn 601「默认流程分类,不允许删除」
        │         │否
        ├─ 存在 parent_id=id 的行 ? ──是──► R.warn 601「存在下级…」
        │         │否
        ├─ DefService.exists(category=id.toString()) ? ──是──► R.warn 601「存在流程定义…」
        │         │否
        └─ deleteById + 踢译名缓存     （逻辑删）

 种子根 OA审批 = 1762300000000000100 ≠ 100L
        └─ 日常撞「有下级」，不是撞 100L
```

**图题 / caption：** 黄灯在门卫，Service 的 `deleteWithValidById` 不再验。alt：三道短路；种子根 id 与常量不一致。

**文字等价物：** 删除先比三个条件。前三个命中都回 601，一行都不删。真正删除只是逻辑删并清这一 id 的名称缓存。种子里叫「OA审批」的根不是 `100L`，常量那一道对它无效；它通常被「有孩子」挡住。

**图的边界：** 不画 LiteFlow 启动链（L-070）。不画 `WorkflowService.startWorkFlow`（L-069）。不保证以后会把 `100L` 改成种子 id。不把 hutool `Tree` 画成菜单 `sys_menu`。不把 e2e 夹具 id `'c1'` 当成生产主键。不把厨房逗号删除画成已支持的后端合同。

## 正例、反例与边界

**正例 1 — 打开分类本。** 持 `workflow:category:list` 的人打开 `workflow/category/index`。页 `listCategories({ categoryName })` → `GET /workflow/category/list`。后端扁列表，前端拼树。

**正例 2 — 在「审批」下贴新标签。** 行上「新增」把 `parentId` 设成该行 id。POST `/workflow/category`，AddGroup 校验名称和父。唯一闸过了才 insert，ancestors 接在父链后面。e2e 锁 body 含 `categoryName` / `parentId` / `orderNum`。

**正例 3 — 改名。** 行上「修改」先 `GET /{id}` 填表，再 PUT。同父不能撞名。把父改成自己：门卫 500，进不了 `updateByBo`。

**正例 4 — 撕一张叶子。** 无孩子、无定义、id 不是 100 → 逻辑删，译名缓存踢掉。页上一次删一行。

**正例 5 — 撕一张还有孩子的。** `R.warn` 601「存在下级流程分类,不允许删除」。axios warning，前端当失败，不当成功。

**正例 6 — 图纸页要点分类。** `DefinitionPage` `categoryTree()` → `GET /categoryTree`。点节点把 `queryParams.category = data.id`；`id === '0'` 时清空，表示不按分类筛。这一枪**不**要求 `workflow:category:list`。

**正例 7 — 导出。** 持 export 权限的人用 HTTP 客户端 POST `/export`，收到 `.xlsx` 流。分类 Vue **没有**这颗按钮。

**正例 8 — 关电闸。** `warm-flow.enabled=false` 时本类不注册。不要指望还能 `GET /list` 拿空数组。

**正例 9 — 译名缓存。** 某 Vo 的 `parentId` 第一次翻译打 SQL；之后同 id 走 `flow_category_name#30d`（TTL 三十天，不是三十分钟）。改名 PUT 会 evict。

**反例 1 — 「七扇都要 `workflow:category:*`。」** `categoryTree` 没有权限注解。

**反例 2 — 「`list` 是分页树。」** 扁 `List`，不分页；树在浏览器或在 `categoryTree`。

**反例 3 — 「删除是 `/{ids}` 批量，和 SpEL 一样。」** 单个 `Long`。厨房测试的 `1,2` 不是后端合同。

**反例 4 — 「默认分类就是种子 OA 审批。」** 常量 `100L`；种子 `1762300000000000100`。

**反例 5 — 「表名 `wf_category`。」** 注解和 DDL 都是 `flow_category`。JavaDoc 过期。

**反例 6 — 「这是 layered，Service 不许碰 Mapper。」** 登记 classic；Impl 必须能指到 `categoryMapper`。

**反例 7 — 「门卫注入了 `DefService`。」** 定义课才直接抱引擎。分类课只在 ServiceImpl 删前 exists。

**反例 8 — 「`queryCategory` 是第八扇 REST。」** Warm-Flow UI 插件口。

**反例 9 — 「`R.warn` 仍是成功信封。」** code 601；axios reject；`kind: 'warning'`。

**反例 10 — 「分类页喊 `categoryTree`。」** 它喊两次 `listCategories`。

**反例 11 — 「可以 POST 一个 `parentId=0` 的新根。」** `selectById(0)` 为空，抛「父级流程分类不存在!」。

**反例 12 — 「名称全局唯一。」** 只在同一 `parentId` 下唯一。

**反例 13 — 「`deleteWithValidById` 会再查孩子。」** 不会。Valid 在 Controller。

**反例 14 — 「OBJ-67 包含 `createWorkflowDefinitionService`。」** 那是 OBJ-73。

**边界 1 — getInfo 无行仍 200。** 编辑页要把 `res.data` 空值当失败，后端不会帮你 fail。

**边界 2 — 唯一闸在门卫，父存在闸在厨师。** 重名 500 且不进 insert；父缺失是异常，不一定是同一句 `R.fail` 文案。

**边界 3 — `parentId.equals` 与 `== 0L` 不是同一套比较。** 自父用 equals；顶级用拆箱。

**边界 4 — 逻辑删。** `exists` / `queryList` 受 `@TableLogic` 影响；已删行不再当孩子、不再撞名。

**边界 5 — 缓存可能把「查不到」也记住。** `selectCategoryNameById` 无 `unless`。雪花 id 让「先缓存 null 再插入同 id」极稀有，但不要说「没行就不进缓存」。

**边界 6 — 列表 like 与树过滤。** `categoryTree` 也走 `queryList`。只搜一个叶子时，`buildMultiRoot` 会把它当根，因为父不在本批里。

**边界 7 — 权限藏按钮 ≠ 授权。** e2e：只有 list+query 时新增按钮消失。直接打 POST 仍由 `@SaCheckPermission` 决定。`categoryTree` 连按钮字都没有。

**边界 8 — RepeatSubmit 只罩写名两扇。** 删除连点不靠这注解。导出也不罩。

**边界 9 — 无 Java 测试类。** 2026-09-17 工作树搜不到 `FlwCategory*Test`。本课证据是 Controller/Service/Mapper/DDL/前端对照，不是单测方法名。

**边界 10 — bundle。** 模块在 `bundle-full` 才进 admin；`bundle-core` 不含 workflow。电闸是运行时；bundle 是编译装配。别混成一句话。

## 变式与迁移

1. **和 L-003 对照。** classic 列允许 ServiceImpl 抱 Mapper。本课是「workflow 房间里最像 demo/system 树表」的那一扇。不要用本课去改登记表，也不要给分类加 UseCase。
2. **和 L-015 / L-017 对照。** 用户/部门也是 classic 柜台。部门树同样整表 List + 不能删有孩子的。差别：部门有数据权限注解；分类 **没有** `@DataPermission`。部门缓存是 `SYS_DEPT*`；分类缓存只译名。
3. **和同模块另外五扇对照。** 定义门卫直接抱 `DefService`（L-068）；实例抱 `InsService`（L-069）；任务窗口更长且部分无权限字（L-070）；SpEL 才是真正的 `/{ids}` 批量删（L-071）；请假才 `submitAndFlowStart`（L-072）。本课不要把那五份的方法表背进来。
4. **和 L-073 对照。** 厨房六枪 URL 与 `segment()` 编码、分类页 `handleTree`、定义页 `categoryTree`、权限组漏 export，都是前端课的格子。本课只借用它们当「谁扣扳机」的证据，**不**把 `A:createWorkflowDefinitionService` 标 covered。
5. **和 profile 的 workflow 对照。** 档案申请启动走 `WorkflowService.start`（L-034/L-035），**零**分类 REST。不要在分类窗上画 `startWorkFlow`。
6. **和部门 ancestors 对照。** 都是逗号祖链 + 换父时改子孙。分类换父还禁止动顶级。新增双方都要求父行存在。
7. **以后若要护住种子根。** 应让闸读取真实根 id 或 `parent_id=0`，而不是继续背 `100L`。本课不改代码。
8. **以后若要批量删。** 必须改路径类型并决定黄灯是「一票否决」还是「跳过坏行」。不要让厨房继续 `join(',')` 假装已经批量。
9. **以后若要分类页导出。** 先给厨房加枪，再给 Vue 按钮，权限组补 `workflow:category:export`。种子已经有 F 型。
10. **关模块。** 可选接入用 `ObjectProvider<WorkflowService>`（集成指南）。分类 REST 不是可选合同；关电闸就是没窗。
11. **迁移口诀：** 先数七扇 HTTP → 再数 classic 三跳（门卫不抱引擎）→ 再数 500 vs 601 → 再数 `100L` ≠ 种子根 → 再数 list 扁 / tree 后端 / 设计器侧门。跳步会出现「把分类说成引擎」「把 601 说成成功」「把厨房逗号说成后端批量」。

## 常见误区

1. **「OBJ-67 是整个 workflow 模块。」** 只认分类 Controller。`.*` 是这一份 Java 的七个方法。
2. **「classic 等于每扇窗都一样。」** 定义/实例门卫会直接抱引擎服务；分类不会。
3. **「`categoryTree` 要 list 权限。」** 注解没有。
4. **「管理页的树 = `GET /categoryTree`。」** 管理页是 `GET /list` + `handleTree`。
5. **「JSON 树字段是 `name`。」** 显示名键被改成 `label`。
6. **「导出在分类页工具栏。」** 2026-09-17 没有。后端窗仍在。
7. **「`wf_category`。」** 表是 `flow_category`。
8. **「默认分类 100 就是 OA 审批。」** 种子不是。
9. **「名称全库唯一。」** 同父唯一。
10. **「可以新增顶级。」** 父=0 没有行。
11. **「`deleteWithValidById` 名如其义。」** 校验在门卫。
12. **「601 是成功带警告。」** 客户端当 warning **错误**。
13. **「前端授权。」** `v-hasPermi` 藏按钮；REST 仍是 `@SaCheckPermission`。树这一扇连权限字都没有。
14. **「给分类加五层才算现代化。」** 登记表禁止借机把 classic 房间改 layered。
15. **「跨模块删分类走本课 REST。」** 跨模块合同是 `WorkflowService`，不包含分类 CRUD。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `FlwCategoryController.java`。用手指点 7 个映射。圈 `categoryTree` 没有 `@SaCheckPermission`。圈 `remove` 三道 `R.warn`。圈 `add`/`edit` 的 `R.fail` 与 `@RepeatSubmit`。
2. 打开 `FlowConstant.java`。抄下 `FLOW_CATEGORY_ID` 和 `FLOW_CATEGORY_NAME`。打开 `30-cde-workflow.sql` 的 `flow_category` INSERT，核对根 id 不是 100。
3. 打开 `FlwCategoryServiceImpl.java`。圈 `implements IFlwCategoryService, CategoryService`。圈 `insertByBo` 父行检查、`updateByBo` 顶级父检查、`checkCategoryExistDefinition` 的 `toString()`、`queryCategory` **没有** RequestMapping。圈 XML 空壳。
4. 打开另外五份 `controller/*.java` 的类注解，确认都是 `@ConditionalOnEnable`，再确认 **只有分类这份** 门卫字段是单纯的 `IFlwCategoryService`。
5. 打开 `frontend/packages/domains/workflow/src/index.ts` 分类六枪与 `index.test.ts` 的 `/1,2`。打开 `CategoryPage.vue`，确认删除传单个 id、列表走 `listCategories`、没有 export。打开 `DefinitionPage.vue` 圈 `categoryTree` 与 `label`。
6. 打开 `R.java` 的 `warn` 与 `HttpStatus.WARN`。打开 axios-browser `code === 601` 分支。不要改这些文件。

## 总结、词汇表与下一步

- **宏观七扇分类窗：** 同一门牌 `/workflow/category`，磁盘 7 个方法。classic 抽屉：门卫只打电话给 `IFlwCategoryService`；厨师抱 Mapper，删前才问引擎有没有定义。电闸是 `warm-flow.enabled`。
- **(a) `FlwCategoryController.*`：** `list` 扁列表、`export` Excel 流、`getInfo` 可空、`add`/`edit` 500 闸 + 5 秒防重、`remove` 601 三道黄灯后逻辑删、`categoryTree` 无权限字。不要把 `queryCategory`、厨房逗号删除、前端 export 按钮算进这一格。
- **种子与常量分手。** OA 根是雪花 id；`100L` 护栏对它无效。表名是 `flow_category`。
- **两张树。** 管理页前端拼；图纸/实例页打 `categoryTree`。设计器还有第三张，不走 HTTP。

词汇表：`FlwCategoryController` / `IFlwCategoryService` / `FlwCategoryServiceImpl` / `FlowCategory` / `FlowCategoryBo` / `FlowCategoryVo` / `flow_category` / `@ConditionalOnEnable` / `FLOW_CATEGORY_ID` / `FLOW_CATEGORY_NAME` / `R.warn` / `HttpStatus.WARN` / `ancestors` / `TreeBuildUtils.buildMultiRoot` / `CategoryService.queryCategory` / classic。

下一步：L-068 把分类 id 写进流程定义并发布/导入。L-069 才是实例与 `WorkflowService`。L-070 任务枢纽。L-073 才把厨房六枪和分类页/定义侧栏对完。本课结束不发作业、不打分；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` Controller | 业务模块公开入口；本课类在 `wta-workflow/controller` | `FlwCategoryController.java` | 2026-09-17 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-workflow` = classic；不按 layered 口述 | 登记表 classic 行 | 2026-09-17 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/30-cde-workflow.sql` | 表 `flow_category`；种子根 `1762300000000000100`；菜单 C/F 含 export | `create table flow_category`；`sys_menu` 176140…1622–1627 | 2026-09-17 |
| S-L067-01 | `FlwCategoryController.java` | 七扇映射；权限字；`R.fail`/`R.warn`；`categoryTree` 无 perm；`RepeatSubmit` 仅 add/edit | 类上 `/workflow/category` 及各方法 | 2026-09-17 |
| S-L067-02 | `IFlwCategoryService.java`；`FlwCategoryServiceImpl.java` | classic 厨师；父存在/顶级父/ancestors；`DefService.exists`；`queryCategory` 侧门；缓存注解 | insert/update/delete/tree/unique | 2026-09-17 |
| S-L067-03 | `FlowCategory.java`；`FlowCategoryBo.java`；`FlowCategoryVo.java`；`FlwCategoryMapper.java`；`FlwCategoryMapper.xml` | 表名；校验组；译名；空 XML；`findInSet` default | `@TableName`；Add/Edit 组；空 mapper | 2026-09-17 |
| S-L067-04 | `FlowConstant.java`；`ConditionalOnEnable.java`；`application.yml` `warm-flow` | `100L`；缓存名 `#30d`；电闸 property；默认 enabled true | 常量与 yml 408–423 行附近 | 2026-09-17 |
| S-L067-05 | `CategoryNameTranslationImpl.java`；`TreeBuildUtils.java`；`R.java`；`HttpStatus.java`；`BaseController.toAjax`；`RepeatSubmit` | 译名；`label` nameKey；601；rows>0；5s | 各类型字段/方法 | 2026-09-17 |
| S-L067-06 | 同目录 `FlwDefinitionController` / `FlwInstanceController` / `FlwTaskController` / `FlwSpelController` / `TestLeaveController` | 六份都 `@ConditionalOnEnable`；定义/实例另抱引擎；SpEL 才批量 ids | 各类 `@RequestMapping` 与字段 | 2026-09-17 |
| S-L067-07 | `frontend/packages/domains/workflow/src/{index.ts,index.test.ts,category/index.ts}` | 六枪 URL；无 export；`deleteCategory` 逗号拼接；资源标签 | `createWorkflowDefinitionService`；测试 `/1,2` | 2026-09-17 |
| S-L067-08 | `web-domains/workflow/src/category/CategoryPage.vue`；`definition/DefinitionPage.vue`；`index.ts` permissions | 管理页 list+handleTree；定义页 categoryTree+label；权限组无 export | 页内 `getList`/`getTreeselect`；permissions `workflow-category` | 2026-09-17 |
| S-L067-09 | `frontend/e2e/workflow-definition.spec.ts`；`adapters/axios-browser/src/index.ts` | 新增 mutation；失败不假成功；list-only 藏新增；601=warning reject | e2e 三例；axios `code === 601` | 2026-09-17 |
